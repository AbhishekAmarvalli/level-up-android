package com.levelup.app.data

import android.content.Context
import androidx.room.withTransaction
import com.levelup.app.domain.ActivityModel
import com.levelup.app.domain.ActivityRange
import com.levelup.app.domain.DateText
import com.levelup.app.domain.DeadlineType
import com.levelup.app.domain.Modes
import com.levelup.app.domain.NextLesson
import com.levelup.app.domain.TaskState
import com.levelup.app.domain.TierProgress
import com.levelup.app.domain.UpcomingDeadline
import com.levelup.app.domain.UploadState
import com.levelup.app.domain.activityModel
import com.levelup.app.domain.computeModes
import com.levelup.app.domain.minimumComplete
import com.levelup.app.domain.nextLesson
import com.levelup.app.domain.streakDots
import com.levelup.app.domain.stateEnum
import com.levelup.app.domain.streakOf
import com.levelup.app.domain.toDateMap
import com.levelup.app.domain.visibleTasks
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Everything a screen needs, so screens combine flows only once. */
data class CoreData(
    val tasks: List<TaskState>,
    val phases: List<PhaseWithLessons>,
    val deadlines: List<DeadlineEntity>,
    val records: Map<LocalDate, DayRecordEntity>,
    val settings: AppSettings,
    val today: LocalDate,
)

data class WidgetSnapshot(
    val visibleTasks: List<TaskState>,
    val nextLesson: NextLesson?,
    val streak: Int,
    val dots: List<Boolean>,
    val upcoming: List<UpcomingDeadline>,
    val activity: ActivityModel,
    val range: ActivityRange,
    val minComplete: Boolean,
    val tierProgress: List<TierProgress>,
    val today: LocalDate,
    val examMode: Boolean,
)

class LevelUpRepository(
    private val db: LevelUpDatabase,
    val settings: SettingsStore,
    private val appContext: Context,
    private val onDataChanged: suspend () -> Unit,
) {
    private val taskDao = db.taskDao()
    private val taskLogDao = db.taskLogDao()
    private val phaseDao = db.phaseDao()
    private val lessonDao = db.lessonDao()
    private val deadlineDao = db.deadlineDao()
    private val dayRecordDao = db.dayRecordDao()
    private val clipDao = db.clipDao()

    private val seedMutex = Mutex()

    /** Updated on resume and by the midnight worker; flows follow this date. */
    val todayFlow = MutableStateFlow(LocalDate.now())

    fun refreshToday() {
        todayFlow.value = LocalDate.now()
    }

    private suspend fun notifyChange() {
        runCatching { onDataChanged() }
    }

    // ------------------------------------------------------------ seeding and rollover

    suspend fun ensureSeeded() {
        seedMutex.withLock {
            val state = settings.current()
            if (state.seeded) {
                ensureDay(todayFlow.value)
                return
            }
            db.withTransaction {
                if (taskDao.count() == 0) seedTasks()
                if (phaseDao.count() == 0) seedRoadmap()
                if (deadlineDao.getAll().isEmpty()) seedDeadlines()
            }
            settings.setSeeded()
            ensureDay(LocalDate.now())
        }
    }

    private suspend fun seedTasks() {
        val seeds = listOf(
            Triple("20 min AI learning", 0, true),
            Triple("College study", 0, false),
            Triple("Speaking practice 10 min", 0, false),
            Triple("1 lesson + exercise", 1, true),
            Triple("Workout", 1, false),
            Triple("Project time", 2, true),
        )
        val counters = mutableMapOf<Int, Int>()
        seeds.forEach { (title, tier, ai) ->
            val order = counters[tier] ?: 0
            counters[tier] = order + 1
            taskDao.insert(
                TaskEntity(
                    title = title,
                    tier = tier,
                    isAiTagged = ai,
                    sortOrder = order,
                )
            )
        }
    }

    private suspend fun seedRoadmap() {
        val phases = listOf(
            "Foundations" to listOf(
                "Python basics", "Functions and classes", "APIs and JSON", "Git basics",
            ),
            "LLM basics" to listOf(
                "How LLMs work", "Prompting", "Calling an LLM API", "Project 1: chatbot",
            ),
            "RAG" to listOf(
                "Embeddings", "Vector search", "Chunking", "Project 2: RAG app",
            ),
            "Agents" to listOf(
                "Tool use", "Multi-step agents", "Project 3: agent",
            ),
            "Shipping" to listOf(
                "FastAPI", "Docker", "Cloud deploy", "Evaluating outputs",
            ),
            "ML and PyTorch" to listOf(
                "PyTorch basics", "Train a small model",
            ),
            "Portfolio" to listOf(
                "GitHub and READMEs", "2-minute project demos",
            ),
        )
        phases.forEachIndexed { phaseIndex, (name, lessons) ->
            val phaseId = phaseDao.insert(PhaseEntity(name = name, sortOrder = phaseIndex))
            lessons.forEachIndexed { lessonIndex, title ->
                lessonDao.insert(
                    LessonEntity(
                        phaseId = phaseId,
                        title = title,
                        done = false,
                        sortOrder = lessonIndex,
                    )
                )
            }
        }
    }

    /** Deadlines are relative to the install day, never hardcoded dates. */
    private suspend fun seedDeadlines() {
        val today = LocalDate.now()
        val seeds = listOf(
            Triple("DSA exam", today.plusDays(4), DeadlineType.EXAM),
            Triple("Assignment 2", today.plusDays(9), DeadlineType.ASSIGNMENT),
            Triple("RAG project demo", today.plusDays(21), DeadlineType.PROJECT),
        )
        seeds.forEach { (title, date, type) ->
            deadlineDao.insert(
                DeadlineEntity(title = title, date = date.toString(), type = type.name)
            )
        }
    }

    /** Creates the new day's task logs and an empty DayRecord once per date. */
    suspend fun ensureDay(date: LocalDate = todayFlow.value) {
        val dateString = date.toString()
        db.withTransaction {
            if (dayRecordDao.get(dateString) == null) {
                dayRecordDao.upsert(
                    DayRecordEntity(dateString, minimumCompleted = false, tasksDone = 0, tasksTotal = 0)
                )
            }
            val existing = taskLogDao.getForDate(dateString).map { it.taskId }.toSet()
            val missing = taskDao.getActive()
                .filter { it.id !in existing }
                .map { TaskLogEntity(dateString, it.id, done = false, skipped = false) }
            if (missing.isNotEmpty()) taskLogDao.insertIgnore(missing)
        }
        recomputeDayRecord(date)
    }

    // ------------------------------------------------------------ flows

    fun observeTasks(date: LocalDate): Flow<List<TaskState>> =
        combine(taskDao.observeActive(), taskLogDao.observe(date.toString())) { tasks, logs ->
            val byId = logs.associateBy { it.taskId }
            tasks.map { task ->
                TaskState(
                    task = task,
                    done = byId[task.id]?.done ?: false,
                    skipped = byId[task.id]?.skipped ?: false,
                )
            }
        }

    fun observePhases(): Flow<List<PhaseWithLessons>> =
        phaseDao.observeAll().map { phases ->
            phases.map { relation ->
                PhaseWithLessons(
                    phase = relation.phase,
                    lessons = relation.lessons.sortedWith(compareBy({ it.sortOrder }, { it.id })),
                )
            }.sortedWith(compareBy({ it.phase.sortOrder }, { it.phase.id }))
        }

    fun observeDeadlines(): Flow<List<DeadlineEntity>> = deadlineDao.observeAll()

    fun observeRecords(): Flow<Map<LocalDate, DayRecordEntity>> =
        dayRecordDao.observeAll().map { list -> list.associateBy { it.date }.toDateMap() }

    fun observeClips(): Flow<List<ClipEntity>> = clipDao.observeAll()

    /**
     * One combined stream for the whole app. Settings and day records are included
     * so every mutation is reflected everywhere at once.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun core(): Flow<CoreData> = todayFlow.flatMapLatest { date ->
        combine(
            observeTasks(date),
            observePhases(),
            observeDeadlines(),
            observeRecords(),
            settings.flow,
        ) { tasks, phases, deadlines, records, pref ->
            CoreData(
                tasks = tasks,
                phases = phases,
                deadlines = deadlines,
                records = records,
                settings = pref,
                today = date,
            )
        }
    }

    suspend fun currentModes(date: LocalDate = todayFlow.value): Modes =
        computeModes(settings.current(), deadlineDao.getAll(), date)

    // ------------------------------------------------------------ tasks

    suspend fun toggleTask(taskId: Long, date: LocalDate = todayFlow.value): Boolean {
        val log = taskLogDao.getForDate(date.toString()).firstOrNull { it.taskId == taskId }
        return setTaskDone(taskId, date, !(log?.done ?: false))
    }

    /** Returns true only when this toggle completes the Minimum for the first time today. */
    suspend fun setTaskDone(taskId: Long, date: LocalDate, done: Boolean): Boolean {
        val wasComplete = dayRecordDao.get(date.toString())?.minimumCompleted == true
        taskLogDao.upsert(TaskLogEntity(date.toString(), taskId, done = done, skipped = false))
        recomputeDayRecord(date)
        val nowComplete = dayRecordDao.get(date.toString())?.minimumCompleted == true
        notifyChange()
        return !wasComplete && nowComplete
    }

    suspend fun skipTask(taskId: Long, date: LocalDate = todayFlow.value) {
        taskLogDao.upsert(TaskLogEntity(date.toString(), taskId, done = false, skipped = true))
        recomputeDayRecord(date)
        notifyChange()
    }

    suspend fun unskipTask(taskId: Long, date: LocalDate = todayFlow.value) {
        val log = taskLogDao.getForDate(date.toString()).firstOrNull { it.taskId == taskId }
        taskLogDao.upsert(
            (log ?: TaskLogEntity(date.toString(), taskId, done = false)).copy(skipped = false)
        )
        recomputeDayRecord(date)
        notifyChange()
    }

    suspend fun addTask(title: String, tier: Int, aiTagged: Boolean): Long {
        val order = taskDao.maxOrder(tier) + 1
        val id = taskDao.insert(
            TaskEntity(
                title = title.trim(),
                tier = tier,
                isAiTagged = aiTagged,
                sortOrder = order,
            )
        )
        ensureDay(todayFlow.value)
        notifyChange()
        return id
    }

    suspend fun updateTask(task: TaskEntity) {
        taskDao.update(task)
        recomputeDayRecord(todayFlow.value)
        notifyChange()
    }

    suspend fun deleteTask(taskId: Long) {
        db.withTransaction {
            taskLogDao.deleteForTask(taskId)
            taskDao.delete(taskId)
        }
        recomputeDayRecord(todayFlow.value)
        notifyChange()
    }

    /** Recomputes today's aggregate counts, keeping "Minimum completed" sticky for the day. */
    suspend fun recomputeDayRecord(date: LocalDate) {
        val tasks = taskDao.getActive()
        val logs = taskLogDao.getForDate(date.toString()).associateBy { it.taskId }
        val modes = currentModes(date)
        val states = tasks.map { task ->
            TaskState(task, logs[task.id]?.done ?: false, logs[task.id]?.skipped ?: false)
        }
        val visible = visibleTasks(states, modes).filter { !it.skipped }
        val minList = visible.filter { it.task.tier == 0 }
        val minNow = minList.isNotEmpty() && minList.all { it.done }
        val existing = dayRecordDao.get(date.toString())
        dayRecordDao.upsert(
            DayRecordEntity(
                date = date.toString(),
                minimumCompleted = (existing?.minimumCompleted == true) || minNow,
                tasksDone = visible.count { it.done },
                tasksTotal = visible.size,
            )
        )
    }

    suspend fun setBadDayMode(enabled: Boolean) {
        settings.setBadDayMode(enabled)
        recomputeDayRecord(todayFlow.value)
        notifyChange()
    }

    suspend fun setExamModeOverride(value: Boolean?) {
        settings.setExamModeOverride(value)
        recomputeDayRecord(todayFlow.value)
        notifyChange()
    }

    // ------------------------------------------------------------ roadmap

    suspend fun markNextLessonDone(): Boolean {
        val next = nextLesson(phaseDao.getAll()) ?: return false
        lessonDao.setDone(next.lesson.id, true)
        notifyChange()
        return true
    }

    suspend fun skipNextLesson() {
        val next = nextLesson(phaseDao.getAll()) ?: return
        val order = lessonDao.maxOrder(next.phase.phase.id) + 1
        lessonDao.update(next.lesson.copy(sortOrder = order))
        notifyChange()
    }

    suspend fun setLessonDone(id: Long, done: Boolean) {
        lessonDao.setDone(id, done)
        notifyChange()
    }

    suspend fun addPhase(name: String) {
        val order = phaseDao.maxOrder() + 1
        phaseDao.insert(PhaseEntity(name = name.trim(), sortOrder = order))
        notifyChange()
    }

    suspend fun renamePhase(id: Long, name: String) {
        val phase = phaseDao.getAll().firstOrNull { it.phase.id == id }?.phase ?: return
        phaseDao.update(phase.copy(name = name.trim()))
        notifyChange()
    }

    suspend fun movePhase(id: Long, up: Boolean) {
        val phases = phaseDao.getAll().map { it.phase }.sortedWith(compareBy({ it.sortOrder }, { it.id }))
        val index = phases.indexOfFirst { it.id == id }
        val target = if (up) index - 1 else index + 1
        if (index < 0 || target !in phases.indices) return
        db.withTransaction {
            val a = phases[index]
            val b = phases[target]
            phaseDao.update(a.copy(sortOrder = target))
            phaseDao.update(b.copy(sortOrder = index))
        }
        notifyChange()
    }

    suspend fun deletePhase(id: Long) {
        phaseDao.delete(id)
        notifyChange()
    }

    suspend fun addLesson(phaseId: Long, title: String, url: String?) {
        val order = lessonDao.maxOrder(phaseId) + 1
        lessonDao.insert(
            LessonEntity(
                phaseId = phaseId,
                title = title.trim(),
                sortOrder = order,
                url = url?.trim()?.takeIf { it.isNotBlank() },
            )
        )
        notifyChange()
    }

    suspend fun updateLesson(id: Long, title: String, url: String?) {
        val lesson = lessonDao.get(id) ?: return
        lessonDao.update(
            lesson.copy(
                title = title.trim(),
                url = url?.trim()?.takeIf { it.isNotBlank() },
            )
        )
        notifyChange()
    }

    suspend fun setLessonUrl(id: Long, url: String?) {
        val lesson = lessonDao.get(id) ?: return
        lessonDao.update(lesson.copy(url = url?.trim()?.takeIf { it.isNotBlank() }))
        notifyChange()
    }

    suspend fun moveLesson(id: Long, up: Boolean) {
        val lesson = lessonDao.get(id) ?: return
        val siblings = lessonDao.getForPhase(lesson.phaseId)
        val index = siblings.indexOfFirst { it.id == id }
        val target = if (up) index - 1 else index + 1
        if (index < 0 || target !in siblings.indices) return
        db.withTransaction {
            val a = siblings[index]
            val b = siblings[target]
            lessonDao.update(a.copy(sortOrder = target))
            lessonDao.update(b.copy(sortOrder = index))
        }
        notifyChange()
    }

    suspend fun deleteLesson(id: Long) {
        lessonDao.delete(id)
        notifyChange()
    }

    // ------------------------------------------------------------ deadlines

    suspend fun addDeadline(title: String, date: LocalDate, type: DeadlineType): Long {
        val id = deadlineDao.insert(
            DeadlineEntity(title = title.trim(), date = date.toString(), type = type.name)
        )
        notifyChange()
        return id
    }

    suspend fun updateDeadline(id: Long, title: String, date: LocalDate, type: DeadlineType) {
        val existing = deadlineDao.get(id) ?: return
        deadlineDao.update(
            existing.copy(title = title.trim(), date = date.toString(), type = type.name)
        )
        notifyChange()
    }

    suspend fun setDeadlineDone(id: Long, done: Boolean) {
        val existing = deadlineDao.get(id) ?: return
        deadlineDao.update(
            existing.copy(
                done = done,
                doneDate = if (done) LocalDate.now().toString() else null,
            )
        )
        notifyChange()
    }

    suspend fun deleteDeadline(id: Long) {
        deadlineDao.delete(id)
        notifyChange()
    }

    // ------------------------------------------------------------ clips

    fun newClipFile(): File {
        val dir = File(appContext.filesDir, "clips")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "clip_${System.currentTimeMillis()}.mp4")
    }

    suspend fun addClip(file: File, durationSeconds: Int): ClipEntity {
        val state = if (settings.current().backupEnabled) UploadState.QUEUED else UploadState.LOCAL_ONLY
        val clip = ClipEntity(
            filePath = file.absolutePath,
            createdAt = System.currentTimeMillis(),
            durationSeconds = durationSeconds.coerceAtLeast(1),
            uploadState = state.name,
        )
        val id = clipDao.insert(clip)
        notifyChange()
        return clip.copy(id = id)
    }

    suspend fun setClipState(id: Long, state: UploadState, driveFileId: String? = null) {
        clipDao.updateState(id, state.name, driveFileId)
        notifyChange()
    }

    suspend fun updateClipStates(from: List<UploadState>, to: UploadState) {
        clipDao.updateStateWhere(from.map { it.name }, to.name)
        notifyChange()
    }

    suspend fun getClip(id: Long): ClipEntity? = clipDao.get(id)

    suspend fun deleteClip(id: Long) {
        val clip = clipDao.get(id)
        clipDao.delete(id)
        clip?.let { runCatching { File(it.filePath).delete() } }
        notifyChange()
    }

    // ------------------------------------------------------------ widgets

    suspend fun widgetSnapshot(): WidgetSnapshot {
        ensureSeeded()
        val today = LocalDate.now()
        val pref = settings.current()
        val modes = computeModes(pref, deadlineDao.getAll(), today)
        val tasks = taskDao.getActive()
        val logs = taskLogDao.getForDate(today.toString()).associateBy { it.taskId }
        val states = tasks.map { task ->
            TaskState(task, logs[task.id]?.done ?: false, logs[task.id]?.skipped ?: false)
        }
        val visible = visibleTasks(states, modes).filter { !it.skipped }
        val phases = phaseDao.getAll().map { relation ->
            PhaseWithLessons(
                phase = relation.phase,
                lessons = relation.lessons.sortedWith(compareBy({ it.sortOrder }, { it.id })),
            )
        }.sortedWith(compareBy({ it.phase.sortOrder }, { it.phase.id }))
        val deadlines = deadlineDao.getAll().filter { !it.done }
        val upcoming = deadlines
            .mapNotNull { deadline ->
                DateText.parse(deadline.date)?.let { date ->
                    UpcomingDeadline(
                        title = deadline.title,
                        dateText = DateText.header(date),
                        date = date,
                        daysLeft = ChronoUnit.DAYS.between(today, date).coerceAtLeast(0),
                    )
                }
            }
            .sortedBy { it.date }
        val records = dayRecordDao.getAll().associateBy { it.date }.toDateMap()
        val range = if (pref.widgetRange == "4w") ActivityRange.MONTH else ActivityRange.WEEK
        val tierProgress = (0..2).mapNotNull { tier ->
            val group = visible.filter { it.task.tier == tier }
            if (group.isEmpty()) null
            else TierProgress(tier, group.count { it.done }.toFloat() / group.size.toFloat())
        }
        return WidgetSnapshot(
            visibleTasks = visible,
            nextLesson = nextLesson(phases),
            streak = streakOf(records, today),
            dots = streakDots(records, today),
            upcoming = upcoming,
            activity = activityModel(records, today, range),
            range = range,
            minComplete = minimumComplete(visible),
            tierProgress = tierProgress,
            today = today,
            examMode = modes.examMode,
        )
    }

    suspend fun toggleWidgetActivityRange() {
        val current = settings.current().widgetRange
        settings.setWidgetRange(if (current == "4w") "7d" else "4w")
        notifyChange()
    }

    suspend fun clipsForUpload(): List<ClipEntity> = clipDao.getAll().filter {
        it.stateEnum() != UploadState.UPLOADED && it.stateEnum() != UploadState.LOCAL_ONLY
    }

    suspend fun clipsSnapshot(): List<ClipEntity> = clipDao.getAll()

    suspend fun allDeadlines(): List<DeadlineEntity> = deadlineDao.getAll()

    suspend fun getDeadline(id: Long): DeadlineEntity? = deadlineDao.get(id)

    /** Used by the daily reminder worker: is today's Minimum done right now? */
    suspend fun minimumDoneToday(): Boolean {
        val today = LocalDate.now()
        val modes = currentModes(today)
        val logs = taskLogDao.getForDate(today.toString()).associateBy { it.taskId }
        val states = taskDao.getActive().map { task ->
            TaskState(task, logs[task.id]?.done ?: false, logs[task.id]?.skipped ?: false)
        }
        val visible = visibleTasks(states, modes).filter { !it.skipped }
        return minimumComplete(visible)
    }
}

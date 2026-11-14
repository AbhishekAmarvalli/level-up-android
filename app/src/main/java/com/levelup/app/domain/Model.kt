package com.levelup.app.domain

import com.levelup.app.data.AppSettings
import com.levelup.app.data.ClipEntity
import com.levelup.app.data.DayRecordEntity
import com.levelup.app.data.DeadlineEntity
import com.levelup.app.data.LessonEntity
import com.levelup.app.data.PhaseWithLessons
import com.levelup.app.data.TaskEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

// ---------------------------------------------------------------- enums

enum class UploadNetworkMode { WIFI_ONLY, WIFI_AND_MOBILE, ASK_BEFORE_MOBILE }

enum class UploadState {
    LOCAL_ONLY, QUEUED, WAITING_FOR_WIFI, UPLOADING, UPLOADED, PAUSED_STORAGE, FAILED
}

enum class DeadlineType { EXAM, ASSIGNMENT, PROJECT, OTHER }

enum class ActivityRange { WEEK, MONTH }

// ---------------------------------------------------------------- small models

data class TaskState(
    val task: TaskEntity,
    val done: Boolean,
    val skipped: Boolean = false,
)

data class NextLesson(val phase: PhaseWithLessons, val lesson: LessonEntity)

data class Modes(
    val badDay: Boolean,
    val examMode: Boolean,
    /** True when an exam is inside the configured threshold, regardless of the override. */
    val examAuto: Boolean,
    val examDays: Int?,
)

data class ActivityModel(
    val labels: List<String>,
    val values: List<Int>,
    val average: Int,
    val range: ActivityRange,
)

data class TierProgress(val tier: Int, val ratio: Float)

data class UpcomingDeadline(
    val title: String,
    val dateText: String,
    val date: LocalDate,
    val daysLeft: Long,
)

// ---------------------------------------------------------------- date helpers

object DateText {
    private val header = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH)
    private val monthYear = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
    private val short = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

    /** "Sat, 14 Nov" for the Today header and deadline rows. */
    fun header(date: LocalDate): String = date.format(header)

    /** "November 2026" for the calendar card. */
    fun monthYear(date: LocalDate): String = date.format(monthYear)

    /** "6 Nov" for completion dates. */
    fun short(date: LocalDate): String = date.format(short)

    fun weekdayLetter(date: LocalDate): String = when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> "M"
        DayOfWeek.TUESDAY -> "T"
        DayOfWeek.WEDNESDAY -> "W"
        DayOfWeek.THURSDAY -> "T"
        DayOfWeek.FRIDAY -> "F"
        DayOfWeek.SATURDAY -> "S"
        DayOfWeek.SUNDAY -> "S"
    }

    fun parse(value: String): LocalDate? = runCatching { LocalDate.parse(value) }.getOrNull()
}

fun DeadlineEntity.typeEnum(): DeadlineType =
    runCatching { DeadlineType.valueOf(type) }.getOrDefault(DeadlineType.OTHER)

fun ClipEntity.stateEnum(): UploadState =
    runCatching { UploadState.valueOf(uploadState) }.getOrDefault(UploadState.LOCAL_ONLY)

fun DeadlineEntity.daysLeft(today: LocalDate): Long? =
    DateText.parse(date)?.let { ChronoUnit.DAYS.between(today, it) }

// ---------------------------------------------------------------- prototype logic

/**
 * `vis()` from the prototype:
 *  - Bad day mode hides Normal and Bonus.
 *  - Exam mode hides AI-tagged tasks above Minimum.
 */
fun visibleTasks(tasks: List<TaskState>, modes: Modes): List<TaskState> =
    tasks.filter { state ->
        when {
            !state.task.isActive -> false
            modes.badDay && state.task.tier > 0 -> false
            modes.examMode && state.task.isAiTagged && state.task.tier > 0 -> false
            else -> true
        }
    }

/** `tname()` from the prototype: exam mode only changes the display title. */
fun displayTitle(task: TaskEntity, examMode: Boolean): String =
    if (examMode && task.title.equals("20 min AI learning", ignoreCase = true)) {
        "10 min AI learning"
    } else {
        task.title
    }

/** `nextL()`: first not-done lesson in phase order. */
fun nextLesson(phases: List<PhaseWithLessons>): NextLesson? {
    val ordered = phases.sortedWith(compareBy({ it.phase.sortOrder }, { it.phase.id }))
    for (phase in ordered) {
        val lessons = phase.lessons.sortedWith(compareBy({ it.sortOrder }, { it.id }))
        val first = lessons.firstOrNull { !it.done } ?: continue
        return NextLesson(phase, first)
    }
    return null
}

/** Exam mode is automatic when an undone exam is inside the threshold. */
fun computeModes(
    settings: AppSettings,
    deadlines: List<DeadlineEntity>,
    today: LocalDate,
): Modes {
    val nearest = deadlines
        .asSequence()
        .filter { !it.done && it.typeEnum() == DeadlineType.EXAM }
        .mapNotNull { deadline ->
            DateText.parse(deadline.date)?.let { date -> ChronoUnit.DAYS.between(today, date) }
        }
        .filter { it >= 0 && it <= settings.examAutoThresholdDays }
        .minOrNull()
    val auto = nearest != null
    val exam = when (settings.examModeOverride) {
        true -> true
        false -> false
        null -> auto
    }
    return Modes(
        badDay = settings.badDayMode,
        examMode = exam,
        examAuto = auto,
        examDays = nearest?.toInt(),
    )
}

/**
 * A day counts when its Minimum was completed. The streak counts consecutive
 * completed days ending today, or ending yesterday when today is not complete yet.
 */
fun streakOf(records: Map<LocalDate, DayRecordEntity>, today: LocalDate): Int {
    var cursor = if (records[today]?.minimumCompleted == true) today else today.minusDays(1)
    var count = 0
    while (records[cursor]?.minimumCompleted == true) {
        count++
        cursor = cursor.minusDays(1)
        if (count > 3650) break // defensive guard against corrupted data
    }
    return count
}

/** Seven dots for the last seven days, oldest first, today last. */
fun streakDots(records: Map<LocalDate, DayRecordEntity>, today: LocalDate): List<Boolean> =
    (6 downTo 0).map { offset ->
        records[today.minusDays(offset.toLong())]?.minimumCompleted == true
    }

private fun percentFor(record: DayRecordEntity?): Int {
    if (record == null || record.tasksTotal <= 0) return 0
    return (record.tasksDone.toFloat() / record.tasksTotal.toFloat() * 100f)
        .roundToInt()
        .coerceIn(0, 100)
}

/** `actW()` with real DayRecord data instead of the prototype's fake numbers. */
fun activityModel(
    records: Map<LocalDate, DayRecordEntity>,
    today: LocalDate,
    range: ActivityRange,
): ActivityModel = when (range) {
    ActivityRange.WEEK -> {
        val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
        val values = days.map { percentFor(records[it]) }
        ActivityModel(
            labels = days.map { DateText.weekdayLetter(it) },
            values = values,
            average = (values.sum().toFloat() / 7f).roundToInt().coerceIn(0, 100),
            range = range,
        )
    }

    ActivityRange.MONTH -> {
        val weekAverages = (0..3).map { week ->
            val start = today.minusDays((27L - week * 7L))
            val values = (0..6).map { day -> percentFor(records[start.plusDays(day.toLong())]) }
            (values.sum().toFloat() / 7f).roundToInt().coerceIn(0, 100)
        }
        ActivityModel(
            labels = listOf("W1", "W2", "W3", "W4"),
            values = weekAverages,
            average = (weekAverages.sum().toFloat() / 4f).roundToInt().coerceIn(0, 100),
            range = range,
        )
    }
}

fun minimumComplete(visible: List<TaskState>): Boolean {
    val minimum = visible.filter { it.task.tier == 0 && !it.skipped }
    return minimum.isNotEmpty() && minimum.all { it.done }
}

fun Map<String, DayRecordEntity>.toDateMap(): Map<LocalDate, DayRecordEntity> =
    mapNotNull { (key, value) -> DateText.parse(key)?.let { it to value } }.toMap()

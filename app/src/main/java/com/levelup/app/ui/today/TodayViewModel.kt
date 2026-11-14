package com.levelup.app.ui.today

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.levelup.app.LevelUpApp
import com.levelup.app.data.CoreData
import com.levelup.app.data.DayRecordEntity
import com.levelup.app.data.TaskEntity
import com.levelup.app.domain.DateText
import com.levelup.app.domain.Modes
import com.levelup.app.domain.NextLesson
import com.levelup.app.domain.TaskState
import com.levelup.app.domain.computeModes
import com.levelup.app.domain.daysLeft
import com.levelup.app.domain.nextLesson
import com.levelup.app.domain.streakDots
import com.levelup.app.domain.streakOf
import com.levelup.app.domain.visibleTasks
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TodayUiState(
    val today: LocalDate,
    val visibleTasks: List<TaskState>,
    val modes: Modes,
    val nextLesson: NextLesson?,
    val streak: Int,
    val dots: List<Boolean>,
    val nextDeadlineTitle: String?,
    val nextDeadlineDays: Long?,
    val minComplete: Boolean,
    val records: Map<LocalDate, DayRecordEntity>,
)

class TodayViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as LevelUpApp).container.repo

    val uiState = repo.core()
        .map { it.toUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _celebration = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val celebration = _celebration.asSharedFlow()

    private fun CoreData.toUiState(): TodayUiState {
        val modes = computeModes(settings, deadlines, today)
        val visible = visibleTasks(tasks, modes).filter { !it.skipped }
        val upcoming = deadlines
            .filter { !it.done }
            .sortedBy { it.date }
            .firstOrNull()
        return TodayUiState(
            today = today,
            visibleTasks = visible,
            modes = modes,
            nextLesson = nextLesson(phases),
            streak = streakOf(records, today),
            dots = streakDots(records, today),
            nextDeadlineTitle = upcoming?.title,
            nextDeadlineDays = upcoming?.daysLeft(today)?.coerceAtLeast(0),
            minComplete = visible.filter { it.task.tier == 0 }.let { list ->
                list.isNotEmpty() && list.all { it.done }
            },
            records = records,
        )
    }

    fun toggleTask(taskId: Long) {
        val done = uiState.value?.visibleTasks?.firstOrNull { it.task.id == taskId }?.done ?: false
        viewModelScope.launch {
            val newly = repo.setTaskDone(taskId, repo.todayFlow.value, !done)
            if (newly) emitCelebration()
        }
    }

    fun markNextLessonDone() = viewModelScope.launch { repo.markNextLessonDone() }

    fun skipNextLesson() = viewModelScope.launch { repo.skipNextLesson() }

    fun toggleBadDay() = viewModelScope.launch {
        val current = uiState.value?.modes?.badDay ?: false
        repo.setBadDayMode(!current)
    }

    fun toggleExamMode() = viewModelScope.launch {
        val current = uiState.value?.modes?.examMode ?: false
        repo.setExamModeOverride(!current)
    }

    fun skipTask(taskId: Long) = viewModelScope.launch {
        repo.skipTask(taskId, repo.todayFlow.value)
    }

    fun unskipTask(taskId: Long) = viewModelScope.launch {
        repo.unskipTask(taskId, repo.todayFlow.value)
    }

    fun addTask(title: String, tier: Int, aiTagged: Boolean) = viewModelScope.launch {
        repo.addTask(title, tier, aiTagged)
    }

    fun updateTask(task: TaskEntity, title: String, tier: Int, aiTagged: Boolean) =
        viewModelScope.launch {
            repo.updateTask(task.copy(title = title.trim(), tier = tier, isAiTagged = aiTagged))
        }

    fun deleteTask(task: TaskEntity) = viewModelScope.launch { repo.deleteTask(task.id) }

    private suspend fun emitCelebration() {
        val core = repo.core().first()
        val streak = streakOf(core.records, core.today).coerceAtLeast(1)
        _celebration.emit(streak)
    }
}

package com.levelup.app.ui.deadlines

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.levelup.app.LevelUpApp
import com.levelup.app.data.DeadlineEntity
import com.levelup.app.domain.DeadlineType
import com.levelup.app.work.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DeadlinesUiState(
    val today: LocalDate,
    val all: List<DeadlineEntity>,
    val upcoming: List<DeadlineEntity>,
    val done: List<DeadlineEntity>,
)

class DeadlinesViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as LevelUpApp).container.repo

    val uiState = repo.core()
        .map { core ->
            DeadlinesUiState(
                today = core.today,
                all = core.deadlines,
                upcoming = core.deadlines
                    .filter { !it.done }
                    .sortedWith(compareBy({ it.date }, { it.id })),
                done = core.deadlines
                    .filter { it.done }
                    .sortedWith(
                        compareByDescending<DeadlineEntity> { it.doneDate ?: "" }
                            .thenByDescending { it.date }
                    ),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun add(title: String, date: LocalDate, type: DeadlineType) = viewModelScope.launch {
        repo.addDeadline(title, date, type)
        ReminderScheduler.rescheduleDeadlines(getApplication(), repo)
    }

    fun update(deadline: DeadlineEntity, title: String, date: LocalDate, type: DeadlineType) =
        viewModelScope.launch {
            repo.updateDeadline(deadline.id, title, date, type)
            ReminderScheduler.rescheduleDeadlines(getApplication(), repo)
        }

    fun setDone(deadline: DeadlineEntity, done: Boolean) = viewModelScope.launch {
        repo.setDeadlineDone(deadline.id, done)
        ReminderScheduler.rescheduleDeadlines(getApplication(), repo)
    }

    fun delete(deadline: DeadlineEntity) = viewModelScope.launch {
        repo.deleteDeadline(deadline.id)
        ReminderScheduler.rescheduleDeadlines(getApplication(), repo)
    }
}

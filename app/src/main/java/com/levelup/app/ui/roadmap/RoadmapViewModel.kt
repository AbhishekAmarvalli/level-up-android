package com.levelup.app.ui.roadmap

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.levelup.app.LevelUpApp
import com.levelup.app.data.LessonEntity
import com.levelup.app.data.PhaseEntity
import com.levelup.app.data.PhaseWithLessons
import com.levelup.app.domain.NextLesson
import com.levelup.app.domain.nextLesson
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class RoadmapUiState(
    val today: LocalDate,
    val phases: List<PhaseWithLessons>,
    val next: NextLesson?,
)

class RoadmapViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as LevelUpApp).container.repo

    val uiState = repo.core()
        .map { core ->
            RoadmapUiState(
                today = core.today,
                phases = core.phases,
                next = nextLesson(core.phases),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun markNextDone() = viewModelScope.launch { repo.markNextLessonDone() }

    fun skipNext() = viewModelScope.launch { repo.skipNextLesson() }

    fun setLessonDone(lesson: LessonEntity, done: Boolean) = viewModelScope.launch {
        repo.setLessonDone(lesson.id, done)
    }

    fun addPhase(name: String) = viewModelScope.launch { repo.addPhase(name) }

    fun renamePhase(phase: PhaseEntity, name: String) = viewModelScope.launch {
        repo.renamePhase(phase.id, name)
    }

    fun movePhase(phase: PhaseEntity, up: Boolean) = viewModelScope.launch {
        repo.movePhase(phase.id, up)
    }

    fun deletePhase(phase: PhaseEntity) = viewModelScope.launch { repo.deletePhase(phase.id) }

    fun addLesson(phaseId: Long, title: String, url: String?) = viewModelScope.launch {
        repo.addLesson(phaseId, title, url)
    }

    fun updateLesson(lesson: LessonEntity, title: String, url: String?) = viewModelScope.launch {
        repo.updateLesson(lesson.id, title, url)
    }

    fun moveLesson(lesson: LessonEntity, up: Boolean) = viewModelScope.launch {
        repo.moveLesson(lesson.id, up)
    }

    fun deleteLesson(lesson: LessonEntity) = viewModelScope.launch { repo.deleteLesson(lesson.id) }
}

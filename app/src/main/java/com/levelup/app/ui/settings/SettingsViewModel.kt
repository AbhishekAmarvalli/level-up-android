package com.levelup.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.levelup.app.LevelUpApp
import com.levelup.app.data.AppSettings
import com.levelup.app.work.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(val settings: AppSettings)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as LevelUpApp).container.repo
    private val settings = (application as LevelUpApp).container.settings

    val uiState = settings.flow
        .map { SettingsUiState(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setDailyReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settings.setDailyReminderEnabled(enabled)
            ReminderScheduler.scheduleDaily(getApplication())
        }
    }

    fun setReminderTime(minutesOfDay: Int) {
        viewModelScope.launch {
            settings.setDailyReminderMinutes(minutesOfDay)
            ReminderScheduler.scheduleDaily(getApplication())
        }
    }

    fun setExamThreshold(days: Int) {
        viewModelScope.launch { settings.setExamThresholdDays(days) }
    }

    fun resetExamOverride() {
        viewModelScope.launch {
            settings.setExamModeOverride(null)
            repo.recomputeDayRecord(repo.todayFlow.value)
        }
    }

    fun setMonthlyCap(megabytes: Int?) {
        viewModelScope.launch { settings.setMonthlyCapMb(megabytes) }
    }

    fun savePrompt(index: Int?, text: String) {
        val current = uiState.value?.settings?.prompts ?: return
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val list = current.toMutableList()
        if (index == null || index !in list.indices) list.add(trimmed) else list[index] = trimmed
        viewModelScope.launch { settings.setPrompts(list) }
    }

    fun deletePrompt(index: Int) {
        val current = uiState.value?.settings?.prompts ?: return
        if (current.size <= 1) return
        val list = current.toMutableList()
        if (index in list.indices) list.removeAt(index)
        viewModelScope.launch { settings.setPrompts(list) }
    }
}

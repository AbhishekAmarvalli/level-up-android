package com.levelup.app.ui.speaking

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.levelup.app.LevelUpApp
import com.levelup.app.R
import com.levelup.app.data.AppSettings
import com.levelup.app.data.ClipEntity
import com.levelup.app.drive.DriveQuota
import com.levelup.app.domain.UploadNetworkMode
import com.levelup.app.domain.UploadState
import com.levelup.app.domain.stateEnum
import com.levelup.app.work.UploadScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

data class SpeakingUiState(
    val today: LocalDate,
    val settings: AppSettings,
    val prompt: String,
    val clips: List<ClipEntity>,
    val quota: DriveQuota?,
    val quotaLoading: Boolean,
    val quotaError: Boolean,
    val mobileBytes: Long,
)

class SpeakingViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as LevelUpApp
    private val repo = app.container.repo
    private val drive = app.container.drive
    private val settings = app.container.settings
    private val context = application.applicationContext

    private val quota = MutableStateFlow<DriveQuota?>(null)
    private val quotaLoading = MutableStateFlow(false)
    private val quotaError = MutableStateFlow(false)
    private val mobileBytes = MutableStateFlow(0L)

    val uiState = combine(
        repo.core(),
        repo.observeClips(),
        quota,
        quotaLoading,
    ) { core, clips, quotaValue, loading ->
        val prompts = core.settings.prompts
        val prompt = if (prompts.isEmpty()) {
            app.getString(R.string.prompt_default)
        } else {
            prompts[core.today.toEpochDay().mod(prompts.size.toLong()).toInt()]
        }
        SpeakingUiState(
            today = core.today,
            settings = core.settings,
            prompt = prompt,
            clips = clips,
            quota = quotaValue,
            quotaLoading = loading,
            quotaError = false,
            mobileBytes = mobileBytes.value,
        )
    }
        .combine(quotaError) { state, error -> state.copy(quotaError = error) }
        .combine(mobileBytes) { state, usage -> state.copy(mobileBytes = usage) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        refreshAll()
    }

    fun newClipFile(): File = repo.newClipFile()

    fun saveClip(file: File, durationSeconds: Int) {
        viewModelScope.launch {
            val clip = repo.addClip(file, durationSeconds)
            val current = settings.current()
            if (current.backupEnabled) {
                UploadScheduler.enqueue(context, clip.id, current.uploadNetworkMode)
            }
        }
    }

    fun enableBackup(email: String) {
        viewModelScope.launch {
            settings.setDriveEmail(email)
            settings.setBackupEnabled(true)
            val mode = settings.current().uploadNetworkMode
            repo.clipsSnapshot()
                .filter { it.stateEnum() != UploadState.UPLOADED }
                .forEach { clip ->
                    repo.setClipState(clip.id, UploadState.QUEUED)
                    UploadScheduler.enqueue(context, clip.id, mode)
                }
            refreshStorage()
        }
    }

    fun disableBackup() {
        viewModelScope.launch {
            settings.setBackupEnabled(false)
            UploadScheduler.cancelAll(context)
            repo.updateClipStates(
                listOf(
                    UploadState.QUEUED,
                    UploadState.WAITING_FOR_WIFI,
                    UploadState.UPLOADING,
                    UploadState.PAUSED_STORAGE,
                    UploadState.FAILED,
                ),
                UploadState.LOCAL_ONLY,
            )
            quota.value = null
            quotaError.value = false
        }
    }

    fun setUploadMode(mode: UploadNetworkMode) {
        viewModelScope.launch {
            settings.setUploadNetworkMode(mode)
            if (!settings.current().backupEnabled) return@launch
            UploadScheduler.cancelAll(context)
            repo.clipsSnapshot()
                .filter { it.stateEnum() != UploadState.UPLOADED }
                .forEach { clip ->
                    repo.setClipState(clip.id, UploadState.QUEUED)
                    UploadScheduler.enqueue(context, clip.id, mode)
                }
        }
    }

    fun setMonthlyCap(megabytes: Int?) {
        viewModelScope.launch { settings.setMonthlyCapMb(megabytes) }
    }

    fun toggleChecklist(item: String) {
        viewModelScope.launch { settings.toggleChecklistItem(item) }
    }

    fun deleteClip(clip: ClipEntity, alsoOnDrive: Boolean) {
        viewModelScope.launch {
            UploadScheduler.cancelClip(context, clip.id)
            if (alsoOnDrive && !clip.driveFileId.isNullOrBlank()) {
                runCatching { drive.deleteFile(clip.driveFileId) }
            }
            repo.deleteClip(clip.id)
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            mobileBytes.value = settings.mobileBytesThisMonth()
            refreshStorageInternal()
        }
    }

    fun refreshStorage() {
        viewModelScope.launch { refreshStorageInternal() }
    }

    private suspend fun refreshStorageInternal() {
        val current = settings.current()
        mobileBytes.value = settings.mobileBytesThisMonth()
        if (!current.backupEnabled) {
            quotaLoading.value = false
            quotaError.value = false
            quota.value = null
            return
        }
        quotaLoading.value = true
        quotaError.value = false
        try {
            val result = drive.storageQuota()
            quota.value = result
            val paused = result.limitBytes != null && result.percent >= 90
            if (paused) {
                repo.updateClipStates(
                    listOf(
                        UploadState.QUEUED,
                        UploadState.WAITING_FOR_WIFI,
                        UploadState.UPLOADING,
                    ),
                    UploadState.PAUSED_STORAGE,
                )
            } else {
                val toResume = repo.clipsSnapshot().filter {
                    it.stateEnum() == UploadState.PAUSED_STORAGE
                }
                if (toResume.isNotEmpty()) {
                    repo.updateClipStates(listOf(UploadState.PAUSED_STORAGE), UploadState.QUEUED)
                    val mode = settings.current().uploadNetworkMode
                    toResume.forEach { clip ->
                        UploadScheduler.enqueue(context, clip.id, mode)
                    }
                }
            }
        } catch (error: Exception) {
            quotaError.value = true
        } finally {
            quotaLoading.value = false
        }
    }
}

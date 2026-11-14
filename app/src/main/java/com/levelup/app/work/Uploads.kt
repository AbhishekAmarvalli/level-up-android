package com.levelup.app.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.levelup.app.LevelUpApp
import com.levelup.app.domain.UploadNetworkMode
import com.levelup.app.domain.UploadState
import com.levelup.app.domain.stateEnum
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeUnit

object UploadScheduler {
    const val TAG = "clip-upload"

    fun enqueue(
        context: Context,
        clipId: Long,
        mode: UploadNetworkMode,
        allowMobile: Boolean = false,
    ) {
        val type = when {
            allowMobile -> NetworkType.CONNECTED
            mode == UploadNetworkMode.WIFI_ONLY -> NetworkType.UNMETERED
            else -> NetworkType.CONNECTED
        }
        enqueueWith(context, clipId, type, allowMobile, "upload_$clipId")
    }

    fun enqueueWifiOnly(context: Context, clipId: Long) {
        enqueueWith(context, clipId, NetworkType.UNMETERED, false, "upload_wifi_$clipId")
    }

    private fun enqueueWith(
        context: Context,
        clipId: Long,
        networkType: NetworkType,
        allowMobile: Boolean,
        uniqueName: String,
    ) {
        val request: OneTimeWorkRequest = OneTimeWorkRequestBuilder<UploadWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(networkType)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .setInputData(
                workDataOf(
                    UploadWorker.KEY_CLIP_ID to clipId,
                    UploadWorker.KEY_ALLOW_MOBILE to allowMobile,
                )
            )
            .addTag(TAG)
            .addTag("clip_$clipId")
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(uniqueName, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(TAG)
    }

    fun cancelClip(context: Context, clipId: Long) {
        WorkManager.getInstance(context).cancelAllWorkByTag("clip_$clipId")
    }

    fun cancelWifiWait(context: Context, clipId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork("upload_wifi_$clipId")
    }
}

private data class NetworkState(
    val wifi: Boolean,
    val roaming: Boolean,
    val dataSaver: Boolean,
)

private object NetworkMonitor {
    fun read(context: Context): NetworkState {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val capabilities = manager?.activeNetwork?.let { manager.getNetworkCapabilities(it) }
        val wifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val roaming = capabilities != null &&
            !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING)
        val saver = manager?.restrictBackgroundStatus ==
            ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED
        return NetworkState(wifi = wifi, roaming = roaming, dataSaver = saver)
    }
}

class UploadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    companion object {
        const val KEY_CLIP_ID = "clip_id"
        const val KEY_ALLOW_MOBILE = "allow_mobile"
        private const val MAX_ATTEMPTS = 5
    }

    override suspend fun doWork(): Result {
        val app = applicationContext as LevelUpApp
        val repo = app.container.repo
        val settingsStore = app.container.settings
        val clipId = inputData.getLong(KEY_CLIP_ID, -1L)
        if (clipId <= 0L) return Result.failure()
        val allowMobile = inputData.getBoolean(KEY_ALLOW_MOBILE, false)

        val clip = repo.getClip(clipId) ?: return Result.success()
        if (clip.stateEnum() == UploadState.UPLOADED) return Result.success()

        val pref = settingsStore.current()
        if (!pref.backupEnabled) {
            repo.setClipState(clipId, UploadState.LOCAL_ONLY)
            return Result.success()
        }

        val file = File(clip.filePath)
        if (!file.exists()) {
            repo.setClipState(clipId, UploadState.FAILED)
            return Result.success()
        }

        val mobileBytes = settingsStore.mobileBytesThisMonth()
        val capBytes = pref.monthlyMobileCapMb?.toLong()?.times(1024L * 1024L)
        val capReached = capBytes != null && mobileBytes >= capBytes
        val mode = if (capReached) UploadNetworkMode.WIFI_ONLY else pref.uploadNetworkMode

        val network = NetworkMonitor.read(applicationContext)
        val mobileAllowed = when (mode) {
            UploadNetworkMode.WIFI_ONLY -> false
            UploadNetworkMode.WIFI_AND_MOBILE -> true
            UploadNetworkMode.ASK_BEFORE_MOBILE -> allowMobile
        }

        if (!network.wifi) {
            val blocked = !mobileAllowed || network.dataSaver || network.roaming
            if (blocked) {
                repo.setClipState(clipId, UploadState.WAITING_FOR_WIFI)
                if (mode == UploadNetworkMode.ASK_BEFORE_MOBILE && !allowMobile && !network.dataSaver) {
                    Notifications.notifyWaitingForWifi(applicationContext, clipId)
                }
                if (!network.roaming) {
                    UploadScheduler.enqueueWifiOnly(applicationContext, clipId)
                }
                return Result.success()
            }
        }

        // Never upload when Drive is at 90% or above.
        try {
            val quota = app.container.drive.storageQuota()
            if (quota.limitBytes != null && quota.percent >= 90) {
                repo.setClipState(clipId, UploadState.PAUSED_STORAGE)
                return Result.success()
            }
        } catch (error: Exception) {
            return if (runAttemptCount < MAX_ATTEMPTS) {
                Result.retry()
            } else {
                repo.setClipState(clipId, UploadState.FAILED)
                Result.failure()
            }
        }

        repo.setClipState(clipId, UploadState.UPLOADING)
        return try {
            val driveId = app.container.drive.upload(file, "Clip ${clip.id}.mp4")
            repo.setClipState(clipId, UploadState.UPLOADED, driveId)
            if (!network.wifi) settingsStore.addMobileBytes(file.length())
            Notifications.cancelWaiting(applicationContext, clipId)
            Result.success()
        } catch (error: Exception) {
            if (runAttemptCount < MAX_ATTEMPTS) {
                repo.setClipState(clipId, UploadState.QUEUED)
                Result.retry()
            } else {
                repo.setClipState(clipId, UploadState.FAILED)
                Result.failure()
            }
        }
    }
}

class UploadActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_UPLOAD_NOW = "com.levelup.app.action.UPLOAD_NOW"
        const val ACTION_WAIT_WIFI = "com.levelup.app.action.WAIT_WIFI"
        const val EXTRA_CLIP_ID = "clip_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val clipId = intent.getLongExtra(EXTRA_CLIP_ID, -1L)
        if (clipId <= 0L) return
        val app = context.applicationContext as LevelUpApp
        when (intent.action) {
            ACTION_UPLOAD_NOW -> app.container.appScope.launch {
                val mode = app.container.settings.current().uploadNetworkMode
                UploadScheduler.cancelWifiWait(context, clipId)
                app.container.repo.setClipState(clipId, UploadState.QUEUED)
                UploadScheduler.enqueue(context, clipId, mode, allowMobile = true)
            }

            ACTION_WAIT_WIFI -> app.container.appScope.launch {
                app.container.repo.setClipState(clipId, UploadState.WAITING_FOR_WIFI)
                UploadScheduler.enqueueWifiOnly(context, clipId)
            }
        }
        Notifications.cancelWaiting(context, clipId)
    }
}

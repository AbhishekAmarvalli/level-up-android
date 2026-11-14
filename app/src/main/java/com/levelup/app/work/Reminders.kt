package com.levelup.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.levelup.app.LevelUpApp
import com.levelup.app.data.LevelUpRepository
import com.levelup.app.domain.DateText
import com.levelup.app.widget.Widgets
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    private const val DAILY_WORK = "daily_reminder"
    private const val MIDNIGHT_WORK = "midnight_rollover"
    private const val DEADLINE_TAG = "deadline_reminder"

    suspend fun scheduleDaily(context: Context) {
        val app = context.applicationContext as LevelUpApp
        val settings = app.container.settings.current()
        val workManager = WorkManager.getInstance(context)
        if (!settings.dailyReminderEnabled) {
            workManager.cancelUniqueWork(DAILY_WORK)
            return
        }
        val request = OneTimeWorkRequestBuilder<DailyReminderWorker>()
            .setInitialDelay(delayUntil(settings.dailyReminderMinutes), TimeUnit.MILLISECONDS)
            .addTag("reminders")
            .build()
        workManager.enqueueUniqueWork(DAILY_WORK, ExistingWorkPolicy.REPLACE, request)
    }

    fun scheduleMidnight(context: Context) {
        val request = OneTimeWorkRequestBuilder<MidnightWorker>()
            .setInitialDelay(delayUntil(0) + 60_000L, TimeUnit.MILLISECONDS)
            .addTag("rollover")
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(MIDNIGHT_WORK, ExistingWorkPolicy.REPLACE, request)
    }

    /** Cancels and recreates every deadline reminder at 9:00 (-3, -1 days) and 8:00 (day). */
    suspend fun rescheduleDeadlines(context: Context, repo: LevelUpRepository) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag(DEADLINE_TAG)
        val now = ZonedDateTime.now()
        repo.allDeadlines().filter { !it.done }.forEach { deadline ->
            val date = DateText.parse(deadline.date) ?: return@forEach
            val targets = listOf(
                0 to date.minusDays(3).atTime(9, 0),
                1 to date.minusDays(1).atTime(9, 0),
                2 to date.atTime(8, 0),
            )
            targets.forEach { (kind, time) ->
                val zoned = time.atZone(ZoneId.systemDefault())
                if (!zoned.isAfter(now)) return@forEach
                val request = OneTimeWorkRequestBuilder<DeadlineReminderWorker>()
                    .setInitialDelay(Duration.between(now, zoned).toMillis(), TimeUnit.MILLISECONDS)
                    .setInputData(
                        workDataOf(
                            DeadlineReminderWorker.KEY_DEADLINE_ID to deadline.id,
                            DeadlineReminderWorker.KEY_KIND to kind,
                        )
                    )
                    .addTag(DEADLINE_TAG)
                    .build()
                workManager.enqueueUniqueWork(
                    "deadline_${deadline.id}_$kind",
                    ExistingWorkPolicy.REPLACE,
                    request,
                )
            }
        }
    }

    /** Delay until the given local time of day, tomorrow if it already passed. */
    private fun delayUntil(minutesOfDay: Int): Long {
        val now = ZonedDateTime.now()
        val target = LocalDate.now()
            .atTime(minutesOfDay / 60, minutesOfDay % 60)
            .atZone(now.zone)
        val next = if (target.isAfter(now)) target else target.plusDays(1)
        return Duration.between(now, next).toMillis().coerceAtLeast(60_000L)
    }
}

class DailyReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as LevelUpApp
        val settings = app.container.settings.current()
        if (settings.dailyReminderEnabled) {
            val minimumDone = app.container.repo.minimumDoneToday()
            if (!minimumDone) Notifications.notifyDailyReminder(applicationContext)
        }
        ReminderScheduler.scheduleDaily(applicationContext)
        return Result.success()
    }
}

class DeadlineReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    companion object {
        const val KEY_DEADLINE_ID = "deadline_id"
        const val KEY_KIND = "deadline_kind"
    }

    override suspend fun doWork(): Result {
        val id = inputData.getLong(KEY_DEADLINE_ID, -1L)
        val kind = inputData.getInt(KEY_KIND, 2)
        if (id <= 0L) return Result.success()
        val app = applicationContext as LevelUpApp
        val deadline = app.container.repo.getDeadline(id) ?: return Result.success()
        if (deadline.done) return Result.success()
        Notifications.notifyDeadline(applicationContext, deadline.title, kind)
        return Result.success()
    }
}

class MidnightWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as LevelUpApp
        val today = LocalDate.now()
        app.container.repo.refreshToday()
        app.container.repo.ensureDay(today)
        Widgets.refreshAll(applicationContext)
        ReminderScheduler.scheduleDaily(applicationContext)
        ReminderScheduler.scheduleMidnight(applicationContext)
        ReminderScheduler.rescheduleDeadlines(applicationContext, app.container.repo)
        return Result.success()
    }
}

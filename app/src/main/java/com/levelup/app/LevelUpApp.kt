package com.levelup.app

import android.app.Application
import android.content.Context
import com.levelup.app.data.LevelUpDatabase
import com.levelup.app.data.LevelUpRepository
import com.levelup.app.data.SettingsStore
import com.levelup.app.drive.DriveClient
import com.levelup.app.work.Notifications
import com.levelup.app.work.ReminderScheduler
import com.levelup.app.widget.Widgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LevelUpApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.createChannels(this)
        container.appScope.launch {
            container.repo.ensureSeeded()
            ReminderScheduler.scheduleDaily(this@LevelUpApp)
            ReminderScheduler.scheduleMidnight(this@LevelUpApp)
            ReminderScheduler.rescheduleDeadlines(this@LevelUpApp, container.repo)
            Widgets.refreshAll(this@LevelUpApp)
        }
    }
}

/** Manual dependency container: one database, one repository, one Drive client. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val settings = SettingsStore(appContext)
    private val database = LevelUpDatabase.build(appContext)
    val drive = DriveClient(appContext)
    val repo = LevelUpRepository(database, settings, appContext) {
        Widgets.refreshAll(appContext)
    }
}

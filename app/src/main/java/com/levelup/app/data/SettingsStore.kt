package com.levelup.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.levelup.app.R
import com.levelup.app.domain.UploadNetworkMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.IsoFields

/** Everything in this class comes from the settings block in the specification. */
data class AppSettings(
    val badDayMode: Boolean = false,
    val examModeOverride: Boolean? = null,
    val examAutoThresholdDays: Int = 7,
    val backupEnabled: Boolean = false,
    val uploadNetworkMode: UploadNetworkMode = UploadNetworkMode.WIFI_ONLY,
    val monthlyMobileCapMb: Int? = null,
    val mobileBytesUsedThisMonth: Long = 0L,
    val dailyReminderEnabled: Boolean = true,
    val dailyReminderMinutes: Int = 20 * 60,
    val prompts: List<String> = emptyList(),
    val checklistItems: Set<String> = emptySet(),
    val widgetRange: String = "7d",
    val driveEmail: String? = null,
    val notificationsAsked: Boolean = false,
    val seeded: Boolean = false,
)

private val Context.levelUpDataStore: DataStore<Preferences> by preferencesDataStore(name = "levelup_settings")

class SettingsStore(context: Context) {
    private val appContext = context.applicationContext
    private val dataStore = appContext.levelUpDataStore

    val flow: Flow<AppSettings> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { it.toSettings() }
        .distinctUntilChanged()

    suspend fun current(): AppSettings = flow.first()

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        dataStore.edit { prefs -> block(prefs) }
    }

    suspend fun setBadDayMode(enabled: Boolean) = edit { it[Keys.BAD_DAY] = enabled }

    suspend fun setExamModeOverride(value: Boolean?) = edit {
        it[Keys.EXAM_OVERRIDE] = when (value) {
            true -> "on"
            false -> "off"
            null -> "auto"
        }
    }

    suspend fun setExamThresholdDays(days: Int) = edit {
        it[Keys.EXAM_THRESHOLD] = days.coerceIn(1, 30)
    }

    suspend fun setBackupEnabled(enabled: Boolean) = edit { it[Keys.BACKUP] = enabled }

    suspend fun setDriveEmail(email: String?) = edit {
        if (email == null) it.remove(Keys.DRIVE_EMAIL) else it[Keys.DRIVE_EMAIL] = email
    }

    suspend fun setUploadNetworkMode(mode: UploadNetworkMode) = edit {
        it[Keys.UPLOAD_MODE] = mode.name
    }

    suspend fun setMonthlyCapMb(mb: Int?) = edit {
        if (mb == null) it.remove(Keys.MONTHLY_CAP) else it[Keys.MONTHLY_CAP] = mb.coerceAtLeast(1)
    }

    suspend fun setDailyReminderEnabled(enabled: Boolean) = edit {
        it[Keys.DAILY_REMINDER] = enabled
    }

    suspend fun setDailyReminderMinutes(minutes: Int) = edit {
        it[Keys.DAILY_MINUTES] = minutes.coerceIn(0, 24 * 60 - 1)
    }

    suspend fun setPrompts(prompts: List<String>) = edit {
        it[Keys.PROMPTS] = prompts.filter { p -> p.isNotBlank() }.joinToString("\n")
    }

    suspend fun setWidgetRange(range: String) = edit { it[Keys.WIDGET_RANGE] = range }

    suspend fun setNotificationsAsked() = edit { it[Keys.NOTIF_ASKED] = true }

    suspend fun setSeeded() = edit { it[Keys.SEEDED] = true }

    /**
     * Reads the mobile data counter, resetting it when the calendar month changed.
     * The reset happens on the 1st (or on the first use after it), as specified.
     */
    suspend fun mobileBytesThisMonth(): Long {
        val month = YearMonth.now().toString()
        var usage = 0L
        edit { prefs ->
            if (prefs[Keys.MOBILE_MONTH] != month) {
                prefs[Keys.MOBILE_MONTH] = month
                prefs[Keys.MOBILE_USED] = 0L
            }
            usage = prefs[Keys.MOBILE_USED] ?: 0L
        }
        return usage
    }

    suspend fun addMobileBytes(bytes: Long) {
        if (bytes <= 0) return
        val month = YearMonth.now().toString()
        edit { prefs ->
            if (prefs[Keys.MOBILE_MONTH] != month) {
                prefs[Keys.MOBILE_MONTH] = month
                prefs[Keys.MOBILE_USED] = 0L
            }
            prefs[Keys.MOBILE_USED] = (prefs[Keys.MOBILE_USED] ?: 0L) + bytes
        }
    }

    /** Weekly review checklist: selecting an item also clears items from older weeks. */
    suspend fun toggleChecklistItem(item: String) {
        val week = currentWeekKey()
        edit { prefs ->
            val current = if (prefs[Keys.CHECKLIST_WEEK] == week) {
                prefs[Keys.CHECKLIST]?.split(",")?.filter { it.isNotBlank() }?.toMutableSet()
                    ?: mutableSetOf()
            } else {
                mutableSetOf()
            }
            if (!current.add(item)) current.remove(item)
            prefs[Keys.CHECKLIST_WEEK] = week
            prefs[Keys.CHECKLIST] = current.joinToString(",")
        }
    }

    private fun currentWeekKey(): String {
        val date = LocalDate.now()
        val year = date.get(IsoFields.WEEK_BASED_YEAR)
        val week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        return "$year-W$week"
    }

    private fun defaultPrompts(): List<String> = listOf(
        appContext.getString(R.string.prompt_default),
        appContext.getString(R.string.prompt_two),
        appContext.getString(R.string.prompt_three),
        appContext.getString(R.string.prompt_four),
        appContext.getString(R.string.prompt_five),
    )

    private fun Preferences.toSettings(): AppSettings {
        val prompts = this[Keys.PROMPTS]
            ?.split("\n")
            ?.filter { it.isNotBlank() }
            ?.takeIf { it.isNotEmpty() }
            ?: defaultPrompts()
        val week = currentWeekKey()
        val checklist = if (this[Keys.CHECKLIST_WEEK] == week) {
            this[Keys.CHECKLIST]?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
        } else {
            emptySet()
        }
        return AppSettings(
            badDayMode = this[Keys.BAD_DAY] ?: false,
            examModeOverride = when (this[Keys.EXAM_OVERRIDE]) {
                "on" -> true
                "off" -> false
                else -> null
            },
            examAutoThresholdDays = this[Keys.EXAM_THRESHOLD] ?: 7,
            backupEnabled = this[Keys.BACKUP] ?: false,
            uploadNetworkMode = runCatching {
                UploadNetworkMode.valueOf(this[Keys.UPLOAD_MODE] ?: "")
            }.getOrDefault(UploadNetworkMode.WIFI_ONLY),
            monthlyMobileCapMb = this[Keys.MONTHLY_CAP],
            mobileBytesUsedThisMonth = this[Keys.MOBILE_USED] ?: 0L,
            dailyReminderEnabled = this[Keys.DAILY_REMINDER] ?: true,
            dailyReminderMinutes = this[Keys.DAILY_MINUTES] ?: (20 * 60),
            prompts = prompts,
            checklistItems = checklist,
            widgetRange = this[Keys.WIDGET_RANGE] ?: "7d",
            driveEmail = this[Keys.DRIVE_EMAIL],
            notificationsAsked = this[Keys.NOTIF_ASKED] ?: false,
            seeded = this[Keys.SEEDED] ?: false,
        )
    }

    private object Keys {
        val BAD_DAY = booleanPreferencesKey("bad_day_mode")
        val EXAM_OVERRIDE = stringPreferencesKey("exam_override")
        val EXAM_THRESHOLD = intPreferencesKey("exam_threshold_days")
        val BACKUP = booleanPreferencesKey("backup_enabled")
        val UPLOAD_MODE = stringPreferencesKey("upload_network_mode")
        val MONTHLY_CAP = intPreferencesKey("monthly_cap_mb")
        val MOBILE_USED = longPreferencesKey("mobile_bytes_used")
        val MOBILE_MONTH = stringPreferencesKey("mobile_month")
        val DAILY_REMINDER = booleanPreferencesKey("daily_reminder_enabled")
        val DAILY_MINUTES = intPreferencesKey("daily_reminder_minutes")
        val PROMPTS = stringPreferencesKey("prompts")
        val CHECKLIST_WEEK = stringPreferencesKey("checklist_week")
        val CHECKLIST = stringPreferencesKey("checklist")
        val WIDGET_RANGE = stringPreferencesKey("widget_range")
        val DRIVE_EMAIL = stringPreferencesKey("drive_email")
        val NOTIF_ASKED = booleanPreferencesKey("notifications_asked")
        val SEEDED = booleanPreferencesKey("seeded")
    }
}

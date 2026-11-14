package com.levelup.app.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.levelup.app.MainActivity
import com.levelup.app.R

object Notifications {
    const val CHANNEL_REMINDERS = "reminders"
    const val CHANNEL_UPLOADS = "uploads"

    private const val ID_DAILY = 100
    private const val ID_DEADLINE_BASE = 200
    private const val ID_UPLOAD_BASE = 500

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val reminders = NotificationChannel(
            CHANNEL_REMINDERS,
            context.getString(R.string.channel_reminders),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.channel_reminders_desc)
        }
        val uploads = NotificationChannel(
            CHANNEL_UPLOADS,
            context.getString(R.string.channel_uploads),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.channel_uploads_desc)
        }
        manager.createNotificationChannels(listOf(reminders, uploads))
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    private fun openApp(context: Context, tab: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(MainActivity.EXTRA_TAB, tab)
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun base(
        context: Context,
        channel: String,
        title: String,
        text: String,
        contentIntent: PendingIntent,
    ): NotificationCompat.Builder =
        NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setColor(ContextCompat.getColor(context, R.color.lv_accent))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

    fun notifyDailyReminder(context: Context) {
        if (!canNotify(context)) return
        val notification = base(
            context,
            CHANNEL_REMINDERS,
            context.getString(R.string.reminder_title),
            context.getString(R.string.reminder_text),
            openApp(context, "today", 1),
        ).build()
        runCatching {
            NotificationManagerCompat.from(context).notify(ID_DAILY, notification)
        }
    }

    fun notifyDeadline(context: Context, title: String, kind: Int) {
        if (!canNotify(context)) return
        val text = when (kind) {
            0 -> context.getString(R.string.deadline_in_days, title, 3)
            1 -> context.getString(R.string.deadline_tomorrow, title)
            else -> context.getString(R.string.deadline_today, title)
        }
        val notification = base(
            context,
            CHANNEL_REMINDERS,
            title,
            text,
            openApp(context, "deadlines", 2 + kind),
        ).build()
        runCatching {
            NotificationManagerCompat.from(context)
                .notify(ID_DEADLINE_BASE + title.hashCode().let { if (it < 0) -it else it } % 1000, notification)
        }
    }

    fun notifyWaitingForWifi(context: Context, clipId: Long) {
        if (!canNotify(context)) return
        val requestBase = (clipId * 10L + 8_000L).toInt()
        val uploadNow = PendingIntent.getBroadcast(
            context,
            requestBase,
            Intent(context, UploadActionReceiver::class.java)
                .setAction(UploadActionReceiver.ACTION_UPLOAD_NOW)
                .putExtra(UploadActionReceiver.EXTRA_CLIP_ID, clipId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val waitWifi = PendingIntent.getBroadcast(
            context,
            requestBase + 1,
            Intent(context, UploadActionReceiver::class.java)
                .setAction(UploadActionReceiver.ACTION_WAIT_WIFI)
                .putExtra(UploadActionReceiver.EXTRA_CLIP_ID, clipId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = base(
            context,
            CHANNEL_UPLOADS,
            context.getString(R.string.upload_waiting_title),
            context.getString(R.string.upload_waiting_text),
            openApp(context, "speaking", 3),
        )
            .setOngoing(false)
            .addAction(0, context.getString(R.string.upload_now), uploadNow)
            .addAction(0, context.getString(R.string.wait_for_wifi), waitWifi)
            .build()
        runCatching {
            NotificationManagerCompat.from(context)
                .notify(ID_UPLOAD_BASE + (clipId % 1000).toInt(), notification)
        }
    }

    fun cancelWaiting(context: Context, clipId: Long) {
        runCatching {
            NotificationManagerCompat.from(context)
                .cancel(ID_UPLOAD_BASE + (clipId % 1000).toInt())
        }
    }
}

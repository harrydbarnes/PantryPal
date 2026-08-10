package com.example.pantrypal.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.pantrypal.MainActivity
import java.time.ZonedDateTime

class ShoppingReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val settings = AppPreferences.readSettings(applicationContext)
        if (!settings.shoppingRemindersEnabled) return Result.success()

        showNotification(settings)
        ShoppingReminderScheduler.update(
            context = applicationContext,
            settings = AppPreferences.readSettings(applicationContext),
            now = ZonedDateTime.now(),
            replaceExisting = false
        )
        return Result.success()
    }

    private fun showNotification(settings: AppSettings) {
        val channelId = Constants.SHOPPING_REMINDER_CHANNEL_ID
        val shoppingTime = ShoppingReminderSchedule.formatShoppingTime(settings.shoppingTimeMinutes)
        val copy = ShoppingReminderCopybook.forTiming(
            date = ZonedDateTime.now().toLocalDate(),
            shoppingTime = shoppingTime,
            timing = settings.shoppingReminderTiming
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Shopping nudges",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Friendly reminders before a usual shopping trip"
            }
            val notificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(copy.title)
            .setContentText(copy.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(copy.message))
            .setContentIntent(shoppingActionPendingIntent(ACTION_OPEN_LIST, REQUEST_OPEN_LIST))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_menu_edit,
                "Refresh my list",
                shoppingActionPendingIntent(ACTION_UPDATE_LIST, REQUEST_UPDATE_LIST)
            )
            .addAction(
                android.R.drawable.ic_menu_view,
                "Open list",
                shoppingActionPendingIntent(ACTION_OPEN_LIST, REQUEST_OPEN_LIST)
            )
            .build()

        try {
            NotificationManagerCompat.from(applicationContext)
                .notify(Constants.SHOPPING_REMINDER_NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            android.util.Log.w(
                TAG,
                "Notification permission not granted. Cannot show shopping reminder.",
                e
            )
        }
    }

    private fun shoppingActionPendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            this.action = action
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            applicationContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val UNIQUE_WORK_NAME = "ShoppingReminder"
        const val ACTION_UPDATE_LIST = "com.example.pantrypal.action.UPDATE_SHOPPING_LIST"
        const val ACTION_OPEN_LIST = "com.example.pantrypal.action.OPEN_SHOPPING_LIST"
        const val REQUEST_UPDATE_LIST = 1021
        const val REQUEST_OPEN_LIST = 1022
        private const val TAG = "ShoppingReminderWorker"
    }
}

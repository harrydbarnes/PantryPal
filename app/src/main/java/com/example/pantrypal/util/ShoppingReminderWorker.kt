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
import com.example.pantrypal.data.entity.MealEntity
import com.example.pantrypal.data.repository.KitchenRepository
import java.time.LocalDate
import java.time.ZonedDateTime

class ShoppingReminderWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val repository: KitchenRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val settings = AppPreferences.readSettings(applicationContext)
        if (!settings.shoppingRemindersEnabled) return Result.success()

        val now = ZonedDateTime.now()
        val outstandingItemCount = repository.countOpenShoppingItemsForWeek(
            currentShoppingWeek(now.toLocalDate())
        )
        if (!ShoppingReminderSchedule.shouldNotify(outstandingItemCount)) {
            NotificationManagerCompat.from(applicationContext)
                .cancel(Constants.SHOPPING_REMINDER_NOTIFICATION_ID)
            ShoppingReminderScheduler.update(
                context = applicationContext,
                settings = AppPreferences.readSettings(applicationContext),
                now = now,
                replaceExisting = false
            )
            return Result.success()
        }

        showNotification(settings, outstandingItemCount, now)
        ShoppingReminderScheduler.update(
            context = applicationContext,
            settings = AppPreferences.readSettings(applicationContext),
            now = now,
            replaceExisting = false
        )
        return Result.success()
    }

    private fun currentShoppingWeek(date: LocalDate): String {
        val preferences = applicationContext.getSharedPreferences(
            AppPreferences.FILE_NAME,
            Context.MODE_PRIVATE
        )
        val anchorWeek = preferences.getString("current_week", MealEntity.WEEK_A)
            ?: MealEntity.WEEK_A
        val anchorMonday = preferences.getLong(
            "meal_week_anchor",
            startOfWeek(date).toEpochDay()
        )
        return rotatingWeek(anchorWeek, anchorMonday, date)
    }

    private fun showNotification(
        settings: AppSettings,
        outstandingItemCount: Int,
        now: ZonedDateTime
    ) {
        val channelId = Constants.SHOPPING_REMINDER_CHANNEL_ID
        val shoppingTime = ShoppingReminderSchedule.formatShoppingTime(settings.shoppingTimeMinutes)
        val copy = ShoppingReminderCopybook.forTiming(
            date = now.toLocalDate(),
            shoppingTime = shoppingTime,
            timing = settings.shoppingReminderTiming,
            outstandingItemCount = outstandingItemCount
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
                "Review list",
                shoppingActionPendingIntent(ACTION_REVIEW_LIST, REQUEST_REVIEW_LIST)
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
        const val ACTION_REVIEW_LIST = "com.example.pantrypal.action.REVIEW_SHOPPING_LIST"
        @Deprecated("Use ACTION_REVIEW_LIST")
        const val ACTION_UPDATE_LIST = "com.example.pantrypal.action.UPDATE_SHOPPING_LIST"
        const val ACTION_OPEN_LIST = "com.example.pantrypal.action.OPEN_SHOPPING_LIST"
        const val REQUEST_REVIEW_LIST = 1021
        @Deprecated("Use REQUEST_REVIEW_LIST")
        const val REQUEST_UPDATE_LIST = REQUEST_REVIEW_LIST
        const val REQUEST_OPEN_LIST = 1022
        private const val TAG = "ShoppingReminderWorker"
    }
}

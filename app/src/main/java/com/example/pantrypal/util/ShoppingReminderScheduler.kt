package com.example.pantrypal.util

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object ShoppingReminderScheduler {
    fun update(
        context: Context,
        settings: AppSettings,
        now: ZonedDateTime = ZonedDateTime.now(),
        replaceExisting: Boolean = true
    ) {
        val workManager = WorkManager.getInstance(context)
        if (!settings.shoppingRemindersEnabled) {
            workManager.cancelUniqueWork(ShoppingReminderWorker.UNIQUE_WORK_NAME)
            return
        }

        val nextReminder = ShoppingReminderSchedule.nextReminderAt(
            now = now,
            shoppingDayOfWeek = settings.shoppingDayOfWeek,
            shoppingTimeMinutes = settings.shoppingTimeMinutes
        )
        val delay = Duration.between(now, nextReminder).toMillis().coerceAtLeast(0L)
        val request = OneTimeWorkRequestBuilder<ShoppingReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniqueWork(
            ShoppingReminderWorker.UNIQUE_WORK_NAME,
            if (replaceExisting) {
                ExistingWorkPolicy.REPLACE
            } else {
                ExistingWorkPolicy.APPEND_OR_REPLACE
            },
            request
        )
    }
}

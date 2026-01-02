package com.example.pantrypal.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.pantrypal.data.database.KitchenDatabase
import com.example.pantrypal.data.repository.KitchenRepository
import kotlinx.coroutines.flow.first

class ExpirationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val database = KitchenDatabase.getDatabase(applicationContext)
        val repository = KitchenRepository(
            database.itemDao(),
            database.inventoryDao(),
            database.consumptionDao(),
            database.shoppingDao()
        )

        val threshold = System.currentTimeMillis() + TWO_DAYS_IN_MILLIS

        // Since getExpiringItems returns a Flow, we take the first emission
        val expiringItems = repository.getExpiringItems(System.currentTimeMillis()).first()
            .filter { it.expirationDate != null && it.expirationDate < threshold }

        if (expiringItems.isNotEmpty()) {
            val count = expiringItems.size
            val contentText = if (count == 1) {
                "${expiringItems[0].name} is expiring soon!"
            } else {
                "$count items are expiring soon!"
            }

            showNotification(contentText)
        }

        return Result.success()
    }

    private fun showNotification(content: String) {
        val channelId = "expiration_channel"
        val notificationId = 101

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Expiration Alerts"
            val descriptionText = "Notifications for expiring pantry items"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Note: Missing smallIcon drawable, using system default or a placeholder if I knew one.
        // Assuming 'ic_launcher_foreground' exists or similar.
        // I will use android.R.drawable.ic_dialog_alert as a safe fallback for this environment.

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("PantryPal Alert")
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        try {
             // Permission check is required for Android 13+, but assume it's handled or we catch SecurityException
             with(NotificationManagerCompat.from(applicationContext)) {
                 notify(notificationId, builder.build())
             }
        } catch (e: SecurityException) {
            // Permission not granted. Log this for debugging purposes.
            android.util.Log.w("ExpirationWorker", "Notification permission not granted. Cannot show expiration alert.")
        }
    }

    companion object {
        private const val TWO_DAYS_IN_MILLIS = 2 * 24 * 60 * 60 * 1000L
    }
}

package com.example.pantrypal.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.pantrypal.MainActivity
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class ShoppingLocationGeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            val settings = AppPreferences.readSettings(context)
            ShoppingLocationGeofenceManager.update(
                context = context,
                enabled = settings.nearbyShoppingRemindersEnabled,
                locations = ShoppingLocationStore.read(context),
                onComplete = pendingResult::finish
            )
            return
        }

        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) {
            android.util.Log.w(
                TAG,
                "Geofence event failed with code ${event.errorCode}."
            )
            return
        }
        if (event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_DWELL) return
        if (!AppPreferences.readSettings(context).nearbyShoppingRemindersEnabled) return

        val triggeredIds = event.triggeringGeofences
            ?.map { it.requestId }
            .orEmpty()
            .toSet()
        val locations = ShoppingLocationStore.read(context)
            .filter { it.id in triggeredIds }
        if (locations.isEmpty()) return

        showNotification(context, locations)
    }

    private fun showNotification(context: Context, locations: List<ShoppingLocation>) {
        val channelId = Constants.SHOPPING_REMINDER_CHANNEL_ID
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Shopping nudges",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Friendly reminders before and during shopping trips"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val place = locations.joinToString(limit = 2) { it.name }
        val title = if (locations.size == 1) {
            "You're near $place"
        } else {
            "You're near a shopping spot"
        }
        val body = if (locations.size == 1) {
            "The list is just a tap away. Need anything from $place?"
        } else {
            "The list is just a tap away. Fancy a quick shopping-list check?"
        }
        val openListIntent = Intent(context, MainActivity::class.java).apply {
            action = ShoppingReminderWorker.ACTION_OPEN_LIST
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openListPendingIntent = PendingIntent.getActivity(
            context,
            ShoppingReminderWorker.REQUEST_OPEN_LIST,
            openListIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openListPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_menu_view,
                "Open my list",
                openListPendingIntent
            )
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(Constants.NEARBY_SHOPPING_NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            android.util.Log.w(TAG, "Notification permission not granted.", e)
        }
    }

    companion object {
        private const val TAG = "ShoppingGeofenceReceiver"
    }
}

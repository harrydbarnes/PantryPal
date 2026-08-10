package com.example.pantrypal.util

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

object ShoppingLocationGeofenceManager {
    private const val TAG = "ShoppingGeofences"
    private const val REQUEST_CODE = 2041
    private const val NOTIFICATION_RESPONSIVENESS_MILLIS = 5 * 60 * 1000

    @Volatile
    private var updateGeneration = 0

    fun update(
        context: Context,
        enabled: Boolean,
        locations: List<ShoppingLocation>,
        onComplete: () -> Unit = {}
    ) {
        val appContext = context.applicationContext
        val generation = synchronized(this) {
            updateGeneration += 1
            updateGeneration
        }
        val client = LocationServices.getGeofencingClient(appContext)
        val pendingIntent = geofencePendingIntent(appContext)

        runCatching {
            client.removeGeofences(pendingIntent).addOnCompleteListener {
                if (generation != updateGeneration || !enabled || locations.isEmpty()) {
                    onComplete()
                    return@addOnCompleteListener
                }
                if (!hasRequiredPermissions(appContext)) {
                    Log.i(TAG, "Nearby shopping reminders are enabled without all location permissions.")
                    onComplete()
                    return@addOnCompleteListener
                }
                registerGeofences(
                    client,
                    pendingIntent,
                    locations,
                    generation,
                    onComplete
                )
            }
        }.onFailure { error ->
            Log.w(TAG, "Could not refresh shopping geofences.", error)
            onComplete()
        }
    }

    fun hasForegroundPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

    fun hasRequiredPermissions(context: Context): Boolean =
        hasForegroundPermission(context) && (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            )

    private fun registerGeofences(
        client: com.google.android.gms.location.GeofencingClient,
        pendingIntent: PendingIntent,
        locations: List<ShoppingLocation>,
        generation: Int,
        onComplete: () -> Unit
    ) {
        val geofences = locations
            .filter(ShoppingLocation::isValid)
            .take(ShoppingLocationStore.MAX_LOCATIONS)
            .map { location ->
                Geofence.Builder()
                    .setRequestId(location.id)
                    .setCircularRegion(
                        location.latitude,
                        location.longitude,
                        location.radiusMeters
                    )
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL)
                    .setLoiteringDelay(2 * 60 * 1000)
                    .setNotificationResponsiveness(NOTIFICATION_RESPONSIVENESS_MILLIS)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .build()
            }
        if (geofences.isEmpty() || generation != updateGeneration) {
            onComplete()
            return
        }

        val request = GeofencingRequest.Builder()
            // Do not alert simply because the user opened Settings while already nearby.
            .setInitialTrigger(0)
            .addGeofences(geofences)
            .build()

        runCatching {
            client.addGeofences(request, pendingIntent)
                .addOnFailureListener { error ->
                    Log.w(TAG, "Could not register shopping geofences.", error)
                }
                .addOnCompleteListener { onComplete() }
        }.onFailure { error ->
            Log.w(TAG, "Could not register shopping geofences.", error)
            onComplete()
        }
    }

    private fun geofencePendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, ShoppingLocationGeofenceReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_MUTABLE
                } else {
                    0
                }
        )
}

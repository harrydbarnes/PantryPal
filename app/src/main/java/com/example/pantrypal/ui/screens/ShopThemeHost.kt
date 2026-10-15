package com.example.pantrypal.ui.screens

import android.Manifest
import android.content.Context
import android.os.Looper
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.pantrypal.ui.theme.ShopPalette
import com.example.pantrypal.util.AppPreferences
import com.example.pantrypal.util.ShoppingLocation
import com.example.pantrypal.util.ShoppingLocationGeofenceManager
import com.google.android.gms.location.*
import kotlinx.coroutines.delay

/** Device-local theme per aisle profile; coordinates never leave the existing location store. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ShopThemeHost(
    profile: String,
    locations: List<ShoppingLocation>,
    content: @Composable (@Composable () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences(AppPreferences.FILE_NAME, Context.MODE_PRIVATE) }
    val prefix = "shop_theme_$profile"
    var palette by remember(profile) { mutableStateOf(ShopPalette.entries.firstOrNull {
        it.name == prefs.getString("${prefix}_palette", null)
    } ?: ShopPalette.NONE) }
    var nearbyOnly by remember(profile) { mutableStateOf(prefs.getBoolean("${prefix}_nearby", false)) }
    var spotId by remember(profile) { mutableStateOf(prefs.getString("${prefix}_spot", null)) }
    var expanded by remember(profile) { mutableStateOf(false) }
    var permission by remember { mutableStateOf(ShoppingLocationGeofenceManager.hasForegroundPermission(context)) }
    val permissionRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permission = ShoppingLocationGeofenceManager.hasForegroundPermission(context)
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var resumed by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ ->
            resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            permission = ShoppingLocationGeofenceManager.hasForegroundPermission(context)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val spot = locations.firstOrNull { it.id == spotId }
    var fix by remember(profile, spotId) { mutableStateOf<android.location.Location?>(null) }
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    val checking = palette != ShopPalette.NONE && nearbyOnly && spot != null && permission && resumed
    DisposableEffect(checking, profile, spotId) {
        fix = null
        val client = LocationServices.getFusedLocationProviderClient(context)
        var active = true
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                if (active) fix = result.lastLocation
            }
            override fun onLocationAvailability(availability: LocationAvailability) {
                if (!availability.isLocationAvailable) fix = null
            }
        }
        if (checking) {
            runCatching {
                client.requestLocationUpdates(
                    LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 30_000L)
                        .setMaxUpdateAgeMillis(0L).build(), callback, Looper.getMainLooper()
                ).addOnFailureListener { if (active) fix = null }
            }.onFailure { fix = null }
        }
        onDispose { active = false; client.removeLocationUpdates(callback) }
    }
    LaunchedEffect(checking) {
        while (checking) { now = SystemClock.elapsedRealtime(); delay(5_000L) }
    }
    val near = checking && fix?.let { location ->
        val distances = FloatArray(1)
        android.location.Location.distanceBetween(location.latitude, location.longitude, spot!!.latitude, spot.longitude, distances)
        com.example.pantrypal.util.ShopThemeProximity.isNear(
            distances[0], location.accuracy, now - location.elapsedRealtimeNanos / 1_000_000,
            spot.radiusMeters
        )
    } == true
    val active = palette != ShopPalette.NONE && (!nearbyOnly || near)
    val base = MaterialTheme.colorScheme
    val scheme = if (active) palette.applyTo(base, base.surface.luminance() < 0.5f) else base
    MaterialTheme(colorScheme = scheme) {
        content {
            Column(Modifier.fillMaxWidth()) {
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (active) "${palette.label} colours · Theme" else "Shop theme${if (palette != ShopPalette.NONE) " · Waiting for shop" else ""}")
                }
                if (expanded) {
                    Text("Colours for this shop profile", style = MaterialTheme.typography.titleMedium)
                    Text("Shopping accents only. Choose a shop; PantryPal uses your usual appearance.", style = MaterialTheme.typography.bodySmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ShopPalette.entries.forEach { choice ->
                            FilterChip(selected = palette == choice, onClick = {
                                palette = choice
                                prefs.edit().putString("${prefix}_palette", choice.name).apply()
                            }, label = { Text(choice.label) })
                        }
                    }
                    if (palette != ShopPalette.NONE) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(false to "While shopping", true to "Only near saved shop").forEach { (nearby, label) ->
                                FilterChip(selected = nearbyOnly == nearby, onClick = {
                                    nearbyOnly = nearby
                                    prefs.edit().putBoolean("${prefix}_nearby", nearby).apply()
                                }, label = { Text(label) })
                            }
                        }
                        if (nearbyOnly) {
                            Text("Checks only while this screen is open. No background location needed. Choose the saved spot for this shop.", style = MaterialTheme.typography.bodySmall)
                            if (locations.isEmpty()) Text("Save this shop’s location in Settings → Nearby shopping nudges. Reminders can stay off.")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                locations.forEach { location ->
                                    FilterChip(selected = spotId == location.id, onClick = {
                                        spotId = location.id
                                        prefs.edit().putString("${prefix}_spot", location.id).apply()
                                    }, label = { Text(location.name) })
                                }
                            }
                            if (!permission) TextButton(onClick = {
                                permissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                            }) { Text("Allow location while using the app") }
                            Text(when {
                                spot == null -> "Choose a saved shop to activate its colours."
                                !permission -> "Location unavailable. Your usual theme stays active. You can also choose While shopping."
                                near -> "Near ${spot.name} · ${palette.label} colours active"
                                else -> "Waiting for a fresh, accurate location near ${spot.name}. Your usual theme stays active."
                            }, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

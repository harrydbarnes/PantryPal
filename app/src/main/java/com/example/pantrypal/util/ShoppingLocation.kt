package com.example.pantrypal.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

data class ShoppingLocation(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float = DEFAULT_RADIUS_METERS
) {
    fun isValid(): Boolean =
        id.isNotBlank() &&
            name.isNotBlank() &&
            latitude in -90.0..90.0 &&
            longitude in -180.0..180.0 &&
            radiusMeters in MIN_RADIUS_METERS..MAX_RADIUS_METERS

    companion object {
        const val DEFAULT_RADIUS_METERS = 200f
        const val MIN_RADIUS_METERS = 100f
        const val MAX_RADIUS_METERS = 500f
    }
}

object ShoppingLocationStore {
    const val MAX_LOCATIONS = 20
    private const val KEY_LOCATIONS = "shopping_locations"

    private val gson = Gson()
    private val locationListType = object : TypeToken<List<ShoppingLocation>>() {}.type

    fun read(context: Context): List<ShoppingLocation> {
        val stored = context.getSharedPreferences(
            AppPreferences.FILE_NAME,
            Context.MODE_PRIVATE
        ).getString(KEY_LOCATIONS, null) ?: return emptyList()

        return runCatching {
            gson.fromJson<List<ShoppingLocation>>(stored, locationListType)
                .orEmpty()
                .map { location ->
                    location.copy(
                        name = location.name.trim(),
                        radiusMeters = location.radiusMeters.coerceIn(
                            ShoppingLocation.MIN_RADIUS_METERS,
                            ShoppingLocation.MAX_RADIUS_METERS
                        )
                    )
                }
                .filter(ShoppingLocation::isValid)
                .take(MAX_LOCATIONS)
        }.getOrDefault(emptyList())
    }

    fun write(context: Context, locations: List<ShoppingLocation>) {
        val safeLocations = locations
            .map { location ->
                location.copy(
                    name = location.name.trim(),
                    radiusMeters = location.radiusMeters.coerceIn(
                        ShoppingLocation.MIN_RADIUS_METERS,
                        ShoppingLocation.MAX_RADIUS_METERS
                    )
                )
            }
            .filter(ShoppingLocation::isValid)
            .take(MAX_LOCATIONS)

        context.getSharedPreferences(AppPreferences.FILE_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LOCATIONS, gson.toJson(safeLocations))
            .apply()
    }
}

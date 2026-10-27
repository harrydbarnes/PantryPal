package com.example.pantrypal.util

object ShopThemeProximity {
    /** Require the accuracy circle to fit inside the saved radius; stale/approximate fixes fail closed. */
    fun isNear(distanceMeters: Float, accuracyMeters: Float, ageMillis: Long, radiusMeters: Float): Boolean =
        distanceMeters.isFinite() && accuracyMeters.isFinite() && radiusMeters.isFinite() &&
            distanceMeters >= 0 && accuracyMeters >= 0 && ageMillis in 0..90_000 &&
            radiusMeters > 0 && distanceMeters + accuracyMeters <= radiusMeters
}

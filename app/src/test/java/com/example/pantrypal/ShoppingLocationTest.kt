package com.example.pantrypal

import com.example.pantrypal.util.ShoppingLocation
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShoppingLocationTest {
    @Test
    fun validLocationUsesSupportedCoordinatesAndRadius() {
        val location = ShoppingLocation(
            name = "Supermarket",
            latitude = 51.5074,
            longitude = -0.1278
        )

        assertTrue(location.isValid())
    }

    @Test
    fun invalidLocationRejectsBadCoordinatesAndRadius() {
        assertFalse(
            ShoppingLocation(
                name = "Nowhere",
                latitude = 91.0,
                longitude = 0.0
            ).isValid()
        )
        assertFalse(
            ShoppingLocation(
                name = "Nowhere",
                latitude = 0.0,
                longitude = 0.0,
                radiusMeters = ShoppingLocation.MIN_RADIUS_METERS - 1f
            ).isValid()
        )
    }
}

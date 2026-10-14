package com.example.pantrypal

import com.example.pantrypal.data.entity.InventoryEntity
import com.example.pantrypal.util.stockQuantityInUnit
import org.junit.Assert.*
import org.junit.Test

class StockQuantityTest {
    private fun batch(quantity: Double, unit: String, expiry: Long? = null) =
        InventoryEntity(itemId = 1, quantity = quantity, unit = unit, expirationDate = expiry)

    @Test fun mixedMassAndVolumeUseCanonicalUnits() {
        assertEquals(1.5, stockQuantityInUnit(listOf(batch(1.0, "kg"), batch(500.0, "g")), "kg")!!, 0.00001)
        assertEquals(1.25, stockQuantityInUnit(listOf(batch(1.0, "l"), batch(250.0, "ml")), "l")!!, 0.00001)
    }

    @Test fun incompatibleRemainingStockRequiresReview() {
        assertNull(stockQuantityInUnit(listOf(batch(1.0, "kg"), batch(1.0, "pack")), "kg"))
        assertEquals(1.0, stockQuantityInUnit(listOf(batch(1.0, "kg"), batch(0.0, "pack")), "kg")!!, 0.0)
    }

    @Test fun expiredStockDoesNotSuppressRestock() {
        assertEquals(0.0, stockQuantityInUnit(listOf(batch(5.0, "kg", 0)), "kg")!!, 0.0)
    }
}

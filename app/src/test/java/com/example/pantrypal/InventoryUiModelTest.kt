package com.example.pantrypal

import com.example.pantrypal.data.dao.InventoryWithItemMap
import com.example.pantrypal.viewmodel.toUiModel
import org.junit.Test
import org.junit.Assert.*

class InventoryUiModelTest {
    @Test
    fun testToUiModelMapping() {
        val input = InventoryWithItemMap(
            inventoryId = 1,
            itemId = 100,
            quantity = 2.0,
            unit = "pcs",
            addedDate = 1000L,
            expirationDate = null,
            name = "Milk",
            barcode = "123",
            defaultUnit = "pcs",
            category = "Dairy",
            isVegetarian = true,
            isGlutenFree = true,
            isUsual = true,
            createdAt = 1000L
        )

        val result = input.toUiModel()

        assertEquals("Milk", result.name)
        assertEquals("2.0 pcs", result.quantity)
        assertTrue(result.tags.contains("Veg"))
        assertTrue(result.tags.contains("GF"))
    }
}

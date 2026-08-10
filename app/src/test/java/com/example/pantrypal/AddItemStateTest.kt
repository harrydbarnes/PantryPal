package com.example.pantrypal

import com.example.pantrypal.ui.screens.AddItemState
import com.example.pantrypal.util.AddItemDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddItemStateTest {
    @Test
    fun defaultsMakeAQuickEntryReadyWithoutOpeningAdvancedSections() {
        val state = AddItemState(
            AddItemDefaults(unit = "kg", category = "Fruit & Veg", storageLocation = "Fridge")
        )

        assertEquals("kg", state.unit)
        assertEquals("Fruit & Veg", state.category)
        assertEquals("Fridge", state.storageLocation)
        assertEquals("1", state.qtyText)
        assertFalse(state.isValid)

        state.name = "Apples"
        assertTrue(state.isValid)
    }

    @Test
    fun saveAnotherClearsItemSpecificFieldsAndKeepsQuickDefaults() {
        val state = AddItemState(AddItemDefaults(unit = "L", category = "Dairy", storageLocation = "Fridge"))
        state.name = "Milk"
        state.qtyText = "2"
        state.isOpened = true
        state.isVegetarian = true
        state.isUsual = true

        state.prepareNextItem()

        assertEquals("", state.name)
        assertEquals("1", state.qtyText)
        assertEquals("L", state.unit)
        assertEquals("Dairy", state.category)
        assertEquals("Fridge", state.storageLocation)
        assertFalse(state.isOpened)
        assertFalse(state.isVegetarian)
        assertFalse(state.isUsual)
    }
}

package com.example.pantrypal

import com.example.pantrypal.util.MealIngredientSelection
import org.junit.Assert.assertEquals
import org.junit.Test

class MealIngredientSelectionTest {
    @Test
    fun choicesKeepSelectedIngredientsAndRemoveCaseInsensitiveDuplicates() {
        assertEquals(
            listOf("Tomatoes", "Milk", "Basil"),
            MealIngredientSelection.choices(
                selected = listOf("Tomatoes"),
                suggestions = listOf("milk", "tomatoes", "Basil")
            )
        )
    }

    @Test
    fun toggleAddsAndRemovesAnIngredientWithoutChangingOtherSelections() {
        val selected = listOf("Milk", "Bread")

        assertEquals(
            listOf("Milk", "Bread", "Eggs"),
            MealIngredientSelection.toggle(selected, "Eggs")
        )
        assertEquals(
            listOf("Milk"),
            MealIngredientSelection.toggle(selected, "bread")
        )
    }

    @Test
    fun addIgnoresBlankAndDuplicateIngredients() {
        val selected = listOf("Milk")

        assertEquals(selected, MealIngredientSelection.add(selected, "  "))
        assertEquals(selected, MealIngredientSelection.add(selected, " milk "))
        assertEquals(listOf("Milk", "Oats"), MealIngredientSelection.add(selected, " Oats "))
    }
}

package com.example.pantrypal

import com.example.pantrypal.data.entity.MealEntity
import com.example.pantrypal.util.mealsForShopping
import com.example.pantrypal.util.rotatingWeek
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class MealScheduleTest {
    @Test
    fun rotatingWeek_alternatesFromAnchoredMonday() {
        val monday = LocalDate.of(2026, 7, 20)
        assertEquals(MealEntity.WEEK_A, rotatingWeek(MealEntity.WEEK_A, monday.toEpochDay(), monday))
        assertEquals(MealEntity.WEEK_B, rotatingWeek(MealEntity.WEEK_A, monday.toEpochDay(), monday.plusWeeks(1)))
        assertEquals(MealEntity.WEEK_A, rotatingWeek(MealEntity.WEEK_A, monday.toEpochDay(), monday.plusWeeks(2)))
    }

    @Test
    fun mealsForShopping_filtersWeekAndDeduplicatesIngredients() {
        val meals = listOf(
            MealEntity(name = "Pasta", week = "A", ingredients = listOf(" Pasta ", "Tomatoes")),
            MealEntity(name = "Soup", week = "A", ingredients = listOf("tomatoes", "Bread")),
            MealEntity(name = "Curry", week = "B", ingredients = listOf("Rice"))
        )
        assertEquals(listOf("Pasta", "Tomatoes", "Bread"), mealsForShopping(meals, "A"))
    }
}

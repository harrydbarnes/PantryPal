package com.example.pantrypal

import com.example.pantrypal.data.entity.MealEntity
import com.example.pantrypal.domain.recipe.Recipe
import com.example.pantrypal.util.RecipeMealConversion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeMealConversionTest {
    @Test
    fun `merges duplicate meal titles and ingredients`() {
        val meals = listOf(
            MealEntity(
                mealId = 1,
                name = "Tomato Pasta",
                week = "A",
                ingredients = listOf("Pasta", "Tomatoes")
            ),
            MealEntity(
                mealId = 2,
                name = " tomato   pasta ",
                week = "B",
                ingredients = listOf("200 g pasta", "Parmesan")
            ),
            MealEntity(
                mealId = 3,
                name = "Eating out",
                week = "C",
                ingredients = emptyList()
            )
        )

        val recipes = RecipeMealConversion.fromMeals(meals, now = 99L)

        assertEquals(1, recipes.size)
        assertEquals("Tomato Pasta", recipes.single().title)
        assertEquals(
            setOf("pasta", "tomato", "parmesan"),
            recipes.single().ingredients.map { it.normalizedName }.toSet()
        )
        assertEquals(listOf("Week A", "Week B"), recipes.single().tags)
    }

    @Test
    fun `omits titles that already exist in recipe library`() {
        val meals = listOf(
            MealEntity(name = "Fish & Chips", week = "A", ingredients = listOf("Fish", "Chips"))
        )
        val existing = listOf(
            Recipe(title = "fish and chips", ingredients = emptyList())
        )

        val converted = RecipeMealConversion.fromMeals(meals, existing)

        assertTrue(converted.isEmpty())
    }
}

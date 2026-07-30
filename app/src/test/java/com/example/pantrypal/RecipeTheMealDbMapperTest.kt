package com.example.pantrypal

import com.example.pantrypal.data.api.RecipeTheMealDbMapper
import com.example.pantrypal.data.api.RecipeTheMealDbMeal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeTheMealDbMapperTest {
    @Test
    fun `maps full meal response with attribution and structured ingredients`() {
        val meal = RecipeTheMealDbMeal(
            idMeal = "123",
            strMeal = "Chickpea Curry",
            strInstructions = "Fry the onion. Add the chickpeas.",
            strCategory = "Vegetarian",
            strArea = "Indian",
            ingredient1 = "Chickpeas",
            measure1 = "2 cans",
            ingredient2 = "Onion",
            measure2 = "1"
        )

        val recipe = RecipeTheMealDbMapper.toDomain(meal, now = 77L)

        assertEquals("Chickpea Curry", recipe.title)
        assertEquals("123", recipe.externalId)
        assertEquals("TheMealDB", recipe.source?.name)
        assertEquals("Recipe data from TheMealDB", recipe.source?.attribution)
        assertEquals(
            "https://www.themealdb.com/api/json/v1/1/lookup.php?i=123",
            recipe.source?.url
        )
        assertEquals(2, recipe.ingredients.size)
        assertEquals("chickpea", recipe.ingredients[0].normalizedName)
        assertEquals(2.0, recipe.ingredients[0].quantity!!, 0.001)
        assertEquals("can", recipe.ingredients[0].unit)
        assertEquals(listOf("Vegetarian", "Indian"), recipe.tags)
        assertTrue(recipe.instructions.isNotEmpty())
    }
}

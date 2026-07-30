package com.example.pantrypal

import com.example.pantrypal.domain.recipe.Recipe
import com.example.pantrypal.domain.recipe.RecipeIngredientNormalizer
import com.example.pantrypal.domain.recipe.RecipePantryIngredient
import com.example.pantrypal.domain.recipe.RecipeRanker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeNormalizationRankingTest {
    @Test
    fun `normalizes quantity unit preparation and plurals`() {
        val ingredient = RecipeIngredientNormalizer.parse("1½ cups finely chopped Tomatoes")

        assertEquals(1.5, ingredient.quantity!!, 0.001)
        assertEquals("cup", ingredient.unit)
        assertEquals("tomato", ingredient.normalizedName)
    }

    @Test
    fun `builds pantry aware shelves`() {
        val now = 1_000_000L
        val tomatoes = RecipeIngredientNormalizer.parse("2 tomatoes", 0)
        val pasta = RecipeIngredientNormalizer.parse("200 g pasta", 1)
        val cheese = RecipeIngredientNormalizer.parse("50 g cheese", 2)
        val recipes = listOf(
            Recipe(
                id = 1,
                title = "Tomato pasta",
                ingredients = listOf(tomatoes, pasta),
                isFavourite = true,
                rating = 5,
                lastCookedAt = null
            ),
            Recipe(
                id = 2,
                title = "Cheesy pasta",
                ingredients = listOf(pasta, cheese),
                rating = 3,
                lastCookedAt = now
            )
        )
        val pantry = listOf(
            RecipePantryIngredient(
                itemId = 1,
                name = "Fresh tomatoes",
                quantity = 3.0,
                expirationDate = now + 24 * 60 * 60 * 1_000L
            ),
            RecipePantryIngredient(
                itemId = 2,
                name = "Pasta",
                quantity = 1.0
            )
        )

        val shelves = RecipeRanker.buildShelves(recipes, pantry, now = now)

        assertEquals(listOf("Tomato pasta"), shelves.cookNow.map { it.recipe.title })
        assertEquals(listOf("Tomato pasta"), shelves.useSoon.map { it.recipe.title })
        assertEquals(listOf("Cheesy pasta"), shelves.missingOneOrTwo.map { it.recipe.title })
        assertEquals(listOf("Tomato pasta"), shelves.forgottenFavourites.map { it.recipe.title })
        assertTrue(shelves.cookNow.first().missingIngredients.isEmpty())
    }
}

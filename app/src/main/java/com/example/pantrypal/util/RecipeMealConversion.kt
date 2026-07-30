package com.example.pantrypal.util

import com.example.pantrypal.data.entity.MealEntity
import com.example.pantrypal.domain.recipe.Recipe
import com.example.pantrypal.domain.recipe.RecipeIngredientNormalizer
import com.example.pantrypal.domain.recipe.normalizeRecipeTitle

object RecipeMealConversion {
    /**
     * Converts useful meal-plan entries into reusable recipes. Meals with the same normalized
     * title are merged, and any titles already present in [existingRecipes] are omitted.
     */
    fun fromMeals(
        meals: List<MealEntity>,
        existingRecipes: List<Recipe> = emptyList(),
        now: Long = System.currentTimeMillis()
    ): List<Recipe> {
        val existingTitles = existingRecipes
            .mapTo(mutableSetOf()) { normalizeRecipeTitle(it.title) }

        return meals
            .asSequence()
            .filter { it.name.isNotBlank() && it.ingredients.any(String::isNotBlank) }
            .groupBy { normalizeRecipeTitle(it.name) }
            .filterKeys { it.isNotBlank() && it !in existingTitles }
            .map { (_, matchingMeals) ->
                val first = matchingMeals.first()
                val ingredients = matchingMeals
                    .flatMap(MealEntity::ingredients)
                    .filter(String::isNotBlank)
                    .mapIndexed { index, raw ->
                        RecipeIngredientNormalizer.parse(raw, index)
                    }
                    .distinctBy { it.normalizedName }
                    .mapIndexed { index, ingredient -> ingredient.copy(sortOrder = index) }

                Recipe(
                    title = first.name.trim(),
                    ingredients = ingredients,
                    tags = matchingMeals
                        .map { "Week ${it.week}" }
                        .distinct(),
                    createdAt = now,
                    updatedAt = now
                )
            }
            .sortedBy { it.title.lowercase() }
    }
}

package com.example.pantrypal.domain.recipe

import java.util.Locale

object RecipeSearch {
    fun local(recipes: List<Recipe>, query: String): List<Recipe> {
        val terms = normalizeRecipeTitle(query).split(' ').filter(String::isNotBlank)
        if (terms.isEmpty()) {
            return recipes.sortedWith(recipeComparator)
        }

        return recipes
            .asSequence()
            .filter { recipe ->
                val searchable = buildString {
                    append(normalizeRecipeTitle(recipe.title))
                    append(' ')
                    append(recipe.ingredients.joinToString(" ") { it.normalizedName })
                    append(' ')
                    append(recipe.tags.joinToString(" ") { normalizeRecipeTitle(it) })
                    append(' ')
                    append(normalizeRecipeTitle(recipe.source?.name.orEmpty()))
                }
                terms.all(searchable::contains)
            }
            .sortedWith(recipeComparator)
            .toList()
    }

    private val recipeComparator = compareByDescending<Recipe> { it.isFavourite }
        .thenByDescending { it.rating ?: 0 }
        .thenBy { it.title.lowercase(Locale.ROOT) }
}

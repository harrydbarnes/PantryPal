package com.example.pantrypal.domain.recipe

import kotlin.math.max

object RecipeRanker {
    private const val DAY_MILLIS = 24L * 60L * 60L * 1_000L

    fun match(
        recipe: Recipe,
        pantry: List<RecipePantryIngredient>,
        now: Long = System.currentTimeMillis(),
        useSoonWindowMillis: Long = 3 * DAY_MILLIS
    ): RecipeMatch {
        val stocked = pantry.filter { it.quantity > 0 }
        val essential = recipe.ingredients.filterNot { it.isOptional }
        val matched = essential.mapNotNull { ingredient ->
            stocked.firstOrNull { pantryItem -> ingredient.matches(pantryItem) }
                ?.let { ingredient to it }
        }
        val matchedIngredients = matched.map { it.first }.toSet()
        val missing = essential.filterNot(matchedIngredients::contains)
        val soonCutoff = now + useSoonWindowMillis
        val expiring = matched
            .filter { (_, pantryItem) ->
                pantryItem.expirationDate?.let { it in now..soonCutoff } == true
            }
            .map { it.first }
            .distinctBy { it.normalizedName }

        val coverage = if (essential.isEmpty()) 0.0 else matched.size.toDouble() / essential.size
        val favouriteBoost = if (recipe.isFavourite) 0.08 else 0.0
        val ratingBoost = ((recipe.rating ?: 0) / 5.0) * 0.07
        val expiryBoost = max(0, expiring.size) * 0.05
        return RecipeMatch(
            recipe = recipe,
            availableIngredientCount = matched.size,
            missingIngredients = missing,
            expiringIngredients = expiring,
            matchScore = (coverage + favouriteBoost + ratingBoost + expiryBoost).coerceAtMost(1.25)
        )
    }

    fun buildShelves(
        recipes: List<Recipe>,
        pantry: List<RecipePantryIngredient>,
        now: Long = System.currentTimeMillis(),
        useSoonWindowMillis: Long = 3 * DAY_MILLIS,
        forgottenAfterMillis: Long = 28 * DAY_MILLIS,
        shelfLimit: Int = 12
    ): RecipeIdeaShelves {
        val matches = recipes
            .map { match(it, pantry, now, useSoonWindowMillis) }
            .sortedWith(
                compareByDescending<RecipeMatch> { it.matchScore }
                    .thenByDescending { it.recipe.rating ?: 0 }
                    .thenBy { it.recipe.title }
            )

        return RecipeIdeaShelves(
            cookNow = matches.filter(RecipeMatch::canCookNow).take(shelfLimit),
            useSoon = matches
                .filter { it.expiringIngredients.isNotEmpty() && it.missingIngredients.size <= 2 }
                .sortedWith(
                    compareByDescending<RecipeMatch> { it.expiringIngredients.size }
                        .thenByDescending { it.matchScore }
                )
                .take(shelfLimit),
            missingOneOrTwo = matches
                .filter { it.missingIngredients.size in 1..2 }
                .take(shelfLimit),
            forgottenFavourites = matches
                .filter {
                    (it.recipe.isFavourite || (it.recipe.rating ?: 0) >= 4) &&
                        (it.recipe.lastCookedAt == null ||
                            now - it.recipe.lastCookedAt >= forgottenAfterMillis)
                }
                .sortedWith(
                    compareBy<RecipeMatch> { it.recipe.lastCookedAt ?: Long.MIN_VALUE }
                        .thenByDescending { it.recipe.rating ?: 0 }
                )
                .take(shelfLimit)
        )
    }

    private fun RecipeIngredient.matches(pantryItem: RecipePantryIngredient): Boolean {
        if (linkedPantryItemId != null && linkedPantryItemId == pantryItem.itemId) return true
        val ingredientName = normalizedName.ifBlank {
            RecipeIngredientNormalizer.normalizeName(name)
        }
        val pantryName = RecipeIngredientNormalizer.normalizeName(pantryItem.name)
        if (ingredientName.isBlank() || pantryName.isBlank()) return false
        if (ingredientName == pantryName) return true

        val ingredientTokens = ingredientName.split(' ')
        val pantryTokens = pantryName.split(' ')
        return ingredientTokens.size > 1 &&
            pantryTokens.size > 1 &&
            (ingredientTokens.containsAll(pantryTokens) || pantryTokens.containsAll(ingredientTokens))
    }
}

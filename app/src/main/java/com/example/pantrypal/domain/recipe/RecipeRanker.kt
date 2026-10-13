package com.example.pantrypal.domain.recipe

import kotlin.math.max
import com.example.pantrypal.util.*

object RecipeRanker {
    private const val DAY_MILLIS = 24L * 60L * 60L * 1_000L

    fun match(
        recipe: Recipe,
        pantry: List<RecipePantryIngredient>,
        now: Long = System.currentTimeMillis(),
        useSoonWindowMillis: Long = 3 * DAY_MILLIS
    ): RecipeMatch {
        val essential = recipe.ingredients.filterNot { it.isOptional }
        val stock = pantry.map { ShoppingStock(it.name, it.quantity, it.unit, it.expirationDate, it.itemId) }
        val reconciliation = reconcileRecipeShopping(essential, stock, now)
        val lines = reconciliation.lines
        val missing = lines.filter { it.status != ShoppingNeedStatus.ALREADY_AT_HOME }.map { line ->
            essential.first { normalizeShoppingName(reconciliation.ingredientNames.getValue(it)) == line.normalizedName }.copy(
                quantity = line.buyQuantity, unit = line.unit
            )
        }
        val matched = essential.filter { ingredient ->
            lines.filter { it.normalizedName == normalizeShoppingName(reconciliation.ingredientNames.getValue(ingredient)) }
                .all { it.status == ShoppingNeedStatus.ALREADY_AT_HOME }
        }
        val soonCutoff = now + useSoonWindowMillis
        val expiring = matched.filter { ingredient -> stock.any {
            normalizeShoppingName(it.name) == normalizeShoppingName(reconciliation.ingredientNames.getValue(ingredient)) &&
                it.expirationDate?.let { date -> date in now..soonCutoff } == true
        } }.distinctBy { it.normalizedName }

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

}

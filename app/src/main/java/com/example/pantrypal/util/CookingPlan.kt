package com.example.pantrypal.util

import com.example.pantrypal.data.dao.InventoryWithItemMap
import com.example.pantrypal.data.entity.MealEntity
import com.example.pantrypal.domain.recipe.Recipe

data class CookingReview(val recipe: Recipe, val stock: List<InventoryWithItemMap>, val week: String,
    val initialServings: Double = recipe.servings ?: 1.0, val meal: MealEntity? = null,
    val recipeSnapshot: Recipe? = recipe.takeIf { it.id > 0 })
data class CookingLine(val stock: InventoryWithItemMap, val suggestedQuantity: Double)
data class CookingPlan(val lines: List<CookingLine>, val notes: List<String>)

/** Allocate each batch once, earliest expiry first. Unknown amounts always require a decision. */
fun planCooking(recipe: Recipe, stock: List<InventoryWithItemMap>, multiplier: Double, now: Long = System.currentTimeMillis()): CookingPlan {
    require(multiplier.isFinite() && multiplier > 0)
    val amounts = mutableMapOf<Long, Double>()
    val matched = linkedMapOf<Long, InventoryWithItemMap>()
    val notes = mutableListOf<String>()
    recipe.ingredients.filterNot { it.isOptional }.forEach { ingredient ->
        val matches = stock.filter {
            it.quantity > 0 && classifyExpiry(it.expirationDate, now) != ExpiryStatus.EXPIRED &&
                (ingredient.linkedPantryItemId?.let { id -> it.itemId == id }
                    ?: (normalizeShoppingName(it.name) == ingredient.normalizedName))
        }.sortedBy { it.expirationDate ?: Long.MAX_VALUE }
        matches.forEach { matched[it.inventoryId] = it }
        val quantity = ingredient.quantity
        if (quantity == null || !quantity.isFinite() || quantity <= 0) {
            notes += "${ingredient.name}: amount unknown; enter what you used."
        } else {
            val unit = ingredient.unit ?: "pcs"
            var remaining = quantity * multiplier
            matches.forEach { batch ->
                val available = batch.quantity - (amounts[batch.inventoryId] ?: 0.0)
                val converted = convertShoppingQuantity(available, batch.unit, unit)
                if (converted == null) {
                    notes += "${ingredient.name}: ${batch.unit} cannot be converted to $unit; review manually."
                } else if (remaining > 0 && available > 0) {
                    val used = minOf(remaining, converted)
                    amounts[batch.inventoryId] = minOf(batch.quantity, (amounts[batch.inventoryId] ?: 0.0) + convertShoppingQuantity(used, unit, batch.unit)!!)
                    remaining -= used
                }
            }
            if (remaining > 0.000001) notes += "${ingredient.name}: $remaining $unit is not covered by recorded, in-date stock."
        }
        if (matches.isEmpty()) notes += "${ingredient.name}: no matching in-date stock; no deduction proposed."
    }
    return CookingPlan(matched.values.map { CookingLine(it, amounts[it.inventoryId] ?: 0.0) }, notes.distinct())
}

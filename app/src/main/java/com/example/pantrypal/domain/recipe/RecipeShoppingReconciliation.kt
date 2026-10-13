package com.example.pantrypal.domain.recipe

import com.example.pantrypal.util.*

data class RecipeShoppingReconciliation(
    val lines: List<ShoppingReconciliationLine>,
    val ingredientNames: Map<RecipeIngredient, String>
)

/** Resolve explicit product links once and share each batch across aliases of the same product. */
fun reconcileRecipeShopping(
    ingredients: List<RecipeIngredient>, stock: List<ShoppingStock>, now: Long = System.currentTimeMillis()
): RecipeShoppingReconciliation {
    val names = ingredients.associateWith { ingredient ->
        stock.firstOrNull { ingredient.linkedPantryItemId != null && it.itemId == ingredient.linkedPantryItemId }
            ?.name ?: ingredient.name
    }
    val texts = ingredients.map { shoppingIngredientText(names.getValue(it), it.quantity, it.unit) }
    return RecipeShoppingReconciliation(reconcileShoppingStock(texts, stock, now), names)
}

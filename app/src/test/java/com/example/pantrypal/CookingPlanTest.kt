package com.example.pantrypal

import com.example.pantrypal.data.dao.InventoryWithItemMap
import com.example.pantrypal.domain.recipe.*
import com.example.pantrypal.util.*
import org.junit.Assert.*
import org.junit.Test

class CookingPlanTest {
    private fun batch(id: Long, quantity: Double, unit: String, item: Long = 1) = InventoryWithItemMap(
        inventoryId = id, itemId = item, quantity = quantity, unit = unit, addedDate = 0, expirationDate = null,
        name = "Rice", barcode = null, defaultUnit = "g", category = "General", isVegetarian = false,
        isGlutenFree = false, isUsual = false, imageUrl = null, createdAt = 0)

    @Test fun portionsConvertUnitsAndNeverAllocateTheSameStockTwice() {
        val recipe = Recipe(title = "Rice", ingredients = listOf(
            RecipeIngredientNormalizer.parse("600 g rice"), RecipeIngredientNormalizer.parse("600 g rice")))
        val plan = planCooking(recipe, listOf(batch(1, 0.5, "kg"), batch(2, 50.0, "g")), 0.5)
        assertEquals(0.5, plan.lines[0].suggestedQuantity, 0.00001)
        assertEquals(50.0, plan.lines[1].suggestedQuantity, 0.00001)
        assertTrue(plan.notes.any { it.contains("not covered") })
    }

    @Test fun unknownAmountsAndIncompatibleUnitsRequireManualReview() {
        val recipe = Recipe(title = "Rice", ingredients = listOf(RecipeIngredientNormalizer.parse("rice")))
        assertEquals(0.0, planCooking(recipe, listOf(batch(1, 500.0, "g")), 1.0).lines.single().suggestedQuantity, 0.0)
        val measured = recipe.copy(ingredients = listOf(RecipeIngredientNormalizer.parse("300 g rice")))
        val plan = planCooking(measured, listOf(batch(1, 2.0, "pack")), 1.0)
        assertEquals(0.0, plan.lines.single().suggestedQuantity, 0.0)
        assertTrue(plan.notes.any { it.contains("cannot be converted") })
    }

    @Test fun pantryLinksTakePriorityOverNamesAndPortionsScaleShopping() {
        val ingredient = RecipeIngredientNormalizer.parse("300 g basmati").copy(linkedPantryItemId = 1)
        val plan = planCooking(Recipe(title = "Bowl", ingredients = listOf(ingredient)), listOf(batch(1, 500.0, "g"), batch(2, 500.0, "g", item = 2)), 1.0)
        assertEquals(listOf(1L), plan.lines.map { it.stock.itemId })
        assertEquals(300.0, plan.lines.single().suggestedQuantity, 0.0)
        val scaled = scaleMealIngredients(listOf("300 g rice", "salt to taste"), 2.0)
        assertEquals(600.0, RecipeIngredientNormalizer.parse(scaled.first()).quantity!!, 0.0)
        assertEquals("salt to taste", scaled.last())
    }
}

package com.example.pantrypal

import com.example.pantrypal.data.entity.ShoppingItemEntity
import com.example.pantrypal.ui.screens.splitShoppingItemsForShoppingMode
import org.junit.Assert.assertEquals
import org.junit.Test

class ShoppingModeItemsTest {

    @Test
    fun `shopping mode keeps unchecked items ahead of checked items`() {
        val milk = ShoppingItemEntity(shoppingId = 1, name = "Milk")
        val bread = ShoppingItemEntity(shoppingId = 2, name = "Bread", isChecked = true)
        val apples = ShoppingItemEntity(shoppingId = 3, name = "Apples")

        val result = splitShoppingItemsForShoppingMode(listOf(milk, bread, apples))

        assertEquals(listOf(milk, apples), result.active)
        assertEquals(listOf(bread), result.checked)
    }
}

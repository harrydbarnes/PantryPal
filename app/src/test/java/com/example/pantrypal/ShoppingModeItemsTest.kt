package com.example.pantrypal

import com.example.pantrypal.data.entity.ShoppingItemEntity
import com.example.pantrypal.ui.screens.shoppingItemsForSection
import org.junit.Assert.assertEquals
import org.junit.Test

class ShoppingModeItemsTest {

    @Test
    fun `shopping mode keeps checked items at the bottom of their own section`() {
        val milk = ShoppingItemEntity(shoppingId = 1, name = "Milk", sectionId = 10)
        val bread = ShoppingItemEntity(shoppingId = 2, name = "Bread", sectionId = 10, isChecked = true)
        val apples = ShoppingItemEntity(shoppingId = 3, name = "Apples", sectionId = 10)
        val coffee = ShoppingItemEntity(shoppingId = 4, name = "Coffee", sectionId = 20, isChecked = true)

        val result = shoppingItemsForSection(listOf(milk, bread, apples, coffee), 10)

        assertEquals(listOf(milk, apples, bread), result)
    }
}

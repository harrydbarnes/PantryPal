package com.example.pantrypal

import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationStructureTest {
    @Test
    fun `primary navigation remains limited to the four core destinations`() {
        assertEquals(
            listOf(
                AppScreen.Dashboard,
                AppScreen.Inventory,
                AppScreen.MealPlan,
                AppScreen.ShoppingList
            ),
            PrimaryNavigationDestinations.map { it.screen }
        )
    }

    @Test
    fun `secondary screens return to their contextual parent`() {
        assertEquals(AppScreen.MealPlan, parentScreenFor(AppScreen.Recipes))
        assertEquals(AppScreen.ShoppingList, parentScreenFor(AppScreen.Receipt))
        assertEquals(AppScreen.ShoppingList, parentScreenFor(AppScreen.ShoppingTools))
        assertEquals(AppScreen.Settings, parentScreenFor(AppScreen.DataManagement))
        assertEquals(AppScreen.Inventory, parentScreenFor(AppScreen.PastItems))
    }
}

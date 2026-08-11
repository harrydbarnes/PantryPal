package com.example.pantrypal

import com.example.pantrypal.ui.screens.OnboardingMeal
import com.example.pantrypal.ui.screens.onboardingMeals
import com.example.pantrypal.ui.screens.onboardingRegulars
import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingSetupTest {
    @Test
    fun regulars_areTrimmedDeduplicatedAndBlankEntriesAreRemoved() {
        assertEquals(listOf("milk", "Bread", "coffee"), onboardingRegulars(" milk, Bread, milk, , coffee "))
    }

    @Test
    fun meals_keepTheChosenDayWhileRemovingBlankAndDuplicateNames() {
        assertEquals(
            listOf(OnboardingMeal("Pasta", 1), OnboardingMeal("Curry", 5)),
            onboardingMeals(listOf(OnboardingMeal(" Pasta ", 1), OnboardingMeal("pasta", 3), OnboardingMeal("", 4), OnboardingMeal("Curry", 5)))
        )
    }
}

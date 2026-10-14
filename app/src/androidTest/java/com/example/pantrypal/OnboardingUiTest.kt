package com.example.pantrypal

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.pantrypal.ui.screens.*
import com.example.pantrypal.util.ShoppingReminderTiming
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class OnboardingUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun replayPreservesRemindersAndWaitsForKitchenSave() {
        var reminders: Boolean? = null
        var completed = false
        var saved: (() -> Unit)? = null
        compose.setContent { MaterialTheme {
            OnboardingScreen(isReplay = true, initialShoppingDay = 6, initialShoppingTimeMinutes = 600,
                initialShoppingReminderTiming = ShoppingReminderTiming.HOUR_BEFORE,
                initialShoppingRemindersEnabled = true,
                onSaveShoppingRoutine = { _, _, _, enabled -> reminders = enabled },
                onSaveKitchen = { _, _, _, done -> saved = done },
                onCompleteToMealPlan = { completed = true }, onSkip = {})
        } }
        repeat(4) { compose.onNodeWithText("Continue").performClick() }
        compose.onNodeWithText("Save and open meal plan").performClick()
        compose.runOnIdle { assertFalse(completed); assertNull(reminders); saved!!.invoke() }
        compose.runOnIdle { assertTrue(completed); assertEquals(true, reminders) }
    }

    @Test fun namedDinnerRequiresIngredientsAndPassesThemToSave() {
        var savedMeals = emptyList<OnboardingMeal>()
        compose.setContent { MaterialTheme {
            OnboardingScreen(isReplay = false, initialShoppingDay = 6, initialShoppingTimeMinutes = 600,
                initialShoppingReminderTiming = ShoppingReminderTiming.HOUR_BEFORE,
                onSaveShoppingRoutine = { _, _, _, _ -> },
                onSaveKitchen = { _, meals, _, _ -> savedMeals = meals },
                onCompleteToMealPlan = {}, onSkip = {})
        } }
        repeat(2) { compose.onNodeWithText("Continue").performClick() }
        compose.onNodeWithText("A dinner you plan to make").performTextInput("Rice bowl")
        compose.onNodeWithText("Continue").assertIsNotEnabled()
        compose.onNodeWithText("Shopping ingredients").performScrollTo().performTextInput("300 g rice, 2 peppers")
        captureReviewScreen(compose, "onboarding-dinner")
        repeat(2) { compose.onNodeWithText("Continue").performClick() }
        compose.onNodeWithText("Save and open meal plan").performClick()
        compose.runOnIdle { assertEquals(listOf("300 g rice", "2 peppers"), savedMeals.single().ingredients) }
    }
}

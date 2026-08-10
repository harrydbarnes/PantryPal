package com.example.pantrypal

import com.example.pantrypal.util.OnboardingGoal
import com.example.pantrypal.ui.screens.firstSuccessSupportingText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingGoalTest {
    @Test
    fun storedGoal_decodesEachSupportedChoice() {
        OnboardingGoal.entries.forEach { goal ->
            assertEquals(goal, OnboardingGoal.fromStoredValue(goal.name))
        }
    }

    @Test
    fun missingOrUnknownStoredGoal_isCompatibleWithExistingUsers() {
        assertNull(OnboardingGoal.fromStoredValue(null))
        assertNull(OnboardingGoal.fromStoredValue("old_goal"))
    }

    @Test
    fun everyGoal_hasAConcreteFirstSuccess() {
        OnboardingGoal.entries.forEach { goal ->
            assertTrue(firstSuccessSupportingText(goal).isNotBlank())
        }
    }
}

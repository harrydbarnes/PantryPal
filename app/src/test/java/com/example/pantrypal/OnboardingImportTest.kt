package com.example.pantrypal

import com.example.pantrypal.util.onboardingNamesMissingFrom
import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingImportTest {
    @Test
    fun `existing regulars and meals are not added again during onboarding`() {
        assertEquals(
            listOf("Coffee"),
            onboardingNamesMissingFrom(
                existingNames = listOf("Milk", "Jacket potatoes"),
                incomingNames = listOf(" milk ", "Coffee", "coffee", "Jacket potato")
            )
        )
    }
}

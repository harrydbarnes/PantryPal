package com.example.pantrypal

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.pantrypal.data.entity.ShoppingItemEntity
import com.example.pantrypal.ui.screens.ShoppingListItemRow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ShoppingHapticTest {
    @get:Rule val compose = createComposeRule()

    @Test fun rowAndCheckboxTickOnceAndUntickingDoesNotPulse() {
        val pulses = mutableListOf<HapticFeedbackType>()
        val checked = mutableStateOf(false)
        val feedback = object : HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) { pulses += hapticFeedbackType }
        }
        compose.setContent {
            CompositionLocalProvider(LocalHapticFeedback provides feedback) {
                MaterialTheme {
                    ShoppingListItemRow(
                        ShoppingItemEntity(name = "Courgettes", isChecked = checked.value),
                        onToggle = { checked.value = !checked.value }, onEdit = {}, onDelete = {}
                    )
                }
            }
        }
        compose.onNodeWithText("Courgettes").performClick()
        compose.onNode(isToggleable()).assertIsOn()
        compose.runOnIdle { assertEquals(listOf(HapticFeedbackType.LongPress), pulses) }
        compose.onNode(isToggleable()).performClick()
        compose.onNode(isToggleable()).assertIsOff()
        compose.runOnIdle { assertEquals(1, pulses.size) }
        compose.onNode(isToggleable()).performClick()
        compose.onNode(isToggleable()).assertIsOn()
        compose.runOnIdle { assertEquals(2, pulses.size) }
    }
}

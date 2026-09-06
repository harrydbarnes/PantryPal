package com.example.pantrypal

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pantrypal.util.AppPreferences
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShoppingUiTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun emptySectionAcceptsItsFirstItemAndShoppingSurvivesRecreation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences(AppPreferences.FILE_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(AppPreferences.KEY_ONBOARDING_COMPLETE, true).commit()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.onNodeWithText("Shop").performClick()
            compose.onNode(hasScrollAction()).performScrollToNode(hasText("Add shopping section"))
            compose.onNodeWithText("Add shopping section").performClick()
            compose.onNodeWithText("Section name").performTextInput("QA first section")
            compose.onNodeWithText("Save").performClick()
            compose.onNode(hasScrollAction()).performScrollToNode(hasContentDescription("Add to QA first section"))
            compose.onNodeWithContentDescription("Add to QA first section").performClick()
            compose.onNodeWithText("Item").performTextInput("QA first item")
            compose.onNodeWithText("Save").performClick()
            compose.onNode(hasScrollAction()).performScrollToNode(hasText("QA first item"))
            compose.onNodeWithText("QA first item").assertIsDisplayed()
            scenario.recreate()
            compose.onNodeWithText("Shopping List").assertIsDisplayed()
            compose.onNode(hasScrollAction()).performScrollToNode(hasText("Shop by Aldi aisle"))
            compose.onNodeWithText("Shop by Aldi aisle").assertIsDisplayed()
        }
    }
}

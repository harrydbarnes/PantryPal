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
            compose.onNode(SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange)).performScrollToNode(hasText("Add shopping section"))
            compose.onNodeWithText("Add shopping section").performClick()
            compose.onNodeWithText("Section name").performTextInput("QA first section")
            compose.onNodeWithText("Save").performClick()
            compose.onNode(SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange)).performScrollToNode(hasContentDescription("Add to QA first section"))
            compose.onNodeWithContentDescription("Add to QA first section").performClick()
            compose.onNodeWithText("Item").performTextInput("QA first item")
            compose.onNodeWithText("Save").performClick()
            compose.onNode(SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange)).performScrollToNode(hasContentDescription("More options for QA first item"))
            compose.onNodeWithContentDescription("More options for QA first item").assertIsDisplayed()
            scenario.recreate()
            compose.onNodeWithText("Shopping List").assertIsDisplayed()
            compose.onNode(SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange)).performScrollToNode(hasText("Shop by Aldi aisle"))
            compose.onNodeWithText("Shop by Aldi aisle").assertIsDisplayed()
        }
    }
    @Test fun tabletMealDaysUseMoreThanOneColumn() {
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        fun shell(cmd: String) = android.os.ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(cmd)).bufferedReader().use { it.readText() }
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences(AppPreferences.FILE_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(AppPreferences.KEY_ONBOARDING_COMPLETE, true)
            .putBoolean(AppPreferences.KEY_MEAL_PLAN_INTRO_SEEN, true).commit()
        try {
            shell("wm size 1920x1200"); shell("wm density 160")
            ActivityScenario.launch(MainActivity::class.java).use {
                compose.onNodeWithText("Plan").performClick()
                compose.onNode(SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange))
                    .performScrollToNode(hasContentDescription("Add meal on Monday"))
                val monday = compose.onNodeWithContentDescription("Add meal on Monday").fetchSemanticsNode().boundsInRoot
                val tuesday = compose.onNodeWithContentDescription("Add meal on Tuesday").fetchSemanticsNode().boundsInRoot
                org.junit.Assert.assertTrue("Adjacent day columns expected", tuesday.left > monday.right)
                val output = java.io.File(context.getExternalFilesDir(null), "review-profile").apply { mkdirs() }
                java.io.File(output, "tablet-meal-plan.png").outputStream().use { stream ->
                    instrumentation.uiAutomation.takeScreenshot().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                }
            }
        } finally {
            shell("mkdir -p /sdcard/Download/pantrypal-profile")
            shell("cp -r ${java.io.File(context.getExternalFilesDir(null), "review-profile").absolutePath}/. /sdcard/Download/pantrypal-profile/")
            shell("wm size reset"); shell("wm density reset")
        }
    }
}

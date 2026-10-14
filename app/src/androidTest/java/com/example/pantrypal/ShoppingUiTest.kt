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
            compose.onNode(SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange)).performScrollToNode(hasText("Shop by aisle · My Aldi"))
            compose.onNodeWithText("Shop by aisle · My Aldi").assertIsDisplayed()
            val list = compose.onNode(SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange))
            list.performScrollToNode(hasContentDescription("More options for QA first item"))
            compose.onNodeWithText("QA first item").performClick()
            val app = context.applicationContext as PantryPalApplication
            compose.waitUntil(10000) {
                kotlinx.coroutines.runBlocking { app.database.shoppingListDao().snapshot().any { it.name == "QA first item" && it.isChecked } }
            }
            // The completion button exposes its visible action and count to accessibility.
            try {
                compose.waitUntil(10000) {
                    runCatching {
                        compose.onNode(hasText("Complete shop & restock (", substring = true)).assertIsDisplayed()
                        true
                    }.getOrDefault(false)
                }
            } catch (failure: Throwable) {
                val output = java.io.File(context.getExternalFilesDir(null), "review-profile").apply { mkdirs() }
                java.io.File(output, "shopping-bottom-action.png").outputStream().use { stream ->
                    androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
                        .compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                }
                throw AssertionError("Bottom action layout:\n" + compose.onRoot(useUnmergedTree = true).printToString(), failure)
            }
            list.performScrollToNode(hasText("In trolley (", substring = true))
            compose.onNodeWithText("QA first item").assertDoesNotExist()
            compose.onNodeWithText("In trolley (", substring = true).performClick()
            list.performScrollToNode(hasContentDescription("More options for QA first item"))
            compose.onNodeWithContentDescription("More options for QA first item").assertIsDisplayed()
            compose.onNodeWithText("QA first item").performClick()
        }
    }
    @Test fun quickEntryDraftSurvivesNavigationAndRecreationAndShopModeIsScoped() {
        val app = ApplicationProvider.getApplicationContext<PantryPalApplication>()
        app.getSharedPreferences(AppPreferences.FILE_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(AppPreferences.KEY_ONBOARDING_COMPLETE, true).putString("shopping_quick_add_draft", "").commit()
        fun hasScreenOn(view: android.view.View): Boolean = view.keepScreenOn ||
            (view is android.view.ViewGroup && (0 until view.childCount).any { hasScreenOn(view.getChildAt(it)) })
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.onNodeWithText("Shop").performClick()
            compose.onNodeWithText("Quick add").performTextInput("500 g QA pasta")
            captureShoppingScreenshot(app, "shopping-quick-add-keyboard.png")
            compose.onNodeWithContentDescription("Keep screen on: disabled").performClick()
            compose.onNodeWithContentDescription("Keep screen on: enabled").assertIsDisplayed()
            scenario.onActivity { org.junit.Assert.assertTrue(hasScreenOn(it.window.decorView)) }
            scenario.recreate()
            compose.onNodeWithText("Quick add").assertTextContains("500 g QA pasta")
            compose.onNodeWithText("Pantry").performClick()
            scenario.onActivity { org.junit.Assert.assertFalse(hasScreenOn(it.window.decorView)) }
            compose.onNodeWithText("Shop").performClick()
            compose.onNodeWithText("Quick add").assertTextContains("500 g QA pasta")
            compose.onNodeWithText("Quick add").performTextClearance()
            compose.onNodeWithText("Quick add").performImeAction()
            captureShoppingScreenshot(app, "shopping-docked-entry.png")
        }
    }

    private fun captureShoppingScreenshot(context: Context, name: String) {
        compose.waitForIdle()
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        val directory = java.io.File(context.getExternalFilesDir(null), "review-profile").apply { mkdirs() }
        val file = java.io.File(directory, name)
        file.outputStream().use { instrumentation.uiAutomation.takeScreenshot()
            .compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        fun shell(command: String) = android.os.ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand(command)).use { it.readBytes() }
        shell("mkdir -p /sdcard/Download/pantrypal-profile")
        shell("cp ${file.absolutePath} /sdcard/Download/pantrypal-profile/$name")
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

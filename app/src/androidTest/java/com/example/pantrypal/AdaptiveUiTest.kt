package com.example.pantrypal

import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.pantrypal.domain.recipe.Recipe
import com.example.pantrypal.ui.screens.*
import java.io.File
import org.junit.*

/** Reproducible layout evidence and a focused, single-run emulator frame snapshot. */
class AdaptiveUiTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val output get() = File(instrumentation.targetContext.getExternalFilesDir(null), "review-profile").apply { mkdirs() }
    private fun shell(command: String): String = ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command)).bufferedReader().use { it.readText() }
    private fun screenshot(name: String) {
        compose.waitForIdle()
        File(output, "$name.png").outputStream().use { instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    @After fun reset() { shell("wm size reset"); shell("wm density reset"); shell("settings put system font_scale 1.0") }

    @Test fun failedAddRetainsInputAndDoesNotDismiss() {
        var error by mutableStateOf<String?>(null)
        var dismissed = false
        compose.setContent { MaterialTheme {
            AddScreen(error = error, onCancel = { dismissed = true }, onAdd = { _, _, _, _, _, _, _, _, _, _, _, _ -> error = "Injected save failure" })
        } }
        compose.onNodeWithText("Name").performTextInput("Keep this draft")
        compose.onNodeWithText("Add item").performClick()
        compose.onNodeWithText("Keep this draft").assertIsDisplayed()
        compose.onNodeWithText("Injected save failure").assertIsDisplayed()
        Assert.assertFalse(dismissed)
    }

    @Test fun tabletRecipePanesAndScrollCapture() {
        shell("wm size 1920x1200"); shell("wm density 160")
        val recipes = (1..200).map { Recipe(id = it.toLong(), title = "Recipe $it", ingredients = emptyList()) }
        var selected by mutableStateOf<Recipe?>(recipes.first())
        compose.setContent { MaterialTheme {
            RecipeScreen(state = RecipeScreenState(savedRecipes = recipes, selectedRecipe = selected),
                onSearchQueryChange = {}, onExternalSearch = { _, _ -> }, onImportUrl = {},
                onRecipeSelected = { r, _ -> selected = r }, onRecipeDismissed = { selected = null },
                onImportPreviewDismissed = {}, onSaveRecipe = {}, onToggleFavourite = { _, _ -> },
                onRateRecipe = { _, _ -> }, onMarkCooked = {}, onOpenSource = {}, onAddToPlan = {}, onAddMissingToShopping = { _, _ -> })
        } }
        compose.onNodeWithTag("recipe-detail-pane").assertIsDisplayed()
        screenshot("tablet-recipes")
        compose.runOnIdle { selected = null }
        shell("dumpsys gfxinfo com.example.pantrypal reset")
        repeat(12) { compose.onNodeWithTag("recipe-list").performTouchInput { swipeUp(durationMillis = 250) } }
        File(output, "recipe-scroll-gfxinfo.txt").writeText(shell("dumpsys gfxinfo com.example.pantrypal"))
        File(output, "recipe-scroll-frames.txt").writeText(shell("dumpsys gfxinfo com.example.pantrypal framestats"))
        File(output, "recipe-scroll-memory.txt").writeText(shell("dumpsys meminfo com.example.pantrypal"))
        screenshot("tablet-recipe-scroll")
        File(output, "environment.txt").writeText("API 35 CI emulator, debug APK, 1920x1200 at 160 dpi, 200 recipes, 12 upward swipes of 250ms, one run. Absolute timings are not phone measurements.\n" + shell("getprop ro.build.fingerprint"))
    }
    @Test fun landscapeLargeFontUsesReachableRecipeDialog() {
        shell("wm size 1200x800"); shell("wm density 160"); shell("settings put system font_scale 1.5")
        val recipe = Recipe(id = 1, title = "Large text recipe", ingredients = emptyList(), instructions = (1..30).map { "Step $it: prepare the ingredients and cook." })
        compose.setContent { MaterialTheme {
            RecipeScreen(state = RecipeScreenState(savedRecipes = listOf(recipe), selectedRecipe = recipe),
                onSearchQueryChange = {}, onExternalSearch = { _, _ -> }, onImportUrl = {},
                onRecipeSelected = { _, _ -> }, onRecipeDismissed = {},
                onImportPreviewDismissed = {}, onSaveRecipe = {}, onToggleFavourite = { _, _ -> },
                onRateRecipe = { _, _ -> }, onMarkCooked = {}, onOpenSource = {}, onAddToPlan = {}, onAddMissingToShopping = { _, _ -> })
        } }
        compose.onNodeWithTag("recipe-detail-pane").assertDoesNotExist()
        compose.onNodeWithText("Done").assertIsDisplayed()
        screenshot("landscape-recipes-font-150")
    }
}

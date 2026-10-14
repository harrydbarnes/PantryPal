package com.example.pantrypal

import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.pantrypal.data.dao.InventoryWithItemMap
import com.example.pantrypal.domain.recipe.*
import com.example.pantrypal.ui.screens.*
import com.example.pantrypal.util.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

internal fun captureReviewScreen(compose: ComposeContentTestRule, name: String) {
    compose.waitForIdle()
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val output = File(instrumentation.targetContext.getExternalFilesDir(null), "review-profile").apply { mkdirs() }
    File(output, "$name.png").outputStream().use { instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it) }
    android.os.ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(
        "mkdir -p /sdcard/Download/pantrypal-profile && cp ${output.absolutePath}/$name.png /sdcard/Download/pantrypal-profile/"
    )).use { it.readBytes() }
}

class KitchenWorkflowUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun cookingScalesPortionsAndRetainsReviewOnFailure() {
        val recipe = Recipe(id = 42, title = "Rice bowl", servings = 4.0, ingredients = listOf(RecipeIngredientNormalizer.parse("600 g rice")))
        val stock = InventoryWithItemMap(inventoryId = 1, itemId = 1, quantity = 800.0, unit = "g", addedDate = 0,
            expirationDate = null, name = "Rice", barcode = null, defaultUnit = "g", category = "General",
            isVegetarian = false, isGlutenFree = false, isUsual = false, imageUrl = null, createdAt = 0)
        var error by mutableStateOf<String?>(null)
        var used: Double? = null
        var dismissed = false
        compose.setContent { MaterialTheme {
            CookingReviewDialog(CookingReview(recipe, listOf(stock), "B"), saving = false, error = error,
                onDismiss = { dismissed = true }, onSave = { uses, _ -> used = uses.single().second; error = "Injected save failure" })
        } }
        compose.onNodeWithText("Servings cooked").performTextReplacement("2")
        compose.onNodeWithText("Confirm usage").performClick()
        compose.runOnIdle { assertEquals(300.0, used!!, 0.0); assertFalse(dismissed) }
        compose.onNodeWithText("Injected save failure").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("300.0").assertExists()
        captureReviewScreen(compose, "cooking-usage-review")
    }

    @Test fun settingsExplainsCurrentHouseholdBoundary() {
        compose.setContent { MaterialTheme {
            SettingsScreen(settings = AppSettings(), hasSeenSettingsIntro = true, onSettingsIntroSeen = {},
                notificationPermissionGranted = true, notificationPermissionRequired = true,
                onThemeModeChange = {}, onDynamicColorChange = {}, onExpiryRemindersChange = {},
                onShoppingRemindersChange = {}, onShoppingDayChange = {}, onShoppingTimeChange = {}, onShoppingReminderTimingChange = {},
                onNearbyShoppingRemindersChange = {}, shoppingLocations = emptyList(), locationPermissionGranted = false,
                backgroundLocationPermissionGranted = false, onRequestLocationPermission = {}, onOpenLocationSettings = {},
                onAddShoppingLocation = {}, onDeleteShoppingLocation = {}, onOpenNotificationSettings = {},
                onLaunchOnboarding = {}, householdConnected = true)
        } }
        compose.onNodeWithText("Household shopping sync connected").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("No account or cloud sync").assertDoesNotExist()
        captureReviewScreen(compose, "settings-household-data")
    }
}

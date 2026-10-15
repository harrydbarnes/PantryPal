package com.example.pantrypal

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.pantrypal.ui.screens.ShopThemeHost
import com.example.pantrypal.util.AppPreferences
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ShopThemeUiTest {
    @get:Rule val compose = createComposeRule()
    @Before fun reset() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences(AppPreferences.FILE_NAME, Context.MODE_PRIVATE).edit()
            .remove("shop_theme_test_palette").remove("shop_theme_test_nearby").remove("shop_theme_test_spot").commit()
    }
    @Test fun manualThemeAndMissingLocationFallbackAreExplicit() {
        compose.setContent { MaterialTheme {
            ShopThemeHost("test", emptyList()) { controls ->
                Column(Modifier.verticalScroll(rememberScrollState())) { controls() }
            }
        } }
        compose.onNodeWithText("Shop theme").performClick()
        compose.onNodeWithText("Tesco").performClick()
        compose.onNodeWithText("Tesco colours · Theme").assertExists()
        captureReviewScreen(compose, "shop-theme-light")
        compose.onNodeWithText("Only near saved shop").performScrollTo().performClick()
        compose.onNodeWithText("Choose a saved shop to activate its colours.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Shop theme · Waiting for shop").assertExists()
        compose.onNodeWithText("While shopping").performScrollTo().performClick()
        compose.onNodeWithText("Tesco colours · Theme").assertExists()
        compose.onNodeWithText("PantryPal", substring = false).performScrollTo().performClick()
        compose.onNodeWithText("Shop theme").assertExists()
    }
}

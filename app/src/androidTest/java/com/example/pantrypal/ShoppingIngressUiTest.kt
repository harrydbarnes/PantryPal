package com.example.pantrypal

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import com.example.pantrypal.util.AppPreferences
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

class ShoppingIngressUiTest {
    @get:Rule val compose = createEmptyComposeRule()
    @Test fun keepSharesReuseActivityWithoutReplayingOnRecreation() {
        val app = ApplicationProvider.getApplicationContext<PantryPalApplication>()
        app.getSharedPreferences(AppPreferences.FILE_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(AppPreferences.KEY_ONBOARDING_COMPLETE, true).remove("shopping_preparation_draft").commit()
        val name = "QA courgettes " + java.util.UUID.randomUUID().toString().take(6)
        val share = Intent(app, MainActivity::class.java).setAction(Intent.ACTION_SEND).setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, "[ ] $name x2\n[x] Already bought")
        ActivityScenario.launch<MainActivity>(share).use { scenario ->
            compose.waitUntil(10000) { runBlocking { app.database.shoppingListDao().snapshot().any { it.name == name } } }
            compose.onNodeWithText("Shopping List").assertIsDisplayed()
            // A committed database row precedes the asynchronous snackbar publication/animation.
            compose.waitUntil(10000) {
                try { compose.onNodeWithText("Added 1 new item; updated 0 quantities; 0 already on your list").assertIsDisplayed(); true }
                catch (_: AssertionError) { false }
            }
            scenario.recreate()
            assertEquals(2.0, runBlocking { app.database.shoppingListDao().snapshot().first { it.name == name }.quantity }, 0.0)
            val voiceName = "QA bread " + java.util.UUID.randomUUID().toString().take(6)
            // ActivityScenario tracks action/data/type. Keep the same action while testing a new delivery.
            scenario.onActivity { activity -> activity.startActivity(Intent(activity, MainActivity::class.java)
                .setAction(Intent.ACTION_SEND).setType("text/plain").addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(Intent.EXTRA_TEXT, "[ ] $voiceName x3")) }
            compose.waitUntil(10000) { runBlocking { app.database.shoppingListDao().snapshot().any { it.name == voiceName } } }
            assertEquals("KEEP_SHARE", runBlocking { app.database.shoppingListDao().snapshot().first { it.name == voiceName }.source })
            scenario.recreate()
            assertEquals(3.0, runBlocking { app.database.shoppingListDao().snapshot().first { it.name == voiceName }.quantity }, 0.0)
        }
    }
    @Test fun voiceIntentsReuseActivityAndRestoreWithoutReimporting() {
        val app = ApplicationProvider.getApplicationContext<PantryPalApplication>()
        app.getSharedPreferences(AppPreferences.FILE_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(AppPreferences.KEY_ONBOARDING_COMPLETE, true).remove("shopping_preparation_draft").commit()
        val first = "QA voice milk " + java.util.UUID.randomUUID().toString().take(6)
        val second = "QA voice bread " + java.util.UUID.randomUUID().toString().take(6)
        val voice = Intent(app, MainActivity::class.java).setAction(Intent.ACTION_INSERT)
            .putStringArrayListExtra("voice_item_names", arrayListOf("$first x2"))
        ActivityScenario.launch<MainActivity>(voice).use { scenario ->
            compose.waitUntil(10000) { runBlocking { app.database.shoppingListDao().snapshot().any { it.name == first } } }
            compose.onNodeWithText("Shopping List").assertIsDisplayed()
            // A committed database row precedes the asynchronous snackbar publication/animation.
            compose.waitUntil(10000) {
                try { compose.onNodeWithText("Added 1 new item; updated 0 quantities; 0 already on your list").assertIsDisplayed(); true }
                catch (_: AssertionError) { false }
            }
            scenario.recreate()
            assertEquals(2.0, runBlocking { app.database.shoppingListDao().snapshot().first { it.name == first }.quantity }, 0.0)
            scenario.onActivity { activity -> activity.startActivity(Intent(activity, MainActivity::class.java)
                .setAction(Intent.ACTION_INSERT).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra("voice_item_names", arrayOf("$second x3"))) }
            compose.waitUntil(10000) { runBlocking { app.database.shoppingListDao().snapshot().any { it.name == second } } }
            assertEquals("VOICE", runBlocking { app.database.shoppingListDao().snapshot().first { it.name == second }.source })
            scenario.recreate()
            assertEquals(3.0, runBlocking { app.database.shoppingListDao().snapshot().first { it.name == second }.quantity }, 0.0)
        }
    }
    @Test fun generatedAgentServiceIsRegisteredBehindSystemBindingPermission() {
        val app = ApplicationProvider.getApplicationContext<PantryPalApplication>()
        val name = android.content.ComponentName(app, "com.example.pantrypal.integration.PantryPalAgentService")
        @Suppress("DEPRECATION")
        val service = app.packageManager.getServiceInfo(name, android.content.pm.PackageManager.MATCH_DISABLED_COMPONENTS)
        assertEquals("android.permission.BIND_APP_FUNCTION_SERVICE", service.permission)
        assertEquals(android.os.Build.VERSION.SDK_INT >= 36, service.enabled)
        val xml = app.assets.open("pantry_pal_voice_functions.xml").bufferedReader().use { it.readText() }
        assertTrue(xml.contains("addShoppingItems"))
    }
}

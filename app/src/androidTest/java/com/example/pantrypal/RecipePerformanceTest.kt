package com.example.pantrypal

import android.graphics.Rect
import android.os.ParcelFileDescriptor
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.example.pantrypal.domain.recipe.Recipe
import com.example.pantrypal.ui.screens.*
import java.io.File
import org.junit.Test
import org.junit.Assert.assertTrue

/** No Compose test clock: shell gestures and frames run in real time. */
class RecipePerformanceTest {
    @Test fun recordRealTimeRecipeScrolling() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val automation = instrumentation.uiAutomation
        fun shell(command: String) = ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command)).bufferedReader().use { it.readText() }
        val output = File(instrumentation.targetContext.getExternalFilesDir(null), "review-profile").apply { mkdirs() }
        fun scrollingNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (node == null) return null
            if (node.isScrollable) return node
            for (i in 0 until node.childCount) scrollingNode(node.getChild(i))?.let { return it }
            return null
        }
        try {
            shell("wm size 1920x1200"); shell("wm density 160")
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                scenario.onActivity { activity -> activity.setContent { MaterialTheme {
                    RecipeScreen(state = RecipeScreenState(savedRecipes = (1..200).map { Recipe(id = it.toLong(), title = "Recipe $it", ingredients = emptyList()) }),
                        onSearchQueryChange = {}, onExternalSearch = { _, _ -> }, onImportUrl = {}, onRecipeSelected = { _, _ -> },
                        onRecipeDismissed = {}, onImportPreviewDismissed = {}, onSaveRecipe = {}, onToggleFavourite = { _, _ -> },
                        onRateRecipe = { _, _ -> }, onMarkCooked = {}, onOpenSource = {}, onAddToPlan = {}, onAddMissingToShopping = { _, _ -> })
                } } }
                automation.waitForIdle(500, 10000)
                val bounds = Rect()
                checkNotNull(scrollingNode(automation.rootInActiveWindow)) { "Recipe list was not accessible" }.getBoundsInScreen(bounds)
                val x = bounds.centerX()
                val bottom = bounds.top + bounds.height() * 4 / 5
                val top = bounds.top + bounds.height() / 5
                shell("dumpsys gfxinfo com.example.pantrypal reset")
                repeat(12) { shell("input swipe $x $bottom $x $top 250") }
                automation.waitForIdle(500, 10000)
                val gfx = shell("dumpsys gfxinfo com.example.pantrypal")
                File(output, "recipe-scroll-gfxinfo.txt").writeText(gfx)
                File(output, "recipe-scroll-frames.txt").writeText(shell("dumpsys gfxinfo com.example.pantrypal framestats"))
                File(output, "recipe-scroll-memory.txt").writeText(shell("dumpsys meminfo com.example.pantrypal"))
                File(output, "environment.txt").writeText("API 35 x86_64 CI emulator, debug APK, 1920x1200 at 160 dpi, 200 recipes, 12 real-time upward shell swipes of 250ms, one run, system animations disabled. Not a physical-phone benchmark.\n" + shell("getprop ro.build.fingerprint"))
                val frames = Regex("Total frames rendered: (\\d+)").find(gfx)?.groupValues?.get(1)?.toInt() ?: 0
                assertTrue("Insufficient rendered frames for a useful sample: $frames", frames >= 30)
            }
        } finally {
            shell("mkdir -p /sdcard/Download/pantrypal-profile")
            shell("cp -r ${output.absolutePath}/. /sdcard/Download/pantrypal-profile/")
            shell("wm size reset"); shell("wm density reset")
        }
    }
}

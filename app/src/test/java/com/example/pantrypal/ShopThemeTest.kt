package com.example.pantrypal

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.example.pantrypal.ui.theme.ShopPalette
import com.example.pantrypal.util.ShopThemeProximity
import org.junit.Assert.*
import org.junit.Test

class ShopThemeTest {
    private fun contrast(a: Color, b: Color): Float {
        val x = a.luminance(); val y = b.luminance()
        return (maxOf(x, y) + 0.05f) / (minOf(x, y) + 0.05f)
    }

    @Test fun everyShopHasReadableActionAndContainerPairsInBothModes() {
        ShopPalette.entries.filter { it != ShopPalette.NONE }.forEach { palette ->
            listOf(false, true).forEach { dark ->
                val base = if (dark) darkColorScheme() else lightColorScheme()
                val scheme = palette.applyTo(base, dark)
                listOf(scheme.primary to scheme.onPrimary,
                    scheme.primaryContainer to scheme.onPrimaryContainer,
                    scheme.secondary to scheme.onSecondary,
                    scheme.secondaryContainer to scheme.onSecondaryContainer).forEach { (bg, fg) ->
                    assertTrue("${palette.name} dark=$dark text contrast", contrast(bg, fg) >= 4.5f)
                }
                assertTrue("${palette.name} control contrast", contrast(scheme.primary, scheme.surface) >= 3f)
                assertEquals(base.error, scheme.error)
                assertEquals(base.onError, scheme.onError)
                assertEquals(base.surface, scheme.surface)
                assertEquals(base.onSurface, scheme.onSurface)
            }
        }
    }

    @Test fun defaultKeepsWallpaperPalette() {
        val base = lightColorScheme(primary = Color.Magenta)
        assertEquals(base, ShopPalette.NONE.applyTo(base, false))
    }

    @Test fun proximityRequiresFreshAccurateFixInsideWholeRadius() {
        assertTrue(ShopThemeProximity.isNear(50f, 20f, 10_000, 200f))
        assertFalse(ShopThemeProximity.isNear(190f, 20f, 0, 200f))
        assertFalse(ShopThemeProximity.isNear(0f, 500f, 0, 200f))
        assertFalse(ShopThemeProximity.isNear(0f, 1f, 90_001, 200f))
        assertFalse(ShopThemeProximity.isNear(0f, 1f, -1, 200f))
        assertFalse(ShopThemeProximity.isNear(Float.NaN, 1f, 0, 200f))
        assertFalse(ShopThemeProximity.isNear(0f, -1f, 0, 200f))
    }
}

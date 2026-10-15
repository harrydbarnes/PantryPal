package com.example.pantrypal.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/** Accents inspired by shops, deliberately adjusted for readable controls. No official branding. */
enum class ShopPalette(val label: String, val light: Long, val dark: Long) {
    NONE("PantryPal", 0xFF2F6B3E, 0xFF9AD5A5),
    TESCO("Tesco", 0xFF00539F, 0xFFA6C8FF),
    ALDI("Aldi", 0xFF004C88, 0xFF9ECBFF),
    LIDL("Lidl", 0xFF0050AA, 0xFFA8C7FF),
    SAINSBURYS("Sainsbury’s", 0xFF934600, 0xFFFFB77D),
    ASDA("Asda", 0xFF426900, 0xFFABD65E),
    MORRISONS("Morrisons", 0xFF006747, 0xFF76DBAD),
    WAITROSE("Waitrose", 0xFF526500, 0xFFC2D773),
    M_AND_S("M&S", 0xFF4B6138, 0xFFB0D194),
    CO_OP("Co-op", 0xFF00677E, 0xFF79D3ED);

    fun applyTo(base: ColorScheme, darkTheme: Boolean): ColorScheme {
        if (this == NONE) return base
        val accent = Color(if (darkTheme) dark else light)
        val onAccent = if (darkTheme) Color(0xFF101510) else Color.White
        // Keep semantic errors, neutral surfaces and all their foregrounds unchanged.
        return base.copy(primary = accent, onPrimary = onAccent,
            primaryContainer = accent, onPrimaryContainer = onAccent,
            secondary = accent, onSecondary = onAccent,
            secondaryContainer = accent, onSecondaryContainer = onAccent,
            surfaceTint = accent, inversePrimary = Color(if (darkTheme) light else dark))
    }
}

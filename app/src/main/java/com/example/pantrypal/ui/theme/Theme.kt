package com.example.pantrypal.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9AD5A5),
    onPrimary = Color(0xFF003918),
    primaryContainer = Color(0xFF19512C),
    onPrimaryContainer = Color(0xFFB5F2BF),
    secondary = Color(0xFFB7CCB5),
    secondaryContainer = Color(0xFF384B37),
    tertiary = Color(0xFFA2CED8),
    tertiaryContainer = Color(0xFF214E57)
)
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF356A42),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB7F2C1),
    onPrimaryContainer = Color(0xFF00210C),
    secondary = Color(0xFF526350),
    secondaryContainer = Color(0xFFD5E8D0),
    tertiary = Color(0xFF38656E),
    tertiaryContainer = Color(0xFFBCEBF5)
)

@Composable
fun PantryPalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            window.navigationBarColor = colorScheme.surfaceContainer.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

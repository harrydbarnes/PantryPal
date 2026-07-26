package com.example.pantrypal.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9AD5A5),
    onPrimary = Color(0xFF003918),
    primaryContainer = Color(0xFF19512C),
    onPrimaryContainer = Color(0xFFB5F2BF),
    secondary = Color(0xFFB7CCB5),
    onSecondary = Color(0xFF233425),
    secondaryContainer = Color(0xFF384B37),
    onSecondaryContainer = Color(0xFFD3E8D0),
    tertiary = Color(0xFFFFB59B),
    onTertiary = Color(0xFF54200E),
    tertiaryContainer = Color(0xFF71351F),
    onTertiaryContainer = Color(0xFFFFDBCE),
    background = Color(0xFF101510),
    onBackground = Color(0xFFE0E4DC),
    surface = Color(0xFF101510),
    onSurface = Color(0xFFE0E4DC),
    surfaceVariant = Color(0xFF414940),
    onSurfaceVariant = Color(0xFFC1C9BE),
    surfaceDim = Color(0xFF101510),
    surfaceBright = Color(0xFF363B35),
    surfaceContainerLowest = Color(0xFF0B0F0B),
    surfaceContainerLow = Color(0xFF181D18),
    surfaceContainer = Color(0xFF1C211C),
    surfaceContainerHigh = Color(0xFF262B26),
    surfaceContainerHighest = Color(0xFF313630),
    outline = Color(0xFF8B9388),
    outlineVariant = Color(0xFF414940)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2F6B3E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB6F2C1),
    onPrimaryContainer = Color(0xFF08210E),
    secondary = Color(0xFF53634F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6E8D1),
    onSecondaryContainer = Color(0xFF111F12),
    tertiary = Color(0xFF8A4D36),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDBCE),
    onTertiaryContainer = Color(0xFF351000),
    background = Color(0xFFF8FBF4),
    onBackground = Color(0xFF191D19),
    surface = Color(0xFFF8FBF4),
    onSurface = Color(0xFF191D19),
    surfaceVariant = Color(0xFFDDE5DA),
    onSurfaceVariant = Color(0xFF414941),
    surfaceDim = Color(0xFFD8DBD4),
    surfaceBright = Color(0xFFF8FBF4),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F5EE),
    surfaceContainer = Color(0xFFECEFE8),
    surfaceContainerHigh = Color(0xFFE6E9E2),
    surfaceContainerHighest = Color(0xFFE0E4DC),
    outline = Color(0xFF727970),
    outlineVariant = Color(0xFFC1C9BE)
)

private val BaseTypography = Typography()

private val PantryPalTypography = Typography(
    displayLarge = BaseTypography.displayLarge.copy(fontWeight = FontWeight.Bold),
    displayMedium = BaseTypography.displayMedium.copy(fontWeight = FontWeight.Bold),
    displaySmall = BaseTypography.displaySmall.copy(fontWeight = FontWeight.Bold),
    headlineLarge = BaseTypography.headlineLarge.copy(fontWeight = FontWeight.Bold),
    headlineMedium = BaseTypography.headlineMedium.copy(fontWeight = FontWeight.Bold),
    headlineSmall = BaseTypography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = BaseTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = BaseTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = BaseTypography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = BaseTypography.bodyLarge,
    bodyMedium = BaseTypography.bodyMedium,
    bodySmall = BaseTypography.bodySmall,
    labelLarge = BaseTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    labelMedium = BaseTypography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
    labelSmall = BaseTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
)

private val PantryPalShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
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
        typography = PantryPalTypography,
        shapes = PantryPalShapes,
        content = content
    )
}

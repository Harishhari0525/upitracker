package com.example.upitracker.util

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.upitracker.viewmodel.MainViewModel

// Assuming your AppLightColorScheme and AppDarkColorScheme are defined here as before
// For brevity, I'll skip re-pasting them, but ensure they are present in your file.


val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),      // Used by components like Chips, TextFields
    medium = RoundedCornerShape(20.dp),    // Used by components like Cards
    large = RoundedCornerShape(28.dp)      // Used by components like Modal sheets
)

private val AppLightColorScheme = lightColorScheme(
    primary = Color(0xFF006A6A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF70F7F7),
    onPrimaryContainer = Color(0xFF002020),
    secondary = Color(0xFFB95636),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDBCF),
    onSecondaryContainer = Color(0xFF4A1A0B),
    tertiary = Color(0xFF7E52A0), // New Purple Tertiary
    onTertiary = Color(0xFFFFFFFF), // Remains the same
    tertiaryContainer = Color(0xFFFADDFF), // New Purple Tertiary Container
    onTertiaryContainer = Color(0xFF31104A), // New Purple OnTertiaryContainer
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFFAFDFB),
    onBackground = Color(0xFF191C1B),
    surface = Color(0xFFF7FCFA),
    onSurface = Color(0xFF191C1B),
    surfaceVariant = Color(0xFFDAE5E3),
    onSurfaceVariant = Color(0xFF3F4948),
    outline = Color(0xFF6F7978),
    outlineVariant = Color(0xFFBEC9C7),
    inverseSurface = Color(0xFF2E3130),
    inverseOnSurface = Color(0xFFF0F1EF),
    inversePrimary = Color(0xFF4CDADA),
    surfaceTint = Color(0xFF006A6A),
    scrim = Color(0xFF000000)
)

private val AppDarkColorScheme = darkColorScheme(
    primary = Color(0xFF50D7D7),
    onPrimary = Color(0xFF003737),
    primaryContainer = Color(0xFF004F4F),
    onPrimaryContainer = Color(0xFF70F7F7),
    secondary = Color(0xFFFFB59C),
    onSecondary = Color(0xFF652916),
    secondaryContainer = Color(0xFF86402A),
    onSecondaryContainer = Color(0xFFFFDBCF),
    tertiary = Color(0xFFF9C63A),
    onTertiary = Color(0xFF402D00),
    tertiaryContainer = Color(0xFF5C4300),
    onTertiaryContainer = Color(0xFFFFDEA4),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF191C1B),
    onBackground = Color(0xFFE1E3E1),
    surface = Color(0xFF191C1B),
    onSurface = Color(0xFFE1E3E1),
    surfaceVariant = Color(0xFF3F4948),
    onSurfaceVariant = Color(0xFFBEC9C7),
    outline = Color(0xFF899391),
    outlineVariant = Color(0xFF3F4948),
    inverseSurface = Color(0xFFE1E3E1),
    inverseOnSurface = Color(0xFF191C1B),
    inversePrimary = Color(0xFF006A6A),
    surfaceTint = Color(0xFF50D7D7),
    scrim = Color(0xFF000000)
)

// Theme 2: Forest
val ForestLightColorScheme = lightColorScheme(
    primary = Color(0xFF386A20),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB7F397),
    onPrimaryContainer = Color(0xFF042100),
    secondary = Color(0xFF55624C),
    onSecondary = Color(0xFFFFFFFF),
    // ... add other colors or use generated ones
    background = Color(0xFFFDFDF5),
    surface = Color(0xFFFDFDF5),
)
val ForestDarkColorScheme = darkColorScheme(
    primary = Color(0xFF9CD67D),
    onPrimary = Color(0xFF0E3900),
    primaryContainer = Color(0xFF215106),
    onPrimaryContainer = Color(0xFFB7F397),
    secondary = Color(0xFFBDCBAF),
    onSecondary = Color(0xFF283420),
    // ... add other colors or use generated ones
    background = Color(0xFF1A1C18),
    surface = Color(0xFF1A1C18),
)

// Theme 3: Ocean
val OceanLightColorScheme = lightColorScheme(
    primary = Color(0xFF00658E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC7E7FF),
    onPrimaryContainer = Color(0xFF001E2E),
    secondary = Color(0xFF4F616E),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFCFCFF),
    surface = Color(0xFFFCFCFF),
)
val OceanDarkColorScheme = darkColorScheme(
    primary = Color(0xFF85CFFF),
    onPrimary = Color(0xFF00344C),
    primaryContainer = Color(0xFF004C6C),
    onPrimaryContainer = Color(0xFFC7E7FF),
    secondary = Color(0xFFB7C9D6),
    onSecondary = Color(0xFF21333E),
    background = Color(0xFF1A1C1E),
    surface = Color(0xFF1A1C1E),
)

// Theme 4: Rose
val RoseLightColorScheme = lightColorScheme(
    primary = Color(0xFF84436A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD8EC),
    onPrimaryContainer = Color(0xFF360024),
    secondary = Color(0xFF6B5861),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFFF7F8),
    surface = Color(0xFFFFF7F8),
)
val RoseDarkColorScheme = darkColorScheme(
    primary = Color(0xFFF8B0D9),
    onPrimary = Color(0xFF50133A),
    primaryContainer = Color(0xFF6A2C51),
    onPrimaryContainer = Color(0xFFFFD8EC),
    secondary = Color(0xFFD5BFCA),
    onSecondary = Color(0xFF3B2A32),
    background = Color(0xFF1F1A1C),
    surface = Color(0xFF1F1A1C),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Theme(
    mainViewModel: MainViewModel,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {

    val isDarkMode by mainViewModel.isDarkMode.collectAsState()
    val appTheme by mainViewModel.appTheme.collectAsState()
    val context = LocalContext.current

    val colorScheme: ColorScheme = when (appTheme) {
        // If the user chooses a specific theme, we MUST use it.
        AppTheme.FOREST -> if (isDarkMode) ForestDarkColorScheme else ForestLightColorScheme
        AppTheme.OCEAN -> if (isDarkMode) OceanDarkColorScheme else OceanLightColorScheme
        AppTheme.ROSE -> if (isDarkMode) RoseDarkColorScheme else RoseLightColorScheme

        // Only for the DEFAULT theme do we check for Dynamic Color.
        AppTheme.DEFAULT -> {
            if (isDarkMode) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = Color.Transparent.toArgb()
                // This check needs to know if the FINAL applied scheme is dark
                val finalSchemeIsDark = when (appTheme) {
                    AppTheme.DEFAULT -> isDarkMode
                    AppTheme.FOREST -> isDarkMode
                    AppTheme.OCEAN -> isDarkMode
                    AppTheme.ROSE -> isDarkMode
                }
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !finalSchemeIsDark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
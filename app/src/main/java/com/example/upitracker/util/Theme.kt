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

val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),      // Used by components like Chips, TextFields
    medium = RoundedCornerShape(20.dp),    // Used by components like Cards
    large = RoundedCornerShape(28.dp)      // Used by components like Modal sheets
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

// Theme 5: Lavender
val LavenderLightColorScheme = lightColorScheme(
    primary = Color(0xFF6A4FA3),
    secondary = Color(0xFF625B71),
    tertiary = Color(0xFF7D5260),
    background = Color(0xFFFEF7FF),
    surface = Color(0xFFFEF7FF)
)
val LavenderDarkColorScheme = darkColorScheme(
    primary = Color(0xFFD3BBFF),
    secondary = Color(0xFFCCC2DC),
    tertiary = Color(0xFFEFB8C8),
    background = Color(0xFF1D1B20),
    surface = Color(0xFF1D1B20)
)

// Theme 6: Sunset
val SunsetLightColorScheme = lightColorScheme(
    primary = Color(0xFF8F4C00),
    secondary = Color(0xFFBD5324),
    tertiary = Color(0xFF666000),
    background = Color(0xFFFFF7F4),
    surface = Color(0xFFFFF7F4)
)
val SunsetDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB786),
    secondary = Color(0xFFE38E64),
    tertiary = Color(0xFFD0C80B),
    background = Color(0xFF201A17),
    surface = Color(0xFF201A17)
)

// Theme 7: Mint
val MintLightColorScheme = lightColorScheme(
    primary = Color(0xFF006C4F),
    secondary = Color(0xFF3F6555),
    tertiary = Color(0xFF006874),
    background = Color(0xFFF4FFFA),
    surface = Color(0xFFF4FFFA)
)
val MintDarkColorScheme = darkColorScheme(
    primary = Color(0xFF76D9B4),
    secondary = Color(0xFFA7CEC0),
    tertiary = Color(0xFF4FD2E4),
    background = Color(0xFF171D1A),
    surface = Color(0xFF171D1A)
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Theme(
    mainViewModel: MainViewModel,
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
        AppTheme.LAVENDER -> if (isDarkMode) LavenderDarkColorScheme else LavenderLightColorScheme
        AppTheme.SUNSET -> if (isDarkMode) SunsetDarkColorScheme else SunsetLightColorScheme
        AppTheme.MINT -> if (isDarkMode) MintDarkColorScheme else MintLightColorScheme

        // Only for the DEFAULT theme do we check for Dynamic Color.
        AppTheme.DEFAULT -> {
            if (isDarkMode) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = Color.Transparent.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDarkMode
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
package com.example.upitracker.util

import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.upitracker.viewmodel.MainViewModel

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

private val ForestLightColorScheme = lightColorScheme(
    primary = Color(0xFF386A20),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8F8C6),
    onPrimaryContainer = Color(0xFF082100),
    secondary = Color(0xFF55624C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD9E7CB),
    onSecondaryContainer = Color(0xFF131F0D),
    tertiary = Color(0xFF386667),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFBCEBEC),
    onTertiaryContainer = Color(0xFF002021),
    background = Color(0xFFF8FAF2),
    onBackground = Color(0xFF191D16),
    surface = Color(0xFFF8FAF2),
    onSurface = Color(0xFF191D16),
    surfaceVariant = Color(0xFFE0E5D8),
    onSurfaceVariant = Color(0xFF43483E),
    outline = Color(0xFF73796D)
)

private val ForestDarkColorScheme = darkColorScheme(
    primary = Color(0xFFBCEFA2),
    onPrimary = Color(0xFF103800),
    primaryContainer = Color(0xFF205106),
    onPrimaryContainer = Color(0xFFD8F8C6),
    secondary = Color(0xFFBDCBAF),
    onSecondary = Color(0xFF283420),
    secondaryContainer = Color(0xFF3E4A35),
    onSecondaryContainer = Color(0xFFD9E7CB),
    tertiary = Color(0xFFA0CFD0),
    onTertiary = Color(0xFF003738),
    tertiaryContainer = Color(0xFF1E4E4F),
    onTertiaryContainer = Color(0xFFBCEBEC),
    background = Color(0xFF11140F),
    onBackground = Color(0xFFE1E4DA),
    surface = Color(0xFF11140F),
    onSurface = Color(0xFFE1E4DA),
    surfaceVariant = Color(0xFF43483E),
    onSurfaceVariant = Color(0xFFC4C8BC),
    outline = Color(0xFF8D9386)
)

private val OceanLightColorScheme = lightColorScheme(
    primary = Color(0xFF00658E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC7E7FF),
    onPrimaryContainer = Color(0xFF001E2E),
    secondary = Color(0xFF4F616E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD2E5F4),
    onSecondaryContainer = Color(0xFF0B1D29),
    tertiary = Color(0xFF66587A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFECDCFF),
    onTertiaryContainer = Color(0xFF211533),
    background = Color(0xFFFAFCFF),
    onBackground = Color(0xFF181C20),
    surface = Color(0xFFFAFCFF),
    onSurface = Color(0xFF181C20)
)

private val OceanDarkColorScheme = darkColorScheme(
    primary = Color(0xFF85CFFF),
    onPrimary = Color(0xFF00344C),
    primaryContainer = Color(0xFF004C6C),
    onPrimaryContainer = Color(0xFFC7E7FF),
    secondary = Color(0xFFB7C9D6),
    onSecondary = Color(0xFF21333E),
    secondaryContainer = Color(0xFF374955),
    onSecondaryContainer = Color(0xFFD2E5F4),
    tertiary = Color(0xFFD1BFE6),
    onTertiary = Color(0xFF372A4A),
    tertiaryContainer = Color(0xFF4E4161),
    onTertiaryContainer = Color(0xFFECDCFF),
    background = Color(0xFF101418),
    onBackground = Color(0xFFE0E3E8),
    surface = Color(0xFF101418),
    onSurface = Color(0xFFE0E3E8)
)

private val RoseLightColorScheme = lightColorScheme(
    primary = Color(0xFF894B6B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD8E9),
    onPrimaryContainer = Color(0xFF380723),
    secondary = Color(0xFF715763),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFBD9E7),
    onSecondaryContainer = Color(0xFF29151E),
    tertiary = Color(0xFF80543A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDBC8),
    onTertiaryContainer = Color(0xFF301400),
    background = Color(0xFFFFF7F9),
    onBackground = Color(0xFF21191D),
    surface = Color(0xFFFFF7F9),
    onSurface = Color(0xFF21191D)
)

private val RoseDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB1D0),
    onPrimary = Color(0xFF541D38),
    primaryContainer = Color(0xFF6E3450),
    onPrimaryContainer = Color(0xFFFFD8E9),
    secondary = Color(0xFFDEC1CC),
    onSecondary = Color(0xFF402A34),
    secondaryContainer = Color(0xFF58404B),
    onSecondaryContainer = Color(0xFFFBD9E7),
    tertiary = Color(0xFFF3BB9A),
    onTertiary = Color(0xFF4A2811),
    tertiaryContainer = Color(0xFF643D25),
    onTertiaryContainer = Color(0xFFFFDBC8),
    background = Color(0xFF181115),
    onBackground = Color(0xFFEBDDE2),
    surface = Color(0xFF181115),
    onSurface = Color(0xFFEBDDE2)
)

private val LavenderLightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F)
)

private val LavenderDarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E0E9),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE6E0E9)
)

private val SunsetLightColorScheme = lightColorScheme(
    primary = Color(0xFF8B5000),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDDB7),
    onPrimaryContainer = Color(0xFF2C1600),
    secondary = Color(0xFF755A40),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDDBF),
    onSecondaryContainer = Color(0xFF2A1704),
    tertiary = Color(0xFF59642F),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDDE9A8),
    onTertiaryContainer = Color(0xFF171E00),
    background = Color(0xFFFFF8F3),
    onBackground = Color(0xFF201A14),
    surface = Color(0xFFFFF8F3),
    onSurface = Color(0xFF201A14)
)

private val SunsetDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB95C),
    onPrimary = Color(0xFF4A2800),
    primaryContainer = Color(0xFF693C00),
    onPrimaryContainer = Color(0xFFFFDDB7),
    secondary = Color(0xFFE4C0A0),
    onSecondary = Color(0xFF422B16),
    secondaryContainer = Color(0xFF5B412A),
    onSecondaryContainer = Color(0xFFFFDDBF),
    tertiary = Color(0xFFC1CD8F),
    onTertiary = Color(0xFF2C3405),
    tertiaryContainer = Color(0xFF424B1A),
    onTertiaryContainer = Color(0xFFDDE9A8),
    background = Color(0xFF17120D),
    onBackground = Color(0xFFECE0D4),
    surface = Color(0xFF17120D),
    onSurface = Color(0xFFECE0D4)
)

private val MintLightColorScheme = lightColorScheme(
    primary = Color(0xFF006C4F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF8EF8CE),
    onPrimaryContainer = Color(0xFF002117),
    secondary = Color(0xFF4C6358),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFE9DA),
    onSecondaryContainer = Color(0xFF092017),
    tertiary = Color(0xFF3D6374),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC0E9FC),
    onTertiaryContainer = Color(0xFF001F2A),
    background = Color(0xFFF6FFF9),
    onBackground = Color(0xFF171D1A),
    surface = Color(0xFFF6FFF9),
    onSurface = Color(0xFF171D1A)
)

private val MintDarkColorScheme = darkColorScheme(
    primary = Color(0xFF72DBB2),
    onPrimary = Color(0xFF003828),
    primaryContainer = Color(0xFF00513B),
    onPrimaryContainer = Color(0xFF8EF8CE),
    secondary = Color(0xFFB3CCBE),
    onSecondary = Color(0xFF1F352C),
    secondaryContainer = Color(0xFF354B42),
    onSecondaryContainer = Color(0xFFCFE9DA),
    tertiary = Color(0xFFA4CDDE),
    onTertiary = Color(0xFF073544),
    tertiaryContainer = Color(0xFF244C5B),
    onTertiaryContainer = Color(0xFFC0E9FC),
    background = Color(0xFF0F1512),
    onBackground = Color(0xFFDEE4DF),
    surface = Color(0xFF0F1512),
    onSurface = Color(0xFFDEE4DF)
)

@Composable
private fun selectedColorScheme(
    appTheme: AppTheme,
    isDarkMode: Boolean
): ColorScheme {
    val context = LocalContext.current

    return when (appTheme) {
        AppTheme.FOREST -> if (isDarkMode) ForestDarkColorScheme else ForestLightColorScheme
        AppTheme.OCEAN -> if (isDarkMode) OceanDarkColorScheme else OceanLightColorScheme
        AppTheme.ROSE -> if (isDarkMode) RoseDarkColorScheme else RoseLightColorScheme
        AppTheme.LAVENDER -> if (isDarkMode) LavenderDarkColorScheme else LavenderLightColorScheme
        AppTheme.SUNSET -> if (isDarkMode) SunsetDarkColorScheme else SunsetLightColorScheme
        AppTheme.MINT -> if (isDarkMode) MintDarkColorScheme else MintLightColorScheme
        AppTheme.DEFAULT -> {
            if (isDarkMode) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
    }
}

@Composable
fun Theme(
    mainViewModel: MainViewModel,
    content: @Composable () -> Unit
) {
    val userDarkMode by mainViewModel.isDarkMode.collectAsState()
    val appTheme by mainViewModel.appTheme.collectAsState()

    // Our explicit manual Dark Mode override fix
    val isDarkMode = userDarkMode

    val colorScheme = selectedColorScheme(appTheme, isDarkMode)
    val context = LocalContext.current

    // Modern API: Control system bar icon appearance without touching deprecated properties
    LaunchedEffect(isDarkMode) {
        val activity = context as? ComponentActivity ?: return@LaunchedEffect
        activity.enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ) { isDarkMode },
            navigationBarStyle = androidx.activity.SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ) { isDarkMode }
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
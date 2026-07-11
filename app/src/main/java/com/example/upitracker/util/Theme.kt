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
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(9.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(22.dp)
)

// Payment Pulse is deliberately stable across devices. Financial hierarchy and
// semantic colours should not change when Android's wallpaper changes.
private val PaymentPulseLightColorScheme = lightColorScheme(
    primary = Color(0xFF4B57DB),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE4E7FF),
    onPrimaryContainer = Color(0xFF171D69),
    secondary = Color(0xFF087F70),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD1F5EC),
    onSecondaryContainer = Color(0xFF003D35),
    tertiary = Color(0xFFE76562),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDAD8),
    onTertiaryContainer = Color(0xFF5B1112),
    error = Color(0xFFC44735),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD3),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF5F3FC),
    onBackground = Color(0xFF17162A),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF17162A),
    surfaceVariant = Color(0xFFE8E5F2),
    onSurfaceVariant = Color(0xFF5F5B70),
    outline = Color(0xFF817C93),
    outlineVariant = Color(0xFFD8D3E2),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1EEF9),
    surfaceContainer = Color(0xFFECE8F5),
    surfaceContainerHigh = Color(0xFFE5E1F0),
    surfaceContainerHighest = Color(0xFFDCD7E9)
)

private val PaymentPulseDarkColorScheme = darkColorScheme(
    primary = Color(0xFFBBC0FF),
    onPrimary = Color(0xFF202778),
    primaryContainer = Color(0xFF353D9F),
    onPrimaryContainer = Color(0xFFE1E3FF),
    secondary = Color(0xFF72DBC7),
    onSecondary = Color(0xFF00382F),
    secondaryContainer = Color(0xFF005047),
    onSecondaryContainer = Color(0xFF9AF8E3),
    tertiary = Color(0xFFFFB3AF),
    onTertiary = Color(0xFF68000A),
    tertiaryContainer = Color(0xFF8D2527),
    onTertiaryContainer = Color(0xFFFFDAD8),
    error = Color(0xFFFFB4A8),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD3),
    background = Color(0xFF0D0C19),
    onBackground = Color(0xFFEAE6F4),
    surface = Color(0xFF151326),
    onSurface = Color(0xFFEAE6F4),
    surfaceVariant = Color(0xFF47425B),
    onSurfaceVariant = Color(0xFFC9C3D8),
    outline = Color(0xFF9690A8),
    outlineVariant = Color(0xFF49445C),
    surfaceContainerLowest = Color(0xFF090811),
    surfaceContainerLow = Color(0xFF18152A),
    surfaceContainer = Color(0xFF211C38),
    surfaceContainerHigh = Color(0xFF2B2550),
    surfaceContainerHighest = Color(0xFF383064)
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

private val MonochromeLightColorScheme = lightColorScheme(
    primary = Color(0xFF2C3E50),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFECF0F1),
    onPrimaryContainer = Color(0xFF2C3E50),
    secondary = Color(0xFF7F8C8D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFBDC3C7),
    onSecondaryContainer = Color(0xFF2C3E50),
    tertiary = Color(0xFF34495E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEAEDED),
    onTertiaryContainer = Color(0xFF212F3D),
    background = Color(0xFFFDFEFE),
    onBackground = Color(0xFF1C2833),
    surface = Color(0xFFFDFEFE),
    onSurface = Color(0xFF1C2833),
    surfaceVariant = Color(0xFFE5E7E9),
    onSurfaceVariant = Color(0xFF566573),
    outline = Color(0xFF7F8C8D)
)

private val MonochromeDarkColorScheme = darkColorScheme(
    primary = Color(0xFFBDC3C7),
    onPrimary = Color(0xFF2C3E50),
    primaryContainer = Color(0xFF34495E),
    onPrimaryContainer = Color(0xFFECF0F1),
    secondary = Color(0xFF95A5A6),
    onSecondary = Color(0xFF2C3E50),
    secondaryContainer = Color(0xFF2C3E50),
    onSecondaryContainer = Color(0xFFBDC3C7),
    tertiary = Color(0xFFEAEDED),
    onTertiary = Color(0xFF2C3E50),
    tertiaryContainer = Color(0xFF2E4053),
    onTertiaryContainer = Color(0xFFECF0F1),
    background = Color(0xFF17202A),
    onBackground = Color(0xFFF2F4F4),
    surface = Color(0xFF17202A),
    onSurface = Color(0xFFF2F4F4),
    surfaceVariant = Color(0xFF2E4053),
    onSurfaceVariant = Color(0xFFBDC3C7),
    outline = Color(0xFF95A5A6)
)

private val GoldLightColorScheme = lightColorScheme(
    primary = Color(0xFF8A6D3B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF5E79E),
    onPrimaryContainer = Color(0xFF2B2000),
    secondary = Color(0xFF706040),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0E4C8),
    onSecondaryContainer = Color(0xFF2B210D),
    tertiary = Color(0xFF5D5A3A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE2DFB7),
    onTertiaryContainer = Color(0xFF1D1B00),
    background = Color(0xFFFFFDF9),
    onBackground = Color(0xFF1E1C18),
    surface = Color(0xFFFFFDF9),
    onSurface = Color(0xFF1E1C18),
    surfaceVariant = Color(0xFFEFEBE4),
    onSurfaceVariant = Color(0xFF4E4639),
    outline = Color(0xFF7F7667)
)

private val GoldDarkColorScheme = darkColorScheme(
    primary = Color(0xFFF7D57F),
    onPrimary = Color(0xFF453000),
    primaryContainer = Color(0xFF634600),
    onPrimaryContainer = Color(0xFFF5E79E),
    secondary = Color(0xFFD6C5A2),
    onSecondary = Color(0xFF3F3017),
    secondaryContainer = Color(0xFF57462B),
    onSecondaryContainer = Color(0xFFF0E4C8),
    tertiary = Color(0xFFC6C39D),
    onTertiary = Color(0xFF2F2C0D),
    tertiaryContainer = Color(0xFF464424),
    onTertiaryContainer = Color(0xFFE2DFB7),
    background = Color(0xFF1A1712),
    onBackground = Color(0xFFECE6DF),
    surface = Color(0xFF1A1712),
    onSurface = Color(0xFFECE6DF),
    surfaceVariant = Color(0xFF4E4639),
    onSurfaceVariant = Color(0xFFD2C5B4),
    outline = Color(0xFF9B8F80)
)

private val CyberLightColorScheme = lightColorScheme(
    primary = Color(0xFF9C27B0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF3E5F5),
    onPrimaryContainer = Color(0xFF4A0072),
    secondary = Color(0xFF00BCD4),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F7FA),
    onSecondaryContainer = Color(0xFF006064),
    tertiary = Color(0xFFE91E63),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFCE4EC),
    onTertiaryContainer = Color(0xFF880E4F),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF212121),
    surface = Color(0xFFFAFAFA),
    onSurface = Color(0xFF212121),
    surfaceVariant = Color(0xFFEEEEEE),
    onSurfaceVariant = Color(0xFF616161),
    outline = Color(0xFF9E9E9E)
)

private val CyberDarkColorScheme = darkColorScheme(
    primary = Color(0xFFE040FB),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF4A0072),
    onPrimaryContainer = Color(0xFFF3E5F5),
    secondary = Color(0xFF18FFFF),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF006064),
    onSecondaryContainer = Color(0xFFE0F7FA),
    tertiary = Color(0xFFFF4081),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF880E4F),
    onTertiaryContainer = Color(0xFFFCE4EC),
    background = Color(0xFF0D0D0D),
    onBackground = Color(0xFFF5F5F5),
    surface = Color(0xFF0D0D0D),
    onSurface = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFF212121),
    onSurfaceVariant = Color(0xFFBDBDBD),
    outline = Color(0xFF757575)
)

@Composable
private fun selectedColorScheme(
    appTheme: AppTheme,
    isDarkMode: Boolean
): ColorScheme {
    val context = LocalContext.current

    return if (isDarkMode) PaymentPulseDarkColorScheme else PaymentPulseLightColorScheme
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

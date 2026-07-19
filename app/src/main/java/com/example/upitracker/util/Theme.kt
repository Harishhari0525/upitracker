package com.example.upitracker.util

import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
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
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// UPI Tracker's blue identity is deliberately stable across devices. Financial hierarchy and
// semantic colors should not change when Android's wallpaper changes.
private val PaymentPulseLightColorScheme = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8E7FF),
    onPrimaryContainer = Color(0xFF312E81),
    secondary = Color(0xFF059669),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF065F46),
    tertiary = Color(0xFF818CF8),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE0E7FF),
    onTertiaryContainer = Color(0xFF312E81),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B),
    background = Color(0xFFF4F6FB),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF4F6FB),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0x1A0F172A),
    outlineVariant = Color(0x0F0F172A),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF9FAFD),
    surfaceContainer = Color(0xFFF2F4F9),
    surfaceContainerHigh = Color(0xFFE9ECF4),
    surfaceContainerHighest = Color(0xFFDDE2ED)
)

private val PaymentPulseDarkColorScheme = darkColorScheme(
    primary = Color(0xFF818CF8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF20204A),
    onPrimaryContainer = Color(0xFFF1F5F9),
    secondary = Color(0xFF34D399),
    onSecondary = Color(0xFF064E3B),
    secondaryContainer = Color(0xFF064E3B),
    onSecondaryContainer = Color(0xFF34D399),
    tertiary = Color(0xFFA78BFA),
    onTertiary = Color(0xFF2E1065),
    tertiaryContainer = Color(0xFF4C1D95),
    onTertiaryContainer = Color(0xFFEDE9FE),
    error = Color(0xFFFB7185),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF450A0A),
    onErrorContainer = Color(0xFFFB7185),
    background = Color.Black,
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF0A0A0F),
    onSurfaceVariant = Color(0x9996A0B2),
    outline = Color(0x1FFFFFFF),
    outlineVariant = Color(0x14FFFFFF),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0A0A0F),
    surfaceContainer = Color(0xFF0D0D14),
    surfaceContainerHigh = Color(0xFF14141E),
    surfaceContainerHighest = Color(0xFF1B1B27)
)

@Composable
fun Theme(
    mainViewModel: MainViewModel,
    content: @Composable () -> Unit
) {
    val userDarkMode by mainViewModel.isDarkMode.collectAsState()
    val isDarkMode = userDarkMode
    val colorScheme = if (isDarkMode) PaymentPulseDarkColorScheme else PaymentPulseLightColorScheme
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

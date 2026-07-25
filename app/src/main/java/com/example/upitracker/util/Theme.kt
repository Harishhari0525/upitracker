package com.example.upitracker.util

import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
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

fun getThemeColorScheme(palette: AppThemePalette, isDarkMode: Boolean): ColorScheme {
    return if (isDarkMode) {
        when (palette) {
            AppThemePalette.SAPPHIRE_BLUE -> darkColorScheme(
                primary = Color(0xFF818CF8),
                onPrimary = Color.White,
                primaryContainer = Color(0xFF20204A),
                onPrimaryContainer = Color(0xFFF1F5F9),
                secondary = Color(0xFF38BDF8),
                onSecondary = Color(0xFF032B45),
                secondaryContainer = Color(0xFF07476F),
                onSecondaryContainer = Color(0xFF7DD3FC),
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

            AppThemePalette.OBSIDIAN_CYAN -> darkColorScheme(
                primary = Color(0xFF38BDF8),
                onPrimary = Color(0xFF0F172A),
                primaryContainer = Color(0xFF0C4A6E),
                onPrimaryContainer = Color(0xFFE0F2FE),
                secondary = Color(0xFF60A5FA),
                onSecondary = Color(0xFF1E3A8A),
                secondaryContainer = Color(0xFF1E40AF),
                onSecondaryContainer = Color(0xFFDBEAFE),
                tertiary = Color(0xFF2DD4BF),
                onTertiary = Color(0xFF042F2E),
                tertiaryContainer = Color(0xFF134E4A),
                onTertiaryContainer = Color(0xFFCCFBF1),
                error = Color(0xFFF87171),
                onError = Color(0xFF450A0A),
                background = Color(0xFF050B14),
                onBackground = Color(0xFFF0F9FF),
                surface = Color(0xFF030712),
                onSurface = Color(0xFFF0F9FF),
                surfaceContainerLow = Color(0xFF08101E),
                surfaceContainer = Color(0xFF0C172B),
                surfaceContainerHigh = Color(0xFF13223D),
                surfaceContainerHighest = Color(0xFF1A2C4C)
            )

            AppThemePalette.VELVET_VIOLET -> darkColorScheme(
                primary = Color(0xFFA78BFA),
                onPrimary = Color(0xFF2E1065),
                primaryContainer = Color(0xFF4C1D95),
                onPrimaryContainer = Color(0xFFEDE9FE),
                secondary = Color(0xFF38BDF8),
                onSecondary = Color(0xFF032B45),
                secondaryContainer = Color(0xFF07476F),
                onSecondaryContainer = Color(0xFF7DD3FC),
                tertiary = Color(0xFFF472B6),
                onTertiary = Color(0xFF500724),
                error = Color(0xFFFB7185),
                background = Color(0xFF080512),
                onBackground = Color(0xFFF5F3FF),
                surface = Color(0xFF05030A),
                onSurface = Color(0xFFF5F3FF),
                surfaceContainerLow = Color(0xFF0D081D),
                surfaceContainer = Color(0xFF130C28),
                surfaceContainerHigh = Color(0xFF1E143B),
                surfaceContainerHighest = Color(0xFF281C4C)
            )

            AppThemePalette.SUNSET_AMBER -> darkColorScheme(
                primary = Color(0xFFFBBF24),
                onPrimary = Color(0xFF451A03),
                primaryContainer = Color(0xFF78350F),
                onPrimaryContainer = Color(0xFFFEF3C7),
                secondary = Color(0xFF38BDF8),
                onSecondary = Color(0xFF032B45),
                secondaryContainer = Color(0xFF07476F),
                onSecondaryContainer = Color(0xFF7DD3FC),
                tertiary = Color(0xFFFB923C),
                onTertiary = Color(0xFF431407),
                error = Color(0xFFF87171),
                background = Color(0xFF0E0B07),
                onBackground = Color(0xFFFFFBEB),
                surface = Color(0xFF080604),
                onSurface = Color(0xFFFFFBEB),
                surfaceContainerLow = Color(0xFF140F0A),
                surfaceContainer = Color(0xFF1C160F),
                surfaceContainerHigh = Color(0xFF2B2117),
                surfaceContainerHighest = Color(0xFF3B2E20)
            )

            AppThemePalette.EMERALD_MINT -> darkColorScheme(
                primary = Color(0xFF34D399),
                onPrimary = Color(0xFF064E3B),
                primaryContainer = Color(0xFF065F46),
                onPrimaryContainer = Color(0xFFD1FAE5),
                secondary = Color(0xFF38BDF8),
                onSecondary = Color(0xFF032B45),
                secondaryContainer = Color(0xFF07476F),
                onSecondaryContainer = Color(0xFF7DD3FC),
                tertiary = Color(0xFF2DD4BF),
                onTertiary = Color(0xFF042F2E),
                error = Color(0xFFFB7185),
                background = Color(0xFF030C08),
                onBackground = Color(0xFFECFDF5),
                surface = Color(0xFF020805),
                onSurface = Color(0xFFECFDF5),
                surfaceContainerLow = Color(0xFF071710),
                surfaceContainer = Color(0xFF0B2118),
                surfaceContainerHigh = Color(0xFF123124),
                surfaceContainerHighest = Color(0xFF194231)
            )
        }.copy(
            // Keep dark mode purely neutral and legible; palette identity comes from accents.
            background = Color.Black,
            onBackground = Color(0xFFF4F7FB),
            surface = Color.Black,
            onSurface = Color(0xFFF4F7FB),
            surfaceVariant = Color(0xFF1B1E24),
            onSurfaceVariant = Color(0xFFC1C7D0),
            outline = Color(0xFF727985),
            outlineVariant = Color(0xFF373C45),
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color(0xFF0A0B0E),
            surfaceContainer = Color(0xFF101217),
            surfaceContainerHigh = Color(0xFF181B21),
            surfaceContainerHighest = Color(0xFF22262E)
        )
    } else {
        when (palette) {
            AppThemePalette.SAPPHIRE_BLUE -> lightColorScheme(
                primary = Color(0xFF4F46E5),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFE8E7FF),
                onPrimaryContainer = Color(0xFF312E81),
                secondary = Color(0xFF2563EB),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFDBEAFE),
                onSecondaryContainer = Color(0xFF1E40AF),
                tertiary = Color(0xFF818CF8),
                error = Color(0xFFDC2626),
                background = Color(0xFFF4F6FB),
                onBackground = Color(0xFF0F172A),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF0F172A),
                surfaceContainerLow = Color(0xFFF9FAFD),
                surfaceContainer = Color(0xFFF2F4F9),
                surfaceContainerHigh = Color(0xFFE9ECF4),
                surfaceContainerHighest = Color(0xFFDDE2ED)
            )

            AppThemePalette.OBSIDIAN_CYAN -> lightColorScheme(
                primary = Color(0xFF0284C7),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFE0F2FE),
                onPrimaryContainer = Color(0xFF0369A1),
                secondary = Color(0xFF2563EB),
                onSecondary = Color.White,
                tertiary = Color(0xFF0D9488),
                error = Color(0xFFDC2626),
                background = Color(0xFFF0F9FF),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF0C4A6E),
                surfaceContainerLow = Color(0xFFF8FAFC),
                surfaceContainer = Color(0xFFF1F5F9),
                surfaceContainerHigh = Color(0xFFE2E8F0),
                surfaceContainerHighest = Color(0xFFCBD5E1)
            )

            AppThemePalette.VELVET_VIOLET -> lightColorScheme(
                primary = Color(0xFF7C3AED),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFEDE9FE),
                onPrimaryContainer = Color(0xFF5B21B6),
                secondary = Color(0xFF2563EB),
                onSecondary = Color.White,
                tertiary = Color(0xFFDB2777),
                error = Color(0xFFDC2626),
                background = Color(0xFFFAF5FF),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF3B0764),
                surfaceContainerLow = Color(0xFFFBF8FF),
                surfaceContainer = Color(0xFFF3E8FF),
                surfaceContainerHigh = Color(0xFFE9D5FF),
                surfaceContainerHighest = Color(0xFFDDD6FE)
            )

            AppThemePalette.SUNSET_AMBER -> lightColorScheme(
                primary = Color(0xFFD97706),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFFEF3C7),
                onPrimaryContainer = Color(0xFF92400E),
                secondary = Color(0xFF2563EB),
                onSecondary = Color.White,
                tertiary = Color(0xFFEA580C),
                error = Color(0xFFDC2626),
                background = Color(0xFFFFFBEB),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF451A03),
                surfaceContainerLow = Color(0xFFFFFDF5),
                surfaceContainer = Color(0xFFFEF9C3),
                surfaceContainerHigh = Color(0xFFFDE68A),
                surfaceContainerHighest = Color(0xFFFCD34D)
            )

            AppThemePalette.EMERALD_MINT -> lightColorScheme(
                primary = Color(0xFF059669),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFD1FAE5),
                onPrimaryContainer = Color(0xFF065F46),
                secondary = Color(0xFF2563EB),
                onSecondary = Color.White,
                tertiary = Color(0xFF0D9488),
                error = Color(0xFFDC2626),
                background = Color(0xFFECFDF5),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF064E3B),
                surfaceContainerLow = Color(0xFFF7FDF9),
                surfaceContainer = Color(0xFFE6F4EA),
                surfaceContainerHigh = Color(0xFFCEEAD6),
                surfaceContainerHighest = Color(0xFFA8DADC)
            )
        }
    }
}

@Composable
fun Theme(
    mainViewModel: MainViewModel,
    content: @Composable () -> Unit
) {
    val userDarkMode by mainViewModel.isDarkMode.collectAsState()
    val palette by mainViewModel.themePalette.collectAsState()
    val isDarkMode = userDarkMode
    val colorScheme = getThemeColorScheme(palette, isDarkMode)
    val context = LocalContext.current

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

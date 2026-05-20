package com.example.upitracker.util

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * App-wide spacing tokens.
 *
 * Keep UI spacing consistent instead of hardcoding 4.dp, 8.dp, 16.dp everywhere.
 */
@Immutable
data class ExpressiveSpacing(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 6.dp,
    val md: Dp = 10.dp,
    val lg: Dp = 14.dp,
    val xl: Dp = 18.dp,
    val xxl: Dp = 22.dp,
    val xxxl: Dp = 30.dp,
    val huge: Dp = 38.dp
)

@Immutable
data class ExpressiveCorners(
    val small: CornerBasedShape = RoundedCornerShape(10.dp),
    val medium: CornerBasedShape = RoundedCornerShape(14.dp),
    val large: CornerBasedShape = RoundedCornerShape(18.dp),
    val extraLarge: CornerBasedShape = RoundedCornerShape(24.dp),
    val hero: CornerBasedShape = RoundedCornerShape(28.dp)
)

@Immutable
data class ExpressiveElevation(
    val card: Dp = 1.dp,
    val cardPressed: Dp = 3.dp,
    val floating: Dp = 6.dp,
    val dialog: Dp = 8.dp
)

@Immutable
data class CompactTokens(
    val screenHorizontal: Dp = 14.dp,
    val cardHorizontal: Dp = 14.dp,
    val cardVertical: Dp = 10.dp,
    val sectionGap: Dp = 12.dp,
    val itemGap: Dp = 8.dp,
    val bottomPadding: Dp = 88.dp,
    val iconSmall: Dp = 18.dp,
    val iconMedium: Dp = 22.dp,
    val avatar: Dp = 34.dp
)

object ExpressiveTokens {
    val spacing = ExpressiveSpacing()
    val corners = ExpressiveCorners()
    val elevation = ExpressiveElevation()
    val compact = CompactTokens()
}

/**
 * Common screen padding.
 */
val ScreenPadding = PaddingValues(
    start = 16.dp,
    top = 16.dp,
    end = 16.dp,
    bottom = 96.dp
)

/**
 * Use this for large home/dashboard hero cards.
 */
@Composable
fun expressiveHeroGradient(): Brush {
    return Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.secondaryContainer
        )
    )
}

/**
 * Use this for subtle card background gradients.
 */
@Composable
fun expressiveSurfaceGradient(): Brush {
    return Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.surfaceContainerLow
        )
    )
}

/**
 * Finance semantic colors.
 * These are intentionally theme-aware.
 */
@Composable
fun debitColor(): Color {
    return MaterialTheme.colorScheme.error
}

@Composable
fun debitContainerColor(): Color {
    return MaterialTheme.colorScheme.errorContainer
}

@Composable
fun creditColor(): Color {
    return Color(0xFF1B7F4C)
}

@Composable
fun creditContainerColor(): Color {
    return Color(0xFFD8F8E6)
}

@Composable
fun warningColor(): Color {
    return Color(0xFF9A6700)
}

@Composable
fun warningContainerColor(): Color {
    return Color(0xFFFFE7A3)
}
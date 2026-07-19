package com.example.upitracker.util

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
    val xxs: Dp = 4.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 24.dp,
    val xxxl: Dp = 32.dp,
    val huge: Dp = 48.dp
)

@Immutable
data class ExpressiveCorners(
    val small: CornerBasedShape = RoundedCornerShape(10.dp),
    val medium: CornerBasedShape = RoundedCornerShape(14.dp),
    val large: CornerBasedShape = RoundedCornerShape(18.dp),
    val extraLarge: CornerBasedShape = RoundedCornerShape(22.dp),
    val hero: CornerBasedShape = RoundedCornerShape(28.dp)
)

@Immutable
data class ExpressiveElevation(
    val card: Dp = 0.dp,
    val cardPressed: Dp = 1.dp,
    val floating: Dp = 8.dp,
    val dialog: Dp = 10.dp
)

@Immutable
data class CompactTokens(
    val screenHorizontal: Dp = 20.dp,
    val cardHorizontal: Dp = 16.dp,
    val cardVertical: Dp = 16.dp,
    val sectionGap: Dp = 14.dp,
    val itemGap: Dp = 10.dp,
    val bottomPadding: Dp = 96.dp,
    val iconSmall: Dp = 20.dp,
    val iconMedium: Dp = 24.dp,
    val avatar: Dp = 40.dp
)

object ExpressiveTokens {
    val spacing = ExpressiveSpacing()
    val corners = ExpressiveCorners()
    val elevation = ExpressiveElevation()
    val compact = CompactTokens()
}

/**
 * Use this for large home/dashboard hero cards.
 */
@Composable
fun expressiveHeroGradient(): Brush {
    return Brush.linearGradient(
        colors = listOf(
            Color(0xFF4F46E5),
            Color(0xFF6366F1),
            Color(0xFF7C3AED),
            Color(0xFF22A7C7)
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
fun creditColor(): Color {
    return MaterialTheme.colorScheme.secondary
}

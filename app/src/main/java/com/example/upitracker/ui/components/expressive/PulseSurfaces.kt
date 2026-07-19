package com.example.upitracker.ui.components.expressive

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.example.upitracker.util.ExpressiveTokens

val PulseCyan = Color(0xFF22D3EE)
val PulseEmerald = Color(0xFF34D399)
val PulseCoral = Color(0xFFFB7185)
val PulseAmber = Color(0xFFFBBF24)
val PulseViolet = Color(0xFFA78BFA)
val PulseRose = Color(0xFFF472B6)

@Composable
fun PulseAmbientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val strength = if (isDark) 0.19f else 0.075f
            val firstCenter = Offset(size.width * 0.06f, size.height * 0.02f)
            val secondCenter = Offset(size.width * 0.94f, size.height * 0.13f)
            val radius = size.width * 0.9f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF6366F1).copy(alpha = strength), Color.Transparent),
                    center = firstCenter,
                    radius = radius
                ),
                radius = radius,
                center = firstCenter
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFA78BFA).copy(alpha = strength * 0.72f), Color.Transparent),
                    center = secondCenter,
                    radius = radius * 0.72f
                ),
                radius = radius * 0.72f,
                center = secondCenter
            )
        }
        content()
    }
}

@Composable
fun PulseGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = ExpressiveTokens.corners.extraLarge,
    selected: Boolean = false,
    content: @Composable () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = modifier.border(
            width = 1.dp,
            color = if (selected) scheme.primary.copy(alpha = 0.34f) else scheme.outlineVariant,
            shape = shape
        ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                scheme.primary.copy(alpha = 0.11f)
            } else {
                scheme.surfaceContainerLow.copy(alpha = 0.88f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        content = { content() }
    )
}

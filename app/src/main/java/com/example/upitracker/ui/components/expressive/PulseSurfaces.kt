package com.example.upitracker.ui.components.expressive

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.getValue
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

    val infiniteTransition = rememberInfiniteTransition(label = "ambient_orbs")
    val pulseOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 24f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_offset"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val strength = if (isDark) 0.22f else 0.08f
            val firstCenter = Offset(size.width * 0.06f + pulseOffset, size.height * 0.02f - pulseOffset)
            val secondCenter = Offset(size.width * 0.94f - pulseOffset, size.height * 0.15f + pulseOffset)
            val thirdCenter = Offset(size.width * 0.48f, size.height * 0.35f + pulseOffset * 0.5f)
            val radius = size.width * 0.95f

            // Primary Indigo ambient orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF6366F1).copy(alpha = strength), Color.Transparent),
                    center = firstCenter,
                    radius = radius
                ),
                radius = radius,
                center = firstCenter
            )
            // Secondary Violet ambient orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFA78BFA).copy(alpha = strength * 0.8f), Color.Transparent),
                    center = secondCenter,
                    radius = radius * 0.75f
                ),
                radius = radius * 0.75f,
                center = secondCenter
            )
            // Accent Cyan highlight orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(PulseCyan.copy(alpha = strength * 0.45f), Color.Transparent),
                    center = thirdCenter,
                    radius = radius * 0.5f
                ),
                radius = radius * 0.5f,
                center = thirdCenter
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
    val isDark = scheme.background.luminance() < 0.5f

    val borderColor = if (selected) {
        scheme.primary.copy(alpha = 0.45f)
    } else if (isDark) {
        Color.White.copy(alpha = 0.08f)
    } else {
        scheme.outlineVariant.copy(alpha = 0.5f)
    }

    val containerColor = if (selected) {
        scheme.primary.copy(alpha = 0.14f)
    } else if (isDark) {
        Color.White.copy(alpha = 0.05f)
    } else {
        scheme.surfaceContainerLow.copy(alpha = 0.92f)
    }

    Card(
        modifier = modifier.border(
            width = 1.dp,
            color = borderColor,
            shape = shape
        ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        content = { content() }
    )
}

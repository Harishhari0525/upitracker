package com.example.upitracker.util

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

fun Modifier.animateEnter(index: Int): Modifier = composed {
    val alpha = remember { Animatable(0f) }
    val initialOffset = with(LocalDensity.current) { 24.dp.toPx() }
    val translationY = remember(initialOffset) { Animatable(initialOffset) }

    LaunchedEffect(Unit) {
        delay((index.coerceAtLeast(0) * 40L).milliseconds)
        coroutineScope {
            launch {
                alpha.animateTo(1f, animationSpec = AppMotion.defaultEffects())
            }
            launch {
                translationY.animateTo(0f, animationSpec = AppMotion.defaultSpatial())
            }
        }
    }

    this.graphicsLayer {
        this.alpha = alpha.value
        this.translationY = translationY.value
    }
}

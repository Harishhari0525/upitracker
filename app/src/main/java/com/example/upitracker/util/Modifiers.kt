package com.example.upitracker.util

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

fun Modifier.animateEnter(index: Int): Modifier = composed {
    val alpha = remember { Animatable(0f) }
    val translationY = remember { Animatable(50f) } // Start 50px down

    LaunchedEffect(Unit) {
        // Stagger delay based on index (e.g., item 0 starts at 0ms, item 1 at 50ms...)
        val delay = index * 50

        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 300, delayMillis = delay, easing = LinearOutSlowInEasing)
        )
        translationY.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 300, delayMillis = delay, easing = LinearOutSlowInEasing)
        )
    }

    this.graphicsLayer {
        this.alpha = alpha.value
        this.translationY = translationY.value
    }
}

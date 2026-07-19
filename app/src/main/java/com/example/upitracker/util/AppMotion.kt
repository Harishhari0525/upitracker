package com.example.upitracker.util

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring

/**
 * App-level mirror of the Material 3 Expressive v0.14 motion tokens bundled
 * internally with Material 3 1.4.0. The public MotionScheme API is not exposed
 * by that stable artifact, so shared app motion consumes these values directly.
 */
object AppMotion {
    fun <T> defaultSpatial(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.8f,
        stiffness = 380f
    )

    fun <T> slowSpatial(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.8f,
        stiffness = 200f
    )

    fun <T> defaultEffects(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 1f,
        stiffness = 1600f
    )

    fun <T> fastEffects(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 1f,
        stiffness = 3800f
    )
}

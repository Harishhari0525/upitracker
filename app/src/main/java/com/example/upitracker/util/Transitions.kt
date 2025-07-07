package com.example.upitracker.util

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.IntOffset

// A more expressive, gentle spring for spatial transitions
private val gentleSpring = spring<IntOffset>(dampingRatio = 0.8f, stiffness = 200f)

/**
 * An expressive enter transition that slides and fades in from the right.
 * Uses a spring animation for a more natural feel.
 */
fun expressiveSlideIn(): EnterTransition {
    return slideInHorizontally(
        initialOffsetX = { 300 },
        animationSpec = gentleSpring
    ) + fadeIn(animationSpec = spring())
}

/**
 * An expressive exit transition that slides and fades out to the left.
 * Uses a spring animation for a more natural feel.
 */
fun expressiveSlideOut(): ExitTransition {
    return slideOutHorizontally(
        targetOffsetX = { -300 },
        animationSpec = gentleSpring
    ) + fadeOut(animationSpec = spring())
}

/**
 * An expressive enter transition that slides and fades in from the left.
 * (For pop/back navigation)
 */
fun expressivePopEnter(): EnterTransition {
    return slideInHorizontally(
        initialOffsetX = { -300 },
        animationSpec = gentleSpring
    ) + fadeIn(animationSpec = spring())
}

/**
 * An expressive exit transition that slides and fades out to the right.
 * (For pop/back navigation)
 */
fun expressivePopExit(): ExitTransition {
    return slideOutHorizontally(
        targetOffsetX = { 300 },
        animationSpec = gentleSpring
    ) + fadeOut(animationSpec = spring())
}
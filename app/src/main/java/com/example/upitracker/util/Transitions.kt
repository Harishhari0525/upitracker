package com.example.upitracker.util

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

private const val TRANSITION_DURATION = 240

/**
 * An expressive, fast enter transition that slides and fades in from the right.
 */
fun expressiveSlideIn(): EnterTransition {
    return slideInHorizontally(
        initialOffsetX = { (it * 0.15f).toInt() },
        animationSpec = tween(durationMillis = TRANSITION_DURATION, easing = FastOutSlowInEasing)
    ) + fadeIn(animationSpec = tween(durationMillis = TRANSITION_DURATION))
}

/**
 * An expressive exit transition that slides and fades out to the left.
 */
fun expressiveSlideOut(): ExitTransition {
    return slideOutHorizontally(
        targetOffsetX = { (-it * 0.15f).toInt() },
        animationSpec = tween(durationMillis = TRANSITION_DURATION, easing = FastOutSlowInEasing)
    ) + fadeOut(animationSpec = tween(durationMillis = TRANSITION_DURATION))
}

/**
 * An expressive enter transition that slides and fades in from the left.
 * (For pop/back navigation)
 */
fun expressivePopEnter(): EnterTransition {
    return slideInHorizontally(
        initialOffsetX = { (-it * 0.15f).toInt() },
        animationSpec = tween(durationMillis = TRANSITION_DURATION, easing = FastOutSlowInEasing)
    ) + fadeIn(animationSpec = tween(durationMillis = TRANSITION_DURATION))
}

/**
 * An expressive exit transition that slides and fades out to the right.
 * (For pop/back navigation)
 */
fun expressivePopExit(): ExitTransition {
    return slideOutHorizontally(
        targetOffsetX = { (it * 0.15f).toInt() },
        animationSpec = tween(durationMillis = TRANSITION_DURATION, easing = FastOutSlowInEasing)
    ) + fadeOut(animationSpec = tween(durationMillis = TRANSITION_DURATION))
}

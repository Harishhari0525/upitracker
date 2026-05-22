package com.example.upitracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.upitracker.R
import com.example.upitracker.util.ExpressiveTokens
import kotlinx.coroutines.delay

@Composable
fun LottieSplashScreen(
    onAnimationFinished: () -> Unit // Restored your exact required parameter
) {
    // 1. High-performance composition tracking
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.loading_animation)
    )

    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1
    )

    // 2. Clear the splash gate cleanly when the animation reaches completion
    LaunchedEffect(progress) {
        if (progress == 1f) {
            delay(150) // Tiny cushion to prevent visual thread stutters
            onAnimationFinished()
        }
    }

    // 3. Static hardware-accelerated background box container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.size(260.dp)
            )

            Spacer(modifier = Modifier.height(ExpressiveTokens.spacing.sm))

            Text(
                text = "UPI Expense Tracker",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
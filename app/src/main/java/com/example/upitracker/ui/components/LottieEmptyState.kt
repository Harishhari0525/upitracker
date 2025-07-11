package com.example.upitracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.*
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty

@Composable
fun LottieEmptyState(
    modifier: Modifier = Modifier,
    message: String,
    lottieResourceId: Int
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(lottieResourceId))

    // 1. Get the theme color you want to use
    val themeColor = MaterialTheme.colorScheme.primary

    // 2. Create dynamic properties to override the animation's colors
    val dynamicProperties = rememberLottieDynamicProperties(
        rememberLottieDynamicProperty(
            property = LottieProperty.COLOR, // The property we want to change
            value = themeColor.toArgb(),      // The new value (our theme color)
            keyPath = arrayOf("**")           // A wildcard to match all layers
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LottieAnimation(
            composition = composition,
            iterations = LottieConstants.IterateForever,
            modifier = Modifier.size(200.dp),
            // 3. Apply the dynamic properties to the animation
            dynamicProperties = dynamicProperties
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
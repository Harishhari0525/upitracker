package com.example.upitracker.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.upitracker.R

@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    onOnboardingComplete: (isUpiLiteEnabled: Boolean) -> Unit // ✨ Changed parameter name for clarity ✨
) {

    var upiLiteChecked by remember { mutableStateOf(true) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 48.dp),
            verticalArrangement = Arrangement.SpaceAround,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(0.5f))

            Image(
                imageVector = Icons.AutoMirrored.Filled.PlaylistAddCheck,
                contentDescription = stringResource(R.string.onboarding_welcome_icon_desc),
                modifier = Modifier.size(120.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
            )
            Spacer(Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.onboarding_welcome_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.onboarding_description),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { upiLiteChecked = !upiLiteChecked }
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = upiLiteChecked,
                    onCheckedChange = { upiLiteChecked = it }
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Track UPI Lite transactions?",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {onOnboardingComplete(upiLiteChecked)}, // ✨ Call the passed lambda ✨
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    stringResource(R.string.onboarding_get_started),
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
package com.example.upitracker.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.upitracker.R
import com.example.upitracker.util.ExpressiveTokens

@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    onOnboardingComplete: (isUpiLiteEnabled: Boolean) -> Unit
) {
    var upiLiteChecked by remember { mutableStateOf(true) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(ExpressiveTokens.spacing.lg)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(
                    ExpressiveTokens.spacing.lg,
                    Alignment.CenterVertically
                ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    shape = ExpressiveTokens.corners.extraLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = ExpressiveTokens.elevation.card
                    )
                ) {
                    Image(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistAddCheck,
                        contentDescription = stringResource(R.string.onboarding_welcome_icon_desc),
                        modifier = Modifier
                            .padding(ExpressiveTokens.spacing.xl)
                            .size(92.dp),
                        colorFilter = ColorFilter.tint(
                            MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_welcome_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(ExpressiveTokens.spacing.sm))

                    Text(
                        text = stringResource(R.string.onboarding_description),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.sm)
                ) {
                    OnboardingFeatureRow(
                        icon = Icons.Rounded.Sms,
                        title = "Automatic SMS tracking",
                        subtitle = "Detects UPI transactions from bank messages"
                    )

                    OnboardingFeatureRow(
                        icon = Icons.Rounded.AutoAwesome,
                        title = "Smart insights",
                        subtitle = "Shows trends, forecast, and recent activity"
                    )

                    OnboardingFeatureRow(
                        icon = Icons.Rounded.Lock,
                        title = "Private by design",
                        subtitle = "Your transaction data stays on your device"
                    )
                }

                UpiLiteOptionCard(
                    checked = upiLiteChecked,
                    onCheckedChange = { upiLiteChecked = it }
                )

                Button(
                    onClick = { onOnboardingComplete(upiLiteChecked) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = ExpressiveTokens.corners.large,
                    contentPadding = PaddingValues(horizontal = ExpressiveTokens.spacing.lg)
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_get_started),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingFeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    ListItem(
        modifier = Modifier.fillMaxWidth(),
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        headlineContent = {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold
            )
        },
        supportingContent = {
            Text(text = subtitle)
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    )
}

@Composable
private fun UpiLiteOptionCard(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        shape = ExpressiveTokens.corners.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = ExpressiveTokens.elevation.card
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = ExpressiveTokens.spacing.lg,
                    vertical = ExpressiveTokens.spacing.md
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange
            )

            Column(
                modifier = Modifier.padding(start = ExpressiveTokens.spacing.sm)
            ) {
                Text(
                    text = "Track UPI Lite transactions?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Enable this if you use UPI Lite for smaller payments.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
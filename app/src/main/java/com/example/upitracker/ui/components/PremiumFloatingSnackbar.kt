package com.example.upitracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun PremiumFloatingSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val currentSnackbarData = hostState.currentSnackbarData

    // ✨ AUTOMATIC DISMISS TIMER FIX: Forces the snackbar to dismiss itself after a set duration
    LaunchedEffect(currentSnackbarData) {
        if (currentSnackbarData != null) {
            delay(4000) // Keeps the toast pill visible for exactly 4 seconds
            currentSnackbarData.dismiss() // Safely clears the current message queue line
        }
    }

    val messageText = currentSnackbarData?.visuals?.message ?: ""
    val (backgroundColor, accentColor, leadingIcon) = remember(messageText) {
        when {
            messageText.isBlank() -> {
                Triple(Color.Transparent, Color.Transparent, Icons.Default.Info)
            }
            messageText.contains("success", ignoreCase = true) ||
                    messageText.contains("complete", ignoreCase = true) ||
                    messageText.contains("saved", ignoreCase = true) ||
                    messageText.contains("synced", ignoreCase = true) ||
                    messageText.contains("imported", ignoreCase = true) -> {
                Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), Icons.Default.CheckCircle)
            }
            messageText.contains("sync", ignoreCase = true) ||
                    messageText.contains("refresh", ignoreCase = true) -> {
                Triple(Color(0xFFFFF3E0), Color(0xFFEF6C00), Icons.Default.Sync)
            }
            messageText.contains("error", ignoreCase = true) ||
                    messageText.contains("fail", ignoreCase = true) ||
                    messageText.contains("denied", ignoreCase = true) -> {
                Triple(Color(0xFFFFEBEE), Color(0xFFC62828), Icons.Default.Error)
            }
            else -> {
                Triple(Color(0xFFF5F5F5), Color(0xFF424242), Icons.Default.Info)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = currentSnackbarData != null,
            enter = slideInVertically { it / 2 } + fadeIn(),
            exit = slideOutVertically { it / 2 } + fadeOut()
        ) {
            if (currentSnackbarData != null) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = backgroundColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )

                        Text(
                            text = currentSnackbarData.visuals.message,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = Color(0xFF1C1C1C),
                            modifier = Modifier.weight(1f)
                        )

                        currentSnackbarData.visuals.actionLabel?.let { label ->
                            Button(
                                onClick = { currentSnackbarData.performAction() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = accentColor,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text(
                                    text = label.uppercase(),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
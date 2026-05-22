package com.example.upitracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import com.example.upitracker.network.GitHubRelease
import com.example.upitracker.util.ExpressiveTokens

@Composable
fun WhatsNewDialog(
    release: GitHubRelease,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = ExpressiveTokens.corners.extraLarge,
        icon = {
            Icon(
                imageVector = Icons.Default.NewReleases,
                contentDescription = "New Release",
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Column(
                verticalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.xs)
            ) {
                Text(
                    text = "What's New",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = release.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.sm)
            ) {
                item {
                    Text(
                        text = release.body.ifBlank { "Bug fixes and improvements." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    uriHandler.openUri(release.htmlUrl)
                    onDismiss()
                },
                shape = ExpressiveTokens.corners.medium
            ) {
                Text("View on GitHub")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    )
}
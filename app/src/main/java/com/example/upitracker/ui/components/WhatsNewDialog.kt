package com.example.upitracker.ui.components

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import com.example.upitracker.network.GitHubRelease

@Composable
fun WhatsNewDialog(
    release: GitHubRelease,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.NewReleases, contentDescription = "New Release") },
        title = { Text(text = "What's New in ${release.name}") },
        text = {
            LazyColumn {
                item {
                    Text(text = release.body)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                uriHandler.openUri(release.htmlUrl)
                onDismiss()
            }) {
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
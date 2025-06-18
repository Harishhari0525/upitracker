package com.example.upitracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.upitracker.R
import com.example.upitracker.data.Transaction

@Composable
fun TransactionActionsDialog(
    transaction: Transaction, // Pass the transaction for context if needed in text, otherwise not strictly required by dialog structure
    onDismissRequest: () -> Unit,
    onArchiveClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.dialog_title_transaction_actions)) }, // Add this string resource
        text = { Text(stringResource(R.string.dialog_text_choose_action_for, transaction.description.take(50))) }, // Add this string resource, show part of description
        confirmButton = {
            // No traditional confirm button, actions are list items
            // TextButton(onClick = onDismissRequest) { Text("Cancel") } // Or keep a cancel button if preferred
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.dialog_button_cancel))
            }
        },
        icon = { Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.dialog_icon_desc_transaction_actions)) }, // Add this string resource
        content = {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                ActionItem(
                    icon = Icons.Filled.Archive,
                    text = stringResource(R.string.action_archive), // Add this string resource
                    onClick = {
                        onArchiveClick()
                        onDismissRequest() // Dismiss after action
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                ActionItem(
                    icon = Icons.Filled.Delete,
                    text = stringResource(R.string.action_delete), // Add this string resource
                    onClick = {
                        onDeleteClick()
                        onDismissRequest() // Dismiss after action
                    }
                )
            }
        }
    )
}

@Composable
private fun ActionItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface( // Use Surface for better click feedback and theming
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp), // Consistent padding
        shape = MaterialTheme.shapes.medium // Optional: for rounded corners
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Text(text = text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

// Remember to add the following string resources to your strings.xml:
// <string name="dialog_title_transaction_actions">Transaction Actions</string>
// <string name="dialog_text_choose_action_for">Choose an action for: %s</string>
// <string name="dialog_icon_desc_transaction_actions">Transaction Actions Icon</string>
// <string name="action_archive">Archive</string>
// <string name="action_delete">Delete</string>
// (and ensure dialog_button_cancel is present)
// <string name="dialog_button_cancel">Cancel</string>

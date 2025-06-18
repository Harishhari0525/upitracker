package com.example.upitracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
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
    transaction: Transaction,
    onDismissRequest: () -> Unit,
    onArchiveClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = { Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.dialog_icon_desc_transaction_actions)) },
        title = { Text(stringResource(R.string.dialog_title_transaction_actions)) },

        // ✨ FIX: Place your custom layout inside the `text` composable slot.
        text = {
            Column {
                Text(stringResource(R.string.dialog_text_choose_action_for, transaction.description.take(50)))
                Spacer(modifier = Modifier.height(16.dp))
                // This is the Column that was previously in the invalid `content` parameter
                ActionItem(
                    icon = Icons.Filled.Archive,
                    text = stringResource(R.string.action_archive),
                    onClick = {
                        onArchiveClick()
                        onDismissRequest() // Dismiss after action
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
                ActionItem(
                    icon = Icons.Filled.Delete,
                    text = stringResource(R.string.action_delete),
                    onClick = {
                        onDeleteClick()
                        onDismissRequest() // Dismiss after action
                    }
                )
            }
        },

        // ✨ FIX: Removed the redundant confirmButton. Only a dismiss/cancel button is needed.
        confirmButton = { }, // This is intentionally left empty
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.dialog_button_cancel))
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
    // ✨ FIX: The Surface should not have a clickable modifier, the Row inside it should.
    // Also, added background color to make the clickable area visible.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp), // Padding for the clickable area
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}
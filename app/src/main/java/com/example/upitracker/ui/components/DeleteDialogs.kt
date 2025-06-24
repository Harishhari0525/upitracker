package com.example.upitracker.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.upitracker.R

@Composable
fun DeleteTransactionConfirmationDialog(
    transactionDescription: String, // To show some context in the dialog
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.DeleteForever,
            contentDescription = stringResource(R.string.delete_transaction_dialog_title), tint = MaterialTheme.colorScheme.error) },
        title = { Text(stringResource(R.string.delete_transaction_dialog_title)) },
        text = { Text(stringResource(R.string.delete_transaction_dialog_message) +
                "\n\n\"${transactionDescription.take(50)}${if (transactionDescription.length > 50) "..." else ""}\"") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.button_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_button_cancel))
            }
        }
    )
}
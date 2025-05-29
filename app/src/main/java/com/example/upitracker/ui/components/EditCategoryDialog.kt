// File: app/src/main/java/com/example/upitracker/ui/components/EditCategoryDialog.kt
package com.example.upitracker.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.upitracker.R
import com.example.upitracker.data.Transaction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCategoryDialog(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onSaveCategory: (transactionId: Int, newCategory: String?) -> Unit
) {
    var categoryText by remember(transaction.id, transaction.category) { mutableStateOf(transaction.category ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.category_edit_icon_desc)) },
        title = {
            Text(
                if (transaction.category.isNullOrBlank()) stringResource(R.string.set_category_dialog_title)
                else stringResource(R.string.edit_category_dialog_title)
            )
        },
        text = {
            OutlinedTextField(
                value = categoryText,
                onValueChange = { categoryText = it },
                label = { Text(stringResource(R.string.category_label)) },
                placeholder = { Text(stringResource(R.string.category_textfield_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = {
                onSaveCategory(transaction.id, categoryText.trim().takeIf { it.isNotBlank() })
            }) {
                Text(stringResource(R.string.button_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_button_cancel))
            }
        }
    )
}
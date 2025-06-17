// File: app/src/main/java/com/example/upitracker/ui/components/EditCategoryDialog.kt
package com.example.upitracker.ui.components

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.upitracker.R
import com.example.upitracker.data.Transaction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.unit.dp

private val suggestionCategories = listOf(
    "Food", "Shopping", "Transport", "Bills", "Health", "Entertainment", "Groceries"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditCategoryDialog(
    transaction: Transaction,
    suggestionCategories: List<String>,
    onDismiss: () -> Unit,
    onSaveCategory: (transactionId: Int, newCategory: String?) -> Unit
) {
    var categoryText by remember { mutableStateOf(transaction.category ?: "") }

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
            Column {
                OutlinedTextField(
                value = categoryText,
                onValueChange = { categoryText = it },
                label = { Text(stringResource(R.string.category_label)) },
                placeholder = { Text(stringResource(R.string.category_textfield_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
                Spacer(Modifier.height(16.dp))
                Text("Suggestions", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    suggestionCategories.forEach { category ->
                        FilterChip(
                            selected = categoryText.equals(category, ignoreCase = true),
                            onClick = { categoryText = category },
                            label = { Text(category) }
                        )
                    }
                }
            }
               }
        ,
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
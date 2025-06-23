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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditCategoryDialog(
    transaction: Transaction,
    suggestionCategories: List<String>,
    onDismiss: () -> Unit,
    onSave: (transactionId: Int, newDescription: String, newAmount: Double, newCategory: String?) -> Unit
) {
    var categoryText by remember { mutableStateOf(transaction.category ?: "") }
    var descriptionText by remember { mutableStateOf(transaction.description) }
    var amountText by remember { mutableStateOf(transaction.amount.toString()) }

    val filteredSuggestions = remember(categoryText, suggestionCategories) {
        if (categoryText.isBlank() || categoryText == transaction.category) {
            // If the input is blank, show the default suggestions
            suggestionCategories
        } else {
            // Otherwise, filter the list to show only categories containing the typed text
            suggestionCategories.filter {
                it.contains(categoryText, ignoreCase = true)
            }
        }
    }

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
            Column(modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = descriptionText,
                    onValueChange = { descriptionText = it },
                    label = { Text("Description") }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.height(8.dp))
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
                    filteredSuggestions.forEach { category ->
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
                val newAmount = amountText.toDoubleOrNull()
                if (newAmount != null) {
                    // CALL THE NEW onSave LAMBDA
                    onSave(
                        transaction.id,
                        descriptionText,
                        newAmount,
                        categoryText.trim().takeIf { it.isNotBlank() }
                    )
                }
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
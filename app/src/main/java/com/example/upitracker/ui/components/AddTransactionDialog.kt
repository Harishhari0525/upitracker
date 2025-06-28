@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.upitracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.upitracker.util.IndianCurrencyVisualTransformation

@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        amount: Double,
        type: String,
        description: String,
        category: String
    ) -> Unit
) {
    // --- STATE MANAGEMENT ---
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("DEBIT") }

    // --- NEW: SEPARATE ERROR STATES FOR EACH FIELD ---
    var isAmountError by remember { mutableStateOf(false) }
    var isDescriptionError by remember { mutableStateOf(false) }
    var isCategoryError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Manual Transaction") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // --- AMOUNT FIELD ---
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        isAmountError = false
                        amount = it.filter { char -> char.isDigit() }
                    },
                    label = { Text("Amount") },
                    prefix = { Text("₹") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = isAmountError,
                    supportingText = {
                        if (isAmountError) {
                            Text(
                                text = "Please enter a valid amount",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    visualTransformation = IndianCurrencyVisualTransformation()
                )

                // --- DESCRIPTION FIELD ---
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        isDescriptionError = false
                    },
                    label = { Text("Description") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    isError = isDescriptionError, // Use new error state
                    supportingText = {
                        if (isDescriptionError) {
                            Text(
                                text = "Description cannot be empty", // Specific message
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )

                // --- CATEGORY FIELD ---
                OutlinedTextField(
                    value = category,
                    onValueChange = {
                        category = it
                        isCategoryError = false
                    },
                    label = { Text("Category") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    isError = isCategoryError, // Use new error state
                    supportingText = {
                        if (isCategoryError) {
                            Text(
                                text = "Category cannot be empty", // Specific message
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )

                // Transaction Type Radio Buttons
                Row(modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier
                            .weight(1f) // Takes up half of the available space
                            .clickable { selectedType = "DEBIT" }, // Make the whole area clickable for better UX
                        verticalAlignment = Alignment.CenterVertically // This aligns the button and text
                    ) {
                        RadioButton(
                            selected = selectedType == "DEBIT",
                            onClick = { selectedType = "DEBIT" }
                        )
                        Text(
                            text = "Debit",
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    // --- Credit Option ---
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedType = "CREDIT" }, // Make the whole area clickable
                        verticalAlignment = Alignment.CenterVertically // This aligns the button and text
                    ) {
                        RadioButton(
                            selected = selectedType == "CREDIT",
                            onClick = { selectedType = "CREDIT" }
                        )
                        Text(
                            text = "Credit",
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // --- NEW: INDIVIDUAL VALIDATION LOGIC ---
                    val amountDouble = amount.toDoubleOrNull()

                    // Check each condition separately
                    isAmountError = amountDouble == null || amountDouble <= 0
                    isDescriptionError = description.isBlank()
                    isCategoryError = category.isBlank()

                    // If all checks pass, confirm and dismiss
                    if (!isAmountError && !isDescriptionError && !isCategoryError) {
                        onConfirm(amountDouble!!, selectedType, description, category)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
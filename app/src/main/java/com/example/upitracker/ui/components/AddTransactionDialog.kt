@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.upitracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import com.example.upitracker.util.IndianCurrencyVisualTransformation

// We pass lambdas for the actions
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
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("DEBIT") } // Debit by default
    var isAmountError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Manual Transaction") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Amount Field
                OutlinedTextField(
                    value = amount,
                    onValueChange = { newValue ->
                        isAmountError = false

                        var filtered = newValue.filter { char -> char.isDigit() || char == '.' }
                        if (filtered.count { it == '.' } > 1) {
                            filtered = amount
                        }
                        amount = filtered
                    },
                    label = { Text("Amount") },
                    prefix = { Text("₹") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = isAmountError,
                    supportingText = {
                        if (isAmountError) {
                            Text(
                                text = "Please enter a valid number",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    // The visual transformation remains the same
                    visualTransformation = IndianCurrencyVisualTransformation()
                )

                // Description Field
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )

                // Category Field
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
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
                    val amountDouble = amount.toDoubleOrNull()
                    if (amountDouble != null && description.isNotBlank() && category.isNotBlank()) {
                        isAmountError = false
                        onConfirm(amountDouble, selectedType, description, category)
                    } else {
                        isAmountError = true
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
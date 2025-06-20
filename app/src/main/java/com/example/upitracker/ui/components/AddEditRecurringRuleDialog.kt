package com.example.upitracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.upitracker.data.BudgetPeriod

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRecurringRuleDialog(
    onDismiss: () -> Unit,
    onConfirm: (description: String, amount: Double, category: String, period: BudgetPeriod, day: Int) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var dayOfMonth by remember { mutableStateOf("") }
    val selectedPeriod by remember { mutableStateOf(BudgetPeriod.MONTHLY) } // For now, only support monthly

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Recurring Transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (e.g. Netflix)") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    singleLine = true
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    prefix = { Text("₹") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    singleLine = true
                )
                OutlinedTextField(
                    value = dayOfMonth,
                    onValueChange = { dayOfMonth = it },
                    label = { Text("Day of Month (1-31)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountDouble = amount.toDoubleOrNull()
                    val dayInt = dayOfMonth.toIntOrNull()
                    if (amountDouble != null && dayInt != null && description.isNotBlank() && category.isNotBlank()) {
                        onConfirm(description, amountDouble, category, selectedPeriod, dayInt)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
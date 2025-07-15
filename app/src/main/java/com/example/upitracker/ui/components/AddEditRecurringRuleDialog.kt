package com.example.upitracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.upitracker.data.BudgetPeriod
import com.example.upitracker.data.RecurringRule
import com.example.upitracker.util.DecimalInputVisualTransformation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRecurringRuleDialog(
    ruleToEdit: RecurringRule?,
    onDismiss: () -> Unit,
    onConfirm: (description: String, amount: Double, category: String, period: BudgetPeriod, day: Int) -> Unit
) {
    var description by remember { mutableStateOf(ruleToEdit?.description ?: "") }
    var amount by remember { mutableStateOf(ruleToEdit?.amount?.toString() ?: "") }
    var category by remember { mutableStateOf(ruleToEdit?.categoryName ?: "") }
    var dayOfMonth by remember { mutableStateOf(ruleToEdit?.dayOfPeriod?.toString() ?: "") }
    val selectedPeriod by remember { mutableStateOf(ruleToEdit?.periodType ?: BudgetPeriod.MONTHLY) }

    // Error states for validation
    var isAmountError by remember { mutableStateOf(false) }
    var isDayOfMonthError by remember { mutableStateOf(false) }
    var isCategoryError by remember { mutableStateOf(false) }
    var isDescriptionError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (ruleToEdit == null) "Add Recurring Transaction" else "Edit Recurring Transaction") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it; isCategoryError = false },
                    label = { Text("Category (e.g., Insurance)") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    singleLine = true,
                    isError = isCategoryError,
                    supportingText = { if (isCategoryError) Text("Please enter a category") }
                )
                // --- CORRECTED AMOUNT FIELD ---
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        // Allows digits and one optional decimal point with up to 2 decimal places
                        if (it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            amount = it
                        }
                        isAmountError = false
                    },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = isAmountError,
                    supportingText = { if (isAmountError) Text("Please enter a valid amount") },
                    prefix = { Text("₹") },
                    singleLine = true,
                    visualTransformation = DecimalInputVisualTransformation()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it; isDescriptionError = false },
                    label = { Text("Description") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    singleLine = true,
                    isError = isDescriptionError,
                    supportingText = { if (isDescriptionError) Text("Please enter a description") }
                )
                // --- CORRECTED DAY OF MONTH FIELD ---
                OutlinedTextField(
                    value = dayOfMonth,
                    onValueChange = {
                        // Limits input to 2 characters and only digits
                        if (it.length <= 2) {
                            dayOfMonth = it.filter { c -> c.isDigit() }
                        }
                        isDayOfMonthError = false
                    },
                    label = { Text("Day of Month (1-31)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = isDayOfMonthError,
                    supportingText = { if (isDayOfMonthError) Text("Must be between 1-31") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountDouble = amount.toDoubleOrNull()
                    val dayInt = dayOfMonth.toIntOrNull()

                    // Individual validation checks
                    val descInvalid = description.isBlank()
                    val catInvalid = category.isBlank()
                    isAmountError = amountDouble == null || amountDouble <= 0
                    isDayOfMonthError = dayInt == null || dayInt !in 1..31

                    if (!descInvalid && !catInvalid && !isAmountError && !isDayOfMonthError) {
                        onConfirm(description, amountDouble!!, category, selectedPeriod, dayInt!!)
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

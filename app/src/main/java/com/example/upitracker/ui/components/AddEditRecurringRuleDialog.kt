package com.example.upitracker.ui.components

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.example.upitracker.data.BudgetPeriod
import com.example.upitracker.data.RecurringRule
import com.example.upitracker.util.DecimalInputVisualTransformation
import com.example.upitracker.util.ExpressiveTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRecurringRuleDialog(
    ruleToEdit: RecurringRule?,
    onDismiss: () -> Unit,
    onConfirm: (
        description: String,
        amount: Double,
        category: String,
        period: BudgetPeriod,
        day: Int
    ) -> Unit
) {
    var description by remember { mutableStateOf(ruleToEdit?.description ?: "") }
    var amount by remember { mutableStateOf(ruleToEdit?.amount?.toString() ?: "") }
    var category by remember { mutableStateOf(ruleToEdit?.categoryName ?: "") }
    var dayOfMonth by remember { mutableStateOf(ruleToEdit?.dayOfPeriod?.toString() ?: "") }

    val selectedPeriod by remember {
        mutableStateOf(ruleToEdit?.periodType ?: BudgetPeriod.MONTHLY)
    }

    var isAmountError by remember { mutableStateOf(false) }
    var isDayOfMonthError by remember { mutableStateOf(false) }
    var isCategoryError by remember { mutableStateOf(false) }
    var isDescriptionError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = ExpressiveTokens.corners.extraLarge,
        title = {
            Column(
                verticalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.xs)
            ) {
                Text(
                    text = if (ruleToEdit == null) {
                        "Add Recurring Payment"
                    } else {
                        "Edit Recurring Payment"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Track subscriptions, rent, EMIs, and fixed bills automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.md)
            ) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = description,
                    onValueChange = {
                        description = it
                        isDescriptionError = false
                    },
                    label = { Text("Description") },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words
                    ),
                    singleLine = true,
                    isError = isDescriptionError,
                    supportingText = {
                        if (isDescriptionError) {
                            Text("Please enter a description")
                        }
                    },
                    shape = ExpressiveTokens.corners.medium
                )

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = category,
                    onValueChange = {
                        category = it
                        isCategoryError = false
                    },
                    label = { Text("Category") },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words
                    ),
                    singleLine = true,
                    isError = isCategoryError,
                    supportingText = {
                        if (isCategoryError) {
                            Text("Please enter a category")
                        }
                    },
                    shape = ExpressiveTokens.corners.medium
                )

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = amount,
                    onValueChange = {
                        if (it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            amount = it
                        }

                        isAmountError = false
                    },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    isError = isAmountError,
                    supportingText = {
                        if (isAmountError) {
                            Text("Please enter a valid amount")
                        }
                    },
                    prefix = { Text("₹") },
                    singleLine = true,
                    visualTransformation = DecimalInputVisualTransformation(),
                    shape = ExpressiveTokens.corners.medium
                )

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = dayOfMonth,
                    onValueChange = {
                        if (it.length <= 2) {
                            dayOfMonth = it.filter { char -> char.isDigit() }
                        }

                        isDayOfMonthError = false
                    },
                    label = { Text("Day of month") },
                    supportingText = {
                        if (isDayOfMonthError) {
                            Text("Must be between 1 and 31")
                        } else {
                            Text("Payment will be created monthly on this day.")
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    isError = isDayOfMonthError,
                    singleLine = true,
                    shape = ExpressiveTokens.corners.medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountDouble = amount.toDoubleOrNull()
                    val dayInt = dayOfMonth.toIntOrNull()

                    val descInvalid = description.isBlank()
                    val catInvalid = category.isBlank()
                    val amountInvalid = amountDouble == null || amountDouble <= 0
                    val dayInvalid = dayInt == null || dayInt !in 1..31

                    isDescriptionError = descInvalid
                    isCategoryError = catInvalid
                    isAmountError = amountInvalid
                    isDayOfMonthError = dayInvalid

                    if (!descInvalid && !catInvalid && !amountInvalid && !dayInvalid) {
                        onConfirm(
                            description,
                            amountDouble,
                            category,
                            selectedPeriod,
                            dayInt
                        )
                    }
                },
                shape = ExpressiveTokens.corners.medium
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
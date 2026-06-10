@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.upitracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.example.upitracker.util.DecimalInputVisualTransformation
import com.example.upitracker.util.ExpressiveTokens
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

@Composable
fun AddTransactionDialog(
    userCategories: List<com.example.upitracker.data.Category>,
    onDismiss: () -> Unit,
    onConfirm: (
        amount: Double,
        type: String,
        description: String,
        category: String,
        date: Long
    ) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("DEBIT") }

    var isAmountError by remember { mutableStateOf(false) }
    var isDescriptionError by remember { mutableStateOf(false) }

    var selectedDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDateMillis
    )

    val displayDateFormat = remember {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false

                        datePickerState.selectedDateMillis?.let { newDateMillis ->
                            val newDateCalendar = Calendar.getInstance(
                                TimeZone.getTimeZone("UTC")
                            ).apply {
                                timeInMillis = newDateMillis
                            }

                            val existingTimeCalendar = Calendar.getInstance().apply {
                                timeInMillis = selectedDateMillis
                            }

                            existingTimeCalendar.set(
                                newDateCalendar.get(Calendar.YEAR),
                                newDateCalendar.get(Calendar.MONTH),
                                newDateCalendar.get(Calendar.DAY_OF_MONTH)
                            )

                            selectedDateMillis = existingTimeCalendar.timeInMillis
                        }
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false }
                ) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = ExpressiveTokens.corners.extraLarge,
        title = {
            Column(
                verticalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.xs)
            ) {
                Text(
                    text = "Add Transaction",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Manually add cash, UPI, or adjustment entries.",
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
                    value = amount,
                    onValueChange = {
                        if (it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            amount = it
                        }
                        isAmountError = false
                    },
                    label = { Text("Amount") },
                    prefix = { Text("₹") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    isError = isAmountError,
                    supportingText = {
                        if (isAmountError) {
                            Text("Please enter a valid amount")
                        }
                    },
                    visualTransformation = DecimalInputVisualTransformation(),
                    shape = ExpressiveTokens.corners.medium,
                    singleLine = true
                )

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = description,
                    onValueChange = {
                        description = it
                        isDescriptionError = false
                    },
                    label = { Text("Description") },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    ),
                    isError = isDescriptionError,
                    supportingText = {
                        if (isDescriptionError) {
                            Text("Description cannot be empty")
                        }
                    },
                    shape = ExpressiveTokens.corners.medium,
                    singleLine = true
                )

                var isCategoryExpanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = isCategoryExpanded,
                    onExpandedChange = { isCategoryExpanded = it }
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                            .fillMaxWidth(),
                        value = category,
                        onValueChange = {
                            category = it
                            isCategoryExpanded = true
                        },
                        label = { Text("Category") },
                        placeholder = { Text("e.g., Groceries, Utilities") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        shape = ExpressiveTokens.corners.medium
                    )

                    val filteredCategories = userCategories.filter {
                        it.name.contains(category, ignoreCase = true)
                    }

                    if (filteredCategories.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = isCategoryExpanded,
                            onDismissRequest = { isCategoryExpanded = false }
                        ) {
                            filteredCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = {
                                        category = cat.name
                                        isCategoryExpanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = ExpressiveTokens.corners.medium
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Select Date",
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )

                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))

                    Text(displayDateFormat.format(Date(selectedDateMillis)))
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.xs)
                ) {
                    Text(
                        text = "Transaction type",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.sm)
                    ) {
                        FilterChip(
                            selected = selectedType == "DEBIT",
                            onClick = { selectedType = "DEBIT" },
                            label = { Text("Debit") },
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            selected = selectedType == "CREDIT",
                            onClick = { selectedType = "CREDIT" },
                            label = { Text("Credit") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountDouble = amount.toDoubleOrNull()

                    isAmountError = amountDouble == null || amountDouble <= 0
                    isDescriptionError = description.isBlank()

                    if (!isAmountError && !isDescriptionError) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onConfirm(
                            amountDouble!!,
                            selectedType,
                            description,
                            category,
                            selectedDateMillis
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
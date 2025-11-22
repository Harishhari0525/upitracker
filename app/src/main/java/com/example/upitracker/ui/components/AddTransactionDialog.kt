@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.upitracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.upitracker.util.DecimalInputVisualTransformation
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import androidx.compose.ui.text.input.ImeAction

@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        amount: Double,
        type: String,
        description: String,
        category: String,
        date: Long
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

    var selectedDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
    val displayDateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    // This robust logic prevents timezone issues
                    datePickerState.selectedDateMillis?.let { newDateMillis ->
                        // Get the new date from the picker (which is in UTC)
                        val newDateCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                            timeInMillis = newDateMillis
                        }

                        // Get the existing time from our current state (in the phone's local timezone)
                        val existingTimeCalendar = Calendar.getInstance().apply {
                            timeInMillis = selectedDateMillis
                        }

                        // Apply the new date to the existing time's calendar
                        existingTimeCalendar.set(
                            newDateCalendar.get(Calendar.YEAR),
                            newDateCalendar.get(Calendar.MONTH),
                            newDateCalendar.get(Calendar.DAY_OF_MONTH)
                        )

                        // Update the state with the new combined timestamp
                        selectedDateMillis = existingTimeCalendar.timeInMillis
                    }
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }


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
                        if (it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            amount = it
                        }
                        isAmountError = false
                    },
                    label = { Text("Amount") },
                    prefix = { Text("₹") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next),
                    isError = isAmountError,
                    supportingText = {
                        if (isAmountError) {
                            Text(
                                text = "Please enter a valid amount",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    visualTransformation = DecimalInputVisualTransformation()
                )

                // --- DESCRIPTION FIELD ---
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        isDescriptionError = false
                    },
                    label = { Text("Description") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next),
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
                       // isCategoryError = false
                    },
                    label = { Text("Category") },
                    placeholder = { Text("e.g., Groceries, Utilities") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next)
                )

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = "Select Date", modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(displayDateFormat.format(Date(selectedDateMillis)))
                }

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
                  //  isCategoryError = category.isBlank()

                    // If all checks pass, confirm and dismiss
                    if (!isAmountError && !isDescriptionError) {
                        onConfirm(amountDouble!!, selectedType, description, category, selectedDateMillis)
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
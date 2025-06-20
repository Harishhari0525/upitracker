package com.example.upitracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.upitracker.R
import com.example.upitracker.data.BudgetPeriod
import com.example.upitracker.viewmodel.BudgetStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBudgetDialog(
    budgetStatus: BudgetStatus?,
    onDismiss: () -> Unit,
    onConfirm: (category: String, amount: Double, period: BudgetPeriod, allowRollover: Boolean) -> Unit
) {
    var category by remember { mutableStateOf(budgetStatus?.categoryName ?: "") }
    var amount by remember { mutableStateOf(budgetStatus?.budgetAmount?.toString() ?: "") }
    var selectedPeriod by remember { mutableStateOf(budgetStatus?.periodType ?: BudgetPeriod.MONTHLY) }
    var allowRollover by remember { mutableStateOf(budgetStatus?.allowRollover == true) }
    var isCategoryError by remember { mutableStateOf(false) }
    var isAmountError by remember { mutableStateOf(false) }
    val isEditing = budgetStatus != null

    // ✨ State to control if the dropdown menu is expanded ✨
    var isPeriodDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (!isEditing) stringResource(R.string.budget_add_title) else stringResource(R.string.budget_edit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (!isEditing) {
                    // ✨ START: This is the new Dropdown Menu section ✨
                    ExposedDropdownMenuBox(
                        expanded = isPeriodDropdownExpanded,
                        onExpandedChange = { isPeriodDropdownExpanded = it },
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                            readOnly = true,
                            value = selectedPeriod.name.replaceFirstChar { it.titlecase() },
                            onValueChange = {},
                            label = { Text("Budget Period") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPeriodDropdownExpanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        )
                        ExposedDropdownMenu(
                            expanded = isPeriodDropdownExpanded,
                            onDismissRequest = { isPeriodDropdownExpanded = false },
                        ) {
                            BudgetPeriod.entries.forEach { period ->
                                DropdownMenuItem(
                                    text = { Text(period.name.replaceFirstChar { it.titlecase() }) },
                                    onClick = {
                                        selectedPeriod = period
                                        isPeriodDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    // ✨ END: New Dropdown Menu section ✨
                }
                OutlinedTextField(value = category, onValueChange = { category = it; isCategoryError = false }, label = { Text(stringResource(R.string.budget_category_label)) }, singleLine = true, isError = isCategoryError)
                OutlinedTextField(value = amount, onValueChange = { amount = it; isAmountError = false }, label = { Text(stringResource(R.string.budget_amount_label)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = isAmountError, prefix = { Text("₹") })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Enable Rollover", modifier = Modifier.weight(1f))
                    Switch(checked = allowRollover, onCheckedChange = { allowRollover = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountDouble = amount.toDoubleOrNull()
                    isCategoryError = category.isBlank()
                    isAmountError = amountDouble == null || amountDouble <= 0
                    if (!isCategoryError && !isAmountError) {
                        onConfirm(category, amountDouble!!, selectedPeriod, allowRollover)
                    }
                }
            ) { Text(stringResource(R.string.button_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_button_cancel)) } }
    )
}
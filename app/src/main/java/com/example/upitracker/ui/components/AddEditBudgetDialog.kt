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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import com.example.upitracker.R
import com.example.upitracker.data.BudgetPeriod
import com.example.upitracker.util.DecimalInputVisualTransformation
import com.example.upitracker.viewmodel.BudgetStatus
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
            Column(modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
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

                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it; isCategoryError = false },
                        label = { Text(stringResource(R.string.budget_category_label)) },
                        singleLine = true,
                        isError = isCategoryError,
                        supportingText = { if (isCategoryError) Text("Category cannot be empty") }
                    )

                    OutlinedTextField(
                        value = amount,
                        // Filter out non-digit characters to prevent commas
                        onValueChange = {
                            if (it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                amount = it
                            }
                            isAmountError = false
                        },
                        label = { Text(stringResource(R.string.budget_amount_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = isAmountError,
                        prefix = { Text("₹") },
                        // Add a specific error message
                        supportingText = { if (isAmountError) Text("Please enter a valid amount") },
                        visualTransformation = DecimalInputVisualTransformation()
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp) // Add some padding if needed
                    ) {
                        Text("Enable Rollover", modifier = Modifier.weight(1f))

                        // This is the correct way to implement a tooltip with composable content
                        val tooltipState = rememberTooltipState()
                        val scope = rememberCoroutineScope()

                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                            tooltip = {
                                PlainTooltip {
                                    Text("If enabled, any amount leftover (or overspent) from the previous period will be added / deducted to this period's budget.")
                                }
                            },
                            state = tooltipState
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = "Help about rollover budgets",
                                // You can optionally make the icon clickable to show the tooltip
                                modifier = Modifier.clickable { scope.launch { tooltipState.show() } },
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.width(8.dp))
                        Switch(checked = allowRollover, onCheckedChange = { allowRollover = it })
                    }
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
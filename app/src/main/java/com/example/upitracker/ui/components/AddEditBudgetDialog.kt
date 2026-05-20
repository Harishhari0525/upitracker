package com.example.upitracker.ui.components

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.example.upitracker.R
import com.example.upitracker.data.BudgetPeriod
import com.example.upitracker.util.DecimalInputVisualTransformation
import com.example.upitracker.util.ExpressiveTokens
import com.example.upitracker.viewmodel.BudgetStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBudgetDialog(
    budgetStatus: BudgetStatus?,
    onDismiss: () -> Unit,
    onConfirm: (
        category: String,
        amount: Double,
        period: BudgetPeriod,
        allowRollover: Boolean
    ) -> Unit
) {
    var category by remember { mutableStateOf(budgetStatus?.categoryName ?: "") }
    var amount by remember { mutableStateOf(budgetStatus?.budgetAmount?.toString() ?: "") }
    var selectedPeriod by remember { mutableStateOf(budgetStatus?.periodType ?: BudgetPeriod.MONTHLY) }
    var allowRollover by remember { mutableStateOf(budgetStatus?.allowRollover == true) }

    var isCategoryError by remember { mutableStateOf(false) }
    var isAmountError by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var isPeriodDropdownExpanded by remember { mutableStateOf(false) }

    val isEditing = budgetStatus != null

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = ExpressiveTokens.corners.extraLarge,
        title = {
            Column(
                verticalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.xs)
            ) {
                Text(
                    text = if (!isEditing) {
                        stringResource(R.string.budget_add_title)
                    } else {
                        stringResource(R.string.budget_edit_title)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Set a spending limit and track how much is safe to spend.",
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
                ExposedDropdownMenuBox(
                    expanded = isPeriodDropdownExpanded,
                    onExpandedChange = {
                        if (!isEditing) {
                            isPeriodDropdownExpanded = it
                        }
                    }
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        readOnly = true,
                        value = selectedPeriod.name.replaceFirstChar { it.titlecase() },
                        onValueChange = {},
                        label = { Text("Budget period") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = isPeriodDropdownExpanded
                            )
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        enabled = !isEditing,
                        shape = ExpressiveTokens.corners.medium,
                        singleLine = true
                    )

                    ExposedDropdownMenu(
                        expanded = isPeriodDropdownExpanded,
                        onDismissRequest = { isPeriodDropdownExpanded = false }
                    ) {
                        BudgetPeriod.entries.forEach { period ->
                            DropdownMenuItem(
                                text = {
                                    Text(period.name.replaceFirstChar { it.titlecase() })
                                },
                                onClick = {
                                    selectedPeriod = period
                                    isPeriodDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = category,
                    onValueChange = {
                        category = it
                        isCategoryError = false
                    },
                    label = { Text(stringResource(R.string.budget_category_label)) },
                    singleLine = true,
                    isError = isCategoryError,
                    supportingText = {
                        if (isCategoryError) {
                            Text("Category cannot be empty")
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
                    label = { Text(stringResource(R.string.budget_amount_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    isError = isAmountError,
                    prefix = { Text("₹") },
                    supportingText = {
                        if (isAmountError) {
                            Text("Please enter a valid amount")
                        }
                    },
                    visualTransformation = DecimalInputVisualTransformation(),
                    shape = ExpressiveTokens.corners.medium
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = ExpressiveTokens.spacing.xs)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.xxs)
                    ) {
                        Text(
                            text = "Enable rollover",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = "Carry leftover or deficit into the next period.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { showHelpDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = "Help about rollover budgets",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(ExpressiveTokens.spacing.xs))

                    Switch(
                        checked = allowRollover,
                        onCheckedChange = { allowRollover = it }
                    )
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
                        onConfirm(
                            category,
                            amountDouble!!,
                            selectedPeriod,
                            allowRollover
                        )
                    }
                },
                shape = ExpressiveTokens.corners.medium
            ) {
                Text(stringResource(R.string.button_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_button_cancel))
            }
        }
    )

    if (showHelpDialog) {
        RolloverHelpDialog(
            onDismiss = { showHelpDialog = false }
        )
    }
}

@Composable
private fun RolloverHelpDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = ExpressiveTokens.corners.extraLarge,
        icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                contentDescription = null
            )
        },
        title = {
            Text(
                text = "About Rollover Budgets",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "When enabled, any amount leftover from the previous period is added to this period's budget.\n\n" +
                        "If you overspent, the deficit will be deducted from the new period's budget, giving you a true picture of your available funds.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it")
            }
        }
    )
}
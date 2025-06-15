// In ui/screens/BudgetScreen.kt

@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.upitracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.upitracker.R
import com.example.upitracker.data.BudgetPeriod
import com.example.upitracker.viewmodel.BudgetStatus
import com.example.upitracker.viewmodel.MainViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun BudgetScreen(mainViewModel: MainViewModel) {
    val budgetStatuses by mainViewModel.budgetStatuses.collectAsState()
    var showAddEditDialog by remember { mutableStateOf(false) }
    var budgetToEdit by remember { mutableStateOf<BudgetStatus?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                budgetToEdit = null
                showAddEditDialog = true
            }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.budget_add_new))
            }
        }
    ) { paddingValues ->
        if (budgetStatuses.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.budget_empty_state),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(budgetStatuses, key = { it.budgetId }) { status ->
                    BudgetCard(
                        status = status,
                        onEdit = { /* Editing logic can be enhanced later */ },
                        onDelete = { mainViewModel.deleteBudget(status.budgetId) }
                    )
                }
            }
        }

        if (showAddEditDialog) {
            AddEditBudgetDialog(
                budgetStatus = budgetToEdit,
                onDismiss = { showAddEditDialog = false },
                onConfirm = { category, amount, period, allowRollover -> // ✨ UPDATED
                    mainViewModel.addOrUpdateBudget(category, amount, period, allowRollover) // ✨ UPDATED
                    showAddEditDialog = false
                }
            )
        }
    }
}

@Composable
private fun BudgetCard(
    status: BudgetStatus,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    var showMenu by remember { mutableStateOf(false) }
    val progressColor = if (status.progress >= 1.0f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val rolloverColor = if (status.rolloverAmount >= 0) Color(0xFF006D3D) else MaterialTheme.colorScheme.error

    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = status.categoryName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = status.periodType.name.lowercase().replaceFirstChar { it.titlecase() }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                }
                Box {
                    IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Budget options") }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        // DropdownMenuItem(text = { Text("Edit") }, onClick = { onEdit(); showMenu = false }) // Edit disabled for now
                        DropdownMenuItem(text = { Text("Delete") }, onClick = { onDelete(); showMenu = false })
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            // ✨ UPDATED: Show effective budget with rollover
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${currencyFormatter.format(status.spentAmount)} / ${currencyFormatter.format(status.effectiveBudget)}",
                    style = MaterialTheme.typography.bodyLarge
                )
                if (status.allowRollover && status.rolloverAmount != 0.0) {
                    Spacer(Modifier.width(8.dp))
                    val sign = if (status.rolloverAmount > 0) "+" else ""
                    Text(
                        text = "(${sign}${currencyFormatter.format(status.rolloverAmount)} rollover)",
                        style = MaterialTheme.typography.bodySmall,
                        color = rolloverColor
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { status.progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = progressColor,
                trackColor = progressColor.copy(alpha = 0.2f)
            )
            Spacer(Modifier.height(4.dp))
            val remainingText = if (status.remainingAmount >= 0) {
                stringResource(R.string.budget_remaining, currencyFormatter.format(status.remainingAmount))
            } else {
                stringResource(R.string.budget_overspent, currencyFormatter.format(status.remainingAmount * -1))
            }
            Text(
                text = remainingText,
                style = MaterialTheme.typography.bodySmall,
                color = if (status.remainingAmount < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
private fun AddEditBudgetDialog(
    budgetStatus: BudgetStatus?,
    onDismiss: () -> Unit,
    onConfirm: (category: String, amount: Double, period: BudgetPeriod, allowRollover: Boolean) -> Unit // ✨ UPDATED
) {
    var category by remember { mutableStateOf(budgetStatus?.categoryName ?: "") }
    var amount by remember { mutableStateOf(budgetStatus?.budgetAmount?.toString() ?: "") }
    var selectedPeriod by remember { mutableStateOf(budgetStatus?.periodType ?: BudgetPeriod.MONTHLY) }
    var allowRollover by remember { mutableStateOf(budgetStatus?.allowRollover == true) } // ✨ NEW
    var isCategoryError by remember { mutableStateOf(false) }
    var isAmountError by remember { mutableStateOf(false) }
    val isEditing = budgetStatus != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (!isEditing) stringResource(R.string.budget_add_title) else stringResource(R.string.budget_edit_title)) },
        text = {
            Column {
                if (!isEditing) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BudgetPeriod.entries.forEach { period ->
                            FilterChip(
                                selected = selectedPeriod == period,
                                onClick = { selectedPeriod = period },
                                label = { Text(period.name.lowercase().replaceFirstChar { it.titlecase() }) },
                                leadingIcon = if (selectedPeriod == period) { { Icon(Icons.Default.Check, null) } } else null
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                OutlinedTextField(value = category, onValueChange = { category = it; isCategoryError = false }, label = { Text(stringResource(R.string.budget_category_label)) }, singleLine = true, isError = isCategoryError)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = amount, onValueChange = { amount = it; isAmountError = false }, label = { Text(stringResource(R.string.budget_amount_label)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = isAmountError, prefix = { Text("₹") })
                Spacer(Modifier.height(16.dp))
                // ✨ NEW: Switch for enabling rollover
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
                        onConfirm(category, amountDouble!!, selectedPeriod, allowRollover) // ✨ UPDATED
                    }
                }
            ) { Text(stringResource(R.string.button_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_button_cancel)) } }
    )
}
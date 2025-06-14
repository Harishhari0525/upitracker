// In app/src/main/java/com/example/upitracker/ui/screens/BudgetScreen.kt

@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.upitracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.example.upitracker.data.Budget
import com.example.upitracker.viewmodel.BudgetStatus
import com.example.upitracker.viewmodel.MainViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun BudgetScreen(mainViewModel: MainViewModel) {
    val budgetStatuses by mainViewModel.budgetStatuses.collectAsState()
    var showAddEditDialog by remember { mutableStateOf(false) }
    var budgetToEdit by remember { mutableStateOf<Budget?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                budgetToEdit = null // Ensure we are in "add" mode
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
                items(budgetStatuses, key = { it.categoryName }) { status ->
                    BudgetCard(
                        status = status,
                        onEdit = {
                            budgetToEdit = Budget(status.categoryName, status.budgetAmount)
                            showAddEditDialog = true
                        },
                        onDelete = { mainViewModel.deleteBudget(status.categoryName) }
                    )
                }
            }
        }

        if (showAddEditDialog) {
            AddEditBudgetDialog(
                budget = budgetToEdit,
                onDismiss = { showAddEditDialog = false },
                onConfirm = { category, amount ->
                    mainViewModel.addOrUpdateBudget(category, amount)
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

    // Change progress bar color when over budget
    val progressColor = if (status.progress >= 1.0f) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = status.categoryName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Budget options")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text("Edit") }, onClick = {
                            onEdit()
                            showMenu = false
                        })
                        DropdownMenuItem(text = { Text("Delete") }, onClick = {
                            onDelete()
                            showMenu = false
                        })
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            Text(
                text = "${currencyFormatter.format(status.spentAmount)} / ${currencyFormatter.format(status.budgetAmount)}",
                style = MaterialTheme.typography.bodyLarge
            )

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
    budget: Budget?, // Null if adding, non-null if editing
    onDismiss: () -> Unit,
    onConfirm: (category: String, amount: Double) -> Unit
) {
    var category by remember { mutableStateOf(budget?.categoryName ?: "") }
    var amount by remember { mutableStateOf(budget?.budgetAmount?.toString() ?: "") }
    var isCategoryError by remember { mutableStateOf(false) }
    var isAmountError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (budget == null) stringResource(R.string.budget_add_title) else stringResource(R.string.budget_edit_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it; isCategoryError = false },
                    label = { Text(stringResource(R.string.budget_category_label)) },
                    singleLine = true,
                    isError = isCategoryError,
                    // If editing, the category name field is read-only
                    readOnly = (budget != null)
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it; isAmountError = false },
                    label = { Text(stringResource(R.string.budget_amount_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = isAmountError,
                    prefix = { Text("₹") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountDouble = amount.toDoubleOrNull()
                    isCategoryError = category.isBlank()
                    isAmountError = amountDouble == null || amountDouble <= 0

                    if (!isCategoryError && !isAmountError) {
                        onConfirm(category, amountDouble!!)
                    }
                }
            ) { Text(stringResource(R.string.button_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_button_cancel)) }
        }
    )
}
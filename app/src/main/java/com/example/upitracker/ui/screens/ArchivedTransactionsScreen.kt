@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.upitracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.upitracker.R
import com.example.upitracker.data.Transaction
import com.example.upitracker.ui.components.DeleteTransactionConfirmationDialog
import com.example.upitracker.ui.components.LottieEmptyState
import com.example.upitracker.ui.components.TransactionCardWithMenu
import com.example.upitracker.util.getCategoryIcon
import com.example.upitracker.util.parseColor
import com.example.upitracker.viewmodel.MainViewModel


@Composable
fun ArchivedTransactionsScreen(
    mainViewModel: MainViewModel,
    onBack: () -> Unit
) {
    // NOTE: We only need one state variable now, for the confirmation dialog.
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }
    val archivedList by mainViewModel.archivedUpiTransactions
        .collectAsState(initial = emptyList())

    val allCategories by mainViewModel.allCategories.collectAsState()

    val isSelectionMode by mainViewModel.isSelectionModeActive.collectAsState()
    val selectedIds by mainViewModel.selectedTransactionIds.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.archived_transactions_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back_description)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        // The main content of the Scaffold is now cleaner.
        // It contains the list and ONE dialog.
        Box(modifier = Modifier.padding(paddingValues)) {
            if (mainViewModel.archivedUpiTransactions.collectAsState().value.isEmpty()) {
                LottieEmptyState(
                    message = stringResource(R.string.archived_empty_state),
                    lottieResourceId = R.raw.empty_box_animation
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(archivedList, key = { it.id }) { transaction ->

                        val categoryDetails = remember(transaction.category, allCategories) {
                            allCategories.find { c -> c.name.equals(transaction.category, ignoreCase = true) }
                        }

                        val categoryColor = remember(categoryDetails) {
                            parseColor(categoryDetails?.colorHex ?: "#808080") // Default to Gray
                        }
                        val categoryIcon = getCategoryIcon(categoryDetails)

                        val isSelected = selectedIds.contains(transaction.id)


                        TransactionCardWithMenu(
                            transaction = transaction,
                            // Pass the state flags directly
                            isSelectionMode = isSelectionMode,
                            isSelected = isSelected,
                            showCheckbox = isSelectionMode,
                            // Provide the new callbacks
                            onToggleSelection = { mainViewModel.toggleSelection(transaction.id) },
                            onShowDetails = {
                                // A regular tap in the archive doesn't do anything if not in selection mode
                            },
                            onDelete = { transactionToDelete = transaction },
                            // Pass the restore action
                            onArchiveAction = { mainViewModel.toggleTransactionArchiveStatus(it, archive = false) },
                            // Pass the correct text and icon
                            archiveActionText = "Restore",
                            archiveActionIcon = Icons.Default.Restore,
                            // Other parameters
                            categoryColor = categoryColor,
                            categoryIcon = categoryIcon,
                            onCategoryClick = { /* No category filtering from archive */ },
                        )
                    }
                }
            }

            // Confirmation dialog for permanent deletion
            if (transactionToDelete != null) {
                DeleteTransactionConfirmationDialog(
                    transactionDescription = transactionToDelete!!.description,
                    onConfirm = {
                        // IMPORTANT: Calling the correct permanent delete function here.
                        mainViewModel.permanentlyDeleteTransaction(transactionToDelete!!)
                        transactionToDelete = null // Dismiss dialog
                    },
                    onDismiss = { transactionToDelete = null }
                )
            }
        }
    }
}
// In ui/screens/ArchivedTransactionsScreen.kt

@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.upitracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever // Added
import androidx.compose.material.icons.filled.Restore // Added
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.upitracker.R
import com.example.upitracker.data.Transaction
import com.example.upitracker.ui.components.DeleteTransactionConfirmationDialog
import com.example.upitracker.ui.components.TransactionCard
import com.example.upitracker.viewmodel.MainViewModel
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

@Composable
fun ArchivedTransactionsScreen(
    mainViewModel: MainViewModel,
    onBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val archivedTransactions by mainViewModel.archivedUpiTransactions.collectAsState()

    // State for the permanent delete confirmation dialog (used by swipe and new dialog)
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }

    // State for the new actions dialog
    var showArchivedTransactionActionsDialog by remember { mutableStateOf(false) }
    var transactionForDialog by remember { mutableStateOf<Transaction?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.archived_transactions_title)) }, // Create this string
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.navigate_back_description))
                    }
                }
            )
        }
    ) { paddingValues ->
        if (archivedTransactions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.archived_empty_state), // Create this string
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(archivedTransactions, key = { it.id }) { transaction ->
                    // We reuse TransactionCard but give it different actions!
                    TransactionCard(
                        modifier = Modifier.animateItem(),
                        transaction = transaction,
                        onClick = { /* No action on simple click */ },
                        onLongClick = {
                            transactionForDialog = it
                            showArchivedTransactionActionsDialog = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onArchiveSwipeAction = { txn -> // Swipe right to RESTORE
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            mainViewModel.toggleTransactionArchiveStatus(txn, archive = false)
                        },
                        onDeleteSwipeAction = { txn -> // Swipe left to DELETE
                            transactionToDelete = txn
                        },
                        swipeActionsEnabled = true // Ensure swipes are on
                    )
                }
            }
        }

        // Confirmation dialog for permanent deletion
        if (transactionToDelete != null) {
            DeleteTransactionConfirmationDialog(
                transactionDescription = transactionToDelete!!.description,
                onConfirm = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    mainViewModel.deleteTransaction(transactionToDelete!!) // This will now use the Undo mechanism
                    transactionToDelete = null // Dismiss dialog
                },
                onDismiss = { transactionToDelete = null }
            )
        }

        if (showArchivedTransactionActionsDialog && transactionForDialog != null) {
            ArchivedTransactionActionsDialog(
                transaction = transactionForDialog!!,
                onDismiss = {
                    showArchivedTransactionActionsDialog = false
                    transactionForDialog = null
                },
                onRestore = { transaction ->
                    mainViewModel.toggleTransactionArchiveStatus(transaction, archive = false)
                    showArchivedTransactionActionsDialog = false
                    transactionForDialog = null
                },
                onDeletePermanent = { transaction ->
                    // This will trigger the existing delete confirmation dialog
                    transactionToDelete = transaction
                    showArchivedTransactionActionsDialog = false
                    transactionForDialog = null
                }
            )
        }
    }
}

@Composable
private fun ArchivedTransactionActionsDialog(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onRestore: (Transaction) -> Unit,
    onDeletePermanent: (Transaction) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Archived Transaction Actions") }, // Hardcoded as per plan
        text = {
            Column {
                ListItem(
                    headlineContent = { Text("Restore") }, // Hardcoded
                    leadingContent = { Icon(Icons.Filled.Restore, contentDescription = "Restore") },
                    modifier = Modifier.clickable { onRestore(transaction) }
                )
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text("Delete Permanently") }, // Hardcoded
                    leadingContent = { Icon(Icons.Filled.DeleteForever, contentDescription = "Delete Permanently") },
                    modifier = Modifier.clickable { onDeletePermanent(transaction) }
                )
            }
        },
        confirmButton = { /* Not used, actions are in the list */ },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel") // Hardcoded
            }
        }
    )
}
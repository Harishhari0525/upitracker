@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.example.upitracker.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.upitracker.ui.components.UpiLiteSummaryCard
import com.example.upitracker.viewmodel.SummaryHistoryItem // ✨ Import SummaryHistoryItem
import com.example.upitracker.viewmodel.TransactionHistoryItem
import androidx.compose.material.ExperimentalMaterialApi
import com.example.upitracker.ui.components.EditCategoryDialog
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController // ✨ Import NavController for navigation
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.upitracker.R
import com.example.upitracker.data.Transaction
import com.example.upitracker.ui.components.TransactionCard
import com.example.upitracker.viewmodel.MainViewModel
import com.example.upitracker.viewmodel.UpiTransactionTypeFilter
import java.text.NumberFormat
import java.util.Calendar // ✨ Import Calendar
import java.util.Locale
import com.example.upitracker.ui.components.DeleteTransactionConfirmationDialog
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback


@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CurrentMonthExpensesScreen(
    mainViewModel: MainViewModel,
    onImportOldSms: () -> Unit,
    navController: NavController, // ✨ Add NavController parameter from MainAppScreen ✨
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isImporting by mainViewModel.isImportingSms.collectAsState()
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isImporting,
        onRefresh = onImportOldSms
    )

    val currentMonthExpensesTotal by mainViewModel.currentMonthTotalExpenses.collectAsState()

    val animatedTotal by animateFloatAsState(
        targetValue = currentMonthExpensesTotal.toFloat(),
        animationSpec = tween(durationMillis = 1000), // Animate over 1 second
        label = "animatedTotalExpense"
    )

    val currentMonthExpenseItems by mainViewModel.currentMonthExpenseItems.collectAsState()

    val swipeActionsEnabled by mainViewModel.swipeActionsEnabled.collectAsState()

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    // --- State for Category Edit Dialog ---
    var transactionToEditCategory by remember { mutableStateOf<Transaction?>(null) }
    var showCategoryEditDialog by remember { mutableStateOf(false) }

    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    val openDeleteConfirmDialog = { transaction: Transaction ->
        transactionToDelete = transaction
        showDeleteConfirmDialog = true
    }

    val context = LocalContext.current

    val openCategoryEditDialog = { transaction: Transaction ->
        transactionToEditCategory = transaction
        showCategoryEditDialog = true
    }

    Box(modifier = modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Summary Card for Total Expenses (as before)
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stringResource(R.string.home_current_month_expenses_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        currencyFormatter.format(animatedTotal),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                stringResource(R.string.home_recent_expenses_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (currentMonthExpenseItems.isEmpty() && !isImporting) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.home_no_expenses_this_month),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(currentMonthExpenseItems.take(25), key = { item ->
                        when(item) {
                            is TransactionHistoryItem -> "txn-${item.transaction.id}"
                            is SummaryHistoryItem -> "summary-${item.summary.id}"
                        }
                    }) { historyItem ->
                        when (historyItem) {
                            is TransactionHistoryItem -> {
                                TransactionCard(
                                    modifier = Modifier.animateItem(),
                                    transaction = historyItem.transaction,
                                    onClick = { openCategoryEditDialog(historyItem.transaction) },
                                    onLongClick = { openDeleteConfirmDialog(historyItem.transaction) },
                                    onArchiveSwipeAction = { txnToArchive -> // ✨ Handle Archive Swipe ✨
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        mainViewModel.toggleTransactionArchiveStatus(txnToArchive)
                                    },
                                    onDeleteSwipeAction = { txnToDeleteFromSwipe -> // ✨ Handle Delete Swipe ✨
                                        // Re-use the confirmation dialog for consistency, or direct delete with undo snackbar
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        openDeleteConfirmDialog(txnToDeleteFromSwipe)
                                    },
                                    swipeActionsEnabled = swipeActionsEnabled
                                )
                            }

                            is SummaryHistoryItem -> {
                                UpiLiteSummaryCard(summary = historyItem.summary)
                            }
                        }
                    }
                    if (currentMonthExpenseItems.size > 25) {
                        item {
                            TextButton(
                                    onClick = {
                                    // ✨ Set date filter to current month and navigate to History screen ✨
                                    val calendar = Calendar.getInstance()
                                    // Start of current month
                                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                                    calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
                                    val monthStartTimestamp = calendar.timeInMillis

                                    // End of current month
                                    calendar.add(Calendar.MONTH, 1)
                                    calendar.add(Calendar.MILLISECOND, -1)
                                    val monthEndTimestamp = calendar.timeInMillis

                                    mainViewModel.setDateRangeFilter(monthStartTimestamp, monthEndTimestamp)
                                    // Also ensure UPI type filter is set to ALL or DEBIT as desired for this view
                                    mainViewModel.setUpiTransactionTypeFilter(
                                        UpiTransactionTypeFilter.DEBIT) // Assuming we want to see DEBITs
                                    navController.navigate(BottomNavItem.History.route) {
                                        // Optional: Pop up to the start destination of the graph to avoid building up a large back stack
                                        // This depends on your NavController structure (rootNavController vs contentNavController)
                                        // If 'navController' here is the contentNavController from MainAppScreen:
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true // Save state of the home screen
                                        }
                                        launchSingleTop = true
                                        restoreState = true // Restore state if coming back
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.home_view_all_this_month_button, currentMonthExpenseItems.size))
                            }
                        }
                    }
                }
                if (showDeleteConfirmDialog && transactionToDelete != null) {
                    DeleteTransactionConfirmationDialog(
                        transactionDescription = transactionToDelete!!.description,
                        onConfirm = {
                            mainViewModel.deleteTransaction(transactionToDelete!!)
                            mainViewModel.toggleTransactionArchiveStatus(transactionToDelete!!, archive = true)
                            mainViewModel.postSnackbarMessage(context.getString(R.string.transaction_deleted_snackbar))
                            showDeleteConfirmDialog = false
                            transactionToDelete = null
                        },
                        onDismiss = {
                            showDeleteConfirmDialog = false
                            transactionToDelete = null
                        }
                    )
                }
            }
        }

        PullRefreshIndicator(
            refreshing = isImporting,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
            contentColor = MaterialTheme.colorScheme.primary
        )

        if (showCategoryEditDialog && transactionToEditCategory != null) {
            EditCategoryDialog( // Ensure this composable is accessible (defined or imported)
                transaction = transactionToEditCategory!!,
                onDismiss = {
                    showCategoryEditDialog = false
                    transactionToEditCategory = null
                },
                onSaveCategory = { transactionId, newCategory ->
                    mainViewModel.updateTransactionCategory(transactionId, newCategory)
                    showCategoryEditDialog = false
                    transactionToEditCategory = null
                }
            )
        }
    }
}
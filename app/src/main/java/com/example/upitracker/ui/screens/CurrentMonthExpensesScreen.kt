@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.example.upitracker.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer // M3 import
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState // M3 import
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.upitracker.R
import com.example.upitracker.data.Transaction
import com.example.upitracker.ui.components.DeleteTransactionConfirmationDialog
import com.example.upitracker.ui.components.TransactionCard
import com.example.upitracker.ui.components.UpiLiteSummaryCard
import com.example.upitracker.viewmodel.MainViewModel
import com.example.upitracker.viewmodel.SummaryHistoryItem
import com.example.upitracker.viewmodel.TransactionHistoryItem
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
// Removed: import androidx.compose.material.pullrefresh.PullRefreshIndicator


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CurrentMonthExpensesScreen(
    mainViewModel: MainViewModel,
    onImportOldSms: () -> Unit,
    onTransactionClick: (Int) -> Unit,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isImporting by mainViewModel.isImportingSms.collectAsState()
    val currentMonthExpensesTotal by mainViewModel.currentMonthTotalExpenses.collectAsState()
    val currentMonthExpenseItems by mainViewModel.currentMonthExpenseItems.collectAsState()
    val swipeActionsEnabled by mainViewModel.swipeActionsEnabled.collectAsState()
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    val context = LocalContext.current

    // --- NEW: State Management for Bottom Sheet ---
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val scope = rememberCoroutineScope()
    var showDetailSheet by remember { mutableStateOf(false) }

    // --- NEW: State Management for M3 Pull-to-Refresh ---
    val pullToRefreshState = rememberPullToRefreshState() // M3 state

    // Synchronize M3 state with ViewModel's isImporting state
    LaunchedEffect(isImporting, pullToRefreshState) {
        if (isImporting) {
            // This block can be left empty if onImportOldSms correctly manages
            // the refreshing state that pullToRefreshState observes.
            // Alternatively, if manual start is needed:
            // if (!pullToRefreshState.isRefreshing) pullToRefreshState.startRefresh()
        } else {
            if (pullToRefreshState.isRefreshing) {
                pullToRefreshState.endRefresh()
            }
        }
    }

    val animatedTotal by animateFloatAsState(
        targetValue = currentMonthExpensesTotal.toFloat(),
        animationSpec = tween(durationMillis = 1000),
        label = "animatedTotalExpense"
    )

    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // The main layout Box
    Box(modifier = modifier.fillMaxSize()) { // Root Box
        PullToRefreshContainer(
            state = pullToRefreshState,
            onRefresh = onImportOldSms, // Pass the function reference
            modifier = Modifier.align(Alignment.TopCenter) // Aligns the indicator itself
            // The content of PullToRefreshContainer is defined in its trailing lambda.
        ) {
            // The content that will be scrollable and trigger the refresh.
            // This Column was previously inside the M2 pullRefresh Box.
            Column(
                modifier = Modifier
                    .fillMaxSize() // Ensure the Column takes up available space for scrolling
                    .padding(16.dp)
            ) {
                // Expressive Summary Card for Total Expenses
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Box(
                        modifier = Modifier.background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                                )
                            )
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 24.dp, vertical = 32.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.home_current_month_expenses_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    currencyFormatter.format(animatedTotal),
                                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Icon(
                                imageVector = Icons.Filled.TrackChanges,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
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
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
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
                            when (item) {
                                is TransactionHistoryItem -> "txn-${item.transaction.id}"
                                is SummaryHistoryItem -> "summary-${item.summary.id}"
                            }
                        }) { historyItem ->
                            when (historyItem) {
                                is TransactionHistoryItem -> {
                                    TransactionCard(
                                        modifier = Modifier.animateItem(),
                                        transaction = historyItem.transaction,
                                        onClick = {
                                            mainViewModel.selectTransaction(historyItem.transaction.id)
                                            showDetailSheet = true
                                        },
                                        onLongClick = {
                                            transactionToDelete = it
                                            showDeleteConfirmDialog = true
                                        },
                                        onArchiveSwipeAction = { txnToArchive ->
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            mainViewModel.toggleTransactionArchiveStatus(txnToArchive, archive = true)
                                        },
                                        onDeleteSwipeAction = { txnToDeleteFromSwipe ->
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            transactionToDelete = txnToDeleteFromSwipe
                                            showDeleteConfirmDialog = true
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
                                    onClick = onViewAllClick,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.home_view_all_this_month_button, currentMonthExpenseItems.size))
                                }
                            }
                        }
                    }
                }
            }
            // M3 PullToRefreshContainer implicitly contains its own indicator.
            // No need to add a separate PullRefreshIndicator composable.
        } // End of PullToRefreshContainer

        // NEW: The ModalBottomSheet for showing details
        if (showDetailSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showDetailSheet = false
                    mainViewModel.selectTransaction(null) // Clear selection on dismiss
                },
                sheetState = sheetState
            ) {
                // This composable should be in your `TransactionDetailSheetContent.kt` file
                TransactionDetailSheetContent(
                    mainViewModel = mainViewModel,
                    onDismiss = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                showDetailSheet = false
                                mainViewModel.selectTransaction(null)
                            }
                        }
                    }
                )
            }
        }

        if (showDeleteConfirmDialog && transactionToDelete != null) {
            DeleteTransactionConfirmationDialog(
                transactionDescription = transactionToDelete!!.description,
                onConfirm = {
                    mainViewModel.toggleTransactionArchiveStatus(transactionToDelete!!, archive = true)
                    mainViewModel.postSnackbarMessage(context.getString(R.string.transaction_archived_snackbar))
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
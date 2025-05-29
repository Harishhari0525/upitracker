@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterialApi::class,
    ExperimentalFoundationApi::class
)

package com.example.upitracker.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.* // Added Edit for dialog icon
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.upitracker.R
import com.example.upitracker.data.Transaction
import com.example.upitracker.data.UpiLiteSummary
import com.example.upitracker.ui.components.TransactionCard
import com.example.upitracker.ui.components.UpiLiteSummaryCard
// ✨ Import EditCategoryDialog if it's in a separate file ✨
// import com.example.upitracker.ui.components.EditCategoryDialog
import com.example.upitracker.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun TabbedHomeScreen( // This screen might be your "History" tab's content now, or a standalone screen
    mainViewModel: MainViewModel,
    navController: NavController, // This could be contentNavController or rootNavController depending on context
    onImportOldSms: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tabTitles = listOf(stringResource(R.string.tab_upi), stringResource(R.string.tab_upi_lite))
    val tabIcons = listOf(Icons.Filled.AccountBalanceWallet, Icons.Filled.Summarize)

    // Using filteredUpiTransactions and filteredUpiLiteSummaries for consistency with History screen
    val upiTransactions by mainViewModel.filteredUpiTransactions.collectAsState()
    val liteSummaries by mainViewModel.filteredUpiLiteSummaries.collectAsState() // Assuming you want filtered summaries too
    val isImporting by mainViewModel.isImportingSms.collectAsState()

    val pagerState = rememberPagerState { tabTitles.size }
    val coroutineScope = rememberCoroutineScope()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isImporting,
        onRefresh = onImportOldSms
    )

    // ✨ --- State and Lambda for Category Edit Dialog --- ✨
    var transactionToEditCategory by remember { mutableStateOf<Transaction?>(null) }
    var showCategoryEditDialog by remember { mutableStateOf(false) }

    val openCategoryEditDialog = { transaction: Transaction ->
        transactionToEditCategory = transaction
        showCategoryEditDialog = true
    }
    // ✨ --- End of Category Edit Dialog State and Lambda --- ✨

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tabbed_home_top_bar_title)) }, // Or a more specific title if this is for history
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) { // Ensure this "settings" route is valid for the passed NavController
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.tabbed_home_settings_icon_desc))
                    }
                }
            )
        }
    ) { innerScaffoldPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerScaffoldPadding)
                .pullRefresh(pullRefreshState)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                TabRow(
                    selectedTabIndex = pagerState.currentPage
                ) {
                    tabTitles.forEachIndexed { index, titleString ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = { Text(titleString, style = MaterialTheme.typography.labelLarge) },
                            icon = { Icon(tabIcons[index], contentDescription = titleString) },
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider()

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) { pageIndex ->
                    when (pageIndex) {
                        0 -> UpiTransactionListContent(
                            transactions = upiTransactions.take(100), // Or full list if this is history
                            onTransactionClick = { transaction -> // ✨ Pass lambda here ✨
                                openCategoryEditDialog(transaction)
                            }
                        )
                        1 -> UpiLiteSummaryListContent(
                            summaries = liteSummaries.take(100) // Or full list
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
        }
    }

    // ✨ --- Category Edit Dialog Invocation --- ✨
    if (showCategoryEditDialog && transactionToEditCategory != null) {
        EditCategoryDialog( // Ensure this composable is accessible (defined below or imported)
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

@Composable
fun EmptyStateView(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun UpiTransactionListContent(
    transactions: List<Transaction>,
    onTransactionClick: (Transaction) -> Unit // ✨ Added parameter ✨
) {
    if (transactions.isEmpty()) {
        EmptyStateView(message = stringResource(R.string.empty_state_no_upi_transactions))
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(transactions, key = { it.id }) { txn ->
            TransactionCard(
                transaction = txn,
                onClick = { onTransactionClick(txn) } // ✨ Call the passed lambda ✨
            )
        }
    }
}

@Composable
fun UpiLiteSummaryListContent(summaries: List<UpiLiteSummary>) {
    if (summaries.isEmpty()) {
        EmptyStateView(message = stringResource(R.string.empty_state_no_upi_lite_summaries))
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(summaries, key = { "${it.date}-${it.bank}" }) { summary ->
            UpiLiteSummaryCard(summary = summary) // UpiLiteSummaryCard doesn't have onClick for category
        }
    }
}

// ✨ EditCategoryDialog definition (move to ui/components if not already there and import it) ✨
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCategoryDialog(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onSaveCategory: (transactionId: Int, newCategory: String?) -> Unit
) {
    var categoryText by remember(transaction.id, transaction.category) { mutableStateOf(transaction.category ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.category_edit_icon_desc)) },
        title = {
            Text(
                if (transaction.category.isNullOrBlank()) stringResource(R.string.set_category_dialog_title)
                else stringResource(R.string.edit_category_dialog_title)
            )
        },
        text = {
            OutlinedTextField(
                value = categoryText,
                onValueChange = { categoryText = it },
                label = { Text(stringResource(R.string.category_label)) },
                placeholder = { Text(stringResource(R.string.category_textfield_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = {
                onSaveCategory(transaction.id, categoryText.trim().takeIf { it.isNotBlank() })
            }) {
                Text(stringResource(R.string.button_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_button_cancel))
            }
        }
    )
}
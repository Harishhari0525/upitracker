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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState // ✨ CRUCIAL IMPORT for collectAsState
import androidx.compose.runtime.getValue // ✨ CRUCIAL IMPORT for 'by' delegate with State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.upitracker.viewmodel.MainViewModel // Ensure this is correctly imported
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow // Ensure StateFlow is imported if directly referencing its type

@Composable
fun TabbedHomeScreen(
    mainViewModel: MainViewModel,
    navController: NavController,
    onImportOldSms: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tabTitles = listOf(stringResource(R.string.tab_upi), stringResource(R.string.tab_upi_lite))
    val tabIcons = listOf(Icons.Filled.AccountBalanceWallet, Icons.Filled.Summarize)

    // Line 49 and similar:
    // Ensure MainViewModel.transactions is public and a StateFlow<List<Transaction>>
    // Ensure MainViewModel.upiLiteSummaries is public and a StateFlow<List<UpiLiteSummary>>
    // Ensure MainViewModel.isImportingSms is public and a StateFlow<Boolean>
    val upiTransactions by mainViewModel.filteredUpiTransactions.collectAsState()
    val liteSummaries: List<UpiLiteSummary> by mainViewModel.upiLiteSummaries.collectAsState()
    val isImporting: Boolean by mainViewModel.isImportingSms.collectAsState()

    val pagerState = rememberPagerState { tabTitles.size }
    val coroutineScope = rememberCoroutineScope()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isImporting,
        onRefresh = onImportOldSms
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tabbed_home_top_bar_title)) },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
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
                        // The .take(100) should now work fine as upiTransactions is List<Transaction>
                        0 -> UpiTransactionListContent(transactions = upiTransactions.take(100))
                        1 -> UpiLiteSummaryListContent(summaries = liteSummaries.take(100))
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
fun UpiTransactionListContent(transactions: List<Transaction>) {
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
            TransactionCard(transaction = txn)
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
        items(summaries, key = { "${it.date}-${it.bank}" }) { summary -> // Assuming UpiLiteSummary.id exists and is unique
            UpiLiteSummaryCard(summary = summary)
        }
    }
}
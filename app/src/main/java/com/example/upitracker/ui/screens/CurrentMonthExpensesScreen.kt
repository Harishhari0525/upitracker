package com.example.upitracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource // ✨ Import stringResource ✨
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.upitracker.R // ✨ Import your app's R class ✨
import com.example.upitracker.data.Transaction
import com.example.upitracker.ui.components.TransactionCard
import com.example.upitracker.viewmodel.MainViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CurrentMonthExpensesScreen(
    mainViewModel: MainViewModel,
    onImportOldSms: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isImporting by mainViewModel.isImportingSms.collectAsState()
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isImporting,
        onRefresh = onImportOldSms
    )

    val currentMonthExpensesTotal by mainViewModel.currentMonthTotalExpenses.collectAsState()
    val currentMonthTransactions by mainViewModel.currentMonthDebitTransactions.collectAsState()

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    Box(modifier = modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Summary Card for Total Expenses
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
                        stringResource(R.string.home_current_month_expenses_title), // ✨
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        currencyFormatter.format(currentMonthExpensesTotal),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                stringResource(R.string.home_recent_expenses_title), // ✨
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (currentMonthTransactions.isEmpty() && !isImporting) {
                // This Box is different from the EmptyStateView used in TabbedHomeScreen for list content.
                // You can choose to use your generic EmptyStateView here too if you prefer.
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(), // Takes remaining space
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.home_no_expenses_this_month), // ✨
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f), // Takes remaining space
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                    // No contentPadding here if the outer Column already has padding for the whole screen
                ) {
                    items(currentMonthTransactions.take(10), key = { "current-month-txn-${it.id}" }) { transaction ->
                        TransactionCard(transaction = transaction)
                    }
                    if (currentMonthTransactions.size > 10) {
                        item {
                            TextButton(
                                onClick = { /* TODO: Navigate to full history, filtered to current month */
                                    mainViewModel.postSnackbarMessage("Navigation to full current month history: TBD")
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.home_view_all_this_month_button, currentMonthTransactions.size)) // ✨
                            }
                        }
                    }
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
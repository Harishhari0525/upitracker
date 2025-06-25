@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.upitracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import com.example.upitracker.data.RecurringRule
import com.example.upitracker.ui.components.TransactionCardWithMenu
import com.example.upitracker.viewmodel.BankMessageCount
import com.example.upitracker.viewmodel.MainViewModel
import com.example.upitracker.viewmodel.TransactionHistoryItem
import java.text.NumberFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CurrentMonthExpensesScreen(
    mainViewModel: MainViewModel,
    onViewAllClick: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Collect all the necessary state from the ViewModel
    val currentMonthExpensesTotal by mainViewModel.currentMonthTotalExpenses.collectAsState()
    val bankMessageCounts by mainViewModel.bankMessageCounts.collectAsState()
    val recentTransactions by mainViewModel.currentMonthExpenseItems.collectAsState()
    val recurringRules by mainViewModel.recurringRules.collectAsState() // State for our new section
    val isImporting by mainViewModel.isImportingSms.collectAsState()

    val pullRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        modifier = Modifier.fillMaxSize(),
        isRefreshing = isImporting,
        onRefresh = onRefresh,
        state = pullRefreshState,
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                modifier = Modifier.align(Alignment.TopCenter),
                state = pullRefreshState,
                isRefreshing = isImporting
            )
        }
    ) {

        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                //     .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp), // Increased spacing for a cleaner look
            contentPadding = PaddingValues(bottom = 10.dp)
        ) {
            stickyHeader {
                Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                    TotalExpensesHeroCard(total = currentMonthExpensesTotal)
                    // Add a spacer to separate the sticky header from the scrolling content
                    Spacer(Modifier.height(20.dp))
                }
            }

            // --- Card 2: Bank Activity (Can be removed if you want further simplification) ---
            item {
                SectionHeader(title = "Bank Activity")
                Spacer(Modifier.height(8.dp))
                BankActivityCard(counts = bankMessageCounts)
            }

            // --- Section 3: Upcoming Payments (The new feature) ---
            item {
                UpcomingPaymentsSection(rules = recurringRules)
            }

            // --- Section 4: Recent Transactions ---
            item {
                // We group the header and the list inside a single Column
                Column(modifier = Modifier.fillMaxWidth()) {
                    RecentTransactionsHeader(onViewAllClick = onViewAllClick)

                    // Use a smaller, explicit Spacer for a tighter look
                    Spacer(Modifier.height(12.dp))

                    if (recentTransactions.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No transactions this month yet.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        // Since we are only ever showing 3 items, a simple Column is efficient
                        // and allows us to control the spacing perfectly.
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            recentTransactions.take(3).forEach { item ->
                                if (item is TransactionHistoryItem) {
                                    TransactionCardWithMenu(
                                        transaction = item.transaction,
                                        onClick = { /* ... */ },
                                        onArchive = {
                                            mainViewModel.toggleTransactionArchiveStatus(
                                                it,
                                                true
                                            )
                                        },
                                        onDelete = { mainViewModel.deleteTransaction(it) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpcomingPaymentsSection(rules: List<RecurringRule>) {
    val upcomingRules = rules.take(3) // We only want to show the next 3 upcoming rules

    // This entire section will only appear if there is at least one upcoming rule
    if (upcomingRules.isNotEmpty()) {
        Column {
            SectionHeader(title = "Upcoming Payments")
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    upcomingRules.forEach { rule ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(rule.description, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)

                            val daysUntil = TimeUnit.MILLISECONDS.toDays(rule.nextDueDate - System.currentTimeMillis())
                            val dueText = when {
                                daysUntil < 0 -> "Overdue"
                                daysUntil == 0L -> "Today"
                                daysUntil == 1L -> "Tomorrow"
                                else -> "in $daysUntil days"
                            }
                            Text(dueText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TotalExpensesHeroCard(total: Double) {
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("en").setRegion("IN").build()) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Current Month's Expenses", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text(
                    currencyFormatter.format(total),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 40.sp
                )
            }
            Icon(
                imageVector = Icons.Filled.TrackChanges,
                contentDescription = "Monthly Expenses",
                modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)).padding(10.dp),
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
private fun BankActivityCard(counts: List<BankMessageCount>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
    ) {
        if (counts.isNotEmpty()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                counts.take(3).forEach { item ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(item.bankName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text("${item.count} messages", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
internal fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun RecentTransactionsHeader(onViewAllClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionHeader(title = "Recent Transactions")
        TextButton(onClick = onViewAllClick) {
            Text("View All")
            Spacer(Modifier.width(4.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "View All Transactions", modifier = Modifier.size(18.dp))
        }
    }
}
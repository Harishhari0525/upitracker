@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.upitracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.upitracker.ui.components.TransactionCardWithMenu
import com.example.upitracker.viewmodel.BankMessageCount
import com.example.upitracker.viewmodel.MainViewModel
import com.example.upitracker.viewmodel.SpendingTrend
import com.example.upitracker.viewmodel.SummaryHistoryItem
import com.example.upitracker.viewmodel.TransactionHistoryItem
import java.text.NumberFormat
import java.util.*

@Composable
fun CurrentMonthExpensesScreen(
    mainViewModel: MainViewModel,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentMonthExpensesTotal by mainViewModel.currentMonthTotalExpenses.collectAsState()
    val bankMessageCounts by mainViewModel.bankMessageCounts.collectAsState()
    val highestSpendingDay by mainViewModel.highestSpendingDay.collectAsState()
    val highestSpendingCategory by mainViewModel.highestSpendingCategory.collectAsState()
    val mostUsedApp by mainViewModel.mostUsedUpiApp.collectAsState()
    val recentTransactions by mainViewModel.currentMonthExpenseItems.collectAsState()

    val trends = listOf(highestSpendingDay, highestSpendingCategory, mostUsedApp)
    val trendIcons = mapOf(
        "Highest Spend Day" to Icons.Default.CalendarToday,
        "Top Spending Category" to Icons.Default.Category,
        "Most Used App" to Icons.Default.PhoneAndroid
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            // first apply system‐bars as padding to the *layout* itself
            .windowInsetsPadding(WindowInsets.systemBars)
            // then apply your own 16.dp padding around the content
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        // you can leave contentPadding empty now
        contentPadding = PaddingValues(0.dp)
    ) {
        item {
            TotalExpensesHeroCard(total = currentMonthExpensesTotal)
        }

        item {
            SectionHeader(title = "Bank Activity")
            BankActivityCard(counts = bankMessageCounts)
        }

        item {
            SectionHeader(title = "Spending Trends")
        }

        items(trends) { trend ->
            TrendCard(
                trend = trend,
                icon = trendIcons[trend.title] ?: Icons.Default.Info
            )
        }

        item {
            RecentTransactionsHeader(onViewAllClick = onViewAllClick)
        }

        if (recentTransactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No transactions this month yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(recentTransactions.take(3), key = { item ->
                when (item) {
                    is TransactionHistoryItem -> "txn-home-${item.transaction.id}-${item.transaction.date}"
                    is SummaryHistoryItem -> "summary-home-${item.summary.id}-${item.summary.date}"
                }
            }) { item ->
                if (item is TransactionHistoryItem) {
                    TransactionCardWithMenu(
                        transaction = item.transaction,
                        onClick = { /* Detail view can be handled here if needed */ },
                        onArchive = { mainViewModel.toggleTransactionArchiveStatus(it, true) },
                        onDelete = { mainViewModel.deleteTransaction(it) }
                    )
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Current Month's Expenses",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
                    .padding(10.dp),
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

@Composable
private fun TrendCard(trend: SpendingTrend, icon: ImageVector) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = trend.title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(trend.title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(2.dp))
                Text(trend.value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(trend.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun RecentTransactionsHeader(onViewAllClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionHeader(title = "Recent Transactions")
        TextButton(onClick = onViewAllClick) {
            Text("View All")
            Spacer(Modifier.width(4.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "View All Transactions", modifier = Modifier.size(18.dp))
        }
    }
}
@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.upitracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.upitracker.data.Transaction
import com.example.upitracker.ui.components.expressive.ExpressiveEmptyState
import com.example.upitracker.ui.components.expressive.ExpressiveHeroCard
import com.example.upitracker.ui.components.expressive.ExpressiveSectionHeader
import com.example.upitracker.ui.components.expressive.ExpressiveStatCard
import com.example.upitracker.ui.components.expressive.ExpressiveTopBar
import com.example.upitracker.ui.components.expressive.ExpressiveTransactionCard
import com.example.upitracker.util.ExpressiveTokens
import com.example.upitracker.viewmodel.MainViewModel
import com.example.upitracker.viewmodel.TransactionHistoryItem
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun InsightGridScreen(
    mainViewModel: MainViewModel,
    onNavigateToHistory: () -> Unit,
    onBackClick: () -> Unit
) {
    val velocityState by mainViewModel.spendingVelocityState.collectAsState()
    val currentMonthTotal by mainViewModel.currentMonthTotalExpenses.collectAsState()
    val previousMonthTotal by mainViewModel.previousMonthTotalExpenses.collectAsState()
    val allHistoryListItems by mainViewModel.currentMonthExpenseItems.collectAsState()

    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(
            Locale.Builder()
                .setLanguage("en")
                .setRegion("IN")
                .build()
        )
    }

    val transactions = remember(allHistoryListItems) {
        allHistoryListItems
            .filterIsInstance<TransactionHistoryItem>()
            .map { it.transaction }
    }

    val diagnostics = remember(currentMonthTotal, velocityState.totalBudget) {
        calculateDiagnostics(
            totalSpent = currentMonthTotal,
            totalBudget = velocityState.totalBudget
        )
    }

    val debitTransactions = remember(transactions) {
        transactions.filter { it.type.equals("DEBIT", ignoreCase = true) }
    }

    val topCategories = remember(debitTransactions) {
        debitTransactions
            .groupBy { it.category?.takeIf { category -> category.isNotBlank() } ?: "Uncategorized" }
            .mapValues { (_, txns) -> txns.sumOf { it.amount } }
            .entries
            .sortedByDescending { it.value }
            .take(3)
    }

    val uncategorizedCount = remember(transactions) {
        transactions.count { it.category.isNullOrBlank() }
    }

    val monthChangePercent = remember(currentMonthTotal, previousMonthTotal) {
        if (previousMonthTotal > 0) {
            (((currentMonthTotal - previousMonthTotal) / previousMonthTotal) * 100).roundToInt()
        } else {
            0
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            ExpressiveTopBar(
                title = "Insights",
                subtitle = "Contextual signals from your payment history",
                showBackButton = true,
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = ExpressiveTokens.spacing.lg,
                top = ExpressiveTokens.spacing.md,
                end = ExpressiveTokens.spacing.lg,
                bottom = ExpressiveTokens.spacing.huge
            ),
            verticalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.md)
        ) {
            item {
                val budgetLabel = if (velocityState.totalBudget > 0) {
                    "Budget ${currencyFormatter.format(velocityState.totalBudget)}"
                } else {
                    "No budget set"
                }

                ExpressiveHeroCard(
                    title = "This Month",
                    amount = currencyFormatter.format(currentMonthTotal),
                    subtitle = diagnostics.paceStatusSubtitle,
                    debitLabel = "${transactions.size} transaction(s)",
                    creditLabel = budgetLabel
                )
            }

            item {
                ExpressiveSectionHeader(
                    title = "What changed",
                    subtitle = "Compared with last month"
                )
            }

            item {
                MonthChangeCard(
                    previousMonthTotal = previousMonthTotal,
                    monthChangePercent = monthChangePercent,
                    currencyFormatter = currencyFormatter
                )
            }

            item {
                ExpressiveSectionHeader(
                    title = "Needs attention",
                    subtitle = "Only signals that need action"
                )
            }

            item {
                AttentionSignals(
                    diagnostics = diagnostics,
                    uncategorizedCount = uncategorizedCount,
                    topCategories = topCategories,
                    currencyFormatter = currencyFormatter,
                    onNavigateToHistory = onNavigateToHistory
                )
            }

            item {
                ExpressiveSectionHeader(
                    title = "Recent activity",
                    subtitle = if (transactions.isEmpty()) "No recent entries recorded" else "Latest transactions",
                    actionText = if (transactions.isNotEmpty()) "View all" else null,
                    onActionClick = if (transactions.isNotEmpty()) onNavigateToHistory else null
                )
            }

            if (transactions.isEmpty()) {
                item {
                    ExpressiveEmptyState(
                        title = "No transactions found",
                        message = "Sync SMS or add transactions manually to generate insights.",
                        icon = Icons.AutoMirrored.Rounded.ReceiptLong
                    )
                }
            } else {
                items(
                    items = transactions.take(3),
                    key = { "simple-insight-txn-${it.id}" }
                ) { transaction ->
                    ExpressiveTransactionCard(
                        title = transaction.displayTitle(),
                        amount = transaction.formattedAmount(currencyFormatter),
                        type = transaction.type,
                        dateText = transaction.formattedDate(),
                        category = transaction.category ?: "Uncategorized",
                        bankName = transaction.bankName,
                        note = transaction.note.takeIf { it.isNotBlank() },
                        onClick = {
                            mainViewModel.selectTransaction(transaction.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthChangeCard(
    previousMonthTotal: Double,
    monthChangePercent: Int,
    currencyFormatter: NumberFormat
) {
    Card(
        shape = ExpressiveTokens.corners.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Last month",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = currencyFormatter.format(previousMonthTotal),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                val isIncrease = monthChangePercent > 0
                val changeColor = if (isIncrease) MaterialTheme.colorScheme.error else Color(0xFF16A34A)
                Text(
                    text = if (previousMonthTotal <= 0) {
                        "New"
                    } else if (isIncrease) {
                        "+$monthChangePercent%"
                    } else {
                        "$monthChangePercent%"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = changeColor
                )
            }

            Text(
                text = when {
                    previousMonthTotal <= 0 -> "Not enough previous-month data yet."
                    monthChangePercent > 10 -> "Spending is noticeably higher. Check top categories below."
                    monthChangePercent < -10 -> "Spending is lower than last month."
                    else -> "Spending is mostly stable."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AttentionSignals(
    diagnostics: InsightDiagnostics,
    uncategorizedCount: Int,
    topCategories: List<Map.Entry<String, Double>>,
    currencyFormatter: NumberFormat,
    onNavigateToHistory: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.md)) {
        if (diagnostics.isPaceRisk) {
            ExpressiveStatCard(
                title = "Budget pace risk",
                value = diagnostics.paceStatusTitle,
                subtitle = diagnostics.paceStatusSubtitle,
                icon = Icons.Rounded.WarningAmber,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (uncategorizedCount > 0) {
            ExpressiveStatCard(
                title = "Uncategorized",
                value = "$uncategorizedCount txn(s)",
                subtitle = "Categorize these to improve budgets and rules",
                icon = Icons.Rounded.Category,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToHistory)
            )
        }

        if (topCategories.isNotEmpty()) {
            Card(
                shape = ExpressiveTokens.corners.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Top categories",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    topCategories.forEach { category ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = category.key,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = currencyFormatter.format(category.value),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        if (!diagnostics.isPaceRisk && uncategorizedCount == 0 && topCategories.isEmpty()) {
            ExpressiveEmptyState(
                title = "No spending signals yet",
                message = "Import transactions to see monthly insights.",
                icon = Icons.AutoMirrored.Rounded.ReceiptLong
            )
        }
    }
}

private data class InsightDiagnostics(
    val paceStatusTitle: String,
    val paceStatusSubtitle: String,
    val isPaceRisk: Boolean
)

private fun calculateDiagnostics(
    totalSpent: Double,
    totalBudget: Double
): InsightDiagnostics {
    val calendar = Calendar.getInstance()
    val currentDay = calendar.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
    val totalDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
    val budgetUsagePercent = if (totalBudget > 0) (totalSpent / totalBudget) * 100 else 0.0
    val expectedElapsedPercent = (currentDay.toFloat() / totalDays.toFloat()) * 100

    return when {
        totalBudget > 0 && totalSpent > totalBudget -> InsightDiagnostics(
            paceStatusTitle = "Exceeded",
            paceStatusSubtitle = "Budget limit crossed.",
            isPaceRisk = true
        )

        totalBudget > 0 && budgetUsagePercent > (expectedElapsedPercent + 15) -> InsightDiagnostics(
            paceStatusTitle = "Burn warning",
            paceStatusSubtitle = "Spending is faster than expected for this point in the month.",
            isPaceRisk = true
        )

        totalSpent == 0.0 -> InsightDiagnostics(
            paceStatusTitle = "No spend",
            paceStatusSubtitle = "No expenses recorded for this month yet.",
            isPaceRisk = false
        )

        else -> InsightDiagnostics(
            paceStatusTitle = "Healthy",
            paceStatusSubtitle = "Spending pace looks normal for this month.",
            isPaceRisk = false
        )
    }
}

private fun Transaction.displayTitle(): String {
    return when {
        senderOrReceiver.isDisplayablePartyName() -> senderOrReceiver
        !bankName.isNullOrBlank() && bankName != "Other Bank" -> bankName
        description.isNotBlank() -> description
        else -> "UPI Payment"
    }
}

private fun String.isDisplayablePartyName(): Boolean {
    val value = trim()
    if (value.isBlank() || value == "Manual Entry") return false
    if (value.length > 60) return false

    val lower = value.lowercase()
    return !(
        lower.contains(" debited") ||
            lower.contains(" credited") ||
            lower.contains(" account") ||
            lower.contains(" balance") ||
            lower.contains(" transaction") ||
            lower.contains(" has been ") ||
            lower.contains(" available ")
    )
}

private fun Transaction.formattedAmount(formatter: NumberFormat): String {
    val prefix = if (type.equals("CREDIT", ignoreCase = true)) "+" else "-"
    return "$prefix${formatter.format(amount)}"
}

private fun Transaction.formattedDate(): String {
    return SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(date))
}

@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.upitracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.upitracker.data.Transaction
import com.example.upitracker.ui.components.TrendCard
import com.example.upitracker.ui.components.expressive.ExpressiveEmptyState
import com.example.upitracker.ui.components.expressive.ExpressiveHeroCard
import com.example.upitracker.ui.components.expressive.ExpressiveSectionHeader
import com.example.upitracker.ui.components.expressive.ExpressiveStatCard
import com.example.upitracker.ui.components.expressive.ExpressiveTopBar
import com.example.upitracker.ui.components.expressive.ExpressiveTransactionCard
import com.example.upitracker.util.ExpressiveTokens
import com.example.upitracker.viewmodel.MainViewModel
import com.example.upitracker.viewmodel.SpendingTrend
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun InsightGridScreen(
    mainViewModel: MainViewModel,
    onNavigateToHistory: () -> Unit
) {
    // ✨ FIX 2: Connect directly to your true, trusted MainViewModel database budget parameters
    val velocityState by mainViewModel.spendingVelocityState.collectAsState()
    val currentMonthTotal by mainViewModel.currentMonthTotalExpenses.collectAsState()
    val allHistoryListItems by mainViewModel.currentMonthExpenseItems.collectAsState()

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.Builder()
        .setLanguage("en")
        .setRegion("IN")
        .build()) }

    val transactions = remember(allHistoryListItems) {
        allHistoryListItems
            .filterIsInstance<com.example.upitracker.viewmodel.TransactionHistoryItem>()
            .map { it.transaction }
    }

    // Advanced dynamic metrics engine calculations
    val diagnosticMetrics = remember(currentMonthTotal, velocityState, transactions) {
        calculateDiagnostics(currentMonthTotal, velocityState.totalBudget, transactions)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            ExpressiveTopBar(
                title = "Insights",
                subtitle = "Algorithmic spending diagnostics and velocity maps"
            )
        }
    ) { paddingValues ->
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = ExpressiveTokens.spacing.lg,
                top = ExpressiveTokens.spacing.md,
                end = ExpressiveTokens.spacing.lg,
                bottom = ExpressiveTokens.spacing.huge
            ),
            horizontalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.md),
            verticalItemSpacing = ExpressiveTokens.spacing.md
        ) {
            // Primary Hero Section Card Block
            item(span = StaggeredGridItemSpan.FullLine) {
                val budgetLabel = if (velocityState.totalBudget > 0) {
                    "Limit: ${currencyFormatter.format(velocityState.totalBudget)}"
                } else {
                    "No target budget set"
                }

                ExpressiveHeroCard(
                    title = "Monthly Diagnostics",
                    amount = currencyFormatter.format(currentMonthTotal),
                    subtitle = diagnosticMetrics.paceStatusSubtitle,
                    debitLabel = "Spent ${currencyFormatter.format(currentMonthTotal)}",
                    creditLabel = budgetLabel
                )
            }

            // --- SECTION 1: VELOCITY CONTROL MAP ---
            item(span = StaggeredGridItemSpan.FullLine) {
                ExpressiveSectionHeader(
                    title = "Velocity Control Map",
                    subtitle = "Projections and safe pacing thresholds matching your real budget limits"
                )
            }

            item {
                TrendCard(
                    trend = SpendingTrend(
                        title = "Projected Spend",
                        value = currencyFormatter.format(diagnosticMetrics.projectedMonthEnd),
                        subtitle = "Estimated month-end total"
                    ),
                    icon = Icons.Rounded.Timeline,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                ExpressiveStatCard(
                    title = "Safe Daily Pace",
                    value = if (diagnosticMetrics.remainingBudget > 0) currencyFormatter.format(diagnosticMetrics.safeDailyPace) else "₹0",
                    subtitle = "${velocityState.daysRemaining} days remaining",
                    icon = Icons.Rounded.Speed,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // --- SECTION 2: THE MICRO-TRANSACTION LEAK DETECTOR ---
            item(span = StaggeredGridItemSpan.FullLine) {
                ExpressiveSectionHeader(
                    title = "Micro-Transaction Leak",
                    subtitle = "High-frequency quick payments under ₹50 draining your balance"
                )
            }

            item {
                ExpressiveStatCard(
                    title = "Drained via Leaks",
                    value = currencyFormatter.format(diagnosticMetrics.leakTotalAmount),
                    subtitle = "Total spent in small slips",
                    icon = Icons.Rounded.ElectricBolt,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                ExpressiveStatCard(
                    title = "Leak Frequency",
                    value = "${diagnosticMetrics.leakCount} Payments",
                    subtitle = "Transactions under ₹50",
                    icon = Icons.Rounded.RestartAlt,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // --- SECTION 3: LIFESTYLE & PATTERN MATRIX ---
            item(span = StaggeredGridItemSpan.FullLine) {
                ExpressiveSectionHeader(
                    title = "Lifestyle Balance Split",
                    subtitle = "Evaluating spikes during weekend activity vs weekday tracking"
                )
            }

            item {
                ExpressiveStatCard(
                    title = "Weekend Share",
                    value = "${diagnosticMetrics.weekendPercentage}%",
                    subtitle = "Total spent on Sat & Sun",
                    icon = Icons.Rounded.Weekend,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                ExpressiveStatCard(
                    title = "Pace Health",
                    value = diagnosticMetrics.paceStatusTitle,
                    subtitle = "Based on daily burn averages",
                    icon = if (diagnosticMetrics.isPaceRisk) Icons.Rounded.WarningAmber else Icons.Rounded.Insights,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // --- SECTION 4: RECENT LOGS TRANSACTION HISTORY LINE ---
            item(span = StaggeredGridItemSpan.FullLine) {
                ExpressiveSectionHeader(
                    title = "Recent Activity Log",
                    subtitle = if (transactions.isEmpty()) "No recent entries recorded" else "Latest localized payments",
                    actionText = if (transactions.isNotEmpty()) "View all" else null,
                    onActionClick = if (transactions.isNotEmpty()) onNavigateToHistory else null
                )
            }

            if (transactions.isEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    ExpressiveEmptyState(
                        title = "No tracking logs found",
                        message = "Import old SMS history or process live alerts to generate pattern intelligence models.",
                        icon = Icons.AutoMirrored.Rounded.ReceiptLong
                    )
                }
            } else {
                items(
                    items = transactions.take(3), // Limit home display density grid lines safely
                    key = { "insight-txn-${it.id}" },
                    span = { StaggeredGridItemSpan.FullLine }
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

// Global immutable structure for data diagnostics parameters mapping pass
private data class ComprehensiveDiagnostics(
    val projectedMonthEnd: Double,
    val remainingBudget: Double,
    val safeDailyPace: Double,
    val leakCount: Int,
    val leakTotalAmount: Double,
    val weekendPercentage: Int,
    val paceStatusTitle: String,
    val paceStatusSubtitle: String,
    val isPaceRisk: Boolean
)

private fun calculateDiagnostics(
    totalSpent: Double,
    totalBudget: Double,
    transactions: List<Transaction>
): ComprehensiveDiagnostics {
    val calendar = Calendar.getInstance()
    val currentDay = calendar.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
    val totalDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
    val daysRemaining = (totalDays - currentDay).coerceAtLeast(0)

    // 1. Core Velocity Tracking Math Formulas
    val averagePerDay = if (currentDay > 0) totalSpent / currentDay else 0.0
    val projectedMonthEnd = averagePerDay * totalDays
    val remainingBudget = (totalBudget - totalSpent).coerceAtLeast(0.0)
    val safeDailyPace = if (daysRemaining > 0) remainingBudget / daysRemaining else 0.0

    // 2. Micro-Transaction Leak Logic Engine Pass (Isolating entries under ₹50)
    val smallLeaks = transactions.filter { it.type.equals("DEBIT", ignoreCase = true) && it.amount > 0 && it.amount <= 50.0 }
    val leakCount = smallLeaks.size
    val leakTotalAmount = smallLeaks.sumOf { it.amount }

    // 3. Weekend vs Weekday Pattern Tracker
    var weekendSpend = 0.0
    var totalDebitSpend = 0.0

    transactions.filter { it.type.equals("DEBIT", ignoreCase = true) }.forEach { txn ->
        totalDebitSpend += txn.amount
        calendar.timeInMillis = txn.date
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            weekendSpend += txn.amount
        }
    }

    val weekendPercentage = if (totalDebitSpend > 0) {
        ((weekendSpend / totalDebitSpend) * 100).roundToInt().coerceIn(0, 100)
    } else {
        0
    }

    // 4. Automated Pace Analysis Diagnostician Engine Check
    val budgetUsagePercent = if (totalBudget > 0) (totalSpent / totalBudget) * 100 else 0.0
    val expectedElapsedPercent = (currentDay.toFloat() / totalDays.toFloat()) * 100

    val paceStatusTitle: String
    val paceStatusSubtitle: String
    var isPaceRisk = false

    when {
        totalBudget > 0 && totalSpent > totalBudget -> {
            paceStatusTitle = "Exceeded"
            paceStatusSubtitle = "Budget limit crossed. Safe pacing terminated."
            isPaceRisk = true
        }
        totalBudget > 0 && budgetUsagePercent > (expectedElapsedPercent + 15) -> {
            paceStatusTitle = "Burn Warning"
            paceStatusSubtitle = "Spending faster than expected calendar pace."
            isPaceRisk = true
        }
        totalSpent == 0.0 -> {
            paceStatusTitle = "Pristine"
            paceStatusSubtitle = "No recorded expenditures for this calendar window."
        }
        else -> {
            paceStatusTitle = "Healthy"
            paceStatusSubtitle = "Your monthly velocity matches active pacing targets."
        }
    }

    return ComprehensiveDiagnostics(
        projectedMonthEnd = projectedMonthEnd,
        remainingBudget = remainingBudget,
        safeDailyPace = safeDailyPace,
        leakCount = leakCount,
        leakTotalAmount = leakTotalAmount,
        weekendPercentage = weekendPercentage,
        paceStatusTitle = paceStatusTitle,
        paceStatusSubtitle = paceStatusSubtitle,
        isPaceRisk = isPaceRisk
    )
}

private fun Transaction.displayTitle(): String {
    return when {
        senderOrReceiver.isNotBlank() && senderOrReceiver != "Manual Entry" -> senderOrReceiver
        description.isNotBlank() -> description
        else -> "UPI Payment"
    }
}

private fun Transaction.formattedAmount(formatter: NumberFormat): String {
    val prefix = if (type.equals("CREDIT", ignoreCase = true)) "+" else "-"
    return "$prefix${formatter.format(amount)}"
}

private fun Transaction.formattedDate(): String {
    return SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(date))
}
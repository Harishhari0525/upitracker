@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.upitracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    onNavigateToHistory: () -> Unit,
    onBackClick: () -> Unit
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

    var selectedTagDetail by remember { mutableStateOf<TagSpendingStat?>(null) }

    val tagStats = remember(transactions) {
        val tagMap = mutableMapOf<String, MutableList<Transaction>>()
        transactions.filter { it.type.equals("DEBIT", ignoreCase = true) }.forEach { txn ->
            val tags = extractAllTags(txn)
            tags.forEach { tag ->
                val normalizedTag = tag.lowercase()
                val list = tagMap.getOrPut(normalizedTag) { mutableListOf() }
                list.add(txn)
            }
        }
        tagMap.map { (tagName, txns) ->
            TagSpendingStat(
                tagName = tagName,
                totalAmount = txns.sumOf { it.amount },
                transactionCount = txns.size,
                transactionsList = txns.sortedByDescending { it.date }
            )
        }.sortedByDescending { it.totalAmount }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            ExpressiveTopBar(
                title = "Insights",
                subtitle = "Algorithmic spending diagnostics and velocity maps",
                showBackButton = true,
                onBackClick = onBackClick
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

            // --- SECTION MoM: MONTH-OVER-MONTH SUMMARY ---
            item(span = StaggeredGridItemSpan.FullLine) {
                ExpressiveSectionHeader(
                    title = "Month-over-Month Comparison",
                    subtitle = "How your current monthly burn rate compares to last month"
                )
            }

            item(span = StaggeredGridItemSpan.FullLine) {
                val previousMonthTotal by mainViewModel.previousMonthTotalExpenses.collectAsState()

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
                        val percentDiff = if (previousMonthTotal > 0) {
                            (((currentMonthTotal - previousMonthTotal) / previousMonthTotal) * 100).roundToInt()
                        } else {
                            0
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Last Month Total",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = currencyFormatter.format(previousMonthTotal),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            val isDecrease = percentDiff <= 0
                            val badgeColor = if (isDecrease) Color(0xFF16A34A) else MaterialTheme.colorScheme.error
                            val badgeLabel = if (isDecrease) "${percentDiff}%" else "+${percentDiff}%"
                            val badgeText = if (isDecrease) "Decrease" else "Increase"

                            Card(
                                colors = CardDefaults.cardColors(containerColor = badgeColor.copy(alpha = 0.15f)),
                                shape = ExpressiveTokens.corners.medium
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = badgeLabel,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = badgeColor
                                    )
                                    Text(
                                        text = badgeText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = badgeColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        val insightText = when {
                            percentDiff < -5 -> "Excellent! You are spending significantly less than last month. Keep up this burn rate to save more."
                            percentDiff in -5..5 -> "Your spending is relatively stable compared to last month. You're maintaining a steady financial pace."
                            else -> "Notice: You are spending more than last month. Check the leak detector or category breakdowns to find high expenditure areas."
                        }

                        Text(
                            text = insightText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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

            // --- SECTION: FORECAST PROJECTION ---
            item(span = StaggeredGridItemSpan.FullLine) {
                ExpressiveSectionHeader(
                    title = "Smart Forecast Projection",
                    subtitle = "Cumulative actual spend vs. estimated burn rate projection"
                )
            }

            item(span = StaggeredGridItemSpan.FullLine) {
                Card(
                    shape = ExpressiveTokens.corners.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SmartForecastChart(
                            totalBudget = velocityState.totalBudget,
                            transactions = transactions,
                            currencyFormatter = currencyFormatter
                        )
                    }
                }
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

            // --- SECTION: TAG TRACKER ---
            if (tagStats.isNotEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    ExpressiveSectionHeader(
                        title = "Interactive Tag Tracker",
                        subtitle = "Spending grouped by #hashtags in transaction descriptions or notes"
                    )
                }

                item(span = StaggeredGridItemSpan.FullLine) {
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
                            val totalDebitSpend = transactions.filter { it.type.equals("DEBIT", ignoreCase = true) }.sumOf { it.amount }
                            tagStats.take(5).forEach { tagStat ->
                                val proportion = if (totalDebitSpend > 0) (tagStat.totalAmount / totalDebitSpend).toFloat() else 0f
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedTagDetail = tagStat }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = tagStat.tagName,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            
                                            Text(
                                                text = "${currencyFormatter.format(tagStat.totalAmount)} (${tagStat.transactionCount} txns)",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(6.dp))

                                        LinearProgressIndicator(
                                        progress = { proportion },
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                                                         alpha = 0.1f
                                                                                     ),
                                        strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                                        )
                                    }
                                    
                                    Icon(
                                        imageVector = Icons.Rounded.ChevronRight,
                                        contentDescription = "Details",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
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

    if (selectedTagDetail != null) {
        val tagStat = selectedTagDetail!!
        ModalBottomSheet(
            onDismissRequest = { selectedTagDetail = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = ExpressiveTokens.corners.extraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = tagStat.tagName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${tagStat.transactionCount} transactions",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Text(
                        text = currencyFormatter.format(tagStat.totalAmount),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(tagStat.transactionsList) { transaction ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = transaction.displayTitle(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = transaction.formattedDate(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Text(
                                text = currencyFormatter.format(transaction.amount),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
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

// ✨ ADVANCED FORECAST & TAG ANALYTICS HELPERS

private data class TagSpendingStat(
    val tagName: String,
    val totalAmount: Double,
    val transactionCount: Int,
    val transactionsList: List<Transaction>
)

private fun extractAllTags(transaction: Transaction): List<String> {
    val tagList = mutableListOf<String>()
    
    // Split the tags field by spaces
    if (transaction.tags.isNotBlank()) {
        transaction.tags.split("\\s+".toRegex()).forEach { tag ->
            val cleanTag = tag.trim()
            if (cleanTag.startsWith("#") && cleanTag.length > 1) {
                tagList.add(cleanTag)
            }
        }
    }
    
    // Scan description and note
    val scanText = "${transaction.description} ${transaction.note}"
    Regex("#\\w+").findAll(scanText).forEach { match ->
        val tag = match.value
        if (!tagList.contains(tag)) {
            tagList.add(tag)
        }
    }
    
    return tagList
}

@Composable
private fun SmartForecastChart(
    totalBudget: Double,
    transactions: List<Transaction>,
    currencyFormatter: NumberFormat
) {
    val calendar = Calendar.getInstance()
    val currentDay = calendar.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
    val totalDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
    val currentYear = calendar.get(Calendar.YEAR)
    val currentMonth = calendar.get(Calendar.MONTH)

    // Calculate daily spends
    val spendByDay = remember(transactions) {
        val map = mutableMapOf<Int, Double>()
        val cal = Calendar.getInstance()
        transactions.filter {
            it.type.equals("DEBIT", ignoreCase = true)
        }.forEach { txn ->
            cal.timeInMillis = txn.date
            if (cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.MONTH) == currentMonth) {
                val day = cal.get(Calendar.DAY_OF_MONTH)
                map[day] = (map[day] ?: 0.0) + txn.amount
            }
        }
        map
    }

    val cumulativeSpend = remember(spendByDay, currentDay, totalDays) {
        val arr = DoubleArray(totalDays + 1)
        var sum = 0.0
        for (day in 1..currentDay) {
            sum += spendByDay[day] ?: 0.0
            arr[day] = sum
        }
        arr
    }

    val projectedSpend = remember(cumulativeSpend, currentDay, totalDays) {
        val arr = DoubleArray(totalDays + 1)
        val todaySpend = cumulativeSpend[currentDay]
        val avgDaily = if (currentDay > 0) todaySpend / currentDay else 0.0
        for (day in 1..totalDays) {
            if (day <= currentDay) {
                arr[day] = cumulativeSpend[day]
            } else {
                arr[day] = todaySpend + avgDaily * (day - currentDay)
            }
        }
        arr
    }

    val projectedMonthEnd = projectedSpend[totalDays]
    val maxVal = maxOf(totalBudget, projectedMonthEnd, cumulativeSpend[currentDay]).coerceAtLeast(100.0) * 1.15

    // Color definitions matching M3 Expressive
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val errorColor = MaterialTheme.colorScheme.error
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Top summary row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Forecast Month-End",
                    style = MaterialTheme.typography.labelMedium,
                    color = onSurfaceVariant
                )
                Text(
                    text = currencyFormatter.format(projectedMonthEnd),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Status Badge
            val isOverBudget = totalBudget > 0 && projectedMonthEnd > totalBudget
            val statusText = when {
                totalBudget <= 0 -> "No Limit Set"
                isOverBudget -> "Over Budget Forecast"
                else -> "On Track"
            }
            val statusColor = when {
                totalBudget <= 0 -> MaterialTheme.colorScheme.secondary
                isOverBudget -> MaterialTheme.colorScheme.error
                else -> Color(0xFF16A34A)
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.15f)),
                shape = ExpressiveTokens.corners.medium
            ) {
                Text(
                    text = statusText,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
        }

        // Canvas chart
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val paddingLeft = 70f
                val paddingRight = 20f
                val paddingTop = 20f
                val paddingBottom = 40f

                val chartWidth = width - paddingLeft - paddingRight
                val chartHeight = height - paddingTop - paddingBottom

                // Helper functions to map coordinates
                fun getX(day: Int): Float {
                    return paddingLeft + ((day - 1).toFloat() / (totalDays - 1).toFloat()) * chartWidth
                }

                fun getY(value: Double): Float {
                    val ratio = (value / maxVal).coerceIn(0.0, 1.0)
                    return (paddingTop + chartHeight - ratio * chartHeight).toFloat()
                }

                // 1. Draw horizontal grid lines & labels
                val gridLines = 4
                for (i in 0..gridLines) {
                    val gridRatio = i.toFloat() / gridLines
                    val gridY = paddingTop + chartHeight - gridRatio * chartHeight
                    val gridVal = gridRatio * maxVal
                    
                    // Faint horizontal line
                    drawLine(
                        color = outlineVariant.copy(alpha = 0.25f),
                        start = androidx.compose.ui.geometry.Offset(paddingLeft, gridY),
                        end = androidx.compose.ui.geometry.Offset(width - paddingRight, gridY),
                        strokeWidth = 1f
                    )

                    // Y Label
                    val labelText = if (gridVal >= 1000) "${(gridVal / 1000).toInt()}k" else "${gridVal.toInt()}"
                    drawContext.canvas.nativeCanvas.drawText(
                        "₹$labelText",
                        10f,
                        gridY + 10f,
                        android.graphics.Paint().apply {
                            color = onSurfaceVariant.toArgb()
                            textSize = 28f
                            textAlign = android.graphics.Paint.Align.LEFT
                        }
                    )
                }

                // 2. Draw Budget Limit line (red dashed line)
                if (totalBudget > 0) {
                    val budgetY = getY(totalBudget)
                    drawLine(
                        color = errorColor,
                        start = androidx.compose.ui.geometry.Offset(paddingLeft, budgetY),
                        end = androidx.compose.ui.geometry.Offset(width - paddingRight, budgetY),
                        strokeWidth = 3f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                    )
                }

                // 3. Draw cumulative actual spend (solid line & gradient fill)
                if (currentDay > 1) {
                    val actualPath = Path().apply {
                        moveTo(getX(1), getY(cumulativeSpend[1]))
                        for (day in 2..currentDay) {
                            lineTo(getX(day), getY(cumulativeSpend[day]))
                        }
                    }

                    // Draw fill area under actual spend
                    val actualFillPath = Path().apply {
                        moveTo(getX(1), getY(0.0))
                        lineTo(getX(1), getY(cumulativeSpend[1]))
                        for (day in 2..currentDay) {
                            lineTo(getX(day), getY(cumulativeSpend[day]))
                        }
                        lineTo(getX(currentDay), getY(0.0))
                        close()
                    }
                    drawPath(
                        path = actualFillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(primaryColor.copy(alpha = 0.2f), Color.Transparent),
                            startY = getY(maxVal),
                            endY = getY(0.0)
                        )
                    )

                    // Draw actual line
                    drawPath(
                        path = actualPath,
                        color = primaryColor,
                        style = Stroke(
                            width = 6f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                // 4. Draw projected spend (dashed line starting from today)
                if (currentDay < totalDays) {
                    val projectedPath = Path().apply {
                        moveTo(getX(currentDay), getY(cumulativeSpend[currentDay]))
                        for (day in (currentDay + 1)..totalDays) {
                            lineTo(getX(day), getY(projectedSpend[day]))
                        }
                    }
                    drawPath(
                        path = projectedPath,
                        color = secondaryColor,
                        style = Stroke(
                            width = 4f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    )
                }

                // 5. Draw "Today" vertical marker line & dot
                val todayX = getX(currentDay)
                val todayY = getY(cumulativeSpend[currentDay])
                
                // Draw vertical line for Today
                drawLine(
                    color = primaryColor.copy(alpha = 0.4f),
                    start = androidx.compose.ui.geometry.Offset(todayX, paddingTop),
                    end = androidx.compose.ui.geometry.Offset(todayX, height - paddingBottom),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                )

                // Today dot
                drawCircle(
                    color = primaryColor,
                    radius = 8f,
                    center = androidx.compose.ui.geometry.Offset(todayX, todayY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 4f,
                    center = androidx.compose.ui.geometry.Offset(todayX, todayY)
                )

                // 6. Draw X-axis labels (days of the month)
                val xLabelInterval = 5
                for (day in 1..totalDays step xLabelInterval) {
                    val labelX = getX(day)
                    drawContext.canvas.nativeCanvas.drawText(
                        "d$day",
                        labelX,
                        height - 10f,
                        android.graphics.Paint().apply {
                            color = onSurfaceVariant.toArgb()
                            textSize = 24f
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
                // Always draw last day
                if ((totalDays - 1) % xLabelInterval != 0) {
                    val labelX = getX(totalDays)
                    drawContext.canvas.nativeCanvas.drawText(
                        "d$totalDays",
                        labelX,
                        height - 10f,
                        android.graphics.Paint().apply {
                            color = onSurfaceVariant.toArgb()
                            textSize = 24f
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
            }
        }

        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(color = primaryColor, label = "Actual Spend", isDashed = false)
            Spacer(modifier = Modifier.width(16.dp))
            LegendItem(color = secondaryColor, label = "Projected Spend", isDashed = true)
            if (totalBudget > 0) {
                Spacer(modifier = Modifier.width(16.dp))
                LegendItem(color = errorColor, label = "Budget Limit", isDashed = true)
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, isDashed: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(16.dp, 8.dp)) {
            if (isDashed) {
                drawLine(
                    color = color,
                    start = androidx.compose.ui.geometry.Offset(0f, size.height / 2),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2),
                    strokeWidth = 3f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
                )
            } else {
                drawLine(
                    color = color,
                    start = androidx.compose.ui.geometry.Offset(0f, size.height / 2),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2),
                    strokeWidth = 4f
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
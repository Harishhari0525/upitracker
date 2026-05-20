@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.upitracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.upitracker.data.HeroState
import com.example.upitracker.data.RecentActivityItem
import com.example.upitracker.data.SmartAction
import com.example.upitracker.data.Transaction
import com.example.upitracker.ui.components.expressive.ExpressiveEmptyState
import com.example.upitracker.ui.components.expressive.ExpressiveHeroCard
import com.example.upitracker.ui.components.expressive.ExpressiveQuickActionCard
import com.example.upitracker.ui.components.expressive.ExpressiveSectionHeader
import com.example.upitracker.ui.components.expressive.ExpressiveStatCard
import com.example.upitracker.ui.components.expressive.ExpressiveTransactionCard
import com.example.upitracker.util.ExpressiveTokens
import com.example.upitracker.viewmodel.DashboardViewModel
import com.example.upitracker.viewmodel.MainViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun InsightGridScreen(
    dashboardViewModel: DashboardViewModel = viewModel(),
    mainViewModel: MainViewModel,
    onNavigateToHistory: () -> Unit
) {
    val state by dashboardViewModel.dashboardState.collectAsState()
    val currentMonthTotal by mainViewModel.currentMonthTotalExpenses.collectAsState()

    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(
            Locale.Builder()
                .setLanguage("en")
                .setRegion("IN")
                .build()
        ).apply {
            maximumFractionDigits = 0
        }
    }

    if (state.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val transactions = state.recentActivity
        .filterIsInstance<RecentActivityItem.TransactionItem>()
        .map { it.transaction }

    val monthProgress = remember(currentMonthTotal, state.hero) {
        buildMonthProgress(currentMonthTotal, state.hero)
    }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = ExpressiveTokens.spacing.lg,
            top = ExpressiveTokens.spacing.lg,
            end = ExpressiveTokens.spacing.lg,
            bottom = ExpressiveTokens.spacing.huge
        ),
        horizontalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.md),
        verticalItemSpacing = ExpressiveTokens.spacing.md
    ) {
        item(span = StaggeredGridItemSpan.FullLine) {
            DashboardHeroSection(
                heroState = state.hero,
                totalSpent = currentMonthTotal,
                monthProgress = monthProgress,
                currencyFormatter = currencyFormatter
            )
        }

        item(span = StaggeredGridItemSpan.FullLine) {
            ExpressiveSectionHeader(
                title = "Smart Forecast",
                subtitle = "Projected spend and safe limit for the rest of this month"
            )
        }

        item {
            ExpressiveStatCard(
                title = "Projected",
                value = currencyFormatter.format(monthProgress.projectedMonthEndSpend),
                subtitle = "Estimated month-end spend",
                icon = Icons.Rounded.Timeline,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            ExpressiveStatCard(
                title = "Safe / day",
                value = currencyFormatter.format(monthProgress.safeDailyLimit),
                subtitle = "${monthProgress.daysRemaining} days remaining",
                icon = Icons.Rounded.Speed,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            ExpressiveStatCard(
                title = "Burn Rate",
                value = "${monthProgress.monthUsedPercent}%",
                subtitle = "Target usage estimate",
                icon = Icons.AutoMirrored.Rounded.TrendingUp,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            ExpressiveStatCard(
                title = "Status",
                value = monthProgress.statusText,
                subtitle = monthProgress.statusSubtitle,
                icon = monthProgress.statusIcon,
                modifier = Modifier.fillMaxWidth()
            )
        }

        val currentHero = state.hero
        if (currentHero is HeroState.Velocity) {
            item(span = StaggeredGridItemSpan.FullLine) {
                ExpressiveSectionHeader(
                    title = "Commitments",
                    subtitle = if (currentHero.daysLeft > 0) {
                        "Bills and recurring payments still expected"
                    } else {
                        "No recurring bills detected for the rest of this month"
                    }
                )
            }

            item(span = StaggeredGridItemSpan.FullLine) {
                ExpressiveStatCard(
                    title = "Upcoming recurring bills",
                    value = currencyFormatter.format(currentHero.dailyLimit),
                    subtitle = if (currentHero.daysLeft > 0) {
                        "${currentHero.daysLeft} bill(s) expected this month"
                    } else {
                        "Nothing upcoming from recurring rules"
                    },
                    icon = Icons.Rounded.CalendarMonth,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (state.actions.isNotEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine) {
                ExpressiveSectionHeader(
                    title = "Smart Signals",
                    subtitle = "Quick signals from your spending pattern"
                )
            }

            items(state.actions.sortedBy { it.priority }) { action ->
                DashboardInsightActionCard(action = action)
            }
        }

        item(span = StaggeredGridItemSpan.FullLine) {
            ExpressiveSectionHeader(
                title = "Recent Activity",
                subtitle = if (transactions.isEmpty()) {
                    "No transactions yet"
                } else {
                    "Latest tracked payments"
                },
                actionText = if (transactions.isNotEmpty()) "View all" else null,
                onActionClick = if (transactions.isNotEmpty()) onNavigateToHistory else null
            )
        }

        if (transactions.isEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine) {
                ExpressiveEmptyState(
                    title = "No transactions found",
                    message = "Import old SMS or wait for new UPI alerts to start tracking your expenses.",
                    icon = Icons.AutoMirrored.Rounded.ReceiptLong
                )
            }
        } else {
            items(
                items = transactions,
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

@Composable
private fun DashboardHeroSection(
    heroState: HeroState,
    totalSpent: Double,
    monthProgress: MonthProgress,
    currencyFormatter: NumberFormat
) {
    val subtitle = when (heroState) {
        is HeroState.Velocity -> {
            when {
                heroState.amountLeft > 0 -> {
                    "${currencyFormatter.format(heroState.amountLeft)} left in monthly target"
                }

                else -> {
                    "Monthly target exceeded"
                }
            }
        }

        is HeroState.OverBudget -> {
            "Over budget in ${heroState.categoryName} by ${currencyFormatter.format(heroState.amountOver)}"
        }

        is HeroState.MonthlySummary -> {
            "Top category: ${heroState.mostExpensiveCategory}"
        }

        HeroState.Loading -> {
            "Preparing your dashboard"
        }
    }

    val debitLabel = "Spent ${currencyFormatter.format(totalSpent)}"

    val creditLabel = when (heroState) {
        is HeroState.Velocity -> {
            if (heroState.dailyLimit > 0) {
                "Committed ${currencyFormatter.format(heroState.dailyLimit)}"
            } else {
                "Safe ${currencyFormatter.format(monthProgress.safeDailyLimit)} / day"
            }
        }

        else -> {
            "Projected ${currencyFormatter.format(monthProgress.projectedMonthEndSpend)}"
        }
    }

    ExpressiveHeroCard(
        title = "Smart Insights",
        amount = currencyFormatter.format(totalSpent),
        subtitle = subtitle,
        debitLabel = debitLabel,
        creditLabel = creditLabel
    )
}

@Composable
private fun DashboardInsightActionCard(
    action: SmartAction
) {
    ExpressiveQuickActionCard(
        title = action.title,
        subtitle = action.subtitle,
        icon = action.icon,
        onClick = action.onClick
    )
}

private data class MonthProgress(
    val daysElapsed: Int,
    val daysRemaining: Int,
    val totalDaysInMonth: Int,
    val projectedMonthEndSpend: Double,
    val safeDailyLimit: Double,
    val monthUsedPercent: Int,
    val statusText: String,
    val statusSubtitle: String,
    val statusIcon: androidx.compose.ui.graphics.vector.ImageVector
)

private fun buildMonthProgress(
    totalSpent: Double,
    heroState: HeroState
): MonthProgress {
    val calendar = Calendar.getInstance()

    val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
    val totalDaysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
    val daysRemaining = (totalDaysInMonth - dayOfMonth).coerceAtLeast(0)

    val averagePerDay = if (dayOfMonth > 0) {
        totalSpent / dayOfMonth
    } else {
        0.0
    }

    val projectedMonthEndSpend = averagePerDay * totalDaysInMonth

    val amountLeft = when (heroState) {
        is HeroState.Velocity -> heroState.amountLeft
        else -> 0.0
    }

    val safeDailyLimit = if (daysRemaining > 0 && amountLeft > 0) {
        amountLeft / daysRemaining
    } else {
        0.0
    }

    val effectiveTarget = when (heroState) {
        is HeroState.Velocity -> totalSpent + heroState.amountLeft
        else -> projectedMonthEndSpend.takeIf { it > 0 } ?: 1.0
    }

    val monthUsedPercent = if (effectiveTarget > 0) {
        ((totalSpent / effectiveTarget) * 100).roundToInt().coerceAtLeast(0)
    } else {
        0
    }

    val statusText: String
    val statusSubtitle: String
    val statusIcon = when {
        monthUsedPercent >= 100 -> {
            statusText = "High"
            statusSubtitle = "You crossed the target pace"
            Icons.Rounded.WarningAmber
        }

        monthUsedPercent >= 85 -> {
            statusText = "Watch"
            statusSubtitle = "Close to monthly target"
            Icons.Rounded.WarningAmber
        }

        monthUsedPercent >= 50 -> {
            statusText = "Normal"
            statusSubtitle = "Spending is active but controlled"
            Icons.Rounded.Insights
        }

        else -> {
            statusText = "Safe"
            statusSubtitle = "You are within a comfortable range"
            Icons.Rounded.Psychology
        }
    }

    return MonthProgress(
        daysElapsed = dayOfMonth,
        daysRemaining = daysRemaining,
        totalDaysInMonth = totalDaysInMonth,
        projectedMonthEndSpend = projectedMonthEndSpend,
        safeDailyLimit = safeDailyLimit,
        monthUsedPercent = monthUsedPercent,
        statusText = statusText,
        statusSubtitle = statusSubtitle,
        statusIcon = statusIcon
    )
}

private fun Transaction.displayTitle(): String {
    return when {
        senderOrReceiver.isNotBlank() && senderOrReceiver != "Manual Entry" -> {
            senderOrReceiver
        }

        description.isNotBlank() -> {
            description
        }

        else -> {
            "Transaction"
        }
    }
}

private fun Transaction.formattedAmount(
    formatter: NumberFormat
): String {
    val prefix = if (type.equals("CREDIT", ignoreCase = true)) "+" else "-"
    return "$prefix${formatter.format(amount)}"
}

private fun Transaction.formattedDate(): String {
    return SimpleDateFormat(
        "dd MMM, hh:mm a",
        Locale.getDefault()
    ).format(Date(date))
}
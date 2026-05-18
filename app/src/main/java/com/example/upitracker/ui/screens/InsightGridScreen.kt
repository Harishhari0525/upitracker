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
import androidx.compose.material.icons.rounded.CalendarMonth
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
import java.util.Date
import java.util.Locale

@Composable
fun InsightGridScreen(
    dashboardViewModel: DashboardViewModel = viewModel(),
    mainViewModel: MainViewModel,
    onNavigateToHistory: () -> Unit
) {
    val state by dashboardViewModel.dashboardState.collectAsState()
    val currentMonthTotal by mainViewModel.currentMonthTotalExpenses.collectAsState()

    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN")).apply {
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
                currencyFormatter = currencyFormatter
            )
        }

        if (state.actions.isNotEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine) {
                ExpressiveSectionHeader(
                    title = "Smart snapshot",
                    subtitle = "Quick signals from your spending"
                )
            }

            items(state.actions.sortedBy { it.priority }) { action ->
                DashboardInsightActionCard(action = action)
            }
        }

        val currentHero = state.hero
        if (currentHero is HeroState.Velocity && currentHero.daysLeft > 0) {
            item(span = StaggeredGridItemSpan.FullLine) {
                ExpressiveStatCard(
                    title = "Upcoming recurring bills",
                    value = currencyFormatter.format(currentHero.dailyLimit),
                    subtitle = "${currentHero.daysLeft} bill(s) expected this month",
                    icon = Icons.Rounded.CalendarMonth,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item(span = StaggeredGridItemSpan.FullLine) {
            ExpressiveSectionHeader(
                title = "Recent activity",
                subtitle = if (transactions.isEmpty()) "No transactions yet" else "Latest tracked payments",
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
    currencyFormatter: NumberFormat
) {
    val subtitle = when (heroState) {
        is HeroState.Velocity -> {
            when {
                heroState.amountLeft > 0 -> "${currencyFormatter.format(heroState.amountLeft)} left in monthly target"
                else -> "Monthly target exceeded"
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
                null
            }
        }

        else -> null
    }

    ExpressiveHeroCard(
        title = "Current month",
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

private fun Transaction.displayTitle(): String {
    return when {
        senderOrReceiver.isNotBlank() && senderOrReceiver != "Manual Entry" -> senderOrReceiver
        description.isNotBlank() -> description
        else -> "Transaction"
    }
}

private fun Transaction.formattedAmount(formatter: NumberFormat): String {
    val prefix = if (type.equals("CREDIT", ignoreCase = true)) "+" else "-"
    return "$prefix${formatter.format(amount)}"
}

private fun Transaction.formattedDate(): String {
    return SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(date))
}
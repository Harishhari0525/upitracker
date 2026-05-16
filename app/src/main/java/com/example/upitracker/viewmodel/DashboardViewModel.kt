package com.example.upitracker.viewmodel

import android.app.Application
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.upitracker.data.*
import com.example.upitracker.util.DateUtils
import kotlinx.coroutines.flow.*
import java.util.Calendar

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)

    // Sources
    private val transactions = db.transactionDao().getAllTransactions()
    private val budgets = db.budgetDao().getAllActiveBudgets()
    private val recurringRules = db.recurringRuleDao().getAllRules()

    // Output State
    val dashboardState: StateFlow<DashboardState> = combine(
        transactions,
        budgets,
        recurringRules
    ) { txns, activeBudgets, rules ->
        generateDashboard(txns, activeBudgets, rules)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DashboardState(
            hero = HeroState.Loading,
            actions = emptyList(),
            recentActivity = emptyList(),
            isLoading = true
        )
    )

    private fun generateDashboard(
        txns: List<Transaction>,
        activeBudgets: List<Budget>,
        rules: List<RecurringRule>
    ): DashboardState {
        // 1. Date Logic
        val (monthStart, monthEnd) = DateUtils.getCurrentMonthRange()
        val (lastMonthStart, lastMonthEnd) = getPreviousMonthRange()

        // 2. Spend Calculation
        val currentMonthTxns = txns.filter { it.date in monthStart..monthEnd && it.type == "DEBIT" && it.category != "Refund" }
        val lastMonthTxns = txns.filter { it.date in lastMonthStart..lastMonthEnd && it.type == "DEBIT" && it.category != "Refund" }

        val totalSpent = currentMonthTxns.sumOf { it.amount }
        val lastMonthTotal = lastMonthTxns.sumOf { it.amount }

        // 3. Target Logic (Sum of Budgets = Monthly Target)
        // If no budgets, we use Last Month's total + 10% buffer as a "Soft Target"
        val userTarget = activeBudgets.sumOf {
            when (it.periodType) {
                BudgetPeriod.WEEKLY -> it.budgetAmount * 4.0
                BudgetPeriod.MONTHLY -> it.budgetAmount
                BudgetPeriod.YEARLY -> it.budgetAmount / 12.0
            }
        }
        val effectiveTarget = if (userTarget > 0) userTarget else (if (lastMonthTotal > 0) lastMonthTotal * 1.1 else 10000.0)

        // 4. Ghost/Committed Calculation (Recurring bills due later this month)
        val now = System.currentTimeMillis()
        val ghostBills = rules.mapNotNull { rule ->
            if (rule.nextDueDate in now..monthEnd) {
                val diff = rule.nextDueDate - now
                val days = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff).coerceAtLeast(0)
                GhostBill(rule.description, rule.amount, days)
            } else null
        }.sortedBy { it.dueInDays }

        val committedAmount = ghostBills.sumOf { it.amount }

        // 5. Determine Status Color & Text
        val percentageUsed = if (effectiveTarget > 0) (totalSpent / effectiveTarget) else 0.0
        val statusColor = when {
            percentageUsed > 1.0 -> Color(0xFFB71C1C) // Deep Red (Exceeded)
            percentageUsed > 0.85 -> Color(0xFFE65100) // Orange (Warning)
            percentageUsed > 0.5 -> Color(0xFFF9A825) // Yellow/Gold (Caution)
            else -> Color(0xFF2E7D32) // Green (Safe)
        }

        // 6. Build Hero State (Status Monitor)
        // We reuse 'Velocity' to carry the data:
        // amountLeft = Remaining Budget
        // daysLeft = Count of Ghosts
        // dailyLimit = Total Amount of Ghosts
        val heroState = HeroState.Velocity(
            amountLeft = (effectiveTarget - totalSpent).coerceAtLeast(0.0),
            daysLeft = ghostBills.size,
            dailyLimit = committedAmount,
            statusColor = statusColor
        )

        // 7. Insight Cards (Middle Section)
        val actions = mutableListOf<SmartAction>()

        // Card 1: Daily Average
        val calendar = Calendar.getInstance()
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val dailyAvg = totalSpent / dayOfMonth
        val currencyFormatter = java.text.NumberFormat.getCurrencyInstance(
            java.util.Locale("en", "IN"))
        currencyFormatter.maximumFractionDigits = 0

        actions.add(SmartAction("avg", "Daily Avg", "${currencyFormatter.format(dailyAvg)} / day", Icons.Default.Timeline, 2))

        // Card 2: Vs Last Month


        val diff = totalSpent - lastMonthTotal
        val diffText = if(diff > 0) "+${currencyFormatter.format(diff)}" else currencyFormatter.format(diff)
        val trendIcon = if(diff > 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown
        actions.add(SmartAction("trend", "Vs Last Month", diffText, trendIcon, 1))

        // 8. Build Activity Feed
        val recentActivity = mutableListOf<RecentActivityItem>()
        // No header needed here, the UI handles the "History" row
        recentActivity.addAll(txns.take(5).map { RecentActivityItem.TransactionItem(it) })

        return DashboardState(
            hero = heroState,
            actions = actions,
            recentActivity = recentActivity,
            isLoading = false
        )
    }

    private fun getPreviousMonthRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -1)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0)
        val start = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val end = calendar.timeInMillis
        return start to end
    }
}
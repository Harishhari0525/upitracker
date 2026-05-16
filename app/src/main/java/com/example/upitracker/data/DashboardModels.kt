package com.example.upitracker.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

// Data class for future commitments
data class GhostBill(
    val name: String,
    val amount: Double,
    val dueInDays: Long,
    val iconName: String? = null
)

// The "Focus Block" (Top Section)
sealed interface HeroState {
    // We use Velocity as the primary state for the "Status Monitor"
    data class Velocity(
        val amountLeft: Double,    // Money Remaining in Target
        val daysLeft: Int,         // Used to store "Count of Ghost Bills"
        val dailyLimit: Double,    // Used to store "Total Ghost Amount"
        val statusColor: Color     // Dynamic Color (Green/Orange/Red)
    ) : HeroState

    // Fallback states (kept for compatibility)
    data class OverBudget(val amountOver: Double, val categoryName: String) : HeroState
    data class MonthlySummary(val totalSpent: Double, val mostExpensiveCategory: String) : HeroState

    // Placeholder for initial load
    data object Loading : HeroState
}

// The "Insight Cards" (Middle Section)
data class SmartAction(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val priority: Int,
    val onClick: () -> Unit = {}
)

// The "Living History" (Bottom Section)
sealed interface RecentActivityItem {
    data class Header(val title: String) : RecentActivityItem
    data class TransactionItem(val transaction: Transaction) : RecentActivityItem
}

// The Container for the whole screen
data class DashboardState(
    val hero: HeroState,
    val actions: List<SmartAction>,
    val recentActivity: List<RecentActivityItem>,
    val isLoading: Boolean = true
)
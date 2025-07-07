package com.example.upitracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class BudgetPeriod {
    WEEKLY,
    MONTHLY,
    YEARLY
}

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val categoryName: String, // e.g., "Food", "Shopping"
    val budgetAmount: Double, // The amount budgeted
    val periodType: BudgetPeriod, // WEEKLY, MONTHLY, or YEARLY
    val startDate: Long, // The timestamp for the first millisecond of the budget period
    val isActive: Boolean = true, // To enable/disable budgets without deleting
    val allowRollover: Boolean = false,
    val lastNotificationTimestamp: Long = 0L
)
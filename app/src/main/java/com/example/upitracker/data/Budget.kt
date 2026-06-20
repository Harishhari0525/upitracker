package com.example.upitracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Ignore
import com.example.upitracker.util.toMajorUnits
import com.example.upitracker.util.toPaise

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
    @ColumnInfo(name = "budgetAmount") val budgetAmountPaise: Long,
    val periodType: BudgetPeriod, // WEEKLY, MONTHLY, or YEARLY
    val startDate: Long, // The timestamp for the first millisecond of the budget period
    val isActive: Boolean = true, // To enable/disable budgets without deleting
    val allowRollover: Boolean = false,
    val lastNotificationTimestamp: Long = 0L
) {
    @get:Ignore
    val budgetAmount: Double get() = budgetAmountPaise.toMajorUnits()

    @Ignore
    constructor(
        id: Int = 0,
        categoryName: String,
        budgetAmount: Double,
        periodType: BudgetPeriod,
        startDate: Long,
        isActive: Boolean = true,
        allowRollover: Boolean = false,
        lastNotificationTimestamp: Long = 0L
    ) : this(
        id, categoryName, budgetAmount.toPaise(), periodType, startDate,
        isActive, allowRollover, lastNotificationTimestamp
    )
}

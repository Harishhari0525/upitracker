package com.example.upitracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_rules")
data class RecurringRule(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val amount: Double,
    val description: String, // e.g., "Netflix Subscription"
    val categoryName: String,
    val periodType: BudgetPeriod, // We can reuse the BudgetPeriod enum (WEEKLY, MONTHLY, YEARLY)
    val dayOfPeriod: Int, // For MONTHLY: 1-31, for WEEKLY: 1-7 (Monday-Sunday)
    val nextDueDate: Long, // The timestamp for the next time this transaction is due. This is crucial.
    val creationDate: Long = System.currentTimeMillis()
)
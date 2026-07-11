package com.example.upitracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Ignore
import com.example.upitracker.util.toMajorUnits
import com.example.upitracker.util.toPaise

@Entity(tableName = "recurring_rules")
data class RecurringRule(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "amount") val amountPaise: Long,
    val description: String, // e.g., "Netflix Subscription"
    val categoryName: String,
    val periodType: BudgetPeriod, // We can reuse the BudgetPeriod enum (WEEKLY, MONTHLY, YEARLY)
    val dayOfPeriod: Int, // For MONTHLY: 1-31, for WEEKLY: 1-7 (Monday-Sunday)
    val nextDueDate: Long, // The timestamp for the next time this transaction is due. This is crucial.
    @ColumnInfo(defaultValue = "1") val createTransactionOnDueDate: Boolean = true,
    val creationDate: Long = System.currentTimeMillis()
) {
    @get:Ignore
    val amount: Double get() = amountPaise.toMajorUnits()

    @Ignore
    constructor(
        id: Int = 0,
        amount: Double,
        description: String,
        categoryName: String,
        periodType: BudgetPeriod,
        dayOfPeriod: Int,
        nextDueDate: Long,
        createTransactionOnDueDate: Boolean = true,
        creationDate: Long = System.currentTimeMillis()
    ) : this(id, amount.toPaise(), description, categoryName, periodType, dayOfPeriod, nextDueDate, createTransactionOnDueDate, creationDate)
}

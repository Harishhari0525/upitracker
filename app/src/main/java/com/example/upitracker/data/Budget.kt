package com.example.upitracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey
    val categoryName: String, // e.g., "Food", "Shopping" - This is the unique key

    val budgetAmount: Double, // The amount the user has budgeted for this category

    // Optional: You could add fields like 'month' or 'year' for historical budgets later
)
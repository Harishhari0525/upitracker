package com.example.upitracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "upi_lite_summaries")
data class UpiLiteSummary(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val transactionCount: Int,
    val totalAmount: Double,
    val date: String,     // Store as ISO string, or as Long if you prefer
    val bank: String
)

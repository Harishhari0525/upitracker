package com.example.upitracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "upi_lite_summaries")
data class UpiLiteSummary(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val transactionCount: Int,
    val totalAmount: Double,
    val date: Long,     // ✨ Changed to Long (timestamp for the start of the day)
    val bank: String
)
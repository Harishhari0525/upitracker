package com.example.upitracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val type: String, // "CREDIT" or "DEBIT" (Consider an Enum or sealed class for more type safety if values are fixed)
    val date: Long,   // Timestamp (Epoch milliseconds)
    val description: String,
    val senderOrReceiver: String, // Consider renaming for clarity, e.g., "counterparty" or "upiId"
    val note: String = "" // Good for user notes
)
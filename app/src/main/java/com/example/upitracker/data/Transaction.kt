package com.example.upitracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val type: String, // "CREDIT" or "DEBIT"
    val date: Long,   // Timestamp (Epoch milliseconds)
    val description: String,
    val senderOrReceiver: String,
    val note: String = "",
    val category: String? = null // ✨ New nullable field for category ✨
)
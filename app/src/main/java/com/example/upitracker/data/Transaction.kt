package com.example.upitracker.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index(
            value = ["amount", "date", "type"],
            unique = true
        )
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val type: String, // "CREDIT" or "DEBIT"
    val date: Long,   // Timestamp (Epoch milliseconds)
    val description: String,
    val senderOrReceiver: String,
    val note: String = "",
    val category: String? = null,
    val isArchived: Boolean = false,
    val pendingDeletionTimestamp: Long? = null,
    val linkedTransactionId: Int? = null,
    val bankName: String? = null,
    val receiptImagePath: String? = null,
    val tags: String = ""
)
package com.example.upitracker.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Ignore
import com.example.upitracker.util.toMajorUnits
import com.example.upitracker.util.toPaise

@Entity(
    tableName = "transactions",
    indices = [
        Index(
            value = ["amount", "date", "type"]
        ),
        Index(
            value = ["isArchived", "pendingDeletionTimestamp", "date"]
        ),
        Index(
            value = ["category"]
        ),
        Index(
            value = ["bankName"]
        )
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "amount") val amountPaise: Long,
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
    @ColumnInfo(name = "balanceAfterTransaction") val balanceAfterTransactionPaise: Long? = null,
    val receiptImagePath: String? = null,
    val tags: String = ""
) {
    @get:Ignore
    val amount: Double get() = amountPaise.toMajorUnits()

    @get:Ignore
    val balanceAfterTransaction: Double? get() = balanceAfterTransactionPaise?.toMajorUnits()

    @Ignore
    constructor(
        id: Int = 0,
        amount: Double,
        type: String,
        date: Long,
        description: String,
        senderOrReceiver: String,
        note: String = "",
        category: String? = null,
        isArchived: Boolean = false,
        pendingDeletionTimestamp: Long? = null,
        linkedTransactionId: Int? = null,
        bankName: String? = null,
        balanceAfterTransaction: Double? = null,
        receiptImagePath: String? = null,
        tags: String = ""
    ) : this(
        id = id,
        amountPaise = amount.toPaise(),
        type = type,
        date = date,
        description = description,
        senderOrReceiver = senderOrReceiver,
        note = note,
        category = category,
        isArchived = isArchived,
        pendingDeletionTimestamp = pendingDeletionTimestamp,
        linkedTransactionId = linkedTransactionId,
        bankName = bankName,
        balanceAfterTransactionPaise = balanceAfterTransaction?.toPaise(),
        receiptImagePath = receiptImagePath,
        tags = tags
    )
}

package com.example.upitracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Ignore
import com.example.upitracker.util.toMajorUnits
import com.example.upitracker.util.toPaise

@Entity(tableName = "upi_lite_summaries")
data class UpiLiteSummary(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val transactionCount: Int,
    @ColumnInfo(name = "totalAmount") val totalAmountPaise: Long,
    val date: Long,     // ✨ Changed to Long (timestamp for the start of the day)
    val bank: String
) {
    @get:Ignore
    val totalAmount: Double get() = totalAmountPaise.toMajorUnits()

    @Ignore
    constructor(
        id: Int = 0,
        transactionCount: Int,
        totalAmount: Double,
        date: Long,
        bank: String
    ) : this(id, transactionCount, totalAmount.toPaise(), date, bank)
}

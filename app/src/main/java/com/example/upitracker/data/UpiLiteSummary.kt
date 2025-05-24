package com.example.upitracker.data

data class UpiLiteSummary(
    val transactionCount: Int,
    val totalAmount: Double,
    val date: String,
    val bank: String
)

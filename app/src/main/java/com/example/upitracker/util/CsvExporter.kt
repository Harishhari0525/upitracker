package com.example.upitracker.util

import com.example.upitracker.data.Transaction

object CsvExporter {
    fun export(transactions: List<Transaction>): String {
        val header = "Date,Type,Amount,Description"
        val rows = transactions.joinToString("\n") { txn ->
            "${txn.date},${txn.type},${txn.amount},\"${txn.description}\""
        }
        return "$header\n$rows"
    }
}

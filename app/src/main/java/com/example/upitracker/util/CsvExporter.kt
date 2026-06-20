package com.example.upitracker.util

import com.example.upitracker.data.Transaction
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object CsvExporter {

    private val csvDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)

    /**
     * Exports a list of transactions to a CSV formatted string.
     * Includes a header row and handles basic CSV escaping.
     */
    fun exportTransactionsToCsvString(transactions: List<Transaction>): String {
        val header = "ID,Date,Type,Amount,Description,SenderOrReceiver,Category,Note" // Added Category
        val stringBuilder = StringBuilder()
        stringBuilder.appendLine(header)

        transactions.forEach { txn ->
            val dateFormatted = try {
                csvDateFormat.format(Instant.ofEpochMilli(txn.date).atZone(ZoneId.systemDefault()))
            } catch (_: Exception) {
                txn.date.toString() // Fallback to raw timestamp
            }
            val amountFormatted = String.format(Locale.US, "%.2f", txn.amount)
            stringBuilder.appendLine(
                listOf(
                    txn.id.toString() to false,
                    dateFormatted to false,
                    txn.type to false,
                    amountFormatted to false,
                    txn.description to true,
                    txn.senderOrReceiver to true,
                    txn.category.orEmpty() to true,
                    txn.note to true
                ).joinToString(",") { (value, protectFormula) -> escapeCsvField(value, protectFormula) }
            )
        }
        return stringBuilder.toString()
    }

    /**
     * Quotes every field, escapes embedded quotes, and neutralizes common spreadsheet formulas.
     */
    private fun escapeCsvField(field: String?, protectFormula: Boolean): String {
        if (field.isNullOrEmpty()) return "\"\""
        val formulaSafeField = if (protectFormula && field.first() in charArrayOf('=', '+', '-', '@')) "'$field" else field
        return "\"${formulaSafeField.replace("\"", "\"\"")}\""
    }
}

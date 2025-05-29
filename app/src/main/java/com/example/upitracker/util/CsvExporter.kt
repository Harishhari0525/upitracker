package com.example.upitracker.util

import com.example.upitracker.data.Transaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    // Consistent date format for CSV
    private val csvDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)

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
                csvDateFormat.format(Date(txn.date))
            } catch (e: Exception) {
                txn.date.toString() // Fallback to raw timestamp
            }
            val amountFormatted = "%.2f".format(txn.amount) // Ensure two decimal places

            // Sanitize fields for CSV
            val descriptionSanitized = escapeCsvField(txn.description)
            val senderOrReceiverSanitized = escapeCsvField(txn.senderOrReceiver)
            val categorySanitized = escapeCsvField(txn.category ?: "") // Handle nullable category
            val noteSanitized = escapeCsvField(txn.note)

            stringBuilder.appendLine(
                "${txn.id},\"$dateFormatted\",\"${txn.type}\",$amountFormatted,\"$descriptionSanitized\",\"$senderOrReceiverSanitized\",\"$categorySanitized\",\"$noteSanitized\""
            )
        }
        return stringBuilder.toString()
    }

    /**
     * Escapes a field for CSV format:
     * - Doubles any existing double quotes within the field.
     * - Encloses the entire field in double quotes if it contains a comma, a double quote, or a newline character.
     */
    private fun escapeCsvField(field: String?): String {
        if (field.isNullOrBlank()) return ""
        // First, escape existing double quotes by replacing them with two double quotes
        val escapedField = field.replace("\"", "\"\"")
        // Then, if the (potentially now quote-escaped) field contains problematic characters,
        // or if it was already quote-escaped, enclose the whole thing in quotes.
        // This simple check is okay for most cases. For perfect RFC 4180 compliance, parsing is more complex.
        return if (escapedField.contains(",") || escapedField.contains("\"") || escapedField.contains("\n") || escapedField.contains("'")) {
            "\"$escapedField\""
        } else {
            escapedField
        }
    }
}
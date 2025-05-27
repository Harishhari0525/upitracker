package com.example.upitracker.util

import com.example.upitracker.data.Transaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    // SimpleDateFormat should ideally be created once if the pattern is fixed,
    // or passed in if different formats are needed.
    private val csvDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH) // ISO-like, good for CSV

    /**
     * Exports a list of transactions to a CSV formatted string.
     * Includes a header row.
     *
     * @param transactions The list of transactions to export.
     * @return A String representing the data in CSV format.
     */
    fun exportTransactionsToCsvString(transactions: List<Transaction>): String {
        val header = "ID,Date,Type,Amount,Description,SenderReceiver,Note" // Added ID and Note
        val stringBuilder = StringBuilder()
        stringBuilder.appendLine(header) // Append header line

        transactions.forEach { txn ->
            val dateFormatted = try {
                csvDateFormat.format(Date(txn.date))
            } catch (e: Exception) {
                txn.date.toString() // Fallback to raw timestamp if formatting fails
            }
            // Escape quotes within fields by doubling them, and enclose fields with commas or quotes in quotes.
            val descriptionSanitized = escapeCsvField(txn.description)
            val senderOrReceiverSanitized = escapeCsvField(txn.senderOrReceiver)
            val noteSanitized = escapeCsvField(txn.note)

            stringBuilder.appendLine(
                "${txn.id},${dateFormatted},${txn.type},${"%.2f".format(txn.amount)},\"${descriptionSanitized}\",\"${senderOrReceiverSanitized}\",\"${noteSanitized}\""
            )
        }
        return stringBuilder.toString()
    }

    /**
     * Escapes a field for CSV format:
     * - Encloses the field in double quotes if it contains a comma, a double quote, or a newline.
     * - Doubles any existing double quotes within the field.
     */
    private fun escapeCsvField(field: String?): String {
        if (field == null) return ""
        val text = field.replace("\"", "\"\"") // Escape double quotes
        // Enclose in quotes if it contains comma, quote, or newline
        return if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            "\"$text\""
        } else {
            text
        }
    }
}
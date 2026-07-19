package com.example.upitracker.util

import com.example.upitracker.data.Transaction
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.io.Writer

object CsvExporter {

    private val csvDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)

    fun writeHeader(writer: Writer) {
        writer.appendLine("ID,Date,Type,Amount,Description,SenderOrReceiver,Category,Note")
    }

    fun writeTransaction(writer: Writer, txn: Transaction) {
        val dateFormatted = try {
            csvDateFormat.format(Instant.ofEpochMilli(txn.date).atZone(ZoneId.systemDefault()))
        } catch (_: Exception) {
            txn.date.toString()
        }
        writer.appendLine(
            listOf(
                txn.id.toString() to false,
                dateFormatted to false,
                txn.type to false,
                String.format(Locale.US, "%.2f", txn.amount) to false,
                txn.description to true,
                txn.senderOrReceiver to true,
                txn.category.orEmpty() to true,
                txn.note to true
            ).joinToString(",") { (value, protectFormula) -> escapeCsvField(value, protectFormula) }
        )
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

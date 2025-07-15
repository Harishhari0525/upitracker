package com.example.upitracker.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.example.upitracker.R
import com.example.upitracker.data.Transaction
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {

    // ✅ The function now accepts theme colors as parameters
    fun generatePassbookPdf(
        context: Context,
        transactions: List<Transaction>,
        statementPeriod: String,
        primaryColor: Int,
        textColor: Int,
        targetUri: Uri
    ) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 page size
        var page = document.startPage(pageInfo)
        var canvas: Canvas = page.canvas

        // --- Define Paints using theme colors ---
        val titlePaint = Paint().apply {
            textSize = 20f
            isFakeBoldText = true
            color = primaryColor // Use theme's primary color
        }
        val headerPaint = Paint().apply {
            textSize = 10f
            isFakeBoldText = true
            color = textColor
        }
        val textPaint = Paint().apply {
            textSize = 9f
            color = textColor
        }
        val tableLinePaint = Paint().apply {
            strokeWidth = 1f
            color = textColor
            alpha = 50 // Make lines semi-transparent
        }

        val currency = NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("en").setRegion("IN").build())

        val currencyFormatter = currency
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

        // ✅ Draw the App Icon
        ContextCompat.getDrawable(context, R.mipmap.ic_launcher_round)?.let { drawable ->
            val iconBitmap = drawable.toBitmap(width = 40, height = 40)
            canvas.drawBitmap(iconBitmap, 40f, 40f, null)
        }

        // --- Draw Header ---
        var yPosition = 60f
        canvas.drawText("Transaction Statement", 95f, yPosition, titlePaint)
        yPosition += 20f
        canvas.drawText(statementPeriod, 95f, yPosition, textPaint)
        yPosition += 40f

        // --- Draw Table ---
        val tableTop = yPosition
        val tableLeft = 40f
        val tableRight = 555f

        // Column X positions
        val dateX = 45f
        val descX = 105f
        val debitX = 405f
        val creditX = 485f

        // Draw Table Header
        canvas.drawText("Date", dateX, yPosition, headerPaint)
        canvas.drawText("Description", descX, yPosition, headerPaint)
        canvas.drawText("Debit", debitX, yPosition, headerPaint)
        canvas.drawText("Credit", creditX, yPosition, headerPaint)
        yPosition += 15f
        canvas.drawLine(tableLeft, yPosition, tableRight, yPosition, tableLinePaint)
        yPosition += 20f

        var rowStartY = yPosition

        // Draw Transaction Rows
        for (txn in transactions) {
            val descriptionLines = splitText(txn.description, 280f, textPaint)
            val rowHeight = (descriptionLines.size * 12f) + 10f // Calculate row height dynamically

            // Check for pagination BEFORE drawing the row
            if (yPosition + rowHeight > 800) {
                // Finish old page
                canvas.drawLine(tableLeft, rowStartY - 25, tableLeft, yPosition - 10, tableLinePaint) // Left border
                canvas.drawLine(95f, rowStartY - 25, 95f, yPosition - 10, tableLinePaint) // Date/Desc separator
                canvas.drawLine(395f, rowStartY - 25, 395f, yPosition - 10, tableLinePaint) // Desc/Debit separator
                canvas.drawLine(475f, rowStartY - 25, 475f, yPosition - 10, tableLinePaint) // Debit/Credit separator
                canvas.drawLine(tableRight, rowStartY - 25, tableRight, yPosition - 10, tableLinePaint) // Right border
                document.finishPage(page)

                // Start new page
                page = document.startPage(pageInfo)
                canvas = page.canvas
                yPosition = 60f
                // Redraw headers on new page
                // ... (header drawing logic can be duplicated here if needed) ...
                rowStartY = yPosition
            }

            // Draw row content
            canvas.drawText(dateFormat.format(Date(txn.date)), dateX, yPosition, textPaint)
            var tempY = yPosition
            for (line in descriptionLines) {
                canvas.drawText(line, descX, tempY, textPaint)
                tempY += 12f
            }
            val debitText = if (txn.type.equals("DEBIT", true)) currencyFormatter.format(txn.amount) else ""
            val creditText = if (txn.type.equals("CREDIT", true)) currencyFormatter.format(txn.amount) else ""
            canvas.drawText(debitText, debitX, yPosition, textPaint)
            canvas.drawText(creditText, creditX, yPosition, textPaint)

            yPosition = tempY + 10f
            canvas.drawLine(tableLeft, yPosition - 10, tableRight, yPosition - 10, tableLinePaint) // Horizontal line after each row
        }

        // ✅ Draw vertical table lines for the last page
        canvas.drawLine(tableLeft, tableTop, tableLeft, yPosition - 10, tableLinePaint) // Left border
        canvas.drawLine(95f, tableTop, 95f, yPosition - 10, tableLinePaint) // Date/Desc separator
        canvas.drawLine(395f, tableTop, 395f, yPosition - 10, tableLinePaint) // Desc/Debit separator
        canvas.drawLine(475f, tableTop, 475f, yPosition - 10, tableLinePaint) // Debit/Credit separator
        canvas.drawLine(tableRight, tableTop, tableRight, yPosition - 10, tableLinePaint) // Right border

        document.finishPage(page)

        context.contentResolver.openFileDescriptor(targetUri, "w")?.use {
            FileOutputStream(it.fileDescriptor).use { outputStream ->
                document.writeTo(outputStream)
            }
        }
        document.close()
    }
    private fun splitText(text: String, maxWidth: Float, paint: Paint): List<String> {
        val lines = mutableListOf<String>()
        var start = 0
        while (start < text.length) {
            val count = paint.breakText(text, start, text.length, true, maxWidth, null)
            lines.add(text.substring(start, start + count))
            start += count
        }
        return lines
    }
}
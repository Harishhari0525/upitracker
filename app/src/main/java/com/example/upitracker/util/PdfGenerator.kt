package com.example.upitracker.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
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
import androidx.core.graphics.toColorInt

object PdfGenerator {

    fun generatePassbookPdf(
        context: Context,
        transactions: List<Transaction>,
        statementPeriod: String,
        targetUri: Uri
    ) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var page = document.startPage(pageInfo)
        var canvas: Canvas = page.canvas

        val primaryColor = "#3F51B5".toColorInt()
        val textColor = Color.BLACK

        val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true; color = primaryColor }
        val periodPaint = Paint().apply { textSize = 12f; color = Color.GRAY }
        val headerPaint = Paint().apply { textSize = 10f; isFakeBoldText = true; color = textColor }
        val textPaint = Paint().apply { textSize = 9f; color = textColor }
        val rightAlignPaint = Paint(textPaint).apply { textAlign = Paint.Align.RIGHT }
        val tableLinePaint = Paint().apply { strokeWidth = 0.5f; color = Color.GRAY }

        // ✅ FIX: All column position variables are declared here
        val leftMargin = 40f
        val rightMargin = 555f
        val dateColX = leftMargin
        val descColX = 95f
        val debitColX = 470f
        val creditColX = rightMargin

        fun drawPageHeader(pageCanvas: Canvas) {
            ContextCompat.getDrawable(context, R.mipmap.ic_launcher_round)?.let { drawable ->
                val iconBitmap = drawable.toBitmap(width = 40, height = 40)
                pageCanvas.drawBitmap(iconBitmap, leftMargin, 40f, null)
            }
            pageCanvas.drawText("Transaction Statement", leftMargin + 55f, 60f, titlePaint)
            pageCanvas.drawText(statementPeriod, leftMargin + 55f, 80f, periodPaint)

            pageCanvas.drawText("Date", dateColX, 120f, headerPaint)
            pageCanvas.drawText("Description", descColX, 120f, headerPaint)
            pageCanvas.drawText("Debit", debitColX, 120f, rightAlignPaint)
            pageCanvas.drawText("Credit", creditColX, 120f, rightAlignPaint)
            pageCanvas.drawLine(leftMargin, 128f, rightMargin, 128f, tableLinePaint)
        }

        drawPageHeader(canvas)
        var yPosition = 145f

        val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("en").setRegion("IN").build())
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

        for (txn in transactions) {
            val descriptionLines = splitText(txn.description, 290f, textPaint)
            val rowHeight = (descriptionLines.size * 12f) + 8f

            if (yPosition + rowHeight > 800) {
                document.finishPage(page)
                page = document.startPage(pageInfo)
                canvas = page.canvas
                drawPageHeader(canvas)
                yPosition = 145f
            }

            val rowTopY = yPosition
            canvas.drawText(dateFormat.format(Date(txn.date)), dateColX, rowTopY, textPaint)

            var descY = rowTopY
            descriptionLines.forEach { line ->
                canvas.drawText(line, descColX, descY, textPaint)
                descY += 12f
            }

            if (txn.type.equals("DEBIT", true)) {
                canvas.drawText(currencyFormatter.format(txn.amount), debitColX, rowTopY, rightAlignPaint)
            } else {
                canvas.drawText(currencyFormatter.format(txn.amount), creditColX, rowTopY, rightAlignPaint)
            }

            yPosition = maxOf(yPosition + 12, descY + 8)
            canvas.drawLine(leftMargin, yPosition, rightMargin, yPosition, tableLinePaint)
            yPosition += 12
        }

        document.finishPage(page)

        try {
            context.contentResolver.openFileDescriptor(targetUri, "w")?.use {
                FileOutputStream(it.fileDescriptor).use { outputStream ->
                    document.writeTo(outputStream)
                }
            }
        } finally {
            document.close()
        }
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
package com.example.upitracker.sms

import com.example.upitracker.data.UpiLiteSummary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.util.Log // For logging errors

fun parseUpiLiteSummarySms(message: String): UpiLiteSummary? {
    val regex = Regex("""(\d+) transactions worth Rs ?([\d,]+\.?\d*|\.?\d+) using your UPI Lite Wallet/s on (\d{2}-\w{3}-\d{2})-(\w+(?: \w+)?) Bank""", RegexOption.IGNORE_CASE)
    val match = regex.find(message)

    return match?.let {
        try {
            val transactionCount = it.groupValues[1].toInt()
            val totalAmountStr = it.groupValues[2].replace(",", "")
            val totalAmount = totalAmountStr.toDouble()
            val dateStringFromSms = it.groupValues[3] // e.g., "26-May-25"
            val bank = it.groupValues[4].trim()

            // ✨ Convert date string to Long timestamp (start of day) ✨
            val dateFormat = SimpleDateFormat("dd-MMM-yy", Locale.ENGLISH) // Be specific with Locale for parsing month abbreviations
            val parsedDate: Date = dateFormat.parse(dateStringFromSms) ?: return null // Return null if date parsing fails

            val calendar = Calendar.getInstance()
            calendar.time = parsedDate
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val dateTimestamp: Long = calendar.timeInMillis

            UpiLiteSummary(
                transactionCount = transactionCount,
                totalAmount = totalAmount,
                date = dateTimestamp, // Store the Long timestamp
                bank = bank
            )
        } catch (e: Exception) {
            Log.e("UpiLiteSummaryParser", "Error parsing UPI Lite SMS: '${e.message}' for message: $message")
            null // Return null if any part of the parsing fails
        }
    }
}
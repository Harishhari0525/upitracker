package com.example.upitracker.sms

import com.example.upitracker.data.Transaction

// Example: Common regex patterns for Indian UPI SMS
private val defaultRegexList = listOf(
    // HDFC UPI
    Regex("""(?:debited|credited).*\bRs\.?\s*([0-9,\.]+).*\bUPI\b""", RegexOption.IGNORE_CASE),
    // SBI UPI
    Regex("""UPI.*\bRs\.?\s*([0-9,\.]+).*(debited|credited)""", RegexOption.IGNORE_CASE),
    // Add more regex for other banks as needed
)

/**
 * Parse a UPI transaction SMS into a Transaction object.
 * @param message The SMS body
 * @param sender The sender address/number
 * @param smsDate The timestamp of the original SMS (epoch ms)
 * @param customRegex Optional: list of regex for custom UPI formats
 */
fun parseUpiSms(
    message: String,
    sender: String,
    smsDate: Long,
    customRegex: List<Regex> = defaultRegexList
): Transaction? {
    for (regex in customRegex) {
        val match = regex.find(message)
        if (match != null) {
            val amountStr = match.groupValues[1].replace(",", "")
            val type = if (message.contains("debited", ignoreCase = true)) "DEBIT" else "CREDIT"
            val amount = amountStr.toDoubleOrNull() ?: return null
            return Transaction(
                id = 0,
                amount = amount,
                type = type,
                date = smsDate, // Use original SMS date
                description = message.take(60),
                senderOrReceiver = sender,
                note = ""
            )
        }
    }
    return null
}

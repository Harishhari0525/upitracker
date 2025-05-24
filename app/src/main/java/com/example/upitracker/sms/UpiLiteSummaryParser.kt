package com.example.upitracker.sms

import com.example.upitracker.data.UpiLiteSummary

fun parseUpiLiteSummarySms(message: String): UpiLiteSummary? {
    val regex = Regex("""(\d+) transactions worth Rs ([\d.]+) using your UPI Lite Wallet/s on (\d{2}-\w{3}-\d{2})-(\w+) Bank""")
    val match = regex.find(message)
    return match?.let {
        UpiLiteSummary(
            transactionCount = it.groupValues[1].toInt(),
            totalAmount = it.groupValues[2].toDouble(),
            date = it.groupValues[3],
            bank = it.groupValues[4]
        )
    }
}


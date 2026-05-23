package com.example.upitracker.sms

import com.example.upitracker.data.Transaction
import java.util.Locale

private val defaultRegexList = listOf(
    // High Confidence: Explicit Salary Credits (Bypasses UPI keyword requirement safely)
    Regex("""\b(salary)\b.*\b(credited|received)\b.*\bRS\s*([0-9,.]+)""", RegexOption.IGNORE_CASE),
    Regex("""\b(credited|received)\b.*\b(salary)\b.*\bRS\s*([0-9,.]+)""", RegexOption.IGNORE_CASE),

    // Strict UPI patterns: Group 1 = Action, Group 2 = Amount
    Regex("""\b(debited|credited)\b.*\bRS\s*([0-9,.]+).*\bUPI\b""", RegexOption.IGNORE_CASE),
    Regex("""\bUPI\b.*\bRS\s*([0-9,.]+).*\b(debited|credited)\b""", RegexOption.IGNORE_CASE),

    // Relaxed patterns: Trust escalated via bankName
    Regex("""\b(debited|credited|sent|received)\b.*\bRS\s*([0-9,.]+)""", RegexOption.IGNORE_CASE),
    Regex("""\bRS\s*([0-9,.]+).*\b(debited|credited|sent|received)\b""", RegexOption.IGNORE_CASE)
)

private val rejectionKeywords = listOf(
    "outstanding", "o/s", "available balance", "a/c bal", "statement", "is rs.", "is inr", "offer", "reward",
    "cashback", "revoked", "declined", "rejected", "failed", "cancelled", "not processed", "not completed", "unblocked",
    "will be debited", "will be credited", "due on", "scheduled for", "mandate", "standing instruction",
    "standing order", "recurring payment", "recurring transfer", "requested"
)

// ✨ NEW FILTER CRITERIA: Isolates and flags explicit Credit Card statement spikes safely
private val creditCardKeywords = listOf(
    "credit card", "credit-card", "spent on card", "charged to card", "cc a/c", "active on card", "card ending"
)

fun parseUpiSms(
    message: String,
    sender: String,
    smsDate: Long,
    customRegexList: List<Regex> = emptyList(),
    bankName: String? = null
): Transaction? {
    // 1. Text Normalization Pre-Processing Pipeline
    val normalizedMessage = message
        .replace(Regex("""\s+"""), " ")
        .replace(Regex("""\b(INR|Rs\.?|INR\s*|Rs\s*)\b""", RegexOption.IGNORE_CASE), "RS")
        .replace(Regex("""RS\s*([0-9])""", RegexOption.IGNORE_CASE), "RS $1")
        .trim()

    val messageLower = normalizedMessage.lowercase(Locale.getDefault())

    // Standard non-transaction spam verification block execution
    if (rejectionKeywords.any { messageLower.contains(it) }) {
        return null
    }

    // ✨ CRITICAL FILTER: Blocks credit card spend tracking leaks
    // Exception Rule: Allows processing if you are explicitly PAYING a card bill using a liquid UPI account app route
    if (creditCardKeywords.any { messageLower.contains(it) }) {
        val isPayingCardViaUpi = messageLower.contains("paid") && messageLower.contains("via upi")
        if (!isPayingCardViaUpi) {
            return null // Dropped completely from saving pathways
        }
    }

    val allRegexToTry = (customRegexList + defaultRegexList).distinct()

    for (regex in allRegexToTry) {
        val matchResult = regex.find(normalizedMessage)
        if (matchResult != null) {
            try {
                val groups = matchResult.groupValues
                var amountStr: String? = null
                var actionKeyword: String? = null
                var isSalaryTxn = false

                for (i in 1 until groups.size) {
                    val value = groups[i].trim()
                    if (value.lowercase(Locale.getDefault()) == "salary") {
                        isSalaryTxn = true
                    }
                    if (value.replace(",", "").toDoubleOrNull() != null) {
                        amountStr = value.replace(",", "")
                    } else if (value.contains("debit") || value.contains("credit") ||
                        value.contains("sent") || value.contains("received") ||
                        value.contains("paid") || value.contains("transfer")) {
                        actionKeyword = value.lowercase(Locale.getDefault())
                    }
                }

                val amount = amountStr?.toDoubleOrNull() ?: continue

                val type: String = when {
                    actionKeyword?.contains("debit") == true ||
                            actionKeyword?.contains("sent") == true ||
                            actionKeyword?.contains("paid") == true ||
                            actionKeyword?.contains("transfer") == true -> "DEBIT"

                    actionKeyword?.contains("credit") == true ||
                            actionKeyword?.contains("received") == true -> "CREDIT"

                    messageLower.contains("debited") ||
                            messageLower.contains("payment of") ||
                            messageLower.contains("sent to") ||
                            messageLower.contains("paid to") -> "DEBIT"

                    messageLower.contains("credited") ||
                            messageLower.contains("received from") ||
                            messageLower.contains("you've received") -> "CREDIT"

                    else -> "UNKNOWN"
                }

                if (type == "UNKNOWN") continue

                val isKnownBank = bankName != null
                val hasUpiKeyword = messageLower.contains("upi")
                val isTrustedStream = isKnownBank || hasUpiKeyword || isSalaryTxn

                if (!isTrustedStream) {
                    continue
                }

                val description = normalizedMessage.take(150)
                val merchantRegex = Regex("""(?:\bat\b|\bto\b|VPA)\s+([A-Za-z0-9\s]+?)(?:\s|$)""", RegexOption.IGNORE_CASE)
                val extractedMerchant = merchantRegex.find(description)?.groupValues?.get(1)?.trim()

                val counterpartyInMsg = if (!extractedMerchant.isNullOrBlank() && extractedMerchant.length < 20) {
                    extractedMerchant.uppercase()
                } else {
                    sender
                }

                return Transaction(
                    amount = amount,
                    type = type,
                    date = smsDate,
                    description = description,
                    senderOrReceiver = counterpartyInMsg,
                    note = if (isSalaryTxn) "Salary Processing" else "",
                    bankName = bankName
                )
            } catch (_: Exception) {
                continue
            }
        }
    }
    return null
}
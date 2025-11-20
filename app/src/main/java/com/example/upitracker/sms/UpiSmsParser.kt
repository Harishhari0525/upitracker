package com.example.upitracker.sms

import com.example.upitracker.data.Transaction
import java.util.Locale

// 1. Added "Relaxed" patterns that don't require the word 'UPI'.
//    These capture standard bank alerts like "Acct XX123 debited by Rs 500"
private val defaultRegexList = listOf(
    // Strict UPI patterns (High confidence)
    Regex("""(?:debited|credited).*\bRs\.?\s*([0-9,.]+).*\bUPI\b""", RegexOption.IGNORE_CASE),
    Regex("""UPI.*\bRs\.?\s*([0-9,.]+).*(debited|credited)""", RegexOption.IGNORE_CASE),

    // Relaxed patterns (No 'UPI' keyword required).
    // We will only trust these if 'bankName' is not null.
    Regex("""(?:debited|credited|sent|received).*\bRs\.?\s*([0-9,.]+)""", RegexOption.IGNORE_CASE),
    Regex("""(?:\bRs\.?|INR)\s*([0-9,.]+).*(?:debited|credited|sent|received)""", RegexOption.IGNORE_CASE)
)

private val rejectionKeywords = listOf(
    "outstanding", "o/s", "available balance", "a/c bal", "statement", "is rs.", "is inr", "offer", "reward",
    "cashback", "revoked", "declined", "rejected", "failed", "cancelled", "not processed", "not completed", "unblocked", "will be debited",
    "will be credited",
    "due on",
    "scheduled for",
    "mandate", "standing instruction", "standing order", "recurring payment", "recurring transfer",
    "requested" // Added to ignore "Money requested" scams
)

fun parseUpiSms(
    message: String,
    sender: String,
    smsDate: Long,
    customRegexList: List<Regex> = emptyList(),
    bankName: String? = null
): Transaction? {
    // Combine custom patterns (if any) with defaults
    val allRegexToTry = (customRegexList + defaultRegexList).distinct()
    val messageLower = message.lowercase(Locale.getDefault())

    if (rejectionKeywords.any { messageLower.contains(it) }) {
        return null
    }

    for (regex in allRegexToTry) {
        val matchResult = regex.find(message)
        if (matchResult != null) {
            try {
                val amountStr = matchResult.groupValues.getOrNull(1)?.replace(",", "")
                val amount = amountStr?.toDoubleOrNull() ?: continue

                val actionKeyword = matchResult.groupValues.getOrNull(2)?.lowercase(Locale.getDefault())

                val type: String = when {
                    actionKeyword?.contains("debit") == true ||
                            actionKeyword?.contains("sent") == true ||
                            actionKeyword?.contains("paid") == true ||
                            actionKeyword?.contains("spent") == true ||
                            actionKeyword?.contains("transfer") == true -> "DEBIT"

                    actionKeyword?.contains("credit") == true ||
                            actionKeyword?.contains("recvd") == true ||
                            actionKeyword?.contains("received") == true -> "CREDIT"

                    messageLower.contains("debited") ||
                            messageLower.contains("payment of") ||
                            messageLower.contains("sent to") ||
                            messageLower.contains("spent on") ||
                            messageLower.contains("paid to") -> "DEBIT"

                    messageLower.contains("credited") ||
                            messageLower.contains("received from") ||
                            messageLower.contains("you've received") -> "CREDIT"

                    else -> "UNKNOWN"
                }

                // ✨ CRITICAL FIX ✨
                // If type is UNKNOWN or "UPI" is missing, we usually skip.
                // BUT, if we successfully identified the Bank Name (e.g., "HDFC Bank"), we trust it more.
                val isKnownBank = bankName != null
                val hasUpiKeyword = messageLower.contains("upi")

                if (type == "UNKNOWN") continue

                // If it's not a known bank AND it doesn't mention UPI, it's likely spam or irrelevant.
                if (!isKnownBank && !hasUpiKeyword) {
                    continue
                }

                val description = message.replace("\n", " ").trim().take(150)
                val noteMsg = matchResult.groupValues.getOrNull(3)?.trim().orEmpty()

                return Transaction(
                    amount = amount,
                    type = type,
                    date = smsDate,
                    description = description,
                    senderOrReceiver = sender,
                    note = noteMsg,
                    bankName = bankName
                )
            } catch (_: Exception) {
                continue
            }
        }
    }
    return null
}
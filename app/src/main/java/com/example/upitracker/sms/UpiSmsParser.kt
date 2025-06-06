package com.example.upitracker.sms

import com.example.upitracker.data.Transaction
import java.util.Locale

// Reverted to your original simpler defaultRegexList.
// You can expand this list with any other patterns you need.
private val defaultRegexList = listOf(
    Regex("""(?:debited|credited).*\bRs\.?\s*([0-9,.]+).*\bUPI\b""", RegexOption.IGNORE_CASE),
    Regex("""UPI.*\bRs\.?\s*([0-9,.]+).*(debited|credited)""", RegexOption.IGNORE_CASE)
    // Add back any other default patterns here if needed
)

fun parseUpiSms(
    message: String,
    sender: String,
    smsDate: Long,
    customRegexList: List<Regex> = emptyList() // Custom regex from user preferences
): Transaction? {
    // Combine custom patterns (if any) with defaults, then remove duplicates
    val allRegexToTry = (customRegexList + defaultRegexList).distinct()
    // Lower‐cased version of the entire SMS text, for fallback keyword checks
    val messageLower = message.lowercase(Locale.getDefault())

    // Try each regex in turn. As soon as one matches, we extract groupValues etc.
    for (regex in allRegexToTry) {
        val matchResult = regex.find(message)
        if (matchResult != null) {
            try {
                // 1) Extract the raw amount string from Group 1, then parse to Double
                val amountStr = matchResult.groupValues.getOrNull(1)
                    ?.replace(",", "")
                val amount = amountStr
                    ?.toDoubleOrNull()
                    ?: continue  // If parsing fails, skip to next regex

                // 2) If your regex has a second group (e.g., "(debited|credited)"),
                //    capture it as actionKeyword; otherwise, actionKeyword will be null.
                val actionKeyword = matchResult.groupValues.getOrNull(2)
                    ?.lowercase(Locale.getDefault())

                // 3) Infer transaction type (DEBIT or CREDIT). First check the captured group:
                var type: String = when {
                    actionKeyword?.contains("debit") == true ||
                            actionKeyword?.contains("sent") == true ||
                            actionKeyword?.contains("paid") == true ||
                            actionKeyword?.contains("spent") == true ||
                            actionKeyword?.contains("transfer") == true -> "DEBIT"

                    actionKeyword?.contains("credit") == true ||
                            actionKeyword?.contains("recvd") == true ||
                            actionKeyword?.contains("received") == true -> "CREDIT"

                    // 4) If regex didn’t capture a reliable keyword, check the entire message text:
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

                // 5) If still UNKNOWN and the SMS doesn’t mention “UPI” at all, skip:
                if (type == "UNKNOWN" && !messageLower.contains("upi")) {
                    continue
                }

                // 6) Build a cleaned‐up description (single‐line, trimmed, up to 150 chars)
                val description = message
                    .replace("\n", " ")
                    .trim()
                    .take(150)

                // 7) Determine senderOrReceiver (counterparty). By default, use the SMS sender ID.
                //    If you had additional capture groups for counterparty, you could override here.
                val counterpartyInMsg = sender

                // 8) Determine note (for things like UPI Ref or transaction ID), if your regex captures it.
                //    Otherwise leave empty. Example: Group 3 might be a reference in a custom pattern.
                val noteMsg = matchResult.groupValues.getOrNull(3)?.trim().orEmpty()

                // 9) Finally, return the Transaction object
                return Transaction(
                    amount = amount,
                    type = type,
                    date = smsDate,
                    description = description,
                    senderOrReceiver = counterpartyInMsg,
                    note = noteMsg
                )
            } catch (ex: Exception) {
                // If anything goes wrong inside this try (unlikely once groups are correct),
                // skip to the next regex pattern.
                continue
            }
        }
    }

    // No regex matched -> not a valid UPI SMS we care about
    return null
}

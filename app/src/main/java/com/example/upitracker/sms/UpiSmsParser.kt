package com.example.upitracker.sms

import com.example.upitracker.data.Transaction
import java.util.Locale

// Reverted to your original simpler defaultRegexList
private val defaultRegexList = listOf(
    Regex("""(?:debited|credited).*\bRs\.?\s*([0-9,\.]+).*\bUPI\b""", RegexOption.IGNORE_CASE),
    Regex("""UPI.*\bRs\.?\s*([0-9,\.]+).*(debited|credited)""", RegexOption.IGNORE_CASE)
    // Add back any other defaults you had here if this list was more extensive
)

fun parseUpiSms(
    message: String,
    sender: String,
    smsDate: Long,
    customRegexList: List<Regex> = emptyList() // Custom regex from user preferences
): Transaction? {
    val allRegexToTry = (customRegexList + defaultRegexList).distinct() // Prioritize custom regex
    val messageLower = message.lowercase(Locale.getDefault())

    for (regex in allRegexToTry) {
        val match = regex.find(message)
        if (match != null) {
            try {
                // Assuming Group 1 is always the amount in your original regex
                val amountStr = match.groupValues.getOrNull(1)?.replace(",", "")
                val amount = amountStr?.toDoubleOrNull() ?: continue // Skip if amount can't be parsed

                var type: String
                // Try to infer type from keywords in the message if regex doesn't capture it explicitly
                // or if the captured group for type (if any) is unreliable in your original regex.
                when {
                    messageLower.contains("debited from") ||
                            messageLower.contains("debited") ||
                            messageLower.contains("payment of") ||
                            messageLower.contains("sent to") ||
                            messageLower.contains("spent on") ||
                            messageLower.contains("paid to") -> type = "DEBIT"

                    messageLower.contains("credited to") ||
                            messageLower.contains("credited by") ||
                            messageLower.contains("credited with") ||
                            messageLower.contains("received from") ||
                            messageLower.contains("you've received") -> type = "CREDIT"
                    else -> {
                        // If your original regex had a group for "debited/credited", use it here.
                        // For example, if group 2 was (debited|credited):
                        // val actionKeyword = match.groupValues.getOrNull(2)?.lowercase(Locale.getDefault())
                        // if (actionKeyword == "debited") type = "DEBIT"
                        // else if (actionKeyword == "credited") type = "CREDIT"
                        // else continue // Or log as unknown if essential keywords are missing

                        // If we can't determine type from general keywords, and the regex doesn't specify,
                        // it's risky to assume. We might skip this match.
                        // However, if "UPI" is present, we can be a bit more lenient.
                        if (messageLower.contains("upi")) {
                            // If still can't determine, maybe set to "UNKNOWN" or skip
                            type = "UNKNOWN" // Or consider skipping by `continue`
                        } else {
                            // If "UPI" isn't even present, and type is unknown, definitely skip
                            continue
                        }
                    }
                }

                if (type == "UNKNOWN" && !messageLower.contains("upi")) {
                    // If "UPI" isn't in the message and type is unknown, it's likely not what we want.
                    continue
                }


                // For description and senderOrReceiver, your original Transaction used:
                // description = message.take(60)
                // senderOrReceiver = sender (the SMS sender ID)
                // You can enhance these if your original regex had more capture groups for counterparty, etc.
                // For now, reverting to a simple description based on the message.
                val description = message.replace("\n", " ").trim().take(150) // Cleaned up and longer

                // Extracting counterparty and note (like UPI Ref) would ideally come from
                // specific capture groups in your regex if they are designed for it.
                // If your original regex didn't have these, senderOrReceiver defaults to SMS sender.
                var counterpartyInMsg = sender // Default to SMS sender
                var noteMsg = "" // Default empty note

                // Example: if your regex (custom or default) happens to have capture groups for these:
                // counterpartyInMsg = match.groupValues.getOrNull(X)?.trim() ?: sender // Where X is group index for party
                // noteMsg = match.groupValues.getOrNull(Y)?.trim() ?: ""       // Where Y is group index for Ref ID

                return Transaction(
                    amount = amount,
                    type = type,
                    date = smsDate,
                    description = description,
                    senderOrReceiver = counterpartyInMsg,
                    note = noteMsg
                )
            } catch (e: Exception) {
                // android.util.Log.w("UpiSmsParser", "Error during parsing with regex '$regex': ${e.message}")
                continue // Try next regex
            }
        }
    }
    return null
}
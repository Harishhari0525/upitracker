package com.example.upitracker.sms

import com.example.upitracker.data.Transaction
import java.util.Locale

/**
 * Data class representing a structured template for parsing a bank's SMS.
 */
data class BankSmsTemplate(
    val type: String, // "DEBIT" or "CREDIT"
    val regex: Regex,
    val amountGroup: Int,
    val accountGroup: Int? = null,
    val counterpartyGroup: Int? = null,
    val dateGroup: Int? = null,
    val refNoGroup: Int? = null,
    val balanceGroup: Int? = null // ✨ New field for Feature 4
)

/**
 * Registry of bank-specific SMS templates based on common transaction formats.
 */
object BankSmsTemplates {
    private val hdfc = listOf(
        BankSmsTemplate("DEBIT", Regex("""A debit of RS ([\d,.]+) was made from account (\d+) to VPA ([\w.\-]+@[\w\-]+).*?Bal(?:ance)?\s*(?:is|:)?\s*RS\s*(\d+[\d,.]*\d+|\d+)""", RegexOption.IGNORE_CASE), 1, 2, 3, null, null, 4),
        BankSmsTemplate("DEBIT", Regex("""A debit of RS ([\d,.]+) was made from account (\d+) to VPA ([\w.\-]+@[\w\-]+)""", RegexOption.IGNORE_CASE), 1, 2, 3),
        BankSmsTemplate("CREDIT", Regex("""RS ([\d,.]+) credited to HDFC Bank A/ c xx(\d+) on ([\d-]+) from VPA ([\w.\-]+@[\w\-]+).*?Bal(?:ance)?\s*(?:is|:)?\s*RS\s*(\d+[\d,.]*\d+|\d+)""", RegexOption.IGNORE_CASE), 1, 2, 4, 3, null, 5),
        BankSmsTemplate("CREDIT", Regex("""RS ([\d,.]+) credited to HDFC Bank A/ c xx(\d+) on ([\d-]+) from VPA ([\w.\-]+@[\w\-]+)""", RegexOption.IGNORE_CASE), 1, 2, 4, 3)
    )

    private val icici = listOf(
        BankSmsTemplate("DEBIT", Regex("""(?:A/c|Acct|Account)\s*(?:XX|[\d*]+)(\d+)\s*(?:has been|is)?\s*debited\s*(?:for|with|by|of)?\s*RS\s*([\d,.]+).*?Bal(?:ance)?\s*(?:is|:)?\s*RS\s*(\d+[\d,.]*\d+|\d+)""", RegexOption.IGNORE_CASE), 2, 1, null, null, null, 3),
        BankSmsTemplate("DEBIT", Regex("""(?:A/c|Acct|Account)\s*(?:XX|[\d*]+)(\d+)\s*(?:has been|is)?\s*debited\s*(?:for|with|by|of)?\s*RS\s*([\d,.]+)""", RegexOption.IGNORE_CASE), 2, 1),
        BankSmsTemplate("CREDIT", Regex("""(?:A/c|Acct|Account)\s*(?:XX|[\d*]+)(\d+)\s*(?:has been|is)?\s*credited\s*(?:for|with|by|of)?\s*RS\s*([\d,.]+).*?Bal(?:ance)?\s*(?:is|:)?\s*RS\s*(\d+[\d,.]*\d+|\d+)""", RegexOption.IGNORE_CASE), 2, 1, null, null, null, 3),
        BankSmsTemplate("CREDIT", Regex("""(?:A/c|Acct|Account)\s*(?:XX|[\d*]+)(\d+)\s*(?:has been|is)?\s*credited\s*(?:for|with|by|of)?\s*RS\s*([\d,.]+)""", RegexOption.IGNORE_CASE), 2, 1)
    )

    private val axis = listOf(
        BankSmsTemplate("DEBIT", Regex("""RS ([\d,.]+) has been debited from (?:A/c|Acct|Account)\s*no\.\s*(?:XX|[\d*]+)(\d+) on ([\d-]+)\. Info-UPI/([\w.\-/]+)/(\d+).*?Bal(?:ance)?\s*(?:is|:)?\s*RS\s*(\d+[\d,.]*\d+|\d+)""", RegexOption.IGNORE_CASE), 1, 2, 4, 3, 5, 6),
        BankSmsTemplate("DEBIT", Regex("""RS ([\d,.]+) has been debited from (?:A/c|Acct|Account)\s*no\.\s*(?:XX|[\d*]+)(\d+) on ([\d-]+)\. Info-UPI/([\w.\-/]+)/(\d+)""", RegexOption.IGNORE_CASE), 1, 2, 4, 3, 5),
        BankSmsTemplate("CREDIT", Regex("""RS ([\d,.]+) has been credited to (?:A/c|Acct|Account)\s*no\.\s*(?:XX|[\d*]+)(\d+) on ([\d-]+)\. Info-UPI/([\w.\-/]+)/(\d+).*?Bal(?:ance)?\s*(?:is|:)?\s*RS\s*(\d+[\d,.]*\d+|\d+)""", RegexOption.IGNORE_CASE), 1, 2, 4, 3, 5, 6),
        BankSmsTemplate("CREDIT", Regex("""RS ([\d,.]+) has been credited to (?:A/c|Acct|Account)\s*no\.\s*(?:XX|[\d*]+)(\d+) on ([\d-]+)\. Info-UPI/([\w.\-/]+)/(\d+)""", RegexOption.IGNORE_CASE), 1, 2, 4, 3, 5)
    )

    private val sbi = listOf(
        BankSmsTemplate("DEBIT", Regex("""Your (?:A/c|Acct|Account)\s*(?:X|[\d*]+)(\d+) debited by RS ([\d,.]+) on ([\d-]+) via UPI Ref (\d+).*?(?:Bal|Balance|Avl\s+Bal)\s*(?:is|:)?\s*RS\s*(\d+[\d,.]*\d+|\d+)""", RegexOption.IGNORE_CASE), 2, 1, null, 3, 4, 5),
        BankSmsTemplate("DEBIT", Regex("""Your (?:A/c|Acct|Account)\s*(?:X|[\d*]+)(\d+) debited by RS ([\d,.]+) on ([\d-]+) via UPI Ref (\d+)""", RegexOption.IGNORE_CASE), 2, 1, null, 3, 4),
        BankSmsTemplate("CREDIT", Regex("""Your (?:A/c|Acct|Account)\s*(?:X|[\d*]+)(\d+) credited by RS ([\d,.]+) on ([\d-]+) via UPI Ref (\d+).*?(?:Bal|Balance|Avl\s+Bal)\s*(?:is|:)?\s*RS\s*(\d+[\d,.]*\d+|\d+)""", RegexOption.IGNORE_CASE), 2, 1, null, 3, 4, 5),
        BankSmsTemplate("CREDIT", Regex("""Your (?:A/c|Acct|Account)\s*(?:X|[\d*]+)(\d+) credited by RS ([\d,.]+) on ([\d-]+) via UPI Ref (\d+)""", RegexOption.IGNORE_CASE), 2, 1, null, 3, 4)
    )

    private val kotak = listOf(
        BankSmsTemplate("DEBIT", Regex("""Your (?:A/c|Acct|Account)\s*(?:XX|[\d*]+)(\d+) debited by ([\d,.]+) on ([\d-]+)\. Ref:\s*(\w+).*?Bal(?:ance)?\s*(?:is|:)?\s*RS\s*(\d+[\d,.]*\d+|\d+)""", RegexOption.IGNORE_CASE), 2, 1, null, 3, 4, 5),
        BankSmsTemplate("DEBIT", Regex("""Your (?:A/c|Acct|Account)\s*(?:XX|[\d*]+)(\d+) debited by ([\d,.]+) on ([\d-]+)\. Ref:\s*(\w+)""", RegexOption.IGNORE_CASE), 2, 1, null, 3, 4),
        BankSmsTemplate("CREDIT", Regex("""Your (?:A/c|Acct|Account)\s*(?:XX|[\d*]+)(\d+) credited by ([\d,.]+) on ([\d-]+)\. Ref:\s*(\w+).*?Bal(?:ance)?\s*(?:is|:)?\s*RS\s*(\d+[\d,.]*\d+|\d+)""", RegexOption.IGNORE_CASE), 2, 1, null, 3, 4, 5),
        BankSmsTemplate("CREDIT", Regex("""Your (?:A/c|Acct|Account)\s*(?:XX|[\d*]+)(\d+) credited by ([\d,.]+) on ([\d-]+)\. Ref:\s*(\w+)""", RegexOption.IGNORE_CASE), 2, 1, null, 3, 4)
    )

    private val common = listOf(
        BankSmsTemplate("DEBIT", Regex("""(?:Your\s+)?(?:A/c|Acct|Account)\s*(?:XX|[\d*]+)?(\d+)\s*(?:has been|is)?\s*debited\s*(?:for|with|by|of)?\s*RS\s*([\d,.]+).*?Bal(?:ance)?\s*(?:is|:)?\s*RS\s*(\d+[\d,.]*\d+|\d+)""", RegexOption.IGNORE_CASE), 2, 1, null, null, null, 3),
        BankSmsTemplate("DEBIT", Regex("""(?:Your\s+)?(?:A/c|Acct|Account)\s*(?:XX|[\d*]+)?(\d+)\s*(?:has been|is)?\s*debited\s*(?:for|with|by|of)?\s*RS\s*([\d,.]+)""", RegexOption.IGNORE_CASE), 2, 1),
        BankSmsTemplate("CREDIT", Regex("""(?:Your\s+)?(?:A/c|Acct|Account)\s*(?:XX|[\d*]+)?(\d+)\s*(?:has been|is)?\s*credited\s*(?:for|with|by|of)?\s*RS\s*([\d,.]+).*?Bal(?:ance)?\s*(?:is|:)?\s*RS\s*(\d+[\d,.]*\d+|\d+)""", RegexOption.IGNORE_CASE), 2, 1, null, null, null, 3),
        BankSmsTemplate("CREDIT", Regex("""(?:Your\s+)?(?:A/c|Acct|Account)\s*(?:XX|[\d*]+)?(\d+)\s*(?:has been|is)?\s*credited\s*(?:for|with|by|of)?\s*RS\s*([\d,.]+)""", RegexOption.IGNORE_CASE), 2, 1)
    )

    fun getTemplatesForBank(bankName: String?): List<BankSmsTemplate> {
        return when {
            bankName?.contains("HDFC", ignoreCase = true) == true -> hdfc
            bankName?.contains("ICICI", ignoreCase = true) == true -> icici
            bankName?.contains("AXIS", ignoreCase = true) == true -> axis
            bankName?.contains("SBI", ignoreCase = true) == true || bankName?.contains("State Bank", ignoreCase = true) == true -> sbi
            bankName?.contains("KOTAK", ignoreCase = true) == true -> kotak
            bankName?.contains("PNB", ignoreCase = true) == true || bankName?.contains("Baroda", ignoreCase = true) == true -> common
            else -> common
        }
    }
}

private val defaultRegexList = listOf(
    // High Confidence: Explicit Salary Credits
    Regex("""\b(salary)\b.*?\b(credited|received)\b.*?\bRS\s*([0-9,.]+)""", RegexOption.IGNORE_CASE),
    Regex("""\b(credited|received)\b.*?\b(salary)\b.*?\bRS\s*([0-9,.]+)""", RegexOption.IGNORE_CASE),

    // Strict UPI patterns: Group 1 = Action, Group 2 = Amount
    Regex("""\b(debited|credited|sent|paid)\b.*?\bRS\s*([0-9,.]+).*?\bUPI\b""", RegexOption.IGNORE_CASE),
    Regex("""\bUPI\b.*?\bRS\s*([0-9,.]+).*?\b(debited|credited|received)\b""", RegexOption.IGNORE_CASE),

    // Relaxed patterns
    Regex("""\b(debited|credited|sent|received|paid)\b.*?\bRS\s*([0-9,.]+)""", RegexOption.IGNORE_CASE),
    Regex("""\bRS\s*([0-9,.]+).*?\b(debited|credited|sent|received|paid)\b""", RegexOption.IGNORE_CASE)
)

private val rejectionKeywords = listOf(
    "statement", "offer", "reward",
    "cashback", "revoked", "declined", "rejected", "failed", "cancelled", "not processed", "not completed", "unblocked",
    "will be debited", "will be credited", "due on", "scheduled for", "mandate", "standing instruction",
    "standing order", "recurring payment", "recurring transfer", "requested", "validity", "policy", "received the premium",
    "otp", "verification code", "password", "login", "security alert", "kyc", "update your", "click here", "NEFT"
)

private val creditCardKeywords = listOf(
    "credit card", "credit-card", "spent on card", "charged to card", "cc a/c", "active on card", "card ending"
)

private fun extractBalanceFromNormalizedMessage(message: String): Double? {
    val balanceRegexes = listOf(
        Regex("""\b(?:available\s+balance\b|avl\.?\s+bal\b\.?|balance\b|bal\b\.?|a/c\s+bal\b\.?)[^\d]*?RS\s*(\d+[\d,.]*\d+|\d+)""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:avl\b\.?|available\b)\s+(?:limit\b|bal\b\.?|balance\b)[^\d]*?RS\s*(\d+[\d,.]*\d+|\d+)""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:outstanding\b|o/s\b)[^\d]*?RS\s*(\d+[\d,.]*\d+|\d+)""", RegexOption.IGNORE_CASE)
    )
    for (regex in balanceRegexes) {
        val matchResult = regex.find(message)
        if (matchResult != null) {
            val balStr = matchResult.groupValues[1].replace(",", "")
            val balance = balStr.toDoubleOrNull()
            if (balance != null) {
                return balance
            }
        }
    }
    return null
}


fun parseUpiSms(
    message: String,
    sender: String,
    smsDate: Long,
    customRegexList: List<Regex> = emptyList(),
    bankName: String? = null
): Transaction? {
    // 1. Text Normalization Pre-Processing
    val normalizedMessage = message
        .replace(Regex("""\s+"""), " ")
        .replace(Regex("""\b(INR|Rs\.?)(?=\s|\d|\.|$)|₹""", RegexOption.IGNORE_CASE), "RS")
        .replace(Regex("""RS\s*([0-9])""", RegexOption.IGNORE_CASE), "RS $1")
        .trim()

    val messageLower = normalizedMessage.lowercase(Locale.getDefault())

    // Standard non-transaction spam verification
    if (rejectionKeywords.any { messageLower.contains(it) }) return null

    // Credit card filter
    if (creditCardKeywords.any { messageLower.contains(it) }) {
        val isPayingCardViaUpi = messageLower.contains("paid") && messageLower.contains("via upi")
        if (!isPayingCardViaUpi) return null
    }

    // --- PHASE 1: Try Bank-Specific Templates (High Accuracy) ---
    val templates = BankSmsTemplates.getTemplatesForBank(bankName)
    for (template in templates) {
        val matchResult = template.regex.find(normalizedMessage)
        if (matchResult != null) {
            try {
                val groups = matchResult.groupValues
                val amount = groups[template.amountGroup].replace(",", "").toDoubleOrNull() ?: continue
                
                val counterpartyInMsg = template.counterpartyGroup?.let { groups.getOrNull(it) }?.trim()?.uppercase()
                
                var balanceAfterTxn = template.balanceGroup?.let { 
                    groups.getOrNull(it)?.replace(",", "")?.toDoubleOrNull() 
                }
                if (balanceAfterTxn == null) {
                    balanceAfterTxn = extractBalanceFromNormalizedMessage(normalizedMessage)
                }
                
                return Transaction(
                    amount = amount,
                    type = template.type,
                    date = smsDate,
                    description = normalizedMessage.take(150),
                    senderOrReceiver = counterpartyInMsg ?: sender,
                    note = "",
                    bankName = bankName,
                    balanceAfterTransaction = balanceAfterTxn
                )
            } catch (_: Exception) { continue }
        }
    }

    // --- PHASE 2: Fallback to General Regexes (High Flexibility) ---
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

                if (!isTrustedStream) continue

                val description = normalizedMessage.take(150)
                val merchantRegex = Regex("""(?:\bat\b|\bto\b|VPA)\s+([A-Za-z0-9.\-_@\s]+?)(?:\s|$)""", RegexOption.IGNORE_CASE)
                val extractedMerchant = merchantRegex.find(description)?.groupValues?.get(1)?.trim()

                val counterpartyInMsg = if (!extractedMerchant.isNullOrBlank() && extractedMerchant.length < 30) {
                    extractedMerchant.uppercase()
                } else {
                    sender
                }

                val balanceAfterTxn = extractBalanceFromNormalizedMessage(normalizedMessage)

                return Transaction(
                    amount = amount,
                    type = type,
                    date = smsDate,
                    description = description,
                    senderOrReceiver = counterpartyInMsg,
                    note = if (isSalaryTxn) "Salary Processing" else "",
                    bankName = bankName,
                    balanceAfterTransaction = balanceAfterTxn
                )
            } catch (_: Exception) {
                continue
            }
        }
    }
    return null
}
package com.example.upitracker.util

object BankIdentifier {
    /**
     * Identifies a bank name from a given SMS sender address.
     * This is the single source of truth for bank name detection.
     * @param sender The original sender address (e.g., "VM-HDFCBK").
     * @return A standardized bank name or null if not identified.
     */
    fun getBankName(sender: String): String? {
        val upperSender = sender.uppercase()
        val normalizedHeader = normalizeDltSenderHeader(sender)
        val searchable = "$upperSender $normalizedHeader"
        return when {
            searchable.contains("HDFC") || searchable.contains("HDFCBK") -> "HDFC Bank"
            searchable.contains("ICICI") || searchable.contains("ICICIT") -> "ICICI Bank"
            searchable.contains("SBI") || searchable.contains("SBIN") -> "State Bank of India"
            searchable.contains("AXIS") || searchable.contains("AXISBK") -> "Axis Bank"
            searchable.contains("KOTAK") || searchable.contains("KOTAKB") -> "Kotak Mahindra Bank"
            searchable.contains("PAYTM") -> "Paytm Payments Bank"
            searchable.contains("CITI") -> "Citibank"
            searchable.contains("YES") || searchable.contains("YESBNK") -> "Yes Bank"
            searchable.contains("IDFC") -> "IDFC FIRST Bank"
            searchable.contains("INDUSIND") || searchable.contains("INDBNK") -> "IndusInd Bank"
            searchable.contains("BARODA") || searchable.contains("BOBTXN") || searchable.contains("BOB") -> "Bank of Baroda"
            searchable.contains("PNB") -> "Punjab National Bank"
            searchable.contains("UNION") || searchable.contains("UNIONB") -> "Union Bank of India"
            searchable.contains("CANARA") || searchable.contains("CANBNK") -> "Canara Bank"
            searchable.contains("AUBANK") || searchable.contains("AUSFB") -> "AU Small Finance Bank"
            searchable.contains("FEDERAL") || searchable.contains("FEDBNK") -> "Federal Bank"
            searchable.contains("RBL") -> "RBL Bank"
            searchable.contains("IDBI") -> "IDBI Bank"
            else -> null
        }
    }

    /**
     * DLT sender IDs commonly look like AX-ICICIT-S, VM-HDFCBK, VK-SBIBNK, etc.
     * The first segment identifies operator/circle routing; the middle/header
     * segment identifies the principal entity or service stream.
     */
    fun normalizeDltSenderHeader(sender: String): String {
        val upper = sender.uppercase().trim()
        val parts = upper.split('-').filter { it.isNotBlank() }
        return when {
            parts.size >= 2 -> parts[1]
            else -> upper
        }.replace(Regex("""[^A-Z0-9]"""), "")
    }
}

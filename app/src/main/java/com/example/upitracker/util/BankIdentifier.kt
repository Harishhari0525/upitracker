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
        return when {
            upperSender.contains("HDFC") -> "HDFC Bank"
            upperSender.contains("ICICI") -> "ICICI Bank"
            upperSender.contains("SBI") || upperSender.contains("SBIN") -> "State Bank of India"
            upperSender.contains("AXIS") -> "Axis Bank"
            upperSender.contains("KOTAK") -> "Kotak Mahindra Bank"
            upperSender.contains("PAYTM") -> "Paytm Payments Bank"
            upperSender.contains("CITI") -> "Citibank"
            upperSender.contains("YES") -> "Yes Bank"
            upperSender.contains("IDFC") -> "IDFC FIRST Bank"
            upperSender.contains("INDUSIND") -> "IndusInd Bank"
            upperSender.contains("BARODA") -> "Bank of Baroda"
            upperSender.contains("PNB") -> "Punjab National Bank"
            upperSender.contains("UNION") -> "Union Bank of India"
            upperSender.contains("CANARA") -> "Canara Bank"
            upperSender.contains("AU") -> "AU Small Finance Bank"
            upperSender.contains("FEDERAL") -> "Federal Bank"
            upperSender.contains("RBL") -> "RBL Bank"
            upperSender.contains("IDBI") -> "IDBI Bank"
            else -> null
        }
    }
}
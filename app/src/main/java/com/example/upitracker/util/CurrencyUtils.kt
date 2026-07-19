package com.example.upitracker.util

import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {
    private val indianRupeeLocale: Locale = Locale.Builder()
        .setLanguage("en")
        .setRegion("IN")
        .build()

    /**
     * Returns a standard Indian Rupee NumberFormat instance.
     * Default maximumFractionDigits is 0 (rounds to nearest whole Rupee).
     */
    fun getRupeeFormatter(maximumFractionDigits: Int = 0): NumberFormat {
        return NumberFormat.getCurrencyInstance(indianRupeeLocale).apply {
            this.maximumFractionDigits = maximumFractionDigits
        }
    }

    /**
     * Directly formats a Double value into a standard Indian Rupee currency string.
     */
    fun formatRupee(amount: Double, maximumFractionDigits: Int = 0): String {
        return getRupeeFormatter(maximumFractionDigits).format(amount)
    }
}

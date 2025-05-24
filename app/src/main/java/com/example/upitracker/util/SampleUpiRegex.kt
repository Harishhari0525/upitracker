package com.example.upitracker.util

val sampleUpiRegex = listOf(
    """(?:debited|credited).*Rs\.?\s*([0-9,.]+).*?UPI""",
    """UPI.*?Rs\.?\s*([0-9,.]+).*?(debited|credited)""",
)

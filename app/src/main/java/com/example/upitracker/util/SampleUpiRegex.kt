package com.example.upitracker.util

// These are example default patterns.
// For real-world use, these would need to be more robust and tested against various SMS formats.
// Consider allowing users to enable/disable/edit these defaults as well,
// or initializing RegexPreference with these if no user patterns exist.
val sampleUpiRegex = listOf(
    // Regex to capture amount, and infer type based on keywords.
    // Group 1: Amount (e.g., 123.45 or 1,234)
    // This pattern is very broad and might need refinement.
    """(?:Rs\.?|INR)\s*([\d,]+\.?\d*|\.?\d+)\s*.*?(?:debited|credited|sent|received|paid|transferred|spent)""",

    // More specific examples (can be expanded):
    """debited\s*by\s*Rs\.?\s*([\d,]+\.?\d*|\.?\d+).*?UPI\s*Ref""",
    """credited\s*with\s*Rs\.?\s*([\d,]+\.?\d*|\.?\d+).*?UPI\s*Ref""",
    """payment\s*of\s*Rs\.?\s*([\d,]+\.?\d*|\.?\d+)""",
    """received\s*Rs\.?\s*([\d,]+\.?\d*|\.?\d+)"""
    // It's generally better to have regex that capture more context (like VPA, account numbers, transaction IDs)
    // if possible, to make parsing more reliable and data richer.
)

// You might want to provide these as actual Regex objects if used directly by parsers:
// val sampleUpiRegexObjects = sampleUpiRegex.map { Regex(it, RegexOption.IGNORE_CASE) }
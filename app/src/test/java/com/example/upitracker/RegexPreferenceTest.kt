package com.example.upitracker

import com.example.upitracker.util.RegexPreference
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RegexPreferenceTest {
    @Test fun acceptsBoundedPaymentPattern() {
        assertNull(
            RegexPreference.validationError(
                """(?:Rs\.?|INR)\s*([\d,]+\.?\d*)"""
            )
        )
    }

    @Test fun rejectsNestedRepeatingGroups() {
        assertNotNull(RegexPreference.validationError("""(a+)+$"""))
    }

    @Test fun rejectsOversizedPatterns() {
        assertNotNull(RegexPreference.validationError("a".repeat(501)))
    }
}

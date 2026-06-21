package com.example.upitracker

import com.example.upitracker.sms.parseUpiSms
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class UpiSmsParserTest {
    @Test fun parsesMaskedSbiDebit() {
        val parsed = parseUpiSms(
            message = "Your A/c X1234 debited by RS 1,250.50 on 21-06-26 via UPI Ref 123456789012",
            sender = "AD-SBIBNK",
            smsDate = 1_781_987_400_000,
            customRegexList = emptyList(),
            bankName = "SBI"
        )
        assertNotNull(parsed)
        assertEquals(125_050L, parsed!!.amountPaise)
        assertEquals("DEBIT", parsed.type)
    }
}

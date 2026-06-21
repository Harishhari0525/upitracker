package com.example.upitracker

import com.example.upitracker.util.toMajorUnits
import com.example.upitracker.util.toPaise
import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyTest {
    @Test fun convertsMajorUnitsUsingFinancialRounding() {
        assertEquals(101L, 1.005.toPaise())
        assertEquals(100L, 1.004.toPaise())
        assertEquals(-101L, (-1.005).toPaise())
    }

    @Test fun convertsPaiseWithoutPrecisionLoss() {
        assertEquals(9_999_999.99, 999_999_999L.toMajorUnits(), 0.0)
    }

    @Test fun categoryTotalsRemainExactInPaise() {
        val amounts = List(10) { 500.0.toPaise() }
        assertEquals(500_000L, amounts.sum())
        assertEquals(5_000.0, amounts.sum().toMajorUnits(), 0.0)
    }
}

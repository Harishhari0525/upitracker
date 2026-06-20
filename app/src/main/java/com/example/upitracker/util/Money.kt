package com.example.upitracker.util

import java.math.BigDecimal
import java.math.RoundingMode

private val ONE_HUNDRED = BigDecimal.valueOf(100L)

fun Double.toPaise(): Long = BigDecimal.valueOf(this)
    .multiply(ONE_HUNDRED)
    .setScale(0, RoundingMode.HALF_UP)
    .longValueExact()

fun Long.toMajorUnits(): Double = BigDecimal.valueOf(this)
    .divide(ONE_HUNDRED, 2, RoundingMode.UNNECESSARY)
    .toDouble()


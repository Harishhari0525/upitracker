package com.example.upitracker.util

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.upitracker.R

/**
 * Payment Pulse type system.
 *
 * Work Sans carries the dense ledger and interface content. Poppins is reserved
 * for a small number of high-emphasis financial statements so the product no
 * longer reads like a Poppins template from top to bottom.
 */
val PulseDisplay = FontFamily(
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold)
)

val PulseText = FontFamily(
    Font(R.font.worksans_regular, FontWeight.Normal),
    Font(R.font.worksans_medium, FontWeight.Medium),
    Font(R.font.worksans_semibold, FontWeight.SemiBold),
    Font(R.font.worksans_bold, FontWeight.Bold)
)

// Compatibility aliases for existing screens while they migrate.
val Poppins = PulseDisplay
val WorkSans = PulseText

private fun display(size: Int, line: Int, weight: FontWeight = FontWeight.SemiBold) = TextStyle(
    fontFamily = PulseDisplay,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = (-0.35).sp
)

private fun text(
    size: Int,
    line: Int,
    weight: FontWeight = FontWeight.Normal,
    tracking: Float = 0f
) = TextStyle(
    fontFamily = PulseText,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp
)

val AppTypography = Typography(
    displayLarge = display(52, 58, FontWeight.SemiBold),
    displayMedium = display(44, 50, FontWeight.SemiBold),
    displaySmall = display(36, 42, FontWeight.SemiBold),
    headlineLarge = display(32, 38, FontWeight.SemiBold),
    headlineMedium = display(28, 34, FontWeight.SemiBold),
    headlineSmall = display(24, 30, FontWeight.SemiBold),
    titleLarge = text(22, 28, FontWeight.SemiBold, -0.15f),
    titleMedium = text(17, 22, FontWeight.SemiBold),
    titleSmall = text(15, 20, FontWeight.SemiBold),
    bodyLarge = text(16, 23),
    bodyMedium = text(14, 20),
    bodySmall = text(12, 17),
    labelLarge = text(14, 20, FontWeight.SemiBold, 0.1f),
    labelMedium = text(12, 16, FontWeight.SemiBold, 0.25f),
    labelSmall = text(11, 15, FontWeight.SemiBold, 0.45f)
)

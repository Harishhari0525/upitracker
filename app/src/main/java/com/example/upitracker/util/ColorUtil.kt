package com.example.upitracker.util

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt

fun parseColor(hex: String, defaultColor: Color = Color.Gray): Color {
    return try {
        Color(hex.toColorInt())
    } catch (_: IllegalArgumentException) {
        defaultColor
    }
}
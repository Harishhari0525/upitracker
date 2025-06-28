package com.example.upitracker.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.DecimalFormat

class DecimalInputVisualTransformation : VisualTransformation {

    // Formatter for Indian currency with up to two decimal places
    private val formatter = DecimalFormat("##,##,##0.##")

    override fun filter(text: AnnotatedString): TransformedText {
        if (text.text.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val originalText = text.text
        val number = originalText.toDoubleOrNull() ?: return TransformedText(text, OffsetMapping.Identity)

        val formattedText = formatter.format(number)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                // This logic is complex, for simplicity we can just move cursor to the end
                return formattedText.length
            }

            override fun transformedToOriginal(offset: Int): Int {
                // This logic is complex, for simplicity we can just use the original length
                return originalText.length
            }
        }

        return TransformedText(AnnotatedString(formattedText), offsetMapping)
    }
}
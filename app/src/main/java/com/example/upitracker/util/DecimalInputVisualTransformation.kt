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
                // Calculate the number of commas that appear before the original cursor position
                val originalStr = text.text
                if (offset >= originalStr.length) {
                    return formattedText.length
                }
                val subString = originalStr.substring(0, offset)
                val number = subString.toLongOrNull() ?: 0L
                val formattedSubString = formatter.format(number)
                return formattedSubString.length
            }

            override fun transformedToOriginal(offset: Int): Int {
                // Calculate how many characters to remove (the commas) to get the original position
                val commas = formattedText.substring(0, offset).count { it == ',' }
                return (offset - commas).coerceIn(0, text.text.length)
            }
        }

        return TransformedText(AnnotatedString(formattedText), offsetMapping)
    }
}
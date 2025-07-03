package com.example.upitracker.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.DecimalFormat
import kotlin.math.max

class DecimalInputVisualTransformation : VisualTransformation {

    // Formatter allows for up to two decimal places
    private val formatter = DecimalFormat("#,##,##0.##")

    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        // Allow up to two decimal places
        val parts = originalText.split('.')
        if (parts.size > 1 && parts[1].length > 2) {
            // Do not allow more than 2 decimal places, return the last valid text
            // This is a simple guard, more complex logic could be used
            return TransformedText(text, OffsetMapping.Identity)
        }

        // ✨ START OF THE FIX ✨
        // If the text is "123.", format "123" and add the "." back manually.
        val textToFormat = if (originalText.endsWith(".")) {
            originalText.substring(0, originalText.lastIndex)
        } else {
            originalText
        }

        // Return early if the part to be formatted is empty (e.g., user just typed ".")
        if (textToFormat.isEmpty()) {
            return TransformedText(AnnotatedString("0."), object: OffsetMapping {
                override fun originalToTransformed(offset: Int): Int = 2
                override fun transformedToOriginal(offset: Int): Int = 1
            })
        }

        val number = textToFormat.toDoubleOrNull() ?: return TransformedText(text, OffsetMapping.Identity)
        var formattedText = formatter.format(number)

        if (originalText.endsWith(".") && !formattedText.contains(".")) {
            formattedText += "."
        }
        // ✨ END OF THE FIX ✨

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val originalTextWithDecimal = if (originalText.contains('.')) originalText else "$originalText.0"
                val beforeCursor = originalTextWithDecimal.substring(0, offset)
                val beforeCursorNumber = beforeCursor.toDoubleOrNull() ?: 0.0

                var commas = 0
                if(beforeCursorNumber > 999) {
                    val formattedBeforeCursor = formatter.format(beforeCursorNumber)
                    commas = formattedBeforeCursor.count { it == ',' }
                }

                return (offset + commas).coerceAtMost(formattedText.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val commasBeforeCursor = formattedText.substring(0, offset).count { it == ',' }
                return max(0, offset - commasBeforeCursor)
            }
        }

        return TransformedText(AnnotatedString(formattedText), offsetMapping)
    }
}
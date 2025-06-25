package com.example.upitracker.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.DecimalFormat

class IndianCurrencyVisualTransformation : VisualTransformation {
    // Using Indian Numbering System pattern
    private val formatter = DecimalFormat("##,##,##0")

    override fun filter(text: AnnotatedString): TransformedText {
        // We only want to format the string if it's not empty
        if (text.text.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val originalText = text.text
        val formattedText = formatter.format(originalText.toLongOrNull() ?: 0L)

        // The offset mapping helps keep the cursor in the correct position
        // as the user types and commas are added/removed.
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val commas = formattedText.count { it == ',' }
                return (offset + commas).coerceIn(0, formattedText.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val commas = formattedText.substring(0, offset).count { it == ',' }
                return (offset - commas).coerceIn(0, originalText.length)
            }
        }

        return TransformedText(AnnotatedString(formattedText), offsetMapping)
    }
}
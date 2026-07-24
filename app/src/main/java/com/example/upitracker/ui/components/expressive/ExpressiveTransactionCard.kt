package com.example.upitracker.ui.components.expressive

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.upitracker.ui.components.BankIcon
import com.example.upitracker.util.ExpressiveTokens
import com.example.upitracker.util.creditColor
import com.example.upitracker.util.debitColor

@Composable
fun ExpressiveTransactionCard(
    title: String,
    amount: String,
    type: String,
    dateText: String,
    modifier: Modifier = Modifier,
    category: String? = null,
    bankName: String? = null,
    note: String? = null,
    onClick: () -> Unit
) {
    val isCredit = type.equals("CREDIT", ignoreCase = true)
    val amountColor = if (isCredit) creditColor() else debitColor()
    PulseGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = ExpressiveTokens.corners.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = ExpressiveTokens.compact.cardHorizontal,
                    vertical = ExpressiveTokens.compact.cardVertical
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BankIcon(
                bankName = bankName,
                fallbackLabel = category ?: title,
                fallbackColor = amountColor
            )

            Spacer(modifier = Modifier.width(ExpressiveTokens.compact.itemGap))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.xs)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = buildString {
                        append(dateText)
                        if (!bankName.isNullOrBlank()) {
                            append(" • $bankName")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!note.isNullOrBlank()) {
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!category.isNullOrBlank()) {
                        Text(
                            text = category.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                }
            }

            Spacer(modifier = Modifier.width(ExpressiveTokens.spacing.sm))

            Text(
                text = amount,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = amountColor,
                maxLines = 1
            )
        }
    }
}

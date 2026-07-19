package com.example.upitracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.upitracker.data.Transaction
import com.example.upitracker.util.ExpressiveTokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionCard(
    modifier: Modifier = Modifier,
    transaction: Transaction,
    categoryColor: Color,
    onCategoryClick: (String) -> Unit,
    isSelected: Boolean,
    showCheckbox: Boolean
) {
    // 1. Pre-calculate and cache strings/dates to maximize scroll performance
    val displayDate = remember(transaction.date) {
        SimpleDateFormat("dd MMM yy, hh:mm a", Locale.getDefault()).format(Date(transaction.date))
    }

    val isCredit = remember(transaction.type) { transaction.type.contains("CREDIT", ignoreCase = true) }
    val isDebit = remember(transaction.type) { transaction.type.contains("DEBIT", ignoreCase = true) }

    val creditColor = MaterialTheme.colorScheme.secondary
    val amountColor = when {
        isDebit -> MaterialTheme.colorScheme.error
        isCredit -> creditColor
        else -> MaterialTheme.colorScheme.onSurface
    }

    val signedAmount = remember(transaction.type, transaction.amount) {
        val prefix = when {
            isCredit -> "+"
            isDebit -> "-"
            else -> ""
        }
        "$prefix₹${"%.2f".format(transaction.amount)}"
    }

    val cachedDisplayTitle = remember(transaction.senderOrReceiver, transaction.description) {
        transaction.displayTitle()
    }

    val cachedDescription = remember(transaction.description) {
        transaction.description.trim()
    }

    // Using standard MaterialTheme surface outline colors to avoid missing token references
    val currentBorderColor = MaterialTheme.colorScheme.outlineVariant

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = ExpressiveTokens.corners.large,
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else currentBorderColor
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.88f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // Kills shadow layout recalculations on scroll
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ExpressiveTokens.spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val category = transaction.category
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(ExpressiveTokens.corners.medium)
                    .background(categoryColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category?.trim()?.firstOrNull()?.uppercase() ?: "•",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = categoryColor
                )
            }

            Spacer(modifier = Modifier.width(ExpressiveTokens.spacing.md))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.xs)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.sm),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = cachedDisplayTitle,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = signedAmount,
                        style = MaterialTheme.typography.bodyLarge,
                        color = amountColor,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                if (cachedDescription.isNotBlank() && cachedDescription != cachedDisplayTitle) {
                    Text(
                        text = cachedDescription,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (transaction.linkedTransactionId != null) {
                                Icon(
                                    imageVector = Icons.Filled.Link,
                                    contentDescription = "Linked Transaction",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }

                            Text(
                                text = displayDate,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f)
                            )
                        }

                        if (transaction.senderOrReceiver != "Manual Entry" &&
                            transaction.senderOrReceiver.isNotBlank() &&
                            transaction.senderOrReceiver != cachedDisplayTitle
                        ) {
                            Text(
                                text = transaction.senderOrReceiver,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(ExpressiveTokens.spacing.sm))

                    if (!category.isNullOrBlank()) {
                        Text(
                            text = category,
                            modifier = Modifier
                                .clip(ExpressiveTokens.corners.small)
                                .background(categoryColor.copy(alpha = 0.10f))
                                .clickable { onCategoryClick(category) }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = categoryColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = showCheckbox,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = null,
                    modifier = Modifier.padding(start = ExpressiveTokens.spacing.sm)
                )
            }
        }
    }
}

private fun Transaction.displayTitle(): String {
    return when {
        senderOrReceiver.isDisplayablePartyName() -> senderOrReceiver
        !bankName.isNullOrBlank() && bankName != "Other Bank" -> bankName
        description.isNotBlank() -> description
        else -> "Transaction"
    }
}

private fun String.isDisplayablePartyName(): Boolean {
    val value = trim()
    if (value.isBlank() || value == "Manual Entry") return false
    if (value.length > 60) return false

    val lower = value.lowercase()
    return !(
        lower.contains(" debited") ||
            lower.contains(" credited") ||
            lower.contains(" account") ||
            lower.contains(" balance") ||
            lower.contains(" transaction") ||
            lower.contains(" has been ") ||
            lower.contains(" available ")
    )
}

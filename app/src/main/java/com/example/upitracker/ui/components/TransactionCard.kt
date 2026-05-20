@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.upitracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.example.upitracker.util.CategoryIcon
import com.example.upitracker.util.ExpressiveTokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.platform.LocalLocale

@Composable
fun TransactionCard(
    modifier: Modifier = Modifier,
    transaction: Transaction,
    categoryColor: Color,
    categoryIcon: CategoryIcon,
    onCategoryClick: (String) -> Unit,
    isSelected: Boolean,
    showCheckbox: Boolean
) {
    val displayDate = remember(transaction.date) {
        SimpleDateFormat(
            "dd MMM yy, hh:mm a",
            Locale.getDefault()
        ).format(Date(transaction.date))
    }

    val creditColor = if (isSystemInDarkTheme()) {
        Color(0xFF63DC94)
    } else {
        Color(0xFF006D3D)
    }

    val isCredit = transaction.type.contains("CREDIT", ignoreCase = true)
    val isDebit = transaction.type.contains("DEBIT", ignoreCase = true)

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

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = ExpressiveTokens.corners.large,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) {
                ExpressiveTokens.elevation.cardPressed
            } else {
                ExpressiveTokens.elevation.card
            }
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = ExpressiveTokens.compact.cardHorizontal,
                    top = ExpressiveTokens.compact.cardVertical,
                    end = ExpressiveTokens.compact.cardVertical,
                    bottom = ExpressiveTokens.compact.cardVertical
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TransactionTypeDot(
                isCredit = isCredit,
                isDebit = isDebit,
                amountColor = amountColor
            )

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
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = transaction.displayTitle(),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = transaction.type.uppercase(LocalLocale.current.platformLocale),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = signedAmount,
                        style = MaterialTheme.typography.bodyLarge,
                        color = amountColor,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                }

                Text(
                    text = transaction.description.trim().ifBlank { "No description" },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        if (
                            transaction.senderOrReceiver != "Manual Entry" &&
                            transaction.senderOrReceiver.isNotBlank()
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

                    AnimatedVisibility(
                        visible = !transaction.category.isNullOrBlank()
                    ) {
                        AssistChip(
                            onClick = {
                                transaction.category?.let(onCategoryClick)
                            },
                            label = {
                                Text(
                                    text = transaction.category.orEmpty(),
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingIcon = {
                                CategoryIconView(
                                    categoryIcon = categoryIcon,
                                    size = ExpressiveTokens.compact.iconSmall,
                                    containerColor = categoryColor,
                                    contentColor = Color.White,
                                    iconTint = Color.White
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = categoryColor.copy(alpha = 0.15f),
                                labelColor = categoryColor,
                                leadingIconContentColor = categoryColor
                            ),
                            border = null
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

@Composable
private fun TransactionTypeDot(
    isCredit: Boolean,
    isDebit: Boolean,
    amountColor: Color
) {
    val label = when {
        isCredit -> "Cr"
        isDebit -> "Dr"
        else -> "Tx"
    }

    Box(
        modifier = Modifier
            .size(ExpressiveTokens.compact.avatar)
            .clip(CircleShape)
            .background(amountColor.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = amountColor
        )
    }
}

private fun Transaction.displayTitle(): String {
    return when {
        senderOrReceiver.isNotBlank() && senderOrReceiver != "Manual Entry" -> {
            senderOrReceiver
        }

        description.isNotBlank() -> {
            description
        }

        else -> {
            "Transaction"
        }
    }
}
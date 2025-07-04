@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.example.upitracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.* // Material 3 components
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.upitracker.data.Transaction
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import com.example.upitracker.util.CategoryIcon

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TransactionCard(
    modifier: Modifier = Modifier,
    transaction: Transaction,
    categoryColor: Color,
    categoryIcon: CategoryIcon,
    onCategoryClick: (String) -> Unit
) {
    val displayDate = remember(transaction.date) {
        try {
            SimpleDateFormat("dd MMM yy, hh:mm a", Locale.getDefault())
                .format(Date(transaction.date))
        } catch (_: ParseException) { "Invalid Date" }
        catch (_: IllegalArgumentException) { "Invalid Date" }
    }
    val creditColor = if (isSystemInDarkTheme()) Color(0xFF63DC94) else Color(0xFF006D3D)

    val amountColor = when {
        transaction.type.contains("DEBIT", ignoreCase = true) ||
                transaction.type.contains("SENT", ignoreCase = true) ->
            MaterialTheme.colorScheme.error
        transaction.type.contains("CREDIT", ignoreCase = true) ||
                transaction.type.contains("RECVD", ignoreCase = true) ||
                transaction.type.contains("RECEIVED", ignoreCase = true) ->
            creditColor // <-- USE OUR NEW GREEN COLOR HERE
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp // Removed animation based on dismissState
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp) // Removed animation based on dismissState
        )
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(), // Content within card fills its width
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = transaction.type.uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "₹${
                        try {
                            "%.2f".format(transaction.amount)
                        } catch (_: Exception) {
                            "0.00"
                        }
                    }", style = MaterialTheme.typography.titleMedium, color = amountColor
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = transaction.description.trim(),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AnimatedVisibility(visible = transaction.category != null) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    AssistChip(
                        onClick = { onCategoryClick(transaction.category!!) },
                        label = { Text(transaction.category ?: "", fontWeight = FontWeight.SemiBold) },
                        leadingIcon = {
                            CategoryIconView(categoryIcon = categoryIcon, categoryColor = categoryColor)
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

            AnimatedVisibility(
                visible = transaction.senderOrReceiver.isNotBlank(),
                enter = fadeIn(animationSpec = tween(150)),
                exit = fadeOut(animationSpec = tween(150))
            ) {
                Column {
                    Spacer(Modifier.height(if (transaction.category == null) 8.dp else 4.dp))
                    Text(
                        text = transaction.senderOrReceiver.trim(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            // Adjust spacer if both category and sender/receiver are not visible
            if (transaction.category == null && transaction.senderOrReceiver.isBlank()) {
                Spacer(Modifier.height(4.dp)) // Or some other default spacing if needed
            }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Only show the link icon if the transaction is linked
                if (transaction.linkedTransactionId != null) {
                    Icon(
                        imageVector = Icons.Filled.Link,
                        contentDescription = "Linked Transaction",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    text = displayDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
@Composable
private fun CategoryIconView(categoryIcon: CategoryIcon, categoryColor: Color) {
    when (categoryIcon) {
        is CategoryIcon.VectorIcon -> { // ✨ CHANGE: from ResourceIcon to VectorIcon
            Icon(
                imageVector = categoryIcon.image, // ✨ CHANGE: use .image instead of painterResource
                contentDescription = null, // Description is handled by the chip's label
                modifier = Modifier.size(18.dp)
            )
        }
        is CategoryIcon.LetterIcon -> {
            // This part remains the same
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .background(color = categoryColor, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = categoryIcon.letter.toString(),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
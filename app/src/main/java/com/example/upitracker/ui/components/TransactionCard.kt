@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.upitracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.upitracker.data.Transaction
import com.example.upitracker.util.CategoryIcon
import java.text.SimpleDateFormat
import java.util.*

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
    val displayDate = remember(transaction.date) { SimpleDateFormat("dd MMM yy, hh:mm a", Locale.getDefault()).format(Date(transaction.date)) }
    val creditColor = if (isSystemInDarkTheme()) Color(0xFF63DC94) else Color(0xFF006D3D)
    val amountColor = when {
        transaction.type.contains("DEBIT", ignoreCase = true) -> MaterialTheme.colorScheme.error
        transaction.type.contains("CREDIT", ignoreCase = true) -> creditColor
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
            else MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        )
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = transaction.type.uppercase(Locale.getDefault()), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(text = "₹${"%.2f".format(transaction.amount)}", style = MaterialTheme.typography.titleMedium, color = amountColor)
                }
                Spacer(Modifier.height(6.dp))
                Text(text = transaction.description.trim(), style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)

                AnimatedVisibility(visible = transaction.category != null) {
                    Column {
                        Spacer(Modifier.height(8.dp))
                        AssistChip(
                            onClick = { onCategoryClick(transaction.category!!) },
                            label = { Text(transaction.category ?: "", fontWeight = FontWeight.SemiBold) },
                            leadingIcon = { CategoryIconView(categoryIcon = categoryIcon, categoryColor = categoryColor) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = categoryColor.copy(alpha = 0.15f), labelColor = categoryColor, leadingIconContentColor = categoryColor),
                            border = null
                        )
                    }
                }

                if (transaction.senderOrReceiver != "Manual Entry" && transaction.senderOrReceiver.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = transaction.senderOrReceiver,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (transaction.linkedTransactionId != null) {
                        Icon(imageVector = Icons.Filled.Link, contentDescription = "Linked Transaction", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(text = displayDate, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }

            AnimatedVisibility(visible = showCheckbox, enter = fadeIn(), exit = fadeOut()) {
                Checkbox(checked = isSelected, onCheckedChange = null, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun CategoryIconView(categoryIcon: CategoryIcon, categoryColor: Color) {
    when (categoryIcon) {
        is CategoryIcon.VectorIcon -> Icon(imageVector = categoryIcon.image, contentDescription = null, modifier = Modifier.size(18.dp))
        is CategoryIcon.LetterIcon -> Box(modifier = Modifier.size(18.dp).background(color = categoryColor, shape = CircleShape), contentAlignment = Alignment.Center) {
            Text(text = categoryIcon.letter.toString(), color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}
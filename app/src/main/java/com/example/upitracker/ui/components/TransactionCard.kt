@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.example.upitracker.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.* // Material 3 components
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.upitracker.R
import com.example.upitracker.data.Transaction
import java.text.ParseException
import androidx.compose.material.icons.filled.RestoreFromTrash
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TransactionCard(
    modifier: Modifier = Modifier,
    transaction: Transaction,
    onClick: (Transaction) -> Unit,
    onLongClick: (Transaction) -> Unit,
    onArchiveSwipeAction: (Transaction) -> Unit,
    onDeleteSwipeAction: (Transaction) -> Unit,
    swipeActionsEnabled: Boolean = true
) {
    val displayDate = remember(transaction.date) {
        try {
            SimpleDateFormat("dd MMM yy, hh:mm a", Locale.getDefault())
                .format(Date(transaction.date))
        } catch (_: ParseException) { "Invalid Date" }
        catch (_: IllegalArgumentException) { "Invalid Date" }
    }

    val amountColor = when {
        transaction.type.contains("DEBIT", ignoreCase = true) ||
                transaction.type.contains("SENT", ignoreCase = true) ->
            MaterialTheme.colorScheme.error
        transaction.type.contains("CREDIT", ignoreCase = true) ||
                transaction.type.contains("RECVD", ignoreCase = true) ||
                transaction.type.contains("RECEIVED", ignoreCase = true) ->
            MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    // ✨ Use Material 3's rememberSwipeToDismissBoxState ✨
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { newDismissValue ->
            when (newDismissValue) {
                SwipeToDismissBoxValue.EndToStart -> { // Swiped Left (Delete)
                    onDeleteSwipeAction(transaction)
                    false // Confirm the dismiss if action is taken
                }
                SwipeToDismissBoxValue.StartToEnd -> { // Swiped Right (Archive)
                    onArchiveSwipeAction(transaction)
                    true // Confirm the dismiss if action is taken
                }
                SwipeToDismissBoxValue.Settled -> false // Do not dismiss if settled back
            }
        },
                positionalThreshold = { totalDistance -> totalDistance * 0.50f }
    )

    SwipeToDismissBox( // ✨ Material 3 SwipeToDismissBox ✨
        state = dismissState,
        modifier = modifier, // Apply fillMaxWidth here
        enableDismissFromStartToEnd = swipeActionsEnabled, // Swipe Right (Archive)
        enableDismissFromEndToStart = swipeActionsEnabled, // Swipe Left (Delete)
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val targetValue = dismissState.targetValue // Use targetValue for more stable color during swipe

            val backgroundColor by animateColorAsState(
                targetValue = when (targetValue) { // Check targetValue
                    SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.secondaryContainer
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                    else -> Color.Transparent // Or a subtle background
                },
                label = "swipe_background_color"
            )

            val (icon, iconTint, text) = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> { // Swiped Right
                    if (transaction.isArchived) {
                        // This is an archived item, so the action is RESTORE
                        Triple(
                            Icons.Filled.RestoreFromTrash,
                            MaterialTheme.colorScheme.onSecondaryContainer,
                            stringResource(R.string.swipe_action_restore) // We will add this string
                        )
                    } else {
                        // This is a normal item, so the action is ARCHIVE
                        Triple(
                            Icons.Filled.Archive,
                            MaterialTheme.colorScheme.onSecondaryContainer,
                            stringResource(R.string.swipe_action_archive)
                        )
                    }
                }
                SwipeToDismissBoxValue.EndToStart -> { // Swiped Left (Delete)
                    Triple(
                        Icons.Filled.Delete,
                        MaterialTheme.colorScheme.onErrorContainer,
                        stringResource(R.string.swipe_action_delete)
                    )
                }
                else -> Triple(null, Color.Transparent, "") // Should not happen when swiping
            }

            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.Center // Should not happen
            }

            val iconScale by animateDpAsState(
                if (targetValue == SwipeToDismissBoxValue.Settled) 0.75.dp else 1.dp, // Use targetValue
                label = "swipe_icon_scale"
            )

            Box(
                Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                if (icon != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = icon,
                            contentDescription = text,
                            modifier = Modifier.scale(iconScale.value),
                            tint = iconTint
                        )
                        Text(text = text, color = iconTint, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    ) { // This is the content that will be swiped
        Card(
            modifier = Modifier // The SwipeToDismissBox now handles fillMaxWidth
                .combinedClickable(
                    onClick = { onClick(transaction) },
                    onLongClick = { onLongClick(transaction) }
                ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = animateDpAsState(
                    if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) 1.dp else 4.dp,  // Use targetValue
                    label = "card_elevation"
                ).value
            ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                    animateDpAsState(
                        if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) 1.dp else 4.dp, // Use targetValue
                        label = "card_surface_elevation"
                    ).value
                )
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
                    Text(text = transaction.type.uppercase(Locale.getDefault()), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(text = "₹${try { "%.2f".format(transaction.amount) } catch (_: Exception) { "0.00" }}", style = MaterialTheme.typography.titleMedium, color = amountColor)
                }
                Spacer(Modifier.height(6.dp))
                Text(text = transaction.description.trim(), style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                transaction.category?.let { categoryValue ->
                    Spacer(Modifier.height(4.dp))
                    Text(text = stringResource(R.string.transaction_card_category_label, categoryValue), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (transaction.senderOrReceiver.isNotBlank()) {
                    Spacer(Modifier.height(if (transaction.category == null) 8.dp else 4.dp))
                    Text(text = transaction.senderOrReceiver.trim(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, maxLines = 1, overflow = TextOverflow.Ellipsis)
                } else if (transaction.category != null) {
                    Spacer(Modifier.height(4.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text(text = displayDate, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}
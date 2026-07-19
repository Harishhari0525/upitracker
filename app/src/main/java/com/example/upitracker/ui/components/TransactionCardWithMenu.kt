package com.example.upitracker.ui.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.example.upitracker.data.Transaction

@Composable
fun TransactionCardWithMenu(
    modifier: Modifier = Modifier,
    transaction: Transaction,
    onArchiveAction: (Transaction) -> Unit,
    onDelete: (Transaction) -> Unit,
    onToggleSelection: () -> Unit,
    onShowDetails: () -> Unit,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    showCheckbox: Boolean,
    archiveActionText: String,
    archiveActionIcon: ImageVector,
    categoryColor: Color,
    onCategoryClick: (String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var cardWidth by remember { mutableIntStateOf(0) }

    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged {
                cardWidth = it.width
            }
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) onToggleSelection() else onShowDetails()
                },
                onLongClick = {
                    if (!isSelectionMode) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        showMenu = true
                    }
                }
            )
    ) {
        TransactionCard(
            transaction = transaction,
            categoryColor = categoryColor,
            onCategoryClick = onCategoryClick,
            isSelected = isSelected,
            showCheckbox = showCheckbox
        )

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            offset = DpOffset(
                x = with(density) { cardWidth.toDp() } - 128.dp,
                y = (-48).dp
            )
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = archiveActionText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onArchiveAction(transaction)
                    showMenu = false
                },
                leadingIcon = {
                    Icon(
                        imageVector = archiveActionIcon,
                        contentDescription = archiveActionText
                    )
                }
            )

            DropdownMenuItem(
                text = {
                    Text(
                        text = "Delete",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                },
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDelete(transaction)
                    showMenu = false
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            )
        }
    }
}

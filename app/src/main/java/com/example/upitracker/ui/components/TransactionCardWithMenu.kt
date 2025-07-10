package com.example.upitracker.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.example.upitracker.data.Transaction
import com.example.upitracker.util.CategoryIcon

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
    categoryIcon: CategoryIcon,
    onCategoryClick: (String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var cardWidth by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current


    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged {
                cardWidth = it.width
            }
            .pointerInput(isSelectionMode) { // Keyed to isSelectionMode to re-evaluate gestures
                detectTapGestures(
                    onLongPress = {
                        // Don't show context menu if in selection mode
                        if (!isSelectionMode) {
                            showMenu = true
                        }
                    },
                    onTap = {
                        // The logic is now inside the component
                        if (isSelectionMode) {
                            onToggleSelection()
                        } else {
                            onShowDetails()
                        }
                    }
                )
            }
    ) {
        TransactionCard(
            transaction = transaction,
            categoryColor = categoryColor,
            categoryIcon = categoryIcon,
            onCategoryClick = onCategoryClick,
            isSelected = isSelected,
            showCheckbox = showCheckbox
        )

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            offset = DpOffset(x = with(density) { cardWidth.toDp() } - 127.dp, y = (-50).dp)
        ) {
            DropdownMenuItem(
                text = { Text(archiveActionText) },
                onClick = {
                    onArchiveAction(transaction)
                    showMenu = false
                },
                leadingIcon = { Icon(archiveActionIcon, contentDescription = archiveActionText) }
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    onDelete(transaction)
                    showMenu = false
                },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "Delete") }
            )
        }
    }
}
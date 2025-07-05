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
    onClick: (Transaction) -> Unit,
    onArchiveAction: (Transaction) -> Unit,
    onDelete: (Transaction) -> Unit,
    archiveActionText: String,
    archiveActionIcon: ImageVector,
    categoryColor: Color,
    categoryIcon: CategoryIcon,
    onCategoryClick: (String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    // ✨ 1. State to store the width of the card in pixels ✨
    var cardWidth by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current


    Box(
        modifier = modifier
            .fillMaxWidth()
            // ✨ 2. Measure the size of the Box and save the width ✨
            .onSizeChanged {
                cardWidth = it.width
            }
            .pointerInput(true) {
                detectTapGestures(
                    onLongPress = {
                        showMenu = true
                    },
                    onTap = { onClick(transaction) }
                )
            }
    ) {
        TransactionCard(
            transaction = transaction,
            categoryColor = categoryColor,
            categoryIcon = categoryIcon,
            onCategoryClick = onCategoryClick
        )

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            // ✨ 3. Use the measured width to create a horizontal offset ✨
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
            // The Delete item remains the same
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
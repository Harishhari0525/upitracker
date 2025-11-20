package com.example.upitracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.upitracker.R
import com.example.upitracker.util.CategoryIcon
import com.example.upitracker.viewmodel.BudgetStatus
import java.text.NumberFormat
import java.util.*

@Composable
fun BudgetCard(
    status: BudgetStatus,
    categoryIcon: CategoryIcon,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("en").setRegion("IN").build()) }
    var showMenu by remember { mutableStateOf(false) }

    val cardColors = when {
        status.progress >= 1.0f -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
        status.progress > 0.85f -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
        else -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        )
    }

    val progressColor = when {
        status.progress >= 1.0f -> MaterialTheme.colorScheme.error
        status.progress > 0.85f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    val rolloverColor = if (status.rolloverAmount >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error

    // ✅ APPLY the new dynamic colors to the Card composable
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = cardColors
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    CategoryIconView(categoryIcon = categoryIcon, size = 24.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(text = status.categoryName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = status.periodType.name.lowercase().replaceFirstChar { it.titlecase() },
                            style = MaterialTheme.typography.labelMedium,
                            // Use the card's content color for the subtitle
                            color = LocalContentColor.current.copy(alpha = 0.7f)
                        )
                    }
                }
                Box {
                    IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Budget options") }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text("Edit") }, onClick = { onEdit(); showMenu = false })
                        DropdownMenuItem(text = { Text("Delete") }, onClick = { onDelete(); showMenu = false })
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${currencyFormatter.format(status.spentAmount)} / ${currencyFormatter.format(status.effectiveBudget)}",
                    style = MaterialTheme.typography.bodyLarge
                )
                if (status.allowRollover && status.rolloverAmount != 0.0) {
                    Spacer(Modifier.width(8.dp))
                    val sign = if (status.rolloverAmount > 0) "+" else ""
                    Text(
                        text = "(${sign}${currencyFormatter.format(status.rolloverAmount)} rollover)",
                        style = MaterialTheme.typography.bodySmall,
                        color = rolloverColor
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { status.progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = progressColor, // Use the new progress color
                trackColor = progressColor.copy(alpha = 0.2f)
            )
            Spacer(Modifier.height(4.dp))
            val remainingText = if (status.remainingAmount >= 0) {
                stringResource(R.string.budget_remaining, currencyFormatter.format(status.remainingAmount))
            } else {
                stringResource(R.string.budget_overspent, currencyFormatter.format(status.remainingAmount * -1))
            }
            Text(
                text = remainingText,
                style = MaterialTheme.typography.bodySmall,
                color = if (status.remainingAmount < 0) MaterialTheme.colorScheme.error else LocalContentColor.current.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

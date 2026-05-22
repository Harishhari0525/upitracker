package com.example.upitracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.upitracker.R
import com.example.upitracker.util.CategoryIcon
import com.example.upitracker.util.ExpressiveTokens
import com.example.upitracker.viewmodel.BudgetStatus
import java.text.NumberFormat
import java.util.Locale

@Composable
fun BudgetCard(
    status: BudgetStatus,
    categoryIcon: CategoryIcon,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(
            Locale.Builder()
                .setLanguage("en")
                .setRegion("IN")
                .build()
        ).apply {
            maximumFractionDigits = 0
        }
    }

    var showMenu by remember { mutableStateOf(false) }

    val cardColors = when {
        status.progress >= 1.0f -> {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        }

        status.progress > 0.85f -> {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }

        else -> {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        }
    }

    val progressColor = when {
        status.progress >= 1.0f -> MaterialTheme.colorScheme.error
        status.progress > 0.85f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    val rolloverColor = if (status.rolloverAmount >= 0) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.error
    }

    val remainingText = if (status.remainingAmount >= 0) {
        stringResource(
            R.string.budget_remaining,
            currencyFormatter.format(status.remainingAmount)
        )
    } else {
        stringResource(
            R.string.budget_overspent,
            currencyFormatter.format(status.remainingAmount * -1)
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ExpressiveTokens.corners.large,
        elevation = CardDefaults.cardElevation(
            defaultElevation = ExpressiveTokens.elevation.card
        ),
        colors = cardColors
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = ExpressiveTokens.compact.cardHorizontal,
                    vertical = ExpressiveTokens.compact.cardVertical
                ),
            verticalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(ExpressiveTokens.compact.avatar),
                    shape = ExpressiveTokens.corners.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CategoryIconView(
                            categoryIcon = categoryIcon,
                            size = ExpressiveTokens.compact.iconMedium,
                            iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.width(ExpressiveTokens.compact.itemGap))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.xxs)
                ) {
                    Text(
                        text = status.categoryName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = LocalContentColor.current
                    )

                    Text(
                        text = status.periodType.name.lowercase().replaceFirstChar { it.titlecase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = LocalContentColor.current.copy(alpha = 0.72f)
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Budget options"
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit"
                                )
                            },
                            onClick = {
                                onEdit()
                                showMenu = false
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Delete") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                onDelete()
                                showMenu = false
                            }
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "${currencyFormatter.format(status.spentAmount)} / ${
                        currencyFormatter.format(status.effectiveBudget)
                    }",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = LocalContentColor.current
                )

                if (status.allowRollover && status.rolloverAmount != 0.0) {
                    Spacer(modifier = Modifier.width(ExpressiveTokens.spacing.sm))

                    val sign = if (status.rolloverAmount > 0) "+" else ""

                    Text(
                        text = "(${sign}${currencyFormatter.format(status.rolloverAmount)} rollover)",
                        style = MaterialTheme.typography.labelSmall,
                        color = rolloverColor
                    )
                }
            }

            LinearProgressIndicator(
                progress = { status.progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = progressColor,
                trackColor = progressColor.copy(alpha = 0.18f)
            )

            Text(
                text = remainingText,
                style = MaterialTheme.typography.bodySmall,
                color = if (status.remainingAmount < 0) {
                    MaterialTheme.colorScheme.error
                } else {
                    LocalContentColor.current.copy(alpha = 0.72f)
                },
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}
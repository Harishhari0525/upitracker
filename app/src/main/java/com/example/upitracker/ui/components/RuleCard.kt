package com.example.upitracker.ui.components

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
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import com.example.upitracker.data.CategorySuggestionRule
import com.example.upitracker.data.RuleLogic
import com.example.upitracker.util.ExpressiveTokens

@Composable
fun RuleCard(
    rule: CategorySuggestionRule,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = ExpressiveTokens.corners.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = ExpressiveTokens.elevation.card
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = ExpressiveTokens.compact.cardHorizontal,
                    top = ExpressiveTokens.compact.cardVertical,
                    end = ExpressiveTokens.spacing.xs,
                    bottom = ExpressiveTokens.compact.cardVertical
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(ExpressiveTokens.compact.avatar),
                shape = ExpressiveTokens.corners.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Rule,
                        contentDescription = null,
                        modifier = Modifier.size(ExpressiveTokens.compact.iconMedium)
                    )
                }
            }

            Spacer(modifier = Modifier.width(ExpressiveTokens.compact.itemGap))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.xs)
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(color = MaterialTheme.colorScheme.secondary)
                        ) {
                            append("IF ")
                        }

                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(rule.fieldToMatch.name.replace("_", " "))
                        }

                        withStyle(
                            style = SpanStyle(color = MaterialTheme.colorScheme.secondary)
                        ) {
                            append(" ${rule.matcher.name.lowercase()} ")
                        }

                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("'${rule.keyword}'")
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                val logicText = if (rule.logic == RuleLogic.ALL) "AND" else "OR"

                Text(
                    text = "Logic: $logicText",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(color = MaterialTheme.colorScheme.primary)
                        ) {
                            append("THEN ")
                        }

                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(rule.categoryName)
                        }
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box {
                IconButton(
                    onClick = { showMenu = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options"
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
    }
}
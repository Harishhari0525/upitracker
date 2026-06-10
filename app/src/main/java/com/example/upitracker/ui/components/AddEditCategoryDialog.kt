package com.example.upitracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.upitracker.data.Category
import com.example.upitracker.util.ExpressiveTokens
import com.example.upitracker.util.availableColors
import com.example.upitracker.util.availableIcons
import com.example.upitracker.util.parseColor

@Composable
fun AddEditCategoryDialog(
    userCategories: List<Category>,
    categoryToEdit: Category?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, iconName: String, colorHex: String) -> Unit
) {
    var name by remember { mutableStateOf(categoryToEdit?.name ?: "") }

    val initialIsEmoji = categoryToEdit != null &&
            !availableIcons.containsKey(categoryToEdit.iconName)

    var isEmojiMode by remember { mutableStateOf(initialIsEmoji) }
    var selectedIconName by remember { mutableStateOf(categoryToEdit?.iconName ?: "MoreHoriz") }
    var emojiInput by remember {
        mutableStateOf(categoryToEdit?.iconName.takeIf { initialIsEmoji } ?: "🍔")
    }

    var selectedColorHex by remember {
        mutableStateOf(categoryToEdit?.colorHex ?: "#607D8B")
    }

    var nameError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = ExpressiveTokens.corners.extraLarge,
        title = {
            Column(
                verticalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.xs)
            ) {
                Text(
                    text = if (categoryToEdit == null) "Add Category" else "Edit Category",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Choose a name, icon, and color for this category.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.md)
            ) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = null
                    },
                    label = { Text("Category name") },
                    isError = nameError != null,
                    supportingText = {
                        if (nameError != null) {
                            Text(nameError!!)
                        }
                    },
                    shape = ExpressiveTokens.corners.medium,
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.sm)
                ) {
                    FilterChip(
                        selected = !isEmojiMode,
                        onClick = { isEmojiMode = false },
                        label = { Text("Icon") },
                        leadingIcon = {
                            if (!isEmojiMode) {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        }
                    )

                    FilterChip(
                        selected = isEmojiMode,
                        onClick = { isEmojiMode = true },
                        label = { Text("Emoji") },
                        leadingIcon = {
                            if (isEmojiMode) {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        }
                    )
                }

                if (!isEmojiMode) {
                    Text(
                        text = "Select icon",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.sm)
                    ) {
                        items(
                            items = availableIcons.entries.toList(),
                            key = { it.key }
                        ) { (iconName, iconVector) ->
                            IconSelector(
                                icon = iconVector,
                                isSelected = iconName == selectedIconName,
                                onClick = { selectedIconName = iconName }
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Type emoji",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedTextField(
                        value = emojiInput,
                        onValueChange = {
                            if (it.length <= 4) {
                                emojiInput = it
                            }
                        },
                        modifier = Modifier.width(88.dp),
                        textStyle = TextStyle(
                            fontSize = 28.sp,
                            textAlign = TextAlign.Center
                        ),
                        shape = ExpressiveTokens.corners.medium,
                        singleLine = true
                    )
                }

                Text(
                    text = "Color",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.sm)
                ) {
                    items(
                        items = availableColors,
                        key = { it }
                    ) { colorHex ->
                        ColorSelector(
                            color = parseColor(colorHex),
                            isSelected = colorHex == selectedColorHex,
                            onClick = { selectedColorHex = colorHex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = "Name cannot be empty"
                    } else if (categoryToEdit == null && userCategories.any { it.name.equals(name.trim(), ignoreCase = true) }) {
                        nameError = "Category already exists"
                    } else {
                        val finalIcon = if (isEmojiMode) {
                            emojiInput.trim().ifEmpty { "🍔" }
                        } else {
                            selectedIconName
                        }

                        onConfirm(name.trim(), finalIcon, selectedColorHex)
                    }
                },
                shape = ExpressiveTokens.corners.medium
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun IconSelector(
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                }
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (isSelected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun ColorSelector(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick)
            .border(
                width = 2.dp,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    Color.Transparent
                },
                shape = CircleShape
            )
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = if (color.luminance() > 0.5f) {
                    Color.Black
                } else {
                    Color.White
                },
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
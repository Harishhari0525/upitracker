package com.example.upitracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items // ✨ Ensure this specific import is used
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.upitracker.data.Category
import com.example.upitracker.util.availableColors // ✨ Import the new centralized lists
import com.example.upitracker.util.availableIcons
import com.example.upitracker.util.parseColor


@Composable
fun AddEditCategoryDialog(
    categoryToEdit: Category?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, iconName: String, colorHex: String) -> Unit
) {
    var name by remember { mutableStateOf(categoryToEdit?.name ?: "") }

    val initialIsEmoji = categoryToEdit != null && !availableIcons.containsKey(categoryToEdit.iconName)
    var isEmojiMode by remember { mutableStateOf(initialIsEmoji) }

    var selectedIconName by remember { mutableStateOf(categoryToEdit?.iconName ?: "MoreHoriz") }
    var emojiInput by remember { mutableStateOf(categoryToEdit?.iconName.takeIf { initialIsEmoji } ?: "🍔") }

    var selectedColorHex by remember { mutableStateOf(categoryToEdit?.colorHex ?: "#607D8B") }
    var nameError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (categoryToEdit == null) "Add Category" else "Edit Category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = null },
                    label = { Text("Category Name") },
                    isError = nameError != null,
                    supportingText = { if (nameError != null) Text(nameError!!) },
                    singleLine = true
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !isEmojiMode,
                        onClick = { isEmojiMode = false },
                        label = { Text("Icon Pack") },
                        leadingIcon = { if (!isEmojiMode) Icon(Icons.Default.Check, null) }
                    )
                    FilterChip(
                        selected = isEmojiMode,
                        onClick = { isEmojiMode = true },
                        label = { Text("Emoji") },
                        leadingIcon = { if (isEmojiMode) Icon(Icons.Default.Check, null) }
                    )
                }

                // ✨ 2. Show either the Icon Grid OR the Emoji Input
                if (!isEmojiMode) {
                    Text("Select Icon", style = MaterialTheme.typography.titleSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(items = availableIcons.entries.toList(), key = { it.key }) { (iconName, iconVector) ->
                            IconSelector(
                                icon = iconVector,
                                isSelected = iconName == selectedIconName,
                                onClick = { selectedIconName = iconName }
                            )
                        }
                    }
                } else {
                    Text("Type an Emoji", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = emojiInput,
                        onValueChange = {
                            // Limit to roughly 2 characters (enough for 1 emoji)
                            if (it.length <= 4) emojiInput = it
                        },
                        modifier = Modifier.width(100.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 32.sp, textAlign = TextAlign.Center),
                        singleLine = true
                    )
                }

                // Color Selector (Unchanged)
                Text("Color", style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(items = availableColors, key = { it }) { colorHex ->
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
            Button(onClick = {
                if (name.isBlank()) {
                    nameError = "Name cannot be empty"
                } else {
                    // ✨ 3. Save the correct value based on mode
                    val finalIcon = if (isEmojiMode) emojiInput.trim().ifEmpty { "🍔" } else selectedIconName
                    onConfirm(name, finalIcon, selectedColorHex)
                }
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun IconSelector(icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ColorSelector(color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick)
            .border(
                width = 2.dp,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                shape = CircleShape
            )
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = if (color.luminance() > 0.5) Color.Black else Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
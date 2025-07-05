package com.example.upitracker.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

// ✨ 1. The list of available icons is now centralized here.
val availableIcons: Map<String, ImageVector> = mapOf(
    "Fastfood" to Icons.Default.Fastfood, "ShoppingBag" to Icons.Default.ShoppingBag,
    "DirectionsCar" to Icons.Default.DirectionsCar, "ReceiptLong" to Icons.AutoMirrored.Filled.ReceiptLong,
    "Theaters" to Icons.Default.Theaters, "LocalGroceryStore" to Icons.Default.LocalGroceryStore,
    "Favorite" to Icons.Default.Favorite, "HomeWork" to Icons.Default.HomeWork,
    "Paid" to Icons.Default.Paid, "CardGiftcard" to Icons.Default.CardGiftcard,
    "School" to Icons.Default.School, "Flight" to Icons.Default.Flight,
    "GasMeter" to Icons.Default.GasMeter, "Pets" to Icons.Default.Pets,
    "Bookmark" to Icons.Default.Bookmark, "MoreHoriz" to Icons.Default.MoreHoriz
)

val availableColors = listOf(
    "#F44336", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5", "#2196F3",
    "#03A9F4", "#00BCD4", "#009688", "#4CAF50", "#8BC34A", "#CDDC39",
    "#FFEB3B", "#FFC107", "#FF9800", "#FF5722", "#795548", "#607D8B"
)

// ✨ 2. The sealed interface is updated to use ImageVector directly.
sealed interface CategoryIcon {
    data class VectorIcon(val image: ImageVector) : CategoryIcon
    data class LetterIcon(val letter: Char) : CategoryIcon
}

/**
 * A robust and efficient way to map category data to its icon representation.
 */
fun getCategoryIcon(category: com.example.upitracker.data.Category?): CategoryIcon {
    if (category == null) {
        // Default to a bookmark icon if category is null
        return CategoryIcon.VectorIcon(availableIcons["Bookmark"] ?: Icons.Default.MoreHoriz)
    }

    // ✨ 3. The logic is now dynamic. It looks up the icon by its stored name.
    val icon = availableIcons[category.iconName]

    return if (icon != null) {
        // If we found a matching icon in our map, use it.
        CategoryIcon.VectorIcon(icon)
    } else {
        // Otherwise, INTELLIGENTLY create a letter icon from the category name.
        CategoryIcon.LetterIcon(category.name.first().uppercaseChar())
    }
}
package com.example.upitracker.util

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.upitracker.R
import com.example.upitracker.data.Category

// This sealed interface represents the two types of icons we can have:
// A real drawable resource, or a procedurally generated letter icon.
sealed interface CategoryIcon {
    data class ResourceIcon(@DrawableRes val id: Int) : CategoryIcon
    data class LetterIcon(val letter: Char) : CategoryIcon
}

/**
 * A robust and efficient way to map category data to its icon representation.
 */
fun getCategoryIcon(category: Category?): CategoryIcon {
    // If there's no category, return the default bookmark icon.
    if (category == null) {
        return CategoryIcon.ResourceIcon(R.drawable.ic_category_bookmark)
    }

    // Convert the iconName to a clean, lowercase key to check against our defaults.
    val key = category.iconName.replace("-", "").replace("_", "").lowercase()

    // This WHEN statement checks for your pre-defined default categories.
    val resourceId = when (key) {
        "fastfood" -> R.drawable.ic_category_fastfood
        "shoppingbag" -> R.drawable.ic_category_shoppingbag
        "directionscar" -> R.drawable.ic_category_directionscar
        "receiptlong" -> R.drawable.ic_category_receiptlong
        "theaters" -> R.drawable.ic_category_theaters
        "localgrocerystore" -> R.drawable.ic_category_localgrocerystore
        "favorite" -> R.drawable.ic_category_favorite
        "homework" -> R.drawable.ic_category_homework
        "morehoriz" -> R.drawable.ic_category_morehoriz
        "bookmark" -> R.drawable.ic_category_bookmark
        else -> null // The icon name is not one of our defaults.
    }

    return if (resourceId != null) {
        // If we found a default icon, use it.
        CategoryIcon.ResourceIcon(resourceId)
    } else {
        // Otherwise, INTELLIGENTLY create a letter icon from the category name.
        CategoryIcon.LetterIcon(category.name.first().uppercaseChar())
    }
}
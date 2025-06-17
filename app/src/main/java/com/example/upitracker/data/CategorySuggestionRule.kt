// In data/CategorySuggestionRule.kt

package com.example.upitracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "category_suggestion_rules")
data class CategorySuggestionRule(
    @PrimaryKey
    val keyword: String, // e.g., "zomato", "swiggy", "uber". Will be stored in lowercase.

    val categoryName: String // The category to apply, e.g., "Food", "Transport"
)
package com.example.upitracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// Enum to define what field the rule should check
enum class RuleField {
    DESCRIPTION,
    SENDER_OR_RECEIVER
}

// Enum to define the type of text matching
enum class RuleMatcher {
    CONTAINS,
    EQUALS,
    STARTS_WITH,
    ENDS_WITH
}

@Entity(tableName = "category_suggestion_rules")
data class CategorySuggestionRule(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val fieldToMatch: RuleField,
    val matcher: RuleMatcher,
    val keyword: String, // The text to look for (e.g., "zomato", "amazon")
    val categoryName: String, // The category to apply (e.g., "Food", "Shopping")
    val priority: Int = 0 // For future use to decide which rule wins if multiple match
)
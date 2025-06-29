package com.example.upitracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    // NEW: Store the name of the Material Icon
    val iconName: String,
    // NEW: Store the color as a hex string (e.g., "#FF5722")
    val colorHex: String
)
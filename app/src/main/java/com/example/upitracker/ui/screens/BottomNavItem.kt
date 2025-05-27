package com.example.upitracker.ui.screens // Or your preferred package

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt // Changed for History
import androidx.compose.material.icons.filled.BarChart // Changed for Graphs
import androidx.compose.material.icons.filled.Home // For Home/Current Month
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.upitracker.R // Assuming string resources are in R

// Sealed class to define each bottom navigation item
sealed class BottomNavItem(
    val route: String,          // Route for navigation
    val labelResId: Int,        // String resource ID for the label
    val icon: ImageVector       // Icon for the item
) {
    object Home : BottomNavItem(
        route = "home_expenses",
        labelResId = R.string.bottom_nav_home, // Create this string in strings.xml ("Home")
        icon = Icons.Filled.Home
    )
    object Graphs : BottomNavItem(
        route = "graphs",
        labelResId = R.string.bottom_nav_graphs, // Create this string ("Graphs")
        icon = Icons.Filled.BarChart
    )
    object History : BottomNavItem(
        route = "history",
        labelResId = R.string.bottom_nav_history, // Create this string ("History")
        icon = Icons.AutoMirrored.Filled.ListAlt
    )
    object AppSettings : BottomNavItem( // Renamed from "Settings" to avoid conflict
        route = "app_settings_main", // Route for settings within bottom nav
        labelResId = R.string.bottom_nav_settings, // Create this string ("Settings")
        icon = Icons.Filled.Settings
    )
}
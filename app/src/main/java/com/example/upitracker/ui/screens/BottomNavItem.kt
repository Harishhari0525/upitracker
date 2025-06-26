package com.example.upitracker.ui.screens // Or your preferred package

import androidx.compose.material.icons.Icons
// New Rounded Icon Imports
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Leaderboard // For Graphs (assuming exists)
import androidx.compose.material.icons.rounded.History // For History (assuming exists)
import androidx.compose.material.icons.rounded.Tune // For AppSettings (assuming exists)
import androidx.compose.material.icons.rounded.AccountBalanceWallet // For Budget (assuming exists)
// Fallback imports (add if primary is not available, or keep if primary ones are added above)
// import androidx.compose.material.icons.rounded.BarChart
// import androidx.compose.material.icons.automirrored.rounded.ListAlt
// import androidx.compose.material.icons.rounded.Settings
// import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.upitracker.R // Assuming string resources are in R

sealed class BottomNavItem(
    val route: String,          // Route for navigation
    val labelResId: Int,        // String resource ID for the label
    val icon: ImageVector       // Icon for the item
) {
    object Home : BottomNavItem(
        route = "home_expenses",
        labelResId = R.string.bottom_nav_home, // Create this string in strings.xml ("Home")
        icon = Icons.Rounded.Home // Changed
    )
    object Graphs : BottomNavItem(
        route = "graphs",
        labelResId = R.string.bottom_nav_graphs, // Create this string ("Graphs")
        icon = Icons.Rounded.Leaderboard // Changed (assuming exists)
    )
    object History : BottomNavItem(
        route = "history",
        labelResId = R.string.bottom_nav_history, // Create this string ("History")
        icon = Icons.Rounded.History // Changed (assuming exists)
    )
    object AppSettings : BottomNavItem( // Renamed from "Settings" to avoid conflict
        route = "app_settings_main", // Route for settings within bottom nav
        labelResId = R.string.bottom_nav_settings, // Create this string ("Settings")
        icon = Icons.Rounded.Tune // Changed (assuming exists)
    )
    object Budget : BottomNavItem(
        route = "budget",
        labelResId = R.string.bottom_nav_budget, // We will add this string
        icon = Icons.Rounded.AccountBalanceWallet // Changed (assuming exists)
    )
}
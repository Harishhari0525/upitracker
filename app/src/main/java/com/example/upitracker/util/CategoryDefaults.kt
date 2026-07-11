package com.example.upitracker.util

data class CategoryDefaults(
    val iconName: String,
    val colorHex: String
)

fun inferCategoryDefaults(categoryName: String): CategoryDefaults {
    val normalized = categoryName.trim().lowercase()
    return when {
        normalized.contains("food") ||
            normalized.contains("dining") ||
            normalized.contains("restaurant") ||
            normalized.contains("swiggy") ||
            normalized.contains("zomato") -> CategoryDefaults("Fast-food", "#FFC107")

        normalized.contains("grocery") ||
            normalized.contains("groceries") ||
            normalized.contains("dmart") ||
            normalized.contains("blinkit") ||
            normalized.contains("zepto") -> CategoryDefaults("LocalGroceryStore", "#FF5722")

        normalized.contains("travel") ||
            normalized.contains("trip") ||
            normalized.contains("flight") ||
            normalized.contains("bus") ||
            normalized.contains("train") ||
            normalized.contains("hotel") -> CategoryDefaults("Flight", "#3F51B5")

        normalized.contains("transport") ||
            normalized.contains("cab") ||
            normalized.contains("fuel") ||
            normalized.contains("petrol") ||
            normalized.contains("metro") -> CategoryDefaults("DirectionsCar", "#2196F3")

        normalized.contains("bill") ||
            normalized.contains("electric") ||
            normalized.contains("recharge") ||
            normalized.contains("internet") ||
            normalized.contains("mobile") -> CategoryDefaults("ReceiptLong", "#9C27B0")

        normalized.contains("shop") ||
            normalized.contains("amazon") ||
            normalized.contains("flipkart") ||
            normalized.contains("myntra") -> CategoryDefaults("ShoppingBag", "#4CAF50")

        normalized.contains("health") ||
            normalized.contains("medical") ||
            normalized.contains("doctor") ||
            normalized.contains("pharmacy") -> CategoryDefaults("Favorite", "#F44336")

        normalized.contains("rent") ||
            normalized.contains("home") ||
            normalized.contains("house") -> CategoryDefaults("HomeWork", "#795548")

        normalized.contains("salary") ||
            normalized.contains("income") -> CategoryDefaults("Payments", "#009688")

        normalized.contains("entertainment") ||
            normalized.contains("movie") ||
            normalized.contains("netflix") ||
            normalized.contains("spotify") -> CategoryDefaults("Theaters", "#E91E63")

        else -> CategoryDefaults("Category", "#607D8B")
    }
}

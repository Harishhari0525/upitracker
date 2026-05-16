@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.upitracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.upitracker.data.*
import com.example.upitracker.ui.components.TransactionCard
import com.example.upitracker.util.getCategoryIcon
import com.example.upitracker.util.parseColor
import com.example.upitracker.viewmodel.DashboardViewModel
import com.example.upitracker.viewmodel.MainViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun InsightGridScreen(
    dashboardViewModel: DashboardViewModel = viewModel(),
    mainViewModel: MainViewModel,
    onNavigateToHistory: () -> Unit
) {
    val state by dashboardViewModel.dashboardState.collectAsState()
    val allCategories by mainViewModel.allCategories.collectAsState()
    val currentMonthTotal by mainViewModel.currentMonthTotalExpenses.collectAsState()

    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalItemSpacing = 12.dp
    ) {
        // 1. STATUS HERO (Full Width)
        item(span = StaggeredGridItemSpan.FullLine) {
            StatusHeroBlock(state.hero, currentMonthTotal)
        }

        // 2. INSIGHT CARDS (Half Width)
        items(state.actions) { action ->
            InsightCard(action)
        }

        // 3. RECURRING / UPCOMING (Full Width)
        // Check if we have ghost data hidden in the Velocity object
        val currentHero = state.hero
        if (currentHero is HeroState.Velocity && currentHero.daysLeft > 0) {
            item(span = StaggeredGridItemSpan.FullLine) {
                UpcomingBillsRow(
                    count = currentHero.daysLeft,   // Now Kotlin knows currentHero is Velocity
                    amount = currentHero.dailyLimit
                )
            }
        }

        // 4. COMPACT HISTORY HEADER
        item(span = StaggeredGridItemSpan.FullLine) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp)
                    .clickable { onNavigateToHistory() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "View All",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 5. TRANSACTIONS
        items(
            items = state.recentActivity,
            // ✨ CRITICAL FIX: Force items to take full width
            span = { StaggeredGridItemSpan.FullLine }
        ) { item ->
            if (item is RecentActivityItem.TransactionItem) {
                val transaction = item.transaction
                val categoryDetails = allCategories.find { c -> c.name.equals(transaction.category, ignoreCase = true) }

                TransactionCard(
                    transaction = transaction,
                    categoryColor = parseColor(categoryDetails?.colorHex ?: "#808080"),
                    categoryIcon = getCategoryIcon(categoryDetails),
                    onCategoryClick = { },
                    isSelected = false,
                    showCheckbox = false
                )
            }
        }
    }
}

@Composable
fun StatusHeroBlock(heroState: HeroState, totalSpent: Double) {
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN")) }

    val (color, remaining, committed) = if (heroState is HeroState.Velocity) {
        Triple(heroState.statusColor, heroState.amountLeft, heroState.dailyLimit)
    } else {
        Triple(MaterialTheme.colorScheme.primary, 0.0, 0.0)
    }

    Card(
        modifier = Modifier.fillMaxWidth().height(150.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background "Wave" decoration
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ShowChart,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(120.dp)
                    .offset(x = 20.dp, y = 20.dp)
                    .graphicsLayer { alpha = 0.1f },
                tint = Color.Black
            )

            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Current Month Spending",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    currencyFormatter.format(totalSpent),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(Modifier.weight(1f))

                // The "Ghost" Warning Logic
                if (remaining < committed && committed > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.2f), CircleShape)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Warning, null, tint = Color(0xFFFFD54F), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Ghost Alert: ₹${currencyFormatter.format(committed)} committed",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFFD54F),
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (remaining > 0) {
                    Text(
                        "₹${currencyFormatter.format(remaining)} left in target",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                } else {
                    Text(
                        "Target Exceeded",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun InsightCard(action: SmartAction) {
    Card(
        modifier = Modifier.fillMaxWidth().height(90.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(action.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Column {
                Text(action.subtitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(action.title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun UpcomingBillsRow(count: Int, amount: Double) {
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN")) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(count.toString(), color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Upcoming Recurring Bills", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("Total: ${currencyFormatter.format(amount)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
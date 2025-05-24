@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.upitracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.upitracker.viewmodel.MainViewModel
import com.example.upitracker.ui.components.TransactionTable
import com.example.upitracker.data.UpiLiteSummary

@Composable
fun HomeScreen(
    onNavigateToDetail: () -> Unit,
    onNavigateToSettings: () -> Unit,
    mainViewModel: MainViewModel
) {
    // Tab state
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("UPI Transactions", "UPI Lite Summaries")

    val transactions by mainViewModel.transactions.collectAsState()
    val upiLiteSummaries by mainViewModel.upiLiteSummaries.collectAsState() // Add this flow in your VM

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("UPI Tracker") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        content = { paddingValues ->
            Column(
                Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                // Tabs
                TabRow(selectedTabIndex = selectedTab) {
                    tabTitles.forEachIndexed { idx, title ->
                        Tab(
                            selected = selectedTab == idx,
                            onClick = { selectedTab = idx },
                            text = { Text(title) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                when (selectedTab) {
                    0 -> TransactionTable(transactions)
                    1 -> UpiLiteSummaryList(upiLiteSummaries)
                }
            }
        }
    )
}

@Composable
fun UpiLiteSummaryList(summaries: List<UpiLiteSummary>) {
    if (summaries.isEmpty()) {
        Text("No UPI Lite summaries found.", style = MaterialTheme.typography.bodyMedium)
    } else {
        Column {
            summaries.forEach { summary ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Transactions: ${summary.transactionCount}")
                        Text("Total: ₹${summary.totalAmount}")
                        Text("Date: ${summary.date}")
                        Text("Bank: ${summary.bank}")
                    }
                }
            }
        }
    }
}

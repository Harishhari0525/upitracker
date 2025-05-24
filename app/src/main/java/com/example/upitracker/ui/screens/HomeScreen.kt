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
import com.example.upitracker.data.Transaction
import com.example.upitracker.data.UpiLiteSummary
import com.example.upitracker.ui.components.TransactionCard
import com.example.upitracker.ui.components.UpiLiteSummaryCard

@Composable
fun HomeScreen(
    onNavigateToDetail: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    mainViewModel: MainViewModel
) {
    val transactions by mainViewModel.transactions.collectAsState()
    val upiLiteSummaries by mainViewModel.upiLiteSummaries.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

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
        }
    ) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .fillMaxSize()
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    text = { Text("UPI Transactions") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                Tab(
                    text = { Text("UPI Lite Summary") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            when (selectedTab) {
                0 -> TransactionTabContent(transactions)
                1 -> UpiLiteSummaryTabContent(upiLiteSummaries)
            }
        }
    }
}

@Composable
fun TransactionTabContent(transactions: List<Transaction>) {
    if (transactions.isEmpty()) {
        Text("No UPI transactions found.", modifier = Modifier.padding(16.dp))
    } else {
        Column(modifier = Modifier.fillMaxWidth()) {
            transactions.forEach { txn ->
                TransactionCard(txn)
            }
        }
    }
}

@Composable
fun UpiLiteSummaryTabContent(summaries: List<UpiLiteSummary>) {
    if (summaries.isEmpty()) {
        Text("No UPI Lite summaries found.", modifier = Modifier.padding(16.dp))
    } else {
        Column(modifier = Modifier.fillMaxWidth()) {
            summaries.forEach { summary ->
                UpiLiteSummaryCard(summary)
            }
        }
    }
}

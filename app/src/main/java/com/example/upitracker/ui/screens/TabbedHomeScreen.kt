@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.upitracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.upitracker.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TabbedHomeScreen(
    mainViewModel: MainViewModel,
    navController: NavController
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("UPI", "UPI Lite")

    val upiTransactions by mainViewModel.transactions.collectAsState()
    val liteSummaries by mainViewModel.upiLiteSummaries.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("UPI Tracker") },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabTitles.forEachIndexed { idx, title ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> UpiTransactionList(upiTransactions.take(100))
                1 -> UpiLiteSummaryList(liteSummaries.take(100))
            }
        }
    }
}

@Composable
fun UpiTransactionList(transactions: List<com.example.upitracker.data.Transaction>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(transactions) { txn ->
            TransactionCard(txn)
        }
    }
}

@Composable
fun UpiLiteSummaryList(summaries: List<com.example.upitracker.data.UpiLiteSummary>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(summaries) { summary ->
            UpiLiteSummaryCard(summary)
        }
    }
}

@Composable
fun TransactionCard(txn: com.example.upitracker.data.Transaction) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
    ) {
        Column(Modifier.padding(8.dp)) {
            Text("Date: ${SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(txn.date))}")
            Text("Type: ${txn.type}")
            Text("Amount: ₹${txn.amount}")
            Text("Desc: ${txn.description}")
        }
    }
}

@Composable
fun UpiLiteSummaryCard(summary: com.example.upitracker.data.UpiLiteSummary) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
    ) {
        Column(Modifier.padding(8.dp)) {
            Text("Date: ${summary.date}")
            Text("Bank: ${summary.bank}")
            Text("Transactions: ${summary.transactionCount}")
            Text("Total: ₹${summary.totalAmount}")
        }
    }
}

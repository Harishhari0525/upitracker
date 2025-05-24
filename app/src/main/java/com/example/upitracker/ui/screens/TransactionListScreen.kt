@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.upitracker.ui.screens

import TransactionViewModel
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.upitracker.data.Transaction

@Composable
fun TransactionListScreen(viewModel: TransactionViewModel) {
    val transactions by viewModel.transactions.collectAsState() // Live update

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("UPI Transactions") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            items(transactions) { txn ->
                TransactionListItem(txn)
            }
        }
    }
}

@Composable
fun TransactionListItem(transaction: Transaction) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${transaction.type.uppercase()}: ₹${transaction.amount}",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(4.dp))
            Text("From/To: ${transaction.senderOrReceiver}")
            Spacer(Modifier.height(4.dp))
            Text("Description: ${transaction.description}")
            Spacer(Modifier.height(4.dp))
            Text("Date: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(java.util.Date(transaction.date))}")
        }
    }
}

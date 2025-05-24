package com.example.upitracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.upitracker.data.UpiLiteSummary

@Composable
fun UpiLiteSummaryCard(summary: UpiLiteSummary) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Date: ${summary.date}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Transactions: ${summary.transactionCount}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Total Amount: ₹${summary.totalAmount}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Bank: ${summary.bank}", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

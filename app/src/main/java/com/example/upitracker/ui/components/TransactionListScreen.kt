package com.example.upitracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.upitracker.data.Transaction
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionListItem(transaction: Transaction) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Date: ${SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(transaction.date))}", style = MaterialTheme.typography.bodyLarge)
            Text("Type: ${transaction.type}", style = MaterialTheme.typography.bodyLarge)
            Text("Amount: ₹${transaction.amount}", style = MaterialTheme.typography.bodyLarge)
            Text("Desc: ${transaction.description}", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

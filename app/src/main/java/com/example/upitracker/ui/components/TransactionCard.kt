package com.example.upitracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.upitracker.data.Transaction
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionCard(transaction: Transaction) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Date: ${dateFormat.format(Date(transaction.date))}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Type: ${transaction.type.uppercase()}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Amount: ₹${transaction.amount}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Desc: ${transaction.description}", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
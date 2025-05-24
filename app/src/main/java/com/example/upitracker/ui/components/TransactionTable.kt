package com.example.upitracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.upitracker.data.Transaction
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionTable(transactions: List<Transaction>) {
    LazyColumn {
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Date", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Text("Type", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Text("Amount", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Text("Desc", Modifier.weight(2f), fontWeight = FontWeight.Bold)
            }
        }
        items(transactions.size) { idx ->
            val txn = transactions[idx]
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(txn.date)), Modifier.weight(1f))
                Text(txn.type, Modifier.weight(1f))
                Text("₹${txn.amount}", Modifier.weight(1f))
                Text(txn.description, Modifier.weight(2f))
            }
            Divider()
        }
    }
}

package com.example.upitracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.upitracker.viewmodel.MerchantDna
import java.text.NumberFormat
import java.util.Locale

@Composable
fun MerchantDnaCard(
    merchantName: String,
    dna: MerchantDna
) {
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    // Premium Dark Blue/Purple Gradient for "Data" feel
    val brush = Brush.horizontalGradient(
        colors = listOf(Color(0xFF263238), Color(0xFF37474F))
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(modifier = Modifier.background(brush).padding(20.dp)) {
            Column {
                Text(
                    text = "Insights for $merchantName",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DnaStat(
                        label = "Total Spent",
                        value = currencyFormatter.format(dna.totalSpent),
                        icon = Icons.AutoMirrored.Filled.ShowChart
                    )
                    DnaStat(
                        label = "Visits",
                        value = "${dna.transactionCount}",
                        icon = Icons.Default.History
                    )
                    DnaStat(
                        label = "Peak Day",
                        value = dna.favoriteDay.take(3), // "Fri"
                        icon = Icons.Default.CalendarToday
                    )
                }
            }
        }
    }
}

@Composable
private fun DnaStat(label: String, value: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF80DEEA), // Cyan tint
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}
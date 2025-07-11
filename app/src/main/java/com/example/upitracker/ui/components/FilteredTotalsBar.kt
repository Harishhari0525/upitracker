package com.example.upitracker.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.*

@Composable
fun FilteredTotalsBar(
    modifier: Modifier = Modifier,
    totalDebit: Double,
    totalCredit: Double
) {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("en").setRegion("IN").build())
    val creditColor = if (isSystemInDarkTheme()) Color(0xFF63DC94) else Color(0xFF006D3D)
    val debitColor = MaterialTheme.colorScheme.error

    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Debit Total
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "DEBIT",
                    style = MaterialTheme.typography.labelMedium,
                    color = debitColor
                )
                Text(
                    text = currencyFormatter.format(totalDebit),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = debitColor
                )
            }

            // Credit Total
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "CREDIT",
                    style = MaterialTheme.typography.labelMedium,
                    color = creditColor
                )
                Text(
                    text = currencyFormatter.format(totalCredit),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = creditColor
                )
            }
        }
    }
}
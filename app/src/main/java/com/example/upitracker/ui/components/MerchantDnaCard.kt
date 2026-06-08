package com.example.upitracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.example.upitracker.util.ExpressiveTokens
import com.example.upitracker.viewmodel.MerchantDna
import java.text.NumberFormat
import java.util.Locale

import androidx.compose.material.icons.filled.StarRate

@Composable
fun MerchantDnaCard(
    merchantName: String,
    dna: MerchantDna
) {
    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(
            Locale.Builder()
                .setLanguage("en")
                .setRegion("IN")
                .build()
        ).apply {
            maximumFractionDigits = 0
        }
    }

    val brush = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF263238),
            Color(0xFF37474F)
        )
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ExpressiveTokens.corners.large,
        elevation = CardDefaults.cardElevation(
            defaultElevation = ExpressiveTokens.elevation.card
        )
    ) {
        Box(
            modifier = Modifier
                .background(brush)
                .padding(
                    horizontal = ExpressiveTokens.compact.cardHorizontal,
                    vertical = ExpressiveTokens.compact.cardVertical
                )
        ) {
            Column {
                Text(
                    text = "Insights for $merchantName",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.72f)
                )

                Spacer(modifier = Modifier.height(ExpressiveTokens.spacing.sm))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DnaStat(
                        label = "Total",
                        value = currencyFormatter.format(dna.totalSpent),
                        icon = Icons.AutoMirrored.Filled.ShowChart
                    )

                    DnaStat(
                        label = "Visits",
                        value = "${dna.transactionCount}",
                        icon = Icons.Default.History
                    )
                    
                    // ✨ Replaced Peak day with Loyalty Share
                    DnaStat(
                        label = "Loyalty",
                        value = "${(dna.loyaltyShare * 100).toInt()}%",
                        icon = Icons.Default.StarRate
                    )
                }
            }
        }
    }
}

@Composable
private fun DnaStat(
    label: String,
    value: String,
    icon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.xxs)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF80DEEA),
            modifier = Modifier.size(ExpressiveTokens.compact.iconSmall)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.62f)
        )
    }
}
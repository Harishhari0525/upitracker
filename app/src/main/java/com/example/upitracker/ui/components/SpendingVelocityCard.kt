package com.example.upitracker.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.upitracker.ui.components.expressive.PulseGlassCard
import com.example.upitracker.util.CurrencyUtils
import com.example.upitracker.util.ExpressiveTokens
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun SpendingVelocityCard(
    modifier: Modifier = Modifier,
    totalBudget: Double,
    totalSpent: Double,
    daysRemaining: Int,
    upcomingBillsTotal: Double = 0.0,
    currentDayOfMonth: Int = 1,
    totalDaysInMonth: Int = 30
) {
    val currencyFormatter = remember { CurrencyUtils.getRupeeFormatter() }

    val totalRemaining = totalBudget - totalSpent
    val discretionaryRemaining = (totalRemaining - upcomingBillsTotal).coerceAtLeast(0.0)
    
    val safeDailySpend = if (daysRemaining > 0) {
        discretionaryRemaining / daysRemaining
    } else {
        0.0
    }

    val actualDailyPace = if (currentDayOfMonth > 0) {
        totalSpent / currentDayOfMonth
    } else {
        0.0
    }

    val targetDailyPace = if (totalDaysInMonth > 0) totalBudget / totalDaysInMonth else 0.0
    val paceRatio = if (targetDailyPace > 0) actualDailyPace / targetDailyPace else 1.0

    val (statusColor, statusText, statusIcon) = when {
        totalRemaining < 0 -> Triple(Color(0xFFFB7185), "Over Budget",
            Icons.AutoMirrored.Filled.TrendingUp
        )
        paceRatio > 1.15 -> Triple(Color(0xFFFBBF24), "Elevated Pace",
            Icons.AutoMirrored.Filled.TrendingUp
        )
        else -> Triple(Color(0xFF34D399), "Safe Pace", Icons.AutoMirrored.Filled.TrendingDown)
    }

    val projectedMonthSpent = actualDailyPace * totalDaysInMonth
    val estimatedRunoutDay = if (actualDailyPace > 0 && totalRemaining > 0 && actualDailyPace > safeDailySpend) {
        (currentDayOfMonth + (discretionaryRemaining / actualDailyPace)).roundToInt().coerceIn(1, totalDaysInMonth)
    } else {
        null
    }

    PulseGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = ExpressiveTokens.corners.extraLarge
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Title & Velocity Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Velocity Meter",
                            tint = statusColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "Daily Safe Allowance",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$daysRemaining days left in cycle",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = statusColor
                        )
                    }
                }
            }

            // Hero Metric: Safe Daily Allowance Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = if (totalRemaining >= 0) currencyFormatter.format(safeDailySpend) else "₹0",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (totalRemaining < 0) Color(0xFFFB7185) else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Safe daily spend",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = currencyFormatter.format(actualDailyPace),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                    Text(
                        text = "Current daily pace",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Burn Rate Progress Bar
            val budgetProgress = if (totalBudget > 0) (totalSpent / totalBudget).toFloat().coerceIn(0f, 1f) else 0f
            LinearProgressIndicator(
                progress = { budgetProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)
            )

            // Insights Footer Details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.50f))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (upcomingBillsTotal > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.EventRepeat,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Reserved for upcoming bills",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = currencyFormatter.format(upcomingBillsTotal),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (estimatedRunoutDay != null) {
                            "At this pace, budget depletes around Day $estimatedRunoutDay"
                        } else if (totalRemaining < 0) {
                            "Overspent by ${currencyFormatter.format(abs(totalRemaining))}"
                        } else {
                            "Projected month end: ${currencyFormatter.format(projectedMonthSpent)}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (totalRemaining < 0 || estimatedRunoutDay != null) statusColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
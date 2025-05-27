package com.example.upitracker.ui.screens

import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.upitracker.R // ✨ Ensure R class is imported
import com.example.upitracker.viewmodel.MainViewModel
import com.example.upitracker.viewmodel.MonthlyExpense
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

// Helper extension functions
fun TextUnit.toPx(density: androidx.compose.ui.unit.Density): Float = with(density) { this@toPx.toPx() }
fun Dp.toPx(density: androidx.compose.ui.unit.Density): Float = with(density) { this@toPx.toPx() }


@Composable
fun GraphsScreen(
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val lastNMonthsExpenses by mainViewModel.lastNMonthsExpenses.collectAsState()
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.bottom_nav_graphs), // Already using string resource
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            stringResource(R.string.graph_monthly_expenses_subtitle, lastNMonthsExpenses.size), // ✨ Updated
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (lastNMonthsExpenses.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.graph_no_data), // ✨ Updated
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            SimpleMonthlyExpenseBarChart(
                monthlyExpenses = lastNMonthsExpenses,
                currencyFormatter = currencyFormatter,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Allow chart to take available space
                    .padding(bottom = 16.dp)
            )
            // TODO: Potentially add selectors for date range or graph type here
        }
    }
}

@Composable
fun SimpleMonthlyExpenseBarChart(
    monthlyExpenses: List<MonthlyExpense>,
    currencyFormatter: NumberFormat,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    axisColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    if (monthlyExpenses.isEmpty()) {
        Text(
            stringResource(R.string.graph_bar_chart_no_data_period), // ✨ Updated
            modifier = modifier.padding(16.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    val density = LocalDensity.current
    val actualMaxAmount = remember(monthlyExpenses) { monthlyExpenses.maxOfOrNull { it.totalAmount } ?: 0.0 }
    val niceMaxAmount = remember(actualMaxAmount) { calculateNiceMax(actualMaxAmount) }

    val themedOnSurfaceColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val themedAxisColor = axisColor.toArgb() // Use the passed axisColor

    val valueLabelPaint = remember(density, themedOnSurfaceColor) { // For labels on top of bars
        Paint().apply {
            color = themedOnSurfaceColor
            textAlign = Paint.Align.CENTER
            textSize = 10.sp.toPx(density) // Smaller for values on bars
            isAntiAlias = true
        }
    }

    val axisLabelPaint = remember(density, themedAxisColor) {
        Paint().apply {
            color = themedAxisColor
            textAlign = Paint.Align.RIGHT // Y-axis labels align right
            textSize = 12.sp.toPx(density)
            isAntiAlias = true
        }
    }
    val monthLabelPaint = remember(density, themedAxisColor) {
        Paint().apply {
            color = themedAxisColor
            textAlign = Paint.Align.CENTER
            textSize = 10.sp.toPx(density)
            isAntiAlias = true
        }
    }

    Canvas(modifier = modifier.padding(top = 16.dp)) {
        val barCount = monthlyExpenses.size
        if (barCount == 0) return@Canvas

        val yAxisLabelHorizontalPadding = 8.dp.toPx(density)
        // Approximate width needed for Y-axis labels
        val yAxisTextWidthApproximation = axisLabelPaint.measureText(currencyFormatter.format(niceMaxAmount).replace("₹", "").trim()) + yAxisLabelHorizontalPadding
        val leftPaddingForYAxis = yAxisTextWidthApproximation + 8.dp.toPx(density)

        val bottomPaddingForXLabels = 30.dp.toPx(density)
        val topPaddingForBarValues = 20.dp.toPx(density)

        val chartWidth = size.width - leftPaddingForYAxis
        val chartHeight = size.height - bottomPaddingForXLabels - topPaddingForBarValues

        if (chartHeight <= 0 || chartWidth <= 0) return@Canvas

        // Draw Y-axis line
        drawLine(
            color = axisColor.copy(alpha = 0.5f),
            start = Offset(leftPaddingForYAxis, topPaddingForBarValues),
            end = Offset(leftPaddingForYAxis, topPaddingForBarValues + chartHeight)
        )
        // Draw X-axis line
        drawLine(
            color = axisColor.copy(alpha = 0.5f),
            start = Offset(leftPaddingForYAxis, topPaddingForBarValues + chartHeight),
            end = Offset(leftPaddingForYAxis + chartWidth, topPaddingForBarValues + chartHeight)
        )

        // Draw Y-axis labels and grid lines
        val yAxisSegments = 5
        for (i in 0..yAxisSegments) {
            val value = niceMaxAmount / yAxisSegments * i
            val yPos = topPaddingForBarValues + chartHeight - (value / niceMaxAmount * chartHeight).toFloat()

            drawLine(
                color = axisColor.copy(alpha = 0.2f),
                start = Offset(leftPaddingForYAxis, yPos),
                end = Offset(size.width, yPos) // Extend grid line to full width
            )
            val textBounds = Rect()
            val labelText = currencyFormatter.format(value).replace("₹", "").trim()
            axisLabelPaint.getTextBounds(labelText, 0, labelText.length, textBounds)
            drawContext.canvas.nativeCanvas.drawText(
                labelText,
                leftPaddingForYAxis - yAxisLabelHorizontalPadding,
                yPos + textBounds.height() / 2f,
                axisLabelPaint
            )
        }

        // Draw Bars and X-axis Labels
        val totalBarWidthAndSpacing = chartWidth / barCount
        val barSpacing = totalBarWidthAndSpacing * 0.25f
        val barWidth = (totalBarWidthAndSpacing - barSpacing).coerceAtLeast(4.dp.toPx(density))

        monthlyExpenses.forEachIndexed { index, expense ->
            val barHeightRatio = if (niceMaxAmount > 0) expense.totalAmount / niceMaxAmount else 0.0
            val barHeight = (barHeightRatio * chartHeight).toFloat().coerceAtLeast(0f)
            val xOffsetInChart = (index * totalBarWidthAndSpacing) + (barSpacing / 2)
            val barLeft = leftPaddingForYAxis + xOffsetInChart
            val barTop = topPaddingForBarValues + chartHeight - barHeight

            drawRect(
                color = barColor,
                topLeft = Offset(x = barLeft, y = barTop),
                size = Size(width = barWidth, height = barHeight)
            )

            val monthTextBounds = Rect()
            monthLabelPaint.getTextBounds(expense.yearMonth, 0, expense.yearMonth.length, monthTextBounds)
            drawContext.canvas.nativeCanvas.drawText(
                expense.yearMonth,
                barLeft + barWidth / 2,
                topPaddingForBarValues + chartHeight + bottomPaddingForXLabels / 2 + monthTextBounds.height() / 2f,
                monthLabelPaint
            )

            if (expense.totalAmount > 0 && barHeight > (valueLabelPaint.textSize + 2.dp.toPx(density))) {
                val valueText = expense.totalAmount.roundToInt().toString()
                drawContext.canvas.nativeCanvas.drawText(
                    valueText,
                    barLeft + barWidth / 2,
                    barTop - 4.dp.toPx(density),
                    valueLabelPaint // Use specific paint for value labels
                )
            }
        }
    }
}

// Helper function to calculate a "nice" maximum value for the Y-axis scale
fun calculateNiceMax(actualMax: Double): Double {
    if (actualMax <= 0) return 100.0

    val exponent = floor(log10(actualMax))
    val fraction = actualMax / 10.0.pow(exponent)

    val niceFraction = when {
        fraction <= 1.0 -> 1.0
        fraction <= 1.5 -> 1.5 // Added more steps for finer control
        fraction <= 2.0 -> 2.0
        fraction <= 3.0 -> 3.0
        fraction <= 4.0 -> 4.0
        fraction <= 5.0 -> 5.0
        fraction <= 7.5 -> 7.5
        else -> 10.0
    }
    return niceFraction * 10.0.pow(exponent)
}
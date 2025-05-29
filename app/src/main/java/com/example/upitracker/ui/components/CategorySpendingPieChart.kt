package com.example.upitracker.ui.components // Or your appropriate package

import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background // For legend color box
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
// import androidx.compose.ui.graphics.drawscope.Stroke // Only if making a donut chart
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.upitracker.R
import com.example.upitracker.viewmodel.CategoryExpense
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// Predefined list of colors for pie chart slices
// You can expand this list or generate colors dynamically if you have many categories
val pieChartColorsDefaults = listOf( // Renamed to avoid conflict if you have another pieChartColors
    Color(0xFFF44336), Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF673AB7),
    Color(0xFF3F51B5), Color(0xFF2196F3), Color(0xFF03A9F4), Color(0xFF00BCD4),
    Color(0xFF009688), Color(0xFF4CAF50), Color(0xFF8BC34A), Color(0xFFCDDC39),
    Color(0xFFFFEB3B), Color(0xFFFFC107), Color(0xFFFF9800), Color(0xFF795548)
)

// Helper extension functions (if not already in a common util file)
fun TextUnit.toPx(density: androidx.compose.ui.unit.Density): Float = with(density) { this@toPx.toPx() }
fun Dp.toPx(density: androidx.compose.ui.unit.Density): Float = with(density) { this@toPx.toPx() }


@Composable
fun CategorySpendingPieChart(
    modifier: Modifier = Modifier,
    categoryExpenses: List<CategoryExpense>,
    // strokeWidth: Dp = 10.dp // For donut chart, not used in full pie
    sliceColors: List<Color> = pieChartColorsDefaults // Allow customizing colors
) {
    if (categoryExpenses.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.graph_pie_chart_no_data),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val totalAmount = remember(categoryExpenses) { categoryExpenses.sumOf { it.totalAmount } }
    val density = LocalDensity.current
    val labelTextSizePx = 10.sp.toPx(density)

    // ✨ Get themed color outside the remember block for Paint ✨
    val themedOnPrimaryColorArgb = MaterialTheme.colorScheme.onPrimary.toArgb()

    // Paint for labels on pie slices (optional)
    val textPaint = remember(themedOnPrimaryColorArgb, labelTextSizePx) { // ✨ Pass ARGB color as key ✨
        Paint().apply {
            color = themedOnPrimaryColorArgb // ✨ Use the resolved ARGB color ✨
            textAlign = Paint.Align.CENTER
            textSize = labelTextSizePx
            isAntiAlias = true
        }
    }

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val diameter = min(canvasWidth, canvasHeight) * 0.8f
        val radius = diameter / 2f
        val center = Offset(canvasWidth / 2, canvasHeight / 2)

        var startAngle = -90f // Start from the top

        categoryExpenses.forEachIndexed { index, expense ->
            if (totalAmount == 0.0) return@forEachIndexed // Avoid division by zero if all amounts are zero
            val proportion = (expense.totalAmount / totalAmount).toFloat()
            val sweepAngle = (360f * proportion).coerceAtLeast(0.5f) // Ensure very small slices are still visible

            val sliceColor = sliceColors[index % sliceColors.size]

            drawArc(
                color = sliceColor,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true, // For a full pie
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(diameter, diameter)
            )

            // Optional: Draw labels on slices
            if (sweepAngle > 10) { // Only draw label if slice is reasonably large (e.g., > 10 degrees)
                val angleMiddle = startAngle + sweepAngle / 2
                val labelRadius = radius * 0.65f // Adjust position for better readability
                val xPos = center.x + (labelRadius * cos(Math.toRadians(angleMiddle.toDouble()))).toFloat()
                val yPos = center.y + (labelRadius * sin(Math.toRadians(angleMiddle.toDouble()))).toFloat()

                val textBounds = Rect()
                // Create a concise label, e.g., category name (truncated) or just percentage
                val categoryLabel = expense.categoryName.let { if (it.length > 8) "${it.take(7)}…" else it }
                val percentageLabel = "${(proportion * 100).toInt()}%"
                val fullLabel = if (sweepAngle > 25) "$categoryLabel\n$percentageLabel" else percentageLabel // Show category name for larger slices

                // Simple approximation for multi-line text vertical centering
                val lines = fullLabel.lines()
                val totalTextHeight = lines.size * textPaint.textSize
                var currentYPos = yPos - (totalTextHeight / 2) + (textPaint.textSize / 2)


                fullLabel.lines().forEach { line ->
                    textPaint.getTextBounds(line, 0, line.length, textBounds)
                    drawContext.canvas.nativeCanvas.drawText(
                        line,
                        xPos,
                        currentYPos, // Adjust for text vertical alignment
                        textPaint
                    )
                    currentYPos += textPaint.textSize // Move to next line
                }
            }
            startAngle += sweepAngle
        }
    }
}

@Composable
fun CategoryLegend(
    modifier: Modifier = Modifier,
    categoryExpenses: List<CategoryExpense>,
    sliceColors: List<Color> = pieChartColorsDefaults // Allow customizing colors
) {
    if (categoryExpenses.isEmpty()) return

    val totalAmount = remember(categoryExpenses) { categoryExpenses.sumOf { it.totalAmount } }


    Column(modifier = modifier) {
        Text(
            stringResource(R.string.graph_legend_title),
            style = MaterialTheme.typography.titleSmall, // Changed to titleSmall for less emphasis than screen title
            modifier = Modifier.padding(bottom = 8.dp)
        )
        categoryExpenses.forEachIndexed { index, expense ->
            if (expense.totalAmount == 0.0 && categoryExpenses.size > 5) return@forEachIndexed // Skip zero amounts if list is long

            val color = sliceColors[index % sliceColors.size]
            val percentage = if (totalAmount > 0) (expense.totalAmount / totalAmount * 100) else 0.0

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(color)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${expense.categoryName}: ${expense.totalAmount.toCurrencyString()} (${percentage.toInt()}%)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// Helper to format currency for legend (can be moved to a common util)
private fun Double.toCurrencyString(): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    return format.format(this)
}
package com.example.upitracker.ui.components // Or your appropriate package if in GraphsScreen.kt

import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures // For tap detection
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable // For legend interaction
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput // For tap detection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.upitracker.R
import com.example.upitracker.viewmodel.CategoryExpense
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// Helper extension functions (if not already globally available)
fun TextUnit.toPx(density: androidx.compose.ui.unit.Density): Float = with(density) { this@toPx.toPx() }
fun Dp.toPx(density: androidx.compose.ui.unit.Density): Float = with(density) { this@toPx.toPx() }


// Predefined list of colors for pie chart slices
val pieChartColorsDefaults = listOf(
    Color(0xFFF44336), Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF673AB7),
    Color(0xFF3F51B5), Color(0xFF2196F3), Color(0xFF03A9F4), Color(0xFF00BCD4),
    Color(0xFF009688), Color(0xFF4CAF50), Color(0xFF8BC34A), Color(0xFFCDDC39),
    Color(0xFFFFEB3B), Color(0xFFFFC107), Color(0xFFFF9800), Color(0xFF795548)
)

@Composable
fun CategorySpendingPieChart(
    modifier: Modifier = Modifier,
    categoryExpenses: List<CategoryExpense>,
    sliceColors: List<Color> = pieChartColorsDefaults,
    onSliceClick: (CategoryExpense?) -> Unit,
    initiallySelectedCategoryName: String? = null
) {
    var selectedSliceIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(initiallySelectedCategoryName, categoryExpenses) {
        selectedSliceIndex = initiallySelectedCategoryName?.let { name ->
            categoryExpenses.indexOfFirst { it.categoryName == name }
                .takeIf { it != -1 }
        }
    }

    if (categoryExpenses.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.graph_pie_chart_no_data), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val totalAmount = remember(categoryExpenses) { categoryExpenses.sumOf { it.totalAmount } }
    if (totalAmount == 0.0) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.graph_pie_chart_no_data), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val density = LocalDensity.current
    val labelTextSizePx = 9.sp.toPx(density)
    val themedOnPrimaryColorArgb = MaterialTheme.colorScheme.onPrimary.toArgb()
    val textPaintOnSlice = remember(themedOnPrimaryColorArgb, labelTextSizePx) {
        Paint().apply { color = themedOnPrimaryColorArgb; textAlign = Paint.Align.CENTER; textSize = labelTextSizePx; isAntiAlias = true }
    }

    // ✨ Resolve themed outline color outside Canvas ✨
    val themedOutlineColor = MaterialTheme.colorScheme.outline

    // Store calculated slice angles for tap detection
    val sliceInfos = remember(categoryExpenses, totalAmount) {
        var currentStartAngle = -90f
        categoryExpenses.map { expense ->
            val proportion = if (totalAmount > 0) (expense.totalAmount / totalAmount).toFloat() else 0f
            val sweepAngle = (360f * proportion).coerceAtLeast(0.1f)
            val info = Triple(currentStartAngle, sweepAngle, expense)
            currentStartAngle += sweepAngle
            info
        }
    }

    Canvas(
        modifier = modifier
            .pointerInput(sliceInfos) {
                detectTapGestures { tapOffset ->
                    val canvasWidth = size.width.toFloat()
                    val canvasHeight = size.height.toFloat()
                    val center = Offset(canvasWidth / 2, canvasHeight / 2)
                    val diameter = min(canvasWidth, canvasHeight) * 0.8f
                    val radius = diameter / 2f

                    val dx = tapOffset.x - center.x
                    val dy = tapOffset.y - center.y
                    val distanceSquared = dx * dx + dy * dy

                    if (distanceSquared <= radius * radius) {
                        var tapAngleDegrees = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                        if (tapAngleDegrees < 0) tapAngleDegrees += 360f

                        var tappedSliceFoundIndex: Int? = null
                        var currentAngleOffset = -90f // Align with drawArc starting logic
                        for ((index, info) in sliceInfos.withIndex()) {
                            val sliceStartDraw = info.first
                            val sliceSweep = info.second

                            // Normalize tap angle to be 0 at top, positive clockwise
                            var normalizedTapAngleForHitTest = tapAngleDegrees
                            if (normalizedTapAngleForHitTest < -90f) normalizedTapAngleForHitTest += 360f // Adjust if necessary based on atan2 range and drawing start

                            // This hit detection is complex. A simpler way is often to iterate using the angles used for drawing.
                            // Our drawing starts at -90 (North) and sweeps clockwise.
                            // Convert tapAngleDegrees (0 East, CCW) to this system:
                            var tapAngleRelativeToNorthCw = (90 - tapAngleDegrees + 360)%360

                            var currentSliceStartAngle = (sliceStartDraw + 360F) % 360F
                            var currentSliceEndAngle = (sliceStartDraw + sliceSweep + 360F) % 360F

                            var inSlice = false
                            if (currentSliceStartAngle <= currentSliceEndAngle) { // Does not cross 0/360 line from North
                                if (tapAngleRelativeToNorthCw >= currentSliceStartAngle && tapAngleRelativeToNorthCw < currentSliceEndAngle) {
                                    inSlice = true
                                }
                            } else { // Crosses 0/360 line (e.g. starts at 350, ends at 10)
                                if (tapAngleRelativeToNorthCw >= currentSliceStartAngle || tapAngleRelativeToNorthCw < currentSliceEndAngle) {
                                    inSlice = true
                                }
                            }

                            if(inSlice){
                                tappedSliceFoundIndex = index
                                break
                            }
                        }

                        selectedSliceIndex = if (tappedSliceFoundIndex != null) {
                            if (selectedSliceIndex == tappedSliceFoundIndex) null else tappedSliceFoundIndex
                        } else { null }
                        onSliceClick(selectedSliceIndex?.let { categoryExpenses[it] })

                    } else {
                        selectedSliceIndex = null; onSliceClick(null)
                    }
                }
            }
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val outerDiameter = min(canvasWidth, canvasHeight) * 0.8f
        val outerRadius = outerDiameter / 2f
        val center = Offset(canvasWidth / 2, canvasHeight / 2)
        val explosionOffset = if (outerRadius > 0) outerRadius * 0.07f else 0f

        sliceInfos.forEachIndexed { index, (startAngle, sweepAngle, expense) ->
            val sliceColor = sliceColors[index % sliceColors.size]
            val currentCenter = if (index == selectedSliceIndex) {
                val midAngleRad = Math.toRadians(startAngle + sweepAngle / 2.0)
                Offset(
                    center.x + (explosionOffset * cos(midAngleRad)).toFloat(),
                    center.y + (explosionOffset * sin(midAngleRad)).toFloat()
                )
            } else { center }

            drawArc(
                color = sliceColor, startAngle = startAngle, sweepAngle = sweepAngle - 0.5f,
                useCenter = true, topLeft = Offset(currentCenter.x - outerRadius, currentCenter.y - outerRadius),
                size = Size(outerDiameter, outerDiameter), style = Fill
            )

            if (index == selectedSliceIndex) {
                drawArc(
                    color = themedOutlineColor, // ✨ Use pre-resolved themed color ✨
                    startAngle = startAngle, sweepAngle = sweepAngle - 0.5f, useCenter = true,
                    topLeft = Offset(currentCenter.x - outerRadius, currentCenter.y - outerRadius),
                    size = Size(outerDiameter, outerDiameter),
                    style = Stroke(width = 2.dp.toPx()) // density is available via DrawScope, or pass it
                )
            }

            if (sweepAngle > 10) { // Draw labels on slices
                val angleMiddleRad = Math.toRadians(startAngle + sweepAngle / 2.0)
                val labelRadius = outerRadius * 0.65f
                val xPos = currentCenter.x + (labelRadius * cos(angleMiddleRad)).toFloat()
                val yPos = currentCenter.y + (labelRadius * sin(angleMiddleRad)).toFloat()
                val textBounds = Rect()
                val percentage = if (totalAmount > 0) (expense.totalAmount / totalAmount * 100).toInt() else 0
                val label = "$percentage%"
                textPaintOnSlice.getTextBounds(label, 0, label.length, textBounds)
                drawContext.canvas.nativeCanvas.drawText(label, xPos, yPos + textBounds.height() / 2f, textPaintOnSlice)
            }
        }
    }
}

@Composable
fun CategoryLegend(
    modifier: Modifier = Modifier,
    categoryExpenses: List<CategoryExpense>,
    sliceColors: List<Color> = pieChartColorsDefaults,
    selectedCategoryName: String?,
    onLegendItemClick: (String) -> Unit
) {
    if (categoryExpenses.isEmpty()) return
    val totalAmount = remember(categoryExpenses) { categoryExpenses.sumOf { it.totalAmount } }

    Column(modifier = modifier) {
        Text(stringResource(R.string.graph_legend_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        val itemsToShow = categoryExpenses.take(6) // Limit legend items
        itemsToShow.forEachIndexed { index, expense ->
            val color = sliceColors[index % sliceColors.size]
            val percentage = if (totalAmount > 0) (expense.totalAmount / totalAmount * 100) else 0.0
            val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
            val isSelected = expense.categoryName == selectedCategoryName

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { onLegendItemClick(expense.categoryName) }.padding(vertical = 4.dp)
                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent)
            ) {
                Box(modifier = Modifier.size(14.dp).background(color))
                Spacer(Modifier.width(8.dp))
                Text(text = expense.categoryName, style = MaterialTheme.typography.bodyMedium, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Text(text = currencyFormat.format(expense.totalAmount), style = MaterialTheme.typography.bodyMedium, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                Text(text = "(${percentage.toInt()}%)", style = MaterialTheme.typography.bodySmall, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (categoryExpenses.size > itemsToShow.size) {
            Text(stringResource(R.string.graph_legend_and_more, categoryExpenses.size - itemsToShow.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 22.dp))
        }
    }
}

// Helper to format currency for legend (can be moved to a common util)
private fun Double.toCurrencyString(): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    return format.format(this)
}
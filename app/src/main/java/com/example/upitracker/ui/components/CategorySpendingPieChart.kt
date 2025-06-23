package com.example.upitracker.ui.components

import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.example.upitracker.R
import com.example.upitracker.viewmodel.CategoryExpense
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import androidx.core.graphics.get
import kotlin.math.roundToInt


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
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    var hitTestBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    val idToCategoryMap = remember(categoryExpenses) {
        categoryExpenses.mapIndexed { index, expense -> (index + 1) to expense }.toMap()
    }
    val canvasDrawScope = remember { CanvasDrawScope() }
    val layoutDirection = LocalLayoutDirection.current

    LaunchedEffect(initiallySelectedCategoryName, categoryExpenses) {
        selectedSliceIndex = initiallySelectedCategoryName?.let { name ->
            categoryExpenses.indexOfFirst { it.categoryName == name }.takeIf { it != -1 }
        }
    }

    if (categoryExpenses.isEmpty() || categoryExpenses.all { it.totalAmount == 0.0 }) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.graph_pie_chart_no_data), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val totalAmount = remember(categoryExpenses) { categoryExpenses.sumOf { it.totalAmount } }
    val sweepAnimations = remember(categoryExpenses) { categoryExpenses.map { Animatable(0f) } }

    LaunchedEffect(categoryExpenses) {
        sweepAnimations.forEachIndexed { index, anim ->
            val sweepAngle = (360f * (categoryExpenses.getOrNull(index)?.totalAmount ?: 0.0) / totalAmount).toFloat()
            anim.snapTo(0f)
            anim.animateTo(sweepAngle, animationSpec = tween(durationMillis = 600))
        }
    }

    Canvas(
        modifier = modifier
            .pointerInput(idToCategoryMap) {
                detectTapGestures { tapOffset ->
                    coroutineScope.launch {
                        val currentSize = this@pointerInput.size
                        if (hitTestBitmap == null || hitTestBitmap!!.width != currentSize.width || hitTestBitmap!!.height != currentSize.height) {
                            hitTestBitmap = ImageBitmap(currentSize.width, currentSize.height)
                        }

                        hitTestBitmap?.let { bmp ->
                            val hitTestCanvas = androidx.compose.ui.graphics.Canvas(bmp)

                            // ✨ FIX: Correctly call draw() with proper types ✨
                            canvasDrawScope.draw(density, layoutDirection, hitTestCanvas, currentSize.toSize()) {
                                drawPie(
                                    density = density, // Pass density down
                                    expenses = categoryExpenses,
                                    totalAmount = totalAmount,
                                    forHitTest = true,
                                    selectedIndex = selectedSliceIndex,
                                    sweepAnimations = sweepAnimations.map { it.value },
                                    idToCategoryMap = idToCategoryMap,
                                )
                            }

                            val x = tapOffset.x.toInt().coerceIn(0, bmp.width - 1)
                            val y = tapOffset.y.toInt().coerceIn(0, bmp.height - 1)
                            val pixelColor = bmp.asAndroidBitmap()[x, y]

                            val tappedId = android.graphics.Color.red(pixelColor)
                            val tappedExpense = idToCategoryMap[tappedId]
                            val tappedIndex = tappedExpense?.let { categoryExpenses.indexOf(it) }

                            selectedSliceIndex = if (tappedIndex != null && selectedSliceIndex == tappedIndex) {
                                null
                            } else {
                                tappedIndex
                            }
                            onSliceClick(selectedSliceIndex?.let { categoryExpenses[it] })
                        }
                    }
                }
            }
    ) {
        // This is the visible chart
        drawPie(
            density = density, // Pass density down
            expenses = categoryExpenses,
            totalAmount = totalAmount,
            forHitTest = false,
            selectedIndex = selectedSliceIndex,
            sweepAnimations = sweepAnimations.map { it.value },
            sliceColors = sliceColors
        )
    }
}

private fun DrawScope.drawPie(
    density: Density, // ✨ Accept density as a parameter
    expenses: List<CategoryExpense>,
    totalAmount: Double,
    forHitTest: Boolean,
    selectedIndex: Int?,
    sweepAnimations: List<Float>,
    sliceColors: List<Color> = pieChartColorsDefaults,
    idToCategoryMap: Map<Int, CategoryExpense> = emptyMap()
) {
    val outerDiameter = min(size.width, size.height) * 0.8f
    val outerRadius = outerDiameter / 2f
    val explosionOffset = if (outerRadius > 0) outerRadius * 0.07f else 0f
    var startAngle = -90f

    val themedOutlineColor = Color.Black

    // ✨ FIX: Do not use `remember` inside a non-composable function ✨
    val labelTextPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = with(density) { 12.sp.toPx() } // Use density to convert sp to px
        isAntiAlias = true
    }

    expenses.forEachIndexed { index, expense ->
        val sweepAngle = sweepAnimations.getOrElse(index) { 0f }
        val isSelected = index == selectedIndex

        val currentCenter = if (isSelected) {
            val midAngleRad = Math.toRadians(startAngle + sweepAngle / 2.0)
            Offset(
                center.x + (cos(midAngleRad) * explosionOffset).toFloat(),
                center.y + (sin(midAngleRad) * explosionOffset).toFloat()
            )
        } else {
            center
        }

        val color = if (forHitTest) {
            val id = idToCategoryMap.entries.find { it.value == expense }?.key ?: 0
            Color(red = id, green = 0, blue = 0)
        } else {
            sliceColors[index % sliceColors.size]
        }

        drawArc(
            color = color,
            startAngle = startAngle,
            sweepAngle = sweepAngle - 0.5f,
            useCenter = true,
            topLeft = Offset(currentCenter.x - outerRadius, currentCenter.y - outerRadius),
            size = Size(outerDiameter, outerDiameter),
            style = Fill
        )

        if (isSelected && !forHitTest) {
            drawArc(
                color = themedOutlineColor,
                startAngle = startAngle,
                sweepAngle = sweepAngle - 0.5f,
                useCenter = true,
                topLeft = Offset(currentCenter.x - outerRadius, currentCenter.y - outerRadius),
                size = Size(outerDiameter, outerDiameter),
                style = Stroke(width = with(density) { 2.dp.toPx() })
            )
        }

        if (sweepAngle > 10 && !forHitTest) {
            val angleMiddleRad = Math.toRadians(startAngle + sweepAngle / 2.0)
            val labelRadius = outerRadius * 0.65f
            val xPos = currentCenter.x + (cos(angleMiddleRad) * labelRadius).toFloat()
            val yPos = currentCenter.y + (sin(angleMiddleRad) * labelRadius).toFloat()
            val percentage = (expense.totalAmount / totalAmount * 100).toInt()
            val textBounds = Rect()
            val label = "$percentage%"
            labelTextPaint.getTextBounds(label, 0, label.length, textBounds)
            drawContext.canvas.nativeCanvas.drawText(label, xPos, yPos + textBounds.height() / 2f, labelTextPaint)
        }
        startAngle += sweepAngle
    }
}


@OptIn(ExperimentalMaterial3Api::class)
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
    var showFullLegendDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(stringResource(R.string.graph_legend_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        val itemsToShow = categoryExpenses.take(6)
        itemsToShow.forEachIndexed { index, expense ->
            val color = sliceColors[index % sliceColors.size]
            val percentage = if (totalAmount > 0) (expense.totalAmount / totalAmount * 100) else 0.0
            val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("en").setRegion("IN").build()) }
            val isSelected = expense.categoryName == selectedCategoryName

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLegendItemClick(expense.categoryName) }
                    .padding(vertical = 4.dp)
                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent)
            ) {
                Box(modifier = Modifier.size(14.dp).background(color))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = expense.categoryName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = currencyFormatter.format(expense.totalAmount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "(${percentage.roundToInt()}%)",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (categoryExpenses.size > itemsToShow.size) {
            TextButton(
                onClick = { showFullLegendDialog = true },
                modifier = Modifier.padding(start = 16.dp)
            ) {
                Text(
                    stringResource(R.string.graph_legend_and_more, categoryExpenses.size - itemsToShow.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 22.dp)
                )
            }
        }
    }
    if (showFullLegendDialog) {
        FullCategoryLegendDialog(
            allCategoryExpenses = categoryExpenses,
            onDismiss = { showFullLegendDialog = false }
        )
    }
}

@Composable
private fun FullCategoryLegendDialog(
    allCategoryExpenses: List<CategoryExpense>,
    onDismiss: () -> Unit
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("en").setRegion("IN").build()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("All Category Spending") },
        text = {
            LazyColumn {
                items(allCategoryExpenses) { expense ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)) {
                        Text(text = expense.categoryName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text(text = currencyFormat.format(expense.totalAmount), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
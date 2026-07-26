package com.example.upitracker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.upitracker.R
import com.example.upitracker.util.ExpressiveTokens
import com.example.upitracker.util.CurrencyUtils
import com.example.upitracker.viewmodel.CategoryExpense
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

val pieChartColorsDefaults = listOf(
    Color(0xFFF44336),
    Color(0xFFE91E63),
    Color(0xFF9C27B0),
    Color(0xFF673AB7),
    Color(0xFF3F51B5),
    Color(0xFF2196F3),
    Color(0xFF03A9F4),
    Color(0xFF00BCD4),
    Color(0xFF009688),
    Color(0xFF4CAF50),
    Color(0xFF8BC34A),
    Color(0xFFCDDC39),
    Color(0xFFFFEB3B),
    Color(0xFFFFC107),
    Color(0xFFFF9800),
    Color(0xFF795548)
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

    val density = LocalDensity.current

    LaunchedEffect(initiallySelectedCategoryName, categoryExpenses) {
        selectedSliceIndex = initiallySelectedCategoryName?.let { name ->
            categoryExpenses.indexOfFirst { it.categoryName == name }
                .takeIf { it != -1 }
        }
    }

    if (categoryExpenses.isEmpty() || categoryExpenses.all { it.totalAmount == 0.0 }) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.graph_pie_chart_no_data),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val totalAmount = remember(categoryExpenses) {
        categoryExpenses.sumOf { it.totalAmount }
    }

    val sweepAnimations = remember(categoryExpenses) {
        categoryExpenses.map { Animatable(0f) }
    }

    LaunchedEffect(categoryExpenses) {
        sweepAnimations.forEachIndexed { index, anim ->
            val sweepAngle = (
                    360f *
                            (categoryExpenses.getOrNull(index)?.totalAmount ?: 0.0) /
                            totalAmount
                    ).toFloat()

            anim.snapTo(0f)
            anim.animateTo(
                targetValue = sweepAngle,
                animationSpec = tween(durationMillis = 600)
            )
        }
    }

    val chartDescription = remember(categoryExpenses, totalAmount) {
        buildString {
            append("Spending by category. ")
            categoryExpenses.forEachIndexed { index, expense ->
                if (index > 0) append(", ")
                val percentage = (expense.totalAmount / totalAmount * 100).roundToInt()
                append("${expense.categoryName} $percentage percent")
            }
        }
    }

    Canvas(
        modifier = modifier
            .semantics { contentDescription = chartDescription }
            .pointerInput(categoryExpenses, selectedSliceIndex) {
            detectTapGestures { tapOffset ->
                val currentSize = this@pointerInput.size
                val centerX = currentSize.width / 2f
                val centerY = currentSize.height / 2f
                val dx = tapOffset.x - centerX
                val dy = tapOffset.y - centerY
                val radius = sqrt(dx * dx + dy * dy)
                val outerRadius = min(currentSize.width, currentSize.height) * 0.72f / 2f
                val halfStroke = with(density) { 30.dp.toPx() } * 0.65f

                val tappedIndex = if (radius in (outerRadius - halfStroke)..(outerRadius + halfStroke)) {
                    val angle = ((Math.toDegrees(atan2(dy, dx).toDouble()).toFloat() + 90f) + 360f) % 360f
                    var accumulated = 0f
                    sweepAnimations.indexOfFirst { animation ->
                        val end = accumulated + animation.value
                        val contains = angle >= accumulated && angle < end
                        accumulated = end
                        contains
                    }.takeIf { it >= 0 }
                } else {
                    null
                }

                selectedSliceIndex =
                    if (tappedIndex != null && selectedSliceIndex == tappedIndex) {
                        null
                    } else {
                        tappedIndex
                    }
                onSliceClick(selectedSliceIndex?.let(categoryExpenses::get))
            }
        }
    ) {
        drawPie(
            expenses = categoryExpenses,
            selectedIndex = selectedSliceIndex,
            sweepAnimations = sweepAnimations.map { it.value },
            sliceColors = sliceColors,
            density = density
        )
    }
}

private fun DrawScope.drawPie(
    expenses: List<CategoryExpense>,
    selectedIndex: Int?,
    sweepAnimations: List<Float>,
    sliceColors: List<Color> = pieChartColorsDefaults,
    density: Density
) {
    val outerDiameter = min(size.width, size.height) * 0.72f
    val outerRadius = outerDiameter / 2f
    val baseStrokeWidth = with(density) { 30.dp.toPx() }

    var startAngle = -90f

    expenses.forEachIndexed { index, _ ->
        val sweepAngle = sweepAnimations.getOrElse(index) { 0f }
        val isSelected = index == selectedIndex

        val color = sliceColors[index % sliceColors.size]

        drawArc(
            color = color,
            startAngle = startAngle,
            sweepAngle = (sweepAngle - 1.2f).coerceAtLeast(0f),
            useCenter = false,
            topLeft = Offset(
                center.x - outerRadius,
                center.y - outerRadius
            ),
            size = Size(outerDiameter, outerDiameter),
            style = Stroke(
                width = if (isSelected) {
                    baseStrokeWidth * 1.16f
                } else {
                    baseStrokeWidth
                }
            )
        )

        startAngle += sweepAngle
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

    val totalAmount = remember(categoryExpenses) {
        categoryExpenses.sumOf { it.totalAmount }
    }

    var showFullLegendDialog by remember { mutableStateOf(false) }

    val currencyFormatter = remember {
        CurrencyUtils.getRupeeFormatter()
    }

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.graph_legend_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = ExpressiveTokens.spacing.sm)
        )

        val itemsToShow = categoryExpenses.take(6)

        itemsToShow.forEachIndexed { index, expense ->
            val color = sliceColors[index % sliceColors.size]
            val percentageDouble = expense.totalAmount / totalAmount * 100
            val label = String.format(Locale.US, "%.2f%%", percentageDouble)
            val isSelected = expense.categoryName == selectedCategoryName

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLegendItemClick(expense.categoryName) }
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        } else {
                            Color.Transparent
                        }
                    )
                    .padding(vertical = ExpressiveTokens.spacing.xs)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(color)
                )

                Spacer(modifier = Modifier.width(ExpressiveTokens.spacing.sm))

                Text(
                    text = expense.categoryName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    fontWeight = if (isSelected) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Normal
                    },
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(ExpressiveTokens.spacing.sm))

                Text(
                    text = currencyFormatter.format(expense.totalAmount),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                Spacer(modifier = Modifier.width(ExpressiveTokens.spacing.sm))

                Text(
                    text = "($label)",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        if (categoryExpenses.size > itemsToShow.size) {
            TextButton(
                onClick = { showFullLegendDialog = true }
            ) {
                Text(
                    text = stringResource(
                        R.string.graph_legend_and_more,
                        categoryExpenses.size - itemsToShow.size
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
    val currencyFormat = remember {
        CurrencyUtils.getRupeeFormatter()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = ExpressiveTokens.corners.extraLarge,
        title = {
            Text(
                text = "All Category Spending",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn {
                items(allCategoryExpenses) { expense ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = ExpressiveTokens.spacing.xs)
                    ) {
                        Text(
                            text = expense.categoryName,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Text(
                            text = currencyFormat.format(expense.totalAmount),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
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

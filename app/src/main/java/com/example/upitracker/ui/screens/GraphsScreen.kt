package com.example.upitracker.ui.screens

import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi // Required for Pager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager // For swipeable tabs
import androidx.compose.foundation.pager.rememberPagerState // For Pager state
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.BarChart // Icon for Monthly Bar Chart tab
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.upitracker.ui.components.CategoryLegend
import com.example.upitracker.ui.components.CategorySpendingPieChart
import com.example.upitracker.viewmodel.MainViewModel
import com.example.upitracker.viewmodel.MonthlyExpense
import com.example.upitracker.viewmodel.GraphPeriod
import com.example.upitracker.viewmodel.DailyExpensePoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

// Helper extension functions (ensure these are in your file or a utility file)
fun TextUnit.toPx(density: androidx.compose.ui.unit.Density): Float = with(density) { this@toPx.toPx() }
fun Dp.toPx(density: androidx.compose.ui.unit.Density): Float = with(density) { this@toPx.toPx() }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GraphsScreen(
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val lastNMonthsExpenses by mainViewModel.lastNMonthsExpenses.collectAsState()
    val dailyTrendExpenses by mainViewModel.dailyExpensesTrend.collectAsState()
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    val selectedGraphPeriod by mainViewModel.selectedGraphPeriod.collectAsState()

    // ✨ Tab setup ✨
    val graphTabTitles = listOf(
        stringResource(R.string.graph_tab_daily_trend),
        stringResource(R.string.graph_tab_monthly_summary),
        stringResource(R.string.graph_tab_category_pie)
    )
    val graphTabIcons = listOf(
        Icons.AutoMirrored.Filled.ShowChart,
        Icons.Filled.BarChart,
        Icons.Filled.PieChart)
    val pagerState = rememberPagerState { graphTabTitles.size }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier.fillMaxSize()
        // No verticalScroll here anymore, content within pager pages will scroll if needed
    ) {
        Text( // Overall screen title
            stringResource(R.string.bottom_nav_graphs),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.CenterHorizontally)
        )

        // ✨ TabRow for switching between graph types ✨
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
            indicator = { tabPositions ->
                if (pagerState.currentPage < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage])
                    )
                }
            }
        ) {
            graphTabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = { Text(title, style = MaterialTheme.typography.labelLarge) },
                    icon = { Icon(graphTabIcons[index], contentDescription = title) }
                )
            }
        }
        HorizontalDivider()

        // ✨ HorizontalPager to hold the content for each graph tab ✨
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().weight(1f) // Pager takes up remaining space
        ) { pageIndex ->
            // Each page content is now in its own Box, applying scroll modifier to an inner Column
            // The Box itself will fill the page constraints given by HorizontalPager.
            Box(modifier = Modifier.fillMaxSize()) { // This Box fills the pager's page
                PageContent( // Extracted page content to a new composable
                    pageIndex = pageIndex,
                    mainViewModel = mainViewModel,
                    currencyFormatter = currencyFormatter,
                    lastNMonthsExpenses = lastNMonthsExpenses,
                    dailyTrendExpenses = dailyTrendExpenses,
                    selectedGraphPeriod = selectedGraphPeriod
                )
            }
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
            "No data for this period",
            modifier = modifier.padding(16.dp).fillMaxSize(),
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
    val themedAxisColor = axisColor.toArgb()

    val valueLabelPaint = remember(density, themedOnSurfaceColor) {
        Paint().apply { color = themedOnSurfaceColor; textAlign = Paint.Align.CENTER; textSize = 10.sp.toPx(density); isAntiAlias = true }
    }
    val axisLabelPaint = remember(density, themedAxisColor) {
        Paint().apply { color = themedAxisColor; textAlign = Paint.Align.RIGHT; textSize = 12.sp.toPx(density); isAntiAlias = true }
    }
    val monthLabelPaint = remember(density, themedAxisColor) {
        Paint().apply { color = themedAxisColor; textAlign = Paint.Align.CENTER; textSize = 10.sp.toPx(density); isAntiAlias = true }
    }

    Canvas(modifier = modifier.padding(top = 16.dp, bottom = 8.dp)) {
        val barCount = monthlyExpenses.size
        val rightPadding = 16.dp.toPx(density)
        val yAxisLabelHorizontalPadding = 8.dp.toPx(density)
        val yAxisTextWidthApproximation = axisLabelPaint.measureText(currencyFormatter.format(niceMaxAmount).replace("₹", "").trim()) + yAxisLabelHorizontalPadding
        val leftPaddingForYAxis = yAxisTextWidthApproximation + 12.dp.toPx(density)
        val bottomPaddingForXLabels = 34.dp.toPx(density)
        val topPaddingForBarValues = 24.dp.toPx(density)

        val chartWidth = (size.width - leftPaddingForYAxis - rightPadding).coerceAtLeast(0f)
        val chartHeight = (size.height - bottomPaddingForXLabels - topPaddingForBarValues).coerceAtLeast(0f)

        if (chartHeight <= 0 || chartWidth <= 0) return@Canvas

        // Draw Y-axis
        drawLine(
            color = axisColor.copy(alpha = 0.5f),
            start = Offset(leftPaddingForYAxis, topPaddingForBarValues),
            end = Offset(leftPaddingForYAxis, topPaddingForBarValues + chartHeight)
        )
        drawLine(
            color = axisColor.copy(alpha = 0.5f),
            start = Offset(leftPaddingForYAxis, topPaddingForBarValues + chartHeight),
            end = Offset(leftPaddingForYAxis + chartWidth, topPaddingForBarValues + chartHeight)
        )

        // Draw Y-axis labels and grid
        val yAxisSegments = 5
        for (i in 0..yAxisSegments) {
            val value = niceMaxAmount / yAxisSegments * i
            val yPos = topPaddingForBarValues + chartHeight - (value / niceMaxAmount * chartHeight).toFloat()
            drawLine(
                color = axisColor.copy(alpha = 0.2f),
                start = Offset(leftPaddingForYAxis, yPos),
                end = Offset(size.width, yPos)
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

        // Bar and label dimensions
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

            // X labels (every bar, but make sure they don't overlap visually)
            if (barCount <= 8 || index % 2 == 0 || index == barCount - 1 || index == 0) {
                val monthTextBounds = Rect()
                monthLabelPaint.getTextBounds(expense.yearMonth, 0, expense.yearMonth.length, monthTextBounds)
                drawContext.canvas.nativeCanvas.drawText(
                    expense.yearMonth,
                    barLeft + barWidth / 2,
                    topPaddingForBarValues + chartHeight + bottomPaddingForXLabels / 2 + monthTextBounds.height() / 2f,
                    monthLabelPaint
                )
            }

            // Bar value label
            if (expense.totalAmount > 0 && barHeight > (valueLabelPaint.textSize + 2.dp.toPx(density))) {
                val valueText = expense.totalAmount.roundToInt().toString()
                drawContext.canvas.nativeCanvas.drawText(
                    valueText,
                    barLeft + barWidth / 2,
                    barTop - 4.dp.toPx(density),
                    valueLabelPaint
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
        fraction <= 1.0 -> 1.0; fraction <= 1.5 -> 1.5; fraction <= 2.0 -> 2.0
        fraction <= 3.0 -> 3.0; fraction <= 4.0 -> 4.0; fraction <= 5.0 -> 5.0
        fraction <= 7.5 -> 7.5; else -> 10.0
    }
    return niceFraction * 10.0.pow(exponent)
}
// ✨ New Composable for Simple Daily Expense Line Chart ✨
@Composable
fun SimpleDailyExpenseLineChart(
    dailyExpenses: List<DailyExpensePoint>,
    currencyFormatter: NumberFormat,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.tertiary,
    axisColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    pointColor: Color = MaterialTheme.colorScheme.tertiary
) {
    if (dailyExpenses.isEmpty()) {
        Text("No daily trend data", modifier = modifier.padding(16.dp),
            textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    val density = LocalDensity.current
    val actualMaxAmount = remember(dailyExpenses) { dailyExpenses.maxOfOrNull { it.totalAmount } ?: 0.0 }
    val niceMaxAmount = remember(actualMaxAmount) { calculateNiceMax(actualMaxAmount) }

    val axisLabelTextSizePx = 10.sp.toPx(density)
    val axisLabelPaint = remember(axisColor, axisLabelTextSizePx) {
        Paint().apply { color = axisColor.toArgb(); textAlign = Paint.Align.RIGHT; textSize = axisLabelTextSizePx; isAntiAlias = true }
    }
    val dayLabelPaint = remember(axisColor, axisLabelTextSizePx) {
        Paint().apply { color = axisColor.toArgb(); textAlign = Paint.Align.CENTER; textSize = 8.sp.toPx(density); isAntiAlias = true }
    }

    Canvas(modifier = modifier.padding(top = 12.dp, end = 8.dp, bottom = 12.dp)) {
        val pointCount = dailyExpenses.size
        if (pointCount < 2) {
            drawContext.canvas.nativeCanvas.drawText(
                "Not enough data for line chart",
                size.width / 2,
                size.height / 2,
                axisLabelPaint.apply { textAlign = Paint.Align.CENTER }
            )
            return@Canvas
        }

        val rightPadding = 18.dp.toPx(density)
        val yAxisLabelHorizontalPadding = 8.dp.toPx(density)
        val yAxisTextWidthApproximation = axisLabelPaint.measureText(currencyFormatter.format(niceMaxAmount).replace("₹", "").trim()) + yAxisLabelHorizontalPadding
        val leftPaddingForYAxis = yAxisTextWidthApproximation + 14.dp.toPx(density)
        val bottomPaddingForXLabels = 30.dp.toPx(density)
        val topPadding = 20.dp.toPx(density)

        val chartWidth = size.width - leftPaddingForYAxis - rightPadding
        val chartHeight = size.height - bottomPaddingForXLabels - topPadding

        if (chartHeight <= 0 || chartWidth <= 0) return@Canvas

        // Draw Y-axis and grid
        drawLine(
            color = axisColor.copy(alpha = 0.5f),
            start = Offset(leftPaddingForYAxis, topPadding),
            end = Offset(leftPaddingForYAxis, topPadding + chartHeight)
        )
        drawLine(
            color = axisColor.copy(alpha = 0.5f),
            start = Offset(leftPaddingForYAxis, topPadding + chartHeight),
            end = Offset(leftPaddingForYAxis + chartWidth, topPadding + chartHeight)
        )

        val yAxisSegments = 4
        for (i in 0..yAxisSegments) {
            val value = niceMaxAmount / yAxisSegments * i
            val yPos = topPadding + chartHeight - (value / niceMaxAmount * chartHeight).toFloat()
            drawLine(
                color = axisColor.copy(alpha = 0.1f),
                start = Offset(leftPaddingForYAxis, yPos),
                end = Offset(size.width, yPos)
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

        // Prepare line path
        val linePath = Path()
        val xStep = chartWidth / (pointCount - 1).toFloat()

        dailyExpenses.forEachIndexed { index, point ->
            val xPos = leftPaddingForYAxis + (index * xStep)
            val yPosRatio = if (niceMaxAmount > 0) point.totalAmount / niceMaxAmount else 0.0
            val yPos = topPadding + chartHeight - (yPosRatio * chartHeight).toFloat().coerceIn(0f, chartHeight)

            if (index == 0) {
                linePath.moveTo(xPos, yPos)
            } else {
                linePath.lineTo(xPos, yPos)
            }
            // Draw data points
            drawCircle(color = pointColor, radius = 3.dp.toPx(density), center = Offset(xPos, yPos))

            // X-axis label: show only every 5th, and first/last
            if (index == 0 || index == pointCount - 1 || index % 5 == 0) {
                val dayTextBounds = Rect()
                dayLabelPaint.getTextBounds(point.dayLabel, 0, point.dayLabel.length, dayTextBounds)
                drawContext.canvas.nativeCanvas.drawText(
                    point.dayLabel,
                    xPos,
                    topPadding + chartHeight + bottomPaddingForXLabels / 2 + dayTextBounds.height() / 2f,
                    dayLabelPaint
                )
            }
        }

        // Draw the line
        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx(density))
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PageContent(
    pageIndex: Int,
    mainViewModel: MainViewModel,
    currencyFormatter: NumberFormat,
    lastNMonthsExpenses: List<MonthlyExpense>,
    dailyTrendExpenses: List<DailyExpensePoint>,
    selectedGraphPeriod: GraphPeriod
    // Pass other necessary states like categoryData if needed
) {
    // This Column is scrollable and fills the page
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        when (pageIndex) {
            0 -> { // Daily Trend Line Chart Page
                Text(
                    stringResource(R.string.graph_daily_trend_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp).align(Alignment.CenterHorizontally)
                )
                if (dailyTrendExpenses.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.graph_daily_trend_no_data), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    SimpleDailyExpenseLineChart(dailyExpenses = dailyTrendExpenses, currencyFormatter = currencyFormatter, modifier = Modifier.fillMaxWidth().height(300.dp))
                }
            }
            1 -> { // Monthly Expenses Bar Chart Page
                Text(
                    if (lastNMonthsExpenses.isNotEmpty()) stringResource(R.string.graph_monthly_expenses_subtitle, lastNMonthsExpenses.size)
                    else stringResource(R.string.graph_monthly_expenses_subtitle_empty),
                    style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp).align(Alignment.CenterHorizontally)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    GraphPeriod.entries.forEach { period ->
                        FilterChip(
                            selected = selectedGraphPeriod == period,
                            onClick = { mainViewModel.setSelectedGraphPeriod(period) },
                            label = { Text(period.displayName) },
                            leadingIcon = if (selectedGraphPeriod == period) { { Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.filter_chip_selected_desc)) } } else null
                        )
                    }
                }
                if (lastNMonthsExpenses.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.graph_no_data), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    SimpleMonthlyExpenseBarChart(monthlyExpenses = lastNMonthsExpenses, currencyFormatter = currencyFormatter, modifier = Modifier.fillMaxWidth().height(300.dp))
                }
            }
            2 -> { // Category Pie Chart Page
                val categoryData by mainViewModel.categoryExpensesData.collectAsState() // Collect here
                Text(stringResource(R.string.graph_category_pie_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp).align(Alignment.CenterHorizontally))
                if (categoryData.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.graph_category_pie_no_data), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    CategorySpendingPieChart(categoryExpenses = categoryData, modifier = Modifier.fillMaxWidth().height(300.dp).align(Alignment.CenterHorizontally))
                    CategoryLegend(categoryExpenses = categoryData, modifier = Modifier.padding(top = 16.dp).align(Alignment.CenterHorizontally))
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp)) // Padding at the bottom of each tab's content
    }
}



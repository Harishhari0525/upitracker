@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.upitracker.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.Paint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.upitracker.R
import com.example.upitracker.ui.components.CategoryLegend
import com.example.upitracker.ui.components.CategorySpendingPieChart
import com.example.upitracker.ui.components.LottieEmptyState
import com.example.upitracker.ui.components.expressive.ExpressiveTopBar
import com.example.upitracker.util.ExpressiveTokens
import com.example.upitracker.viewmodel.CategoryExpense
import com.example.upitracker.viewmodel.GraphPeriod
import com.example.upitracker.viewmodel.IncomeExpensePoint
import com.example.upitracker.viewmodel.MainViewModel
import com.example.upitracker.viewmodel.MonthlyExpense
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

private enum class GraphType(val label: String, val icon: ImageVector) {
    TRENDS("Trends", Icons.AutoMirrored.Filled.ShowChart),
    CATEGORIES("Categories", Icons.Filled.PieChart),
    CASH_FLOW("Cash flow", Icons.Default.SwapHoriz)
}

@Composable
fun GraphsScreen(
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var selectedGraph by remember { mutableStateOf(GraphType.TRENDS) }
    val selectedPeriod by mainViewModel.selectedGraphPeriod.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Column {
                ExpressiveTopBar(
                    title = "Analytics",
                    subtitle = "See where your money is moving"
                )
                AnalyticsNavigation(
                    selectedGraph = selectedGraph,
                    onGraphSelected = { selectedGraph = it },
                    selectedPeriod = selectedPeriod,
                    onPeriodSelected = mainViewModel::setSelectedGraphPeriod
                )
            }
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = selectedGraph,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            transitionSpec = {
                fadeIn(tween(220)).togetherWith(fadeOut(tween(140)))
            },
            label = "analytics-view-switcher"
        ) { graph ->
            AnalyticsPage(
                graphType = graph,
                selectedPeriod = selectedPeriod,
                mainViewModel = mainViewModel
            )
        }
    }
}

@Composable
private fun AnalyticsNavigation(
    selectedGraph: GraphType,
    onGraphSelected: (GraphType) -> Unit,
    selectedPeriod: GraphPeriod,
    onPeriodSelected: (GraphPeriod) -> Unit
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        space = SegmentedButtonDefaults.BorderWidth
    ) {
        GraphType.entries.forEachIndexed { index, graphType ->
            SegmentedButton(
                selected = selectedGraph == graphType,
                onClick = { onGraphSelected(graphType) },
                shape = SegmentedButtonDefaults.itemShape(index, GraphType.entries.size),
                icon = {
                    Icon(
                        imageVector = graphType.icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = {
                    Text(
                        text = graphType.label,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                }
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Analysis window",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        SingleChoiceSegmentedButtonRow(space = SegmentedButtonDefaults.BorderWidth) {
            GraphPeriod.entries.forEachIndexed { index, period ->
                SegmentedButton(
                    selected = selectedPeriod == period,
                    onClick = { onPeriodSelected(period) },
                    shape = SegmentedButtonDefaults.itemShape(index, GraphPeriod.entries.size),
                    label = {
                        Text(
                            text = period.displayName,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun AnalyticsPage(
    graphType: GraphType,
    selectedPeriod: GraphPeriod,
    mainViewModel: MainViewModel
) {
    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(
            Locale.Builder().setLanguage("en").setRegion("IN").build()
        ).apply {
            maximumFractionDigits = 0
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 12.dp,
            end = 16.dp,
            bottom = 32.dp
        )
    ) {
        item {
            when (graphType) {
                GraphType.TRENDS -> SpendingTrendsContent(
                    period = selectedPeriod,
                    mainViewModel = mainViewModel,
                    currencyFormatter = currencyFormatter
                )

                GraphType.CATEGORIES -> CategoryAnalyticsContent(
                    period = selectedPeriod,
                    mainViewModel = mainViewModel,
                    currencyFormatter = currencyFormatter
                )

                GraphType.CASH_FLOW -> CashFlowContent(
                    period = selectedPeriod,
                    mainViewModel = mainViewModel,
                    currencyFormatter = currencyFormatter
                )
            }
        }
    }
}

@Composable
private fun SpendingTrendsContent(
    period: GraphPeriod,
    mainViewModel: MainViewModel,
    currencyFormatter: NumberFormat
) {
    val expenses by mainViewModel.lastNMonthsExpenses.collectAsStateWithLifecycle()
    var selected by remember(period) { mutableStateOf<MonthlyExpense?>(null) }
    var showLandscapeChart by remember(period) { mutableStateOf(false) }
    val total = remember(expenses) { expenses.sumOf { it.totalAmount } }
    val average = remember(expenses, total) {
        if (expenses.isEmpty()) 0.0 else total / expenses.size
    }
    val latest = expenses.lastOrNull()
    val previous = expenses.dropLast(1).lastOrNull()
    val change = if (latest != null && previous != null && previous.totalAmount > 0.0) {
        (latest.totalAmount - previous.totalAmount) / previous.totalAmount * 100.0
    } else {
        null
    }
    val insight = when {
        change == null -> "A clear month-to-month pattern will appear as more transactions arrive."
        change > 1 -> "${latest?.yearMonth} spending is ${change.roundToInt()}% higher than the previous month."
        change < -1 -> "${latest?.yearMonth} spending is ${-change.roundToInt()}% lower than the previous month."
        else -> "Spending is nearly unchanged from the previous month."
    }

    AnalyticsIntro(
        eyebrow = "${period.displayName} · SPENDING",
        title = "Your spending rhythm",
        insight = insight
    )
    Spacer(Modifier.height(16.dp))

    if (expenses.isEmpty()) {
        AnalyticsEmptyState(stringResource(R.string.graph_no_data))
        return
    }

    ChartCard {
        SelectionCaption(
            label = selected?.yearMonth ?: "Tap a bar for its exact amount",
            value = selected?.let { currencyFormatter.format(it.totalAmount) }
        )
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val chartHeight = if (maxWidth >= 600.dp) 340.dp else 280.dp
            val forceAllLabels = maxWidth >= 600.dp
            Box {
                SpendingBarChart(
                    data = expenses,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeight),
                    onBarSelected = { selected = it },
                    forceAllLabels = forceAllLabels
                )
                if (period == GraphPeriod.TWELVE_MONTHS) {
                    IconButton(
                        onClick = { showLandscapeChart = true },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "View all 12 months in landscape"
                        )
                    }
                }
            }
        }
        MetricStrip(
            metrics = listOf(
                Metric("Total", currencyFormatter.format(total)),
                Metric("Average", currencyFormatter.format(average)),
                Metric("Latest", latest?.let { currencyFormatter.format(it.totalAmount) } ?: "—")
            )
        )
    }

    if (showLandscapeChart) {
        LandscapeChartDialog(
            title = "12-month spending",
            onDismiss = { showLandscapeChart = false }
        ) { chartModifier ->
            SpendingBarChart(
                data = expenses,
                modifier = chartModifier,
                onBarSelected = { selected = it },
                forceAllLabels = true
            )
        }
    }
}

@Composable
private fun CategoryAnalyticsContent(
    period: GraphPeriod,
    mainViewModel: MainViewModel,
    currencyFormatter: NumberFormat
) {
    val categoryData by mainViewModel.categoryExpensesData.collectAsStateWithLifecycle()
    var selectedName by remember(period) { mutableStateOf<String?>(null) }
    val displayData = remember(categoryData) {
        if (categoryData.size <= 6) {
            categoryData
        } else {
            categoryData.take(5) + CategoryExpense(
                categoryName = "Other",
                totalAmount = categoryData.drop(5).sumOf { it.totalAmount }
            )
        }
    }
    val total = remember(categoryData) { categoryData.sumOf { it.totalAmount } }
    val selected = displayData.find { it.categoryName == selectedName }
    val topCategory = categoryData.firstOrNull()
    val topShare = if (topCategory != null && total > 0.0) {
        (topCategory.totalAmount / total * 100.0).roundToInt()
    } else {
        0
    }
    val palette = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.primary.copy(alpha = 0.68f),
        MaterialTheme.colorScheme.onSurfaceVariant
    )

    AnalyticsIntro(
        eyebrow = "${period.displayName} · CATEGORIES",
        title = "Where the money went",
        insight = topCategory?.let {
            "${it.categoryName} leads this period at $topShare% of categorized spending."
        } ?: "Categorize transactions to reveal where your spending is concentrated."
    )
    Spacer(Modifier.height(16.dp))

    if (displayData.isEmpty()) {
        AnalyticsEmptyState(stringResource(R.string.graph_category_pie_no_data))
        return
    }

    ChartCard {
        SelectionCaption(
            label = selected?.categoryName ?: "Categorized spending",
            value = currencyFormatter.format(selected?.totalAmount ?: total)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            contentAlignment = Alignment.Center
        ) {
            CategorySpendingPieChart(
                categoryExpenses = displayData,
                sliceColors = palette,
                modifier = Modifier.fillMaxSize(),
                initiallySelectedCategoryName = selectedName,
                onSliceClick = { selectedName = it?.categoryName }
            )
            if (selected == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = currencyFormatter.format(total),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        CategoryLegend(
            categoryExpenses = displayData,
            sliceColors = palette,
            selectedCategoryName = selectedName,
            onLegendItemClick = { categoryName ->
                selectedName = if (selectedName == categoryName) null else categoryName
            }
        )
        Spacer(Modifier.height(12.dp))
        MetricStrip(
            metrics = listOf(
                Metric("Categories", categoryData.size.toString()),
                Metric("Largest", topCategory?.categoryName ?: "—"),
                Metric("Top share", if (topCategory == null) "—" else "$topShare%")
            )
        )
    }
}

@Composable
private fun CashFlowContent(
    period: GraphPeriod,
    mainViewModel: MainViewModel,
    currencyFormatter: NumberFormat
) {
    val data by mainViewModel.incomeVsExpenseData.collectAsStateWithLifecycle()
    var selected by remember(period) { mutableStateOf<IncomeExpensePoint?>(null) }
    var showLandscapeChart by remember(period) { mutableStateOf(false) }
    val income = remember(data) { data.sumOf { it.totalIncome } }
    val expense = remember(data) { data.sumOf { it.totalExpense } }
    val net = income - expense
    val insight = when {
        data.isEmpty() -> "Income and expense comparisons will appear when transactions are available."
        net > 0 -> "Income is ahead of spending by ${currencyFormatter.format(net)} in this window."
        net < 0 -> "Spending is ahead of income by ${currencyFormatter.format(-net)} in this window."
        else -> "Income and spending are balanced in this window."
    }

    AnalyticsIntro(
        eyebrow = "${period.displayName} · CASH FLOW",
        title = "Money in, money out",
        insight = insight
    )
    Spacer(Modifier.height(16.dp))

    if (data.isEmpty()) {
        AnalyticsEmptyState("No income or expense data for this period.")
        return
    }

    ChartCard {
        SelectionCaption(
            label = selected?.yearMonth ?: "Tap a month to compare",
            value = selected?.let {
                "In ${currencyFormatter.format(it.totalIncome)} · Out ${currencyFormatter.format(it.totalExpense)}"
            }
        )
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val chartHeight = if (maxWidth >= 600.dp) 360.dp else 300.dp
            val forceAllLabels = maxWidth >= 600.dp
            Box {
                CashFlowBarChart(
                    data = data,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeight),
                    onGroupSelected = { selected = it },
                    forceAllLabels = forceAllLabels
                )
                if (period == GraphPeriod.TWELVE_MONTHS) {
                    IconButton(
                        onClick = { showLandscapeChart = true },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "View all 12 months in landscape"
                        )
                    }
                }
            }
        }
        MetricStrip(
            metrics = listOf(
                Metric("Income", currencyFormatter.format(income)),
                Metric("Expense", currencyFormatter.format(expense)),
                Metric("Net", currencyFormatter.format(net), isPositive = net >= 0)
            )
        )
    }

    if (showLandscapeChart) {
        LandscapeChartDialog(
            title = "12-month cash flow",
            onDismiss = { showLandscapeChart = false }
        ) { chartModifier ->
            CashFlowBarChart(
                data = data,
                modifier = chartModifier,
                onGroupSelected = { selected = it },
                forceAllLabels = true
            )
        }
    }
}

@Composable
private fun LandscapeChartDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    val context = LocalContext.current

    DisposableEffect(context) {
        val activity = context.findActivity()
        val originalOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        onDispose {
            activity?.requestedOrientation =
                originalOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close landscape chart"
                        )
                    }
                }
                content(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private data class Metric(
    val label: String,
    val value: String,
    val isPositive: Boolean? = null
)

@Composable
private fun AnalyticsIntro(
    eyebrow: String,
    title: String,
    insight: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = eyebrow,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = insight,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ChartCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ExpressiveTokens.corners.extraLarge,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
private fun SelectionCaption(label: String, value: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun MetricStrip(metrics: List<Metric>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            metrics.forEach { metric ->
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = metric.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Text(
                        text = metric.value,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (metric.isPositive) {
                            true -> MaterialTheme.colorScheme.secondary
                            false -> MaterialTheme.colorScheme.error
                            null -> MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalyticsEmptyState(message: String) {
    LottieEmptyState(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        message = message,
        lottieResourceId = R.raw.empty_box_animation
    )
}

@Composable
private fun SpendingBarChart(
    data: List<MonthlyExpense>,
    modifier: Modifier = Modifier,
    onBarSelected: (MonthlyExpense?) -> Unit,
    forceAllLabels: Boolean
) {
    var selectedIndex by remember(data) { mutableStateOf<Int?>(null) }
    val barRegions = remember(data) { MutableList(data.size) { Rect(0f, 0f, 0f, 0f) } }
    val maximum = remember(data) {
        calculateNiceMax(data.maxOfOrNull { it.totalAmount } ?: 0.0)
    }
    val progress = remember { Animatable(0f) }
    val density = LocalDensity.current
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    val barColor = MaterialTheme.colorScheme.primary
    val selectedColor = MaterialTheme.colorScheme.tertiary
    val labelPaint = remember(density, axisColor) {
        Paint().apply {
            color = axisColor.toArgb()
            textAlign = Paint.Align.CENTER
            textSize = with(density) { 10.sp.toPx() }
            isAntiAlias = true
        }
    }
    val axisPaint = remember(density, axisColor) {
        Paint().apply {
            color = axisColor.toArgb()
            textAlign = Paint.Align.RIGHT
            textSize = with(density) { 10.sp.toPx() }
            isAntiAlias = true
        }
    }
    val accessibilityActions = data.mapIndexed { index, point ->
        CustomAccessibilityAction("${point.yearMonth}, ${point.totalAmount.roundToInt()} rupees") {
            selectedIndex = index
            onBarSelected(point)
            true
        }
    }

    LaunchedEffect(data) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(550))
    }

    Canvas(
        modifier = modifier
            .padding(top = 12.dp, bottom = 8.dp)
            .semantics {
                contentDescription = "Monthly spending for ${data.size} months"
                customActions = accessibilityActions
            }
            .pointerInput(data) {
                detectTapGestures { position ->
                    val tapped = barRegions.indexOfFirst { it.contains(position) }
                    if (tapped >= 0) {
                        selectedIndex = if (selectedIndex == tapped) null else tapped
                        onBarSelected(selectedIndex?.let(data::get))
                    } else {
                        selectedIndex = null
                        onBarSelected(null)
                    }
                }
            }
    ) {
        val left = 48.dp.toPx()
        val right = 8.dp.toPx()
        val top = 12.dp.toPx()
        val bottom = 28.dp.toPx()
        val chartWidth = (size.width - left - right).coerceAtLeast(0f)
        val chartHeight = (size.height - top - bottom).coerceAtLeast(0f)
        if (chartWidth <= 0f || chartHeight <= 0f) return@Canvas

        repeat(5) { index ->
            val value = maximum / 4 * index
            val y = top + chartHeight - (value / maximum * chartHeight).toFloat()
            drawLine(
                color = axisColor.copy(alpha = 0.16f),
                start = Offset(left, y),
                end = Offset(left + chartWidth, y)
            )
            drawContext.canvas.nativeCanvas.drawText(
                formatYAxisLabel(value),
                left - 8.dp.toPx(),
                y + axisPaint.textSize / 3,
                axisPaint
            )
        }

        val slotWidth = chartWidth / data.size.coerceAtLeast(1)
        val barWidth = (slotWidth * 0.58f).coerceAtLeast(4.dp.toPx())
        data.forEachIndexed { index, point ->
            val height = (
                point.totalAmount / maximum * chartHeight * progress.value
                ).toFloat().coerceAtLeast(0f)
            val barLeft = left + index * slotWidth + (slotWidth - barWidth) / 2
            val barTop = top + chartHeight - height
            val region = Rect(
                left = barLeft - 8.dp.toPx(),
                top = top,
                right = barLeft + barWidth + 8.dp.toPx(),
                bottom = top + chartHeight
            )
            barRegions[index] = region
            drawRoundRect(
                color = if (selectedIndex == index) selectedColor else barColor,
                topLeft = Offset(barLeft, barTop),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )

            val showLabel = forceAllLabels || data.size <= 6 || index % 2 == 0 || index == data.lastIndex
            if (showLabel) {
                drawContext.canvas.nativeCanvas.drawText(
                    point.yearMonth,
                    barLeft + barWidth / 2,
                    top + chartHeight + 20.dp.toPx(),
                    labelPaint
                )
            }
        }
    }
}

@Composable
private fun CashFlowBarChart(
    data: List<IncomeExpensePoint>,
    modifier: Modifier = Modifier,
    onGroupSelected: (IncomeExpensePoint?) -> Unit,
    forceAllLabels: Boolean
) {
    var selectedIndex by remember(data) { mutableStateOf<Int?>(null) }
    val groupRegions = remember(data) { MutableList(data.size) { Rect(0f, 0f, 0f, 0f) } }
    val maximum = remember(data) {
        calculateNiceMax(
            data.maxOfOrNull { maxOf(it.totalIncome, it.totalExpense) } ?: 0.0
        )
    }
    val density = LocalDensity.current
    val incomeColor = MaterialTheme.colorScheme.secondary
    val expenseColor = MaterialTheme.colorScheme.error
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    val selectionColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    val labelPaint = remember(density, axisColor) {
        Paint().apply {
            color = axisColor.toArgb()
            textAlign = Paint.Align.CENTER
            textSize = with(density) { 10.sp.toPx() }
            isAntiAlias = true
        }
    }
    val axisPaint = remember(density, axisColor) {
        Paint().apply {
            color = axisColor.toArgb()
            textAlign = Paint.Align.RIGHT
            textSize = with(density) { 10.sp.toPx() }
            isAntiAlias = true
        }
    }
    val accessibilityActions = data.mapIndexed { index, point ->
        CustomAccessibilityAction(
            "${point.yearMonth}, income ${point.totalIncome.roundToInt()} rupees, expense ${point.totalExpense.roundToInt()} rupees"
        ) {
            selectedIndex = index
            onGroupSelected(point)
            true
        }
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 48.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            ChartLegendItem("Income", incomeColor)
            Spacer(Modifier.size(16.dp))
            ChartLegendItem("Expense", expenseColor)
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .semantics {
                    contentDescription = "Income and expense comparison for ${data.size} months"
                    customActions = accessibilityActions
                }
                .pointerInput(data) {
                    detectTapGestures { position ->
                        val tapped = groupRegions.indexOfFirst { it.contains(position) }
                        if (tapped >= 0) {
                            selectedIndex = if (selectedIndex == tapped) null else tapped
                            onGroupSelected(selectedIndex?.let(data::get))
                        } else {
                            selectedIndex = null
                            onGroupSelected(null)
                        }
                    }
                }
        ) {
            val left = 48.dp.toPx()
            val right = 8.dp.toPx()
            val top = 8.dp.toPx()
            val bottom = 28.dp.toPx()
            val chartWidth = (size.width - left - right).coerceAtLeast(0f)
            val chartHeight = (size.height - top - bottom).coerceAtLeast(0f)
            if (chartWidth <= 0f || chartHeight <= 0f) return@Canvas

            repeat(5) { index ->
                val value = maximum / 4 * index
                val y = top + chartHeight - (value / maximum * chartHeight).toFloat()
                drawLine(
                    color = axisColor.copy(alpha = 0.16f),
                    start = Offset(left, y),
                    end = Offset(left + chartWidth, y)
                )
                drawContext.canvas.nativeCanvas.drawText(
                    formatYAxisLabel(value),
                    left - 8.dp.toPx(),
                    y + axisPaint.textSize / 3,
                    axisPaint
                )
            }

            val slotWidth = chartWidth / data.size.coerceAtLeast(1)
            val groupWidth = slotWidth * 0.66f
            val gap = (2.dp.toPx()).coerceAtMost(groupWidth * 0.12f)
            val barWidth = ((groupWidth - gap) / 2).coerceAtLeast(2.dp.toPx())
            data.forEachIndexed { index, point ->
                val groupLeft = left + index * slotWidth + (slotWidth - groupWidth) / 2
                val incomeHeight = (point.totalIncome / maximum * chartHeight).toFloat()
                val expenseHeight = (point.totalExpense / maximum * chartHeight).toFloat()
                groupRegions[index] = Rect(
                    left = groupLeft - 8.dp.toPx(),
                    top = top,
                    right = groupLeft + groupWidth + 8.dp.toPx(),
                    bottom = top + chartHeight
                )

                if (selectedIndex == index) {
                    drawRoundRect(
                        color = selectionColor,
                        topLeft = Offset(groupLeft - 6.dp.toPx(), top),
                        size = Size(groupWidth + 12.dp.toPx(), chartHeight),
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                    )
                }
                drawRoundRect(
                    color = incomeColor,
                    topLeft = Offset(groupLeft, top + chartHeight - incomeHeight),
                    size = Size(barWidth, incomeHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
                drawRoundRect(
                    color = expenseColor,
                    topLeft = Offset(groupLeft + barWidth + gap, top + chartHeight - expenseHeight),
                    size = Size(barWidth, expenseHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )

                val showLabel = forceAllLabels || data.size <= 6 || index % 2 == 0 || index == data.lastIndex
                if (showLabel) {
                    drawContext.canvas.nativeCanvas.drawText(
                        point.yearMonth,
                        groupLeft + groupWidth / 2,
                        top + chartHeight + 20.dp.toPx(),
                        labelPaint
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartLegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(3.dp))
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun calculateNiceMax(actualMax: Double): Double {
    if (actualMax <= 0.0) return 100.0
    val exponent = floor(log10(actualMax))
    val fraction = actualMax / 10.0.pow(exponent)
    val niceFraction = when {
        fraction <= 1.0 -> 1.0
        fraction <= 1.5 -> 1.5
        fraction <= 2.0 -> 2.0
        fraction <= 3.0 -> 3.0
        fraction <= 5.0 -> 5.0
        fraction <= 7.5 -> 7.5
        else -> 10.0
    }
    return niceFraction * 10.0.pow(exponent)
}

private fun formatYAxisLabel(value: Double): String = when {
    value < 1_000 -> value.roundToInt().toString()
    value < 1_00_000 -> String.format(Locale.US, "%.1fk", value / 1_000).replace(".0k", "k")
    value < 1_00_00_000 -> String.format(Locale.US, "%.1fL", value / 1_00_000).replace(".0L", "L")
    else -> String.format(Locale.US, "%.1fCr", value / 1_00_00_000).replace(".0Cr", "Cr")
}

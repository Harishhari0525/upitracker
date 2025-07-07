@file:OptIn(ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,ExperimentalAnimationApi::class)

package com.example.upitracker.ui.screens

import android.graphics.Paint
import androidx.compose.animation.ExperimentalAnimationApi
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.upitracker.R
import com.example.upitracker.ui.components.CategoryLegend
import com.example.upitracker.ui.components.CategorySpendingPieChart
import com.example.upitracker.viewmodel.MainViewModel
import com.example.upitracker.viewmodel.MonthlyExpense
import com.example.upitracker.viewmodel.GraphPeriod
import com.example.upitracker.viewmodel.DailyExpensePoint
import com.example.upitracker.viewmodel.CategoryExpense
import com.example.upitracker.viewmodel.IncomeExpensePoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

// Helper extension functions
fun TextUnit.toPx(density: Density): Float = with(density) { this@toPx.toPx() }
fun Dp.toPx(density: Density): Float = with(density) { this@toPx.toPx() }


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun GraphsScreen(
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val lastNMonthsExpenses by mainViewModel.lastNMonthsExpenses.collectAsState()
    val dailyTrendExpenses by mainViewModel.dailyExpensesTrend.collectAsState()
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("en").setRegion("IN").build()) }
    val selectedGraphPeriod by mainViewModel.selectedGraphPeriod.collectAsState()

    val graphTabTitles = listOf(
        stringResource(R.string.graph_tab_daily_trend),
        stringResource(R.string.graph_tab_monthly_summary),
        stringResource(R.string.graph_tab_category_pie),
        stringResource(R.string.income_expense)
    )
    val graphTabIcons = listOf(
        Icons.AutoMirrored.Filled.ShowChart,
        Icons.Filled.BarChart,
        Icons.Filled.PieChart,
        Icons.Default.SwapHoriz
    )
    val pagerState = rememberPagerState { graphTabTitles.size }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier.fillMaxSize().animateContentSize()
    ) {
        SecondaryScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            indicator = {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(pagerState.currentPage)
                )
            },
            edgePadding = 16.dp
        ) {
            graphTabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.targetPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index, animationSpec = tween())
                        }
                    },
                    text = { Text(title, style = MaterialTheme.typography.labelLarge) },
                    icon = { Icon(graphTabIcons[index], contentDescription = title) }
                )
            }
        }
        HorizontalDivider()
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) { pageIndex ->
            AnimatedContent(
                targetState = pagerState.currentPage, // Use currentPage as the target state
                transitionSpec = {
                    // Check if the target page index is greater than the initial page index.
                    if (targetState > initialState) {
                        // Sliding left: New screen slides in from the right, old screen slides out to the left.
                        (slideInHorizontally(animationSpec = tween(300)) { fullWidth -> fullWidth } + fadeIn())
                            .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { fullWidth -> -fullWidth } + fadeOut())
                    } else {
                        // Sliding right: New screen slides in from the left, old screen slides out to the right.
                        (slideInHorizontally(animationSpec = tween(300)) { fullWidth -> -fullWidth } + fadeIn())
                            .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { fullWidth -> fullWidth } + fadeOut())
                    }
                }, label = "pager"
            ) { currentPage ->
                if (currentPage == pageIndex) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        PageContent(
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
) {
    var selectedBarData by remember { mutableStateOf<MonthlyExpense?>(null) }
    var selectedDailyPointData by remember { mutableStateOf<DailyExpensePoint?>(null) }
    var selectedPieSliceData by remember { mutableStateOf<CategoryExpense?>(null) }
    var currentlySelectedCategoryNameForPie by remember { mutableStateOf<String?>(null) }
    var selectedIncomeExpensePoint by remember { mutableStateOf<IncomeExpensePoint?>(null) }

    val chartCardShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp)


    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        when (pageIndex) {
            0 -> { // Daily Trend Line Chart Page
                Text(
                    stringResource(R.string.graph_daily_trend_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp).align(Alignment.CenterHorizontally)
                )
                AnimatedContent(
                    targetState = selectedDailyPointData,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(220, delayMillis = 90)))
                            .togetherWith(fadeOut(animationSpec = tween(90)))
                    },
                    label = "selected_daily_point_transition",
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).heightIn(min = 20.dp)
                ) { targetData ->
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { // Ensure content is centered
                        if (targetData != null) {
                            Text(
                                text = stringResource(R.string.graph_selected_line_point_details, targetData.dayLabel, currencyFormatter.format(targetData.totalAmount)),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.tertiary,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            // Placeholder to maintain height consistency if the Text has a specific line height
                            Spacer(Modifier.height(MaterialTheme.typography.labelLarge.lineHeight.value.dp.times(LocalDensity.current.fontScale)))
                        }
                    }
                }
                if (dailyTrendExpenses.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.graph_daily_trend_no_data), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    ElevatedCard(modifier = Modifier.fillMaxWidth().height(300.dp).animateContentSize(),shape = chartCardShape) {
                        SimpleDailyExpenseLineChart(
                            dailyExpenses = dailyTrendExpenses,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                            onPointClick = { dailyPoint -> selectedDailyPointData = dailyPoint }
                        )
                    }
                    val dailyStats by mainViewModel.dailyTrendSummaryStats.collectAsState()
                    val statsList = listOf(
                        Triple("Total Spend", currencyFormatter.format(dailyStats.totalAmount), Icons.Default.Summarize),
                        Triple("Daily Avg", currencyFormatter.format(dailyStats.dailyAverage), Icons.Default.Functions),
                        Triple("Highest Day", dailyStats.highestDay?.let { currencyFormatter.format(it.totalAmount) } ?: "N/A",
                            Icons.AutoMirrored.Filled.TrendingUp)
                    )
                    Spacer(Modifier.height(24.dp))
                    SectionHeader(title = "Stats :")
                    Spacer(Modifier.height(8.dp))
                    StatsCard(stats = statsList, currencyFormatter = currencyFormatter)
                }
            }
            1 -> { // Monthly Expenses Bar Chart Page

                var showLandscapeDialog by remember { mutableStateOf(false) }
                val summaryStats by mainViewModel.monthlyDebitSummaryStats.collectAsState()

                Text(
                    // All logic is now cleanly assigned to the 'text' parameter
                    text = "Monthly Debit Totals (Last ${selectedGraphPeriod.months} Months)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .align(Alignment.CenterHorizontally)
                )
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 0.dp),
                    space = SegmentedButtonDefaults.BorderWidth
                ) {
                    GraphPeriod.entries.forEachIndexed { index, period ->
                        SegmentedButton(
                            selected = selectedGraphPeriod == period,
                            onClick = {
                                mainViewModel.setSelectedGraphPeriod(period)
                                selectedBarData = null // Clear selection when period changes
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = GraphPeriod.entries.size),
                            label = { Text(period.displayName, fontWeight = FontWeight.Normal) }
                        )
                    }
                }
                AnimatedContent(
                    targetState = selectedBarData,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(220, delayMillis = 90)))
                            .togetherWith(fadeOut(animationSpec = tween(90)))
                    },
                    label = "selected_bar_data_transition",
                    modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp), // .heightIn(min = 15.dp)
                ) { targetData ->
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { // Ensure content is centered
                        if (targetData != null) {
                            Text(
                                text = stringResource(R.string.graph_selected_bar_details, targetData.yearMonth, currencyFormatter.format(targetData.totalAmount)),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Spacer(Modifier.height(MaterialTheme.typography.labelLarge.lineHeight.value.dp.times(LocalDensity.current.fontScale)))
                        }
                    }
                }
                if (lastNMonthsExpenses.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.graph_no_data),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                        ElevatedCard(modifier = Modifier.fillMaxSize().animateContentSize(),shape = chartCardShape) {
                            SimpleMonthlyExpenseBarChart(
                                monthlyExpenses = lastNMonthsExpenses,
                                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                onBarClick = { expense -> selectedBarData = expense },
                                forceShowAllLabels = false
                            )
                        }
                        if (selectedGraphPeriod == GraphPeriod.TWELVE_MONTHS) {
                            IconButton(
                                onClick = { showLandscapeDialog = true },
                                modifier = Modifier.align(Alignment.TopEnd).padding(15.dp)
                            ) {
                                Icon(Icons.Default.Fullscreen, contentDescription = "View in Landscape")
                            }
                        }
                    }

                    if (showLandscapeDialog) {
                        val context = LocalContext.current

                        // This effect will lock and unlock the screen orientation
                        DisposableEffect(Unit) {
                            val activity = context as? Activity
                            val originalOrientation = activity?.requestedOrientation
                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

                            onDispose {
                                // Restore original orientation when the dialog is dismissed
                                activity?.requestedOrientation = originalOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                            }
                        }

                        Dialog(
                            onDismissRequest = { showLandscapeDialog = false },
                            properties = DialogProperties(usePlatformDefaultWidth = false) // Important for fullscreen
                        ) {
                            Scaffold{ paddingValues ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(paddingValues)
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Reuse the chart, but tell it to show all labels
                                    SimpleMonthlyExpenseBarChart(
                                        monthlyExpenses = lastNMonthsExpenses,
                                        modifier = Modifier.fillMaxSize(),
                                        onBarClick = { /* Clicks can be disabled or handled here */ },
                                        forceShowAllLabels = true // The key change!
                                    )
                                }
                                IconButton(
                                    onClick = { showLandscapeDialog = false },
                                    modifier = Modifier
                                        .align(Alignment.End) // This positions it within the parent Box
                                        .padding(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close Landscape View",
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    }
                    val statsList = listOf(
                        Triple("Total Spend", currencyFormatter.format(summaryStats.totalAmount), Icons.Default.Summarize),
                        Triple("Monthly Avg", currencyFormatter.format(summaryStats.averageAmount), Icons.Default.Functions),
                        Triple("Highest Month", summaryStats.highestMonth?.let { currencyFormatter.format(it.totalAmount) } ?: "N/A",
                            Icons.AutoMirrored.Filled.TrendingUp)
                    )
                    Spacer(Modifier.height(15.dp))
                    SectionHeader(title = "Stats :")
                    Spacer(Modifier.height(8.dp))
                    StatsCard(stats = statsList, currencyFormatter = currencyFormatter)
                }
            }
            2 -> { // Category Pie Chart Page
                val categoryData by mainViewModel.categoryExpensesData.collectAsState()
                Text(stringResource(R.string.graph_category_pie_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp).align(Alignment.CenterHorizontally))
                AnimatedContent(
                    targetState = selectedPieSliceData,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(220, delayMillis = 90)))
                            .togetherWith(fadeOut(animationSpec = tween(90)))
                    },
                    label = "selected_pie_slice_transition",
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).heightIn(min = 20.dp)
                ) { targetData ->
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { // Ensure content is centered
                        if (targetData != null) {
                            val totalAllCategories = categoryData.sumOf { it.totalAmount } // Recalculate here or pass as part of targetData if complex
                            val percentage = if (totalAllCategories > 0)
                                    (targetData.totalAmount / totalAllCategories * 100).roundToInt() else 0
                            Text(
                                text = stringResource(R.string.graph_selected_pie_slice_details, targetData.categoryName,
                                    currencyFormatter.format(targetData.totalAmount), percentage),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Spacer(Modifier.height(MaterialTheme.typography.labelLarge.lineHeight.value.dp.times(LocalDensity.current.fontScale)))
                        }
                    }
                }
                if (categoryData.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(300.dp + 100.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.graph_category_pie_no_data), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    ElevatedCard(modifier = Modifier.fillMaxWidth().height(300.dp).align(Alignment.CenterHorizontally).animateContentSize(),shape = chartCardShape) {
                        CategorySpendingPieChart(
                            categoryExpenses = categoryData,
                            modifier = Modifier.fillMaxSize(),
                            initiallySelectedCategoryName = currentlySelectedCategoryNameForPie,
                            onSliceClick = { categoryExpense ->
                                currentlySelectedCategoryNameForPie = categoryExpense?.categoryName
                                selectedPieSliceData = categoryExpense
                            }
                        )
                    }
                    AnimatedVisibility(visible = categoryData.isNotEmpty()) {
                        CategoryLegend(
                            categoryExpenses = categoryData,
                            modifier = Modifier.padding(top = 16.dp).align(Alignment.Start),
                            selectedCategoryName = currentlySelectedCategoryNameForPie,
                            onLegendItemClick = { categoryName ->
                                currentlySelectedCategoryNameForPie =
                                    if (currentlySelectedCategoryNameForPie == categoryName) null else categoryName
                                selectedPieSliceData = categoryData.find {
                                    it.categoryName == currentlySelectedCategoryNameForPie }
                            }
                        )
                    }
                }
            }
            3 -> { // ✨ This is the updated section for the Income vs. Expense chart
                val incomeExpenseData by mainViewModel.incomeVsExpenseData.collectAsState()
                val recentData = incomeExpenseData.takeLast(3)

                Text(
                    "Income vs. Expense",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp).align(Alignment.CenterHorizontally)
                )

                // ✨ 2. ADD AnimatedContent to display the selected month's details ✨
                AnimatedContent(
                    targetState = selectedIncomeExpensePoint,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(220, delayMillis = 90)))
                            .togetherWith(fadeOut(animationSpec = tween(90)))
                    },
                    label = "selected_income_expense_transition",
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp), //.heightIn(min = 40.dp)
                ) { targetData ->
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        if (targetData != null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = targetData.yearMonth,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Income: ${currencyFormatter.format(targetData.totalIncome)} | Expense: ${currencyFormatter.format(targetData.totalExpense)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (incomeExpenseData.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(300.dp),
                        contentAlignment = Alignment.Center) {
                        Text("No income or expense data for this period.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    ElevatedCard(modifier = Modifier.fillMaxWidth().height(350.dp).animateContentSize(),shape = chartCardShape) {
                        // ✨ 3. PASS the selection lambda to the chart ✨
                        IncomeExpenseGroupedBarChart(
                            data = recentData,
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            onBarGroupClick = { selectedIncomeExpensePoint = it }
                        )
                    }
                }
                val incomeStats by mainViewModel.incomeExpenseSummaryStats.collectAsState()
                val statsList = listOf(
                    Triple("Total Income", currencyFormatter.format(incomeStats.totalIncome), Icons.Default.ArrowUpward),
                    Triple("Total Expense", currencyFormatter.format(incomeStats.totalExpense), Icons.Default.ArrowDownward),
                    Triple("Net Savings", currencyFormatter.format(incomeStats.netSavings), Icons.Default.AccountBalance)
                )
                Spacer(Modifier.height(24.dp))
                SectionHeader(title = "Stats :")
                Spacer(Modifier.height(8.dp))
                StatsCard(stats = statsList, currencyFormatter = currencyFormatter)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun SimpleMonthlyExpenseBarChart(
    monthlyExpenses: List<MonthlyExpense>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    selectedBarColor: Color = MaterialTheme.colorScheme.secondary,
    axisColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onBarClick: (expense: MonthlyExpense?) -> Unit,
    forceShowAllLabels: Boolean = false
) {
    var selectedBarIndex by remember { mutableStateOf<Int?>(null) }
    val density = LocalDensity.current
    val barRegions = remember { mutableStateListOf<ComposeRect>() }

    val themedOutlineColor = MaterialTheme.colorScheme.outline
    val themedBarColor = barColor
    val themedSelectedBarColor = selectedBarColor
    val themedAxisColorArgb = axisColor.toArgb()
    val themedOnSurfaceColorArgb = MaterialTheme.colorScheme.onSurface.toArgb()

    val valueLabelPaint = remember(density, themedOnSurfaceColorArgb) {
        Paint().apply { color = themedOnSurfaceColorArgb; textAlign = Paint.Align.CENTER; textSize = 10.sp.toPx(density); isAntiAlias = true } }
    val axisLabelPaint = remember(density, themedAxisColorArgb) {
        Paint().apply { color = themedAxisColorArgb; textAlign = Paint.Align.RIGHT; textSize = 12.sp.toPx(density); isAntiAlias = true } }
    val monthLabelPaint = remember(density, themedAxisColorArgb) {
        Paint().apply { color = themedAxisColorArgb; textAlign = Paint.Align.CENTER; textSize = 10.sp.toPx(density); isAntiAlias = true } }

    if (monthlyExpenses.isEmpty()) {
        Text(stringResource(R.string.graph_bar_chart_no_data_period),
            modifier = modifier.padding(16.dp).fillMaxSize(),
            textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }

    val actualMaxAmount = remember(monthlyExpenses) { monthlyExpenses.maxOfOrNull { it.totalAmount } ?: 0.0 }
    val niceMaxAmount = remember(actualMaxAmount) { calculateNiceMax(actualMaxAmount) }

    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(monthlyExpenses) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 800)) // A single, smooth animation
    }

    Canvas(
        modifier = modifier
            .padding(top = 16.dp, bottom = 8.dp)
            .pointerInput(monthlyExpenses, barRegions.size) { // Key with current data size
                detectTapGestures { tapOffset ->
                    val currentRegions = barRegions.toList() // Work with a stable copy
                    val tappedIndex = currentRegions.indexOfFirst { region -> region.contains(tapOffset) }

                    if (tappedIndex != -1) {
                        selectedBarIndex = if (selectedBarIndex == tappedIndex) null else tappedIndex
                        onBarClick(selectedBarIndex?.let { monthlyExpenses[it] })
                    } else {
                        selectedBarIndex = null
                        onBarClick(null)
                    }
                }
            }
    ) {
        barRegions.clear()
        val barCount = monthlyExpenses.size
        if (barCount == 0) return@Canvas

        val yAxisLabelHorizontalPadding = 8.dp.toPx()
        val yAxisTextWidthApproximation = axisLabelPaint.measureText(formatYAxisLabel(niceMaxAmount)) + yAxisLabelHorizontalPadding
        val leftPaddingForYAxis = yAxisTextWidthApproximation + 12.dp.toPx(); val bottomPaddingForXLabels = 34.dp.toPx()
        val topPaddingForBarValues = 24.dp.toPx(); val rightPadding = 16.dp.toPx()
        val chartWidth = (size.width - leftPaddingForYAxis - rightPadding).coerceAtLeast(0f)
        val chartHeight = (size.height - bottomPaddingForXLabels - topPaddingForBarValues).coerceAtLeast(0f)
        if (chartHeight <= 0f || chartWidth <= 0f) { Log.w("BarChartDraw", "Not enough space: $chartWidth x $chartHeight"); return@Canvas }

        drawLine(color = axisColor.copy(alpha = 0.5f),
            start = Offset(leftPaddingForYAxis, topPaddingForBarValues),
            end = Offset(leftPaddingForYAxis, topPaddingForBarValues + chartHeight))
        drawLine(color = axisColor.copy(alpha = 0.5f),
            start = Offset(leftPaddingForYAxis, topPaddingForBarValues + chartHeight),
            end = Offset(leftPaddingForYAxis + chartWidth, topPaddingForBarValues + chartHeight))
        val yAxisSegments = 5
        for (i in 0..yAxisSegments) {
            val value = niceMaxAmount / yAxisSegments * i
            val yPos = topPaddingForBarValues + chartHeight - (value / niceMaxAmount * chartHeight).toFloat()
            drawLine(color = axisColor.copy(alpha = 0.2f),
                start = Offset(leftPaddingForYAxis - 4.dp.toPx(), yPos),
                end = Offset(leftPaddingForYAxis + chartWidth, yPos))
            val textBounds = Rect(); val labelText = formatYAxisLabel(value)
            axisLabelPaint.getTextBounds(labelText, 0, labelText.length, textBounds)
            drawContext.canvas.nativeCanvas.drawText(labelText, leftPaddingForYAxis - yAxisLabelHorizontalPadding, yPos + textBounds.height() / 2f, axisLabelPaint)
        }

        val totalBarWidthAndSpacing = chartWidth / barCount
        val barSpacing = totalBarWidthAndSpacing * 0.25f
        val barWidth = (totalBarWidthAndSpacing - barSpacing).coerceAtLeast(4.dp.toPx())

        monthlyExpenses.forEachIndexed { index, expense ->
            // --- Bar position and size calculations (no changes here) ---
            val ratio = if (niceMaxAmount > 0) (expense.totalAmount / niceMaxAmount).toFloat() else 0f
            val barHeight = (ratio * chartHeight * animationProgress.value).coerceAtLeast(0f)
            val xOffsetInChart = (index * totalBarWidthAndSpacing) + (barSpacing / 2)
            val barLeft = leftPaddingForYAxis + xOffsetInChart
            val barTop = topPaddingForBarValues + chartHeight - barHeight
            val barRect = ComposeRect(left = barLeft, top = barTop, right = barLeft + barWidth,
                bottom = topPaddingForBarValues + chartHeight)
            barRegions.add(barRect)

            // --- Draw the bar and its selection outline (no changes here) ---
            val currentBarColorToDraw = if (index == selectedBarIndex) themedSelectedBarColor else themedBarColor
            drawRect(color = currentBarColorToDraw, topLeft = Offset(x = barLeft, y = barTop), size = Size(width = barWidth, height = barHeight))
            if (index == selectedBarIndex) {
                drawRect(color = themedOutlineColor, topLeft = Offset(x = barLeft, y = barTop), size = Size(width = barWidth, height = barHeight), style = Stroke(width = 2.dp.toPx()))
            }
            if (expense.totalAmount > 0) { // Only draw labels for non-zero amounts
                val valueText = expense.totalAmount.roundToInt().toString()
                valueLabelPaint.textAlign = Paint.Align.CENTER
                // Always draw the text ABOVE the bar with a theme-aware color
                valueLabelPaint.color = themedOnSurfaceColorArgb
                drawContext.canvas.nativeCanvas.drawText(
                    valueText,
                    barLeft + barWidth / 2,
                    barTop - 6.dp.toPx(), // Positioned safely above the top of the bar
                    valueLabelPaint
                )
            }

            val showMonthLabel = forceShowAllLabels || when {
                barCount <= 7 -> true
                barCount <= 12 -> index % 2 == 0
                else -> false
            }
            if (showMonthLabel) {
                val monthTextBounds = Rect()
                monthLabelPaint.getTextBounds(expense.yearMonth, 0, expense.yearMonth.length, monthTextBounds)
                drawContext.canvas.nativeCanvas.drawText(expense.yearMonth, barLeft + barWidth / 2, topPaddingForBarValues + chartHeight + bottomPaddingForXLabels / 2f + monthTextBounds.height() / 2f, monthLabelPaint)
            }
        }
    }
}

@Composable
fun SimpleDailyExpenseLineChart(
    dailyExpenses: List<DailyExpensePoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.tertiary,
    axisColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    pointColor: Color = MaterialTheme.colorScheme.tertiary,
    selectedPointColor: Color = MaterialTheme.colorScheme.primary,
    onPointClick: (DailyExpensePoint?) -> Unit
) {
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }
    val pointCoordinates = remember { mutableStateListOf<Offset>() }

    // ✨ Resolve string resources outside the Canvas block ✨
    val noDataForLineChartText = stringResource(R.string.graph_line_chart_no_data)
    val notEnoughDataForLineChartText = stringResource(R.string.graph_line_chart_not_enough_data)


    if (dailyExpenses.isEmpty()) {
        Text(
            noDataForLineChartText, // ✨ Use resolved string
            modifier = modifier.padding(16.dp).fillMaxSize(),
            textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    val density = LocalDensity.current
    val actualMaxAmount = remember(dailyExpenses) { dailyExpenses.maxOfOrNull { it.totalAmount } ?: 0.0 }
    val niceMaxAmount = remember(actualMaxAmount) { calculateNiceMax(actualMaxAmount) }

    val themedAxisColor = axisColor.toArgb()
    val axisLabelPaint = remember(density, themedAxisColor) {
        Paint().apply { color = themedAxisColor; textAlign = Paint.Align.RIGHT; textSize = 10.sp.toPx(density); isAntiAlias = true }
    }
    val dayLabelPaint = remember(density, themedAxisColor) {
        Paint().apply { color = themedAxisColor; textAlign = Paint.Align.CENTER; textSize = 8.sp.toPx(density); isAntiAlias = true }
    }

    val drawProgress = remember { Animatable(0f) }

    LaunchedEffect(dailyExpenses) {
        drawProgress.snapTo(0f)
        drawProgress.animateTo(1f, tween(durationMillis = 600))
    }

    Canvas(
        modifier = modifier
            .padding(top = 12.dp, end = 8.dp, bottom = 12.dp)
            .pointerInput(dailyExpenses, pointCoordinates.size) {
                detectTapGestures { tapOffset ->
                    var newSelectedPointIndex: Int? = null
                    var minDistanceSq = Float.MAX_VALUE
                    val tapRegionRadiusSq = (12.dp.toPx(density) * 12.dp.toPx(density))
                    val currentPointCoords = pointCoordinates.toList()
                    currentPointCoords.forEachIndexed { index, pointCoord ->
                        val dx = tapOffset.x - pointCoord.x
                        val dy = tapOffset.y - pointCoord.y
                        val distanceSq = dx * dx + dy * dy
                        if (distanceSq < minDistanceSq && distanceSq < tapRegionRadiusSq) {
                            minDistanceSq = distanceSq
                            newSelectedPointIndex = index
                        }
                    }
                    if (selectedPointIndex == newSelectedPointIndex && newSelectedPointIndex != null) {
                        selectedPointIndex = null; onPointClick(null)
                    } else {
                        selectedPointIndex = newSelectedPointIndex
                        onPointClick(newSelectedPointIndex?.let { dailyExpenses[it] })
                    }
                }
            }
    ) {
        pointCoordinates.clear()
        val pointCount = dailyExpenses.size
        if (pointCount < 2) {
            // ✨ Use pre-resolved string for drawText ✨
            drawContext.canvas.nativeCanvas.drawText(
                notEnoughDataForLineChartText, // Use the resolved string
                size.width / 2,
                size.height / 2,
                axisLabelPaint.apply { textAlign = Paint.Align.CENTER }
            ); return@Canvas
        }

        val rightPadding = 18.dp.toPx()
        val yAxisLabelHorizontalPadding = 8.dp.toPx()
        val yAxisTextWidthApproximation = axisLabelPaint.measureText(formatYAxisLabel(niceMaxAmount)) + yAxisLabelHorizontalPadding
        val leftPaddingForYAxis = yAxisTextWidthApproximation + 14.dp.toPx()
        val bottomPaddingForXLabels = 30.dp.toPx(); val topPadding = 20.dp.toPx()
        val chartWidth = (size.width - leftPaddingForYAxis - rightPadding).coerceAtLeast(0f)
        val chartHeight = (size.height - bottomPaddingForXLabels - topPadding).coerceAtLeast(0f)

        if (chartHeight <= 0f || chartWidth <= 0f) {
            Log.w("LineChartDraw", "Not enough space: $chartWidth x $chartHeight"); return@Canvas
        }

        drawLine(color = axisColor.copy(alpha = 0.5f),
            start = Offset(leftPaddingForYAxis, topPadding),
            end = Offset(leftPaddingForYAxis, topPadding + chartHeight))

        drawLine(color = axisColor.copy(alpha = 0.5f),
            start = Offset(leftPaddingForYAxis, topPadding + chartHeight),
            end = Offset(leftPaddingForYAxis + chartWidth, topPadding + chartHeight))

        val yAxisSegments = 4; for (i in 0..yAxisSegments) {
            val value = niceMaxAmount / yAxisSegments * i
        val yPos = topPadding + chartHeight - (value / niceMaxAmount * chartHeight).toFloat()
        drawLine(color = axisColor.copy(alpha = 0.1f),
            start = Offset(leftPaddingForYAxis - 4.dp.toPx(), yPos),
            end = Offset(leftPaddingForYAxis + chartWidth, yPos))
        val textBounds = Rect(); val labelText = formatYAxisLabel(value)
        axisLabelPaint.getTextBounds(labelText, 0, labelText.length, textBounds)

        drawContext.canvas.nativeCanvas.drawText(
            labelText, leftPaddingForYAxis - yAxisLabelHorizontalPadding, yPos + textBounds.height() / 2f, axisLabelPaint)
        }

        val linePath = Path()
        val xStep = if (pointCount > 1) chartWidth / (pointCount - 1).toFloat() else 0f
        dailyExpenses.forEachIndexed { index, point ->
            val xPos = leftPaddingForYAxis + (index * xStep)
            val yPosRatio = if (niceMaxAmount > 0) point.totalAmount / niceMaxAmount else 0.0
            val yPosOnCanvas = topPadding + chartHeight - (yPosRatio * chartHeight).toFloat()
            pointCoordinates.add(Offset(xPos,yPosOnCanvas))
            if (index == 0) {
                linePath.moveTo(xPos, yPosOnCanvas)
            }
            else {
                linePath.lineTo(xPos, yPosOnCanvas)
            }
            val currentPointColorToDraw =
                if (index == selectedPointIndex) selectedPointColor
            else pointColor

            val currentPointRadius = if (index == selectedPointIndex) 6.dp.toPx()
            else 3.dp.toPx()

            drawCircle(color = currentPointColorToDraw, radius = currentPointRadius, center = Offset(xPos, yPosOnCanvas))

            if (pointCount <= 7 || index % ((pointCount / 7).coerceAtLeast(1)) == 0 || index == pointCount -1 ) { val dayTextBounds = Rect(); dayLabelPaint.getTextBounds(point.dayLabel, 0, point.dayLabel.length, dayTextBounds); drawContext.canvas.nativeCanvas.drawText(point.dayLabel, xPos, topPadding + chartHeight + bottomPaddingForXLabels / 2 + dayTextBounds.height()/2f, dayLabelPaint) }
        }
        val segmentPath = Path()

        if (pointCoordinates.isNotEmpty()) {
            segmentPath.moveTo(pointCoordinates.first().x, pointCoordinates.first().y)
            val totalLength = pointCoordinates.zipWithNext { a, b ->
                hypot((b.x - a.x).toDouble(), (b.y - a.y).toDouble()).toFloat()
            }.sum()
            var traversed = 0f
            val target = totalLength * drawProgress.value
            for (i in 1 until pointCoordinates.size) {
                val start = pointCoordinates[i - 1]
                val end = pointCoordinates[i]
                val segLength = hypot(
                    (end.x - start.x).toDouble(),
                    (end.y - start.y).toDouble()
                ).toFloat()
                if (traversed + segLength <= target) {
                    segmentPath.lineTo(end.x, end.y)
                    traversed += segLength
                } else {
                    val remain = (target - traversed).coerceAtLeast(0f)
                    val ratio = if (segLength == 0f) 0f else remain / segLength
                    val x = start.x + (end.x - start.x) * ratio
                    val y = start.y + (end.y - start.y) * ratio
                    segmentPath.lineTo(x, y)
                    break
                }
            }
        }
        drawPath(
            path = segmentPath,
            color = lineColor, // Use the solid theme color
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round, // Makes the start/end of the line smooth
                join = StrokeJoin.Round // Makes the corners of the line smooth
            )
        )
    }
}

// calculateNiceMax function
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
// Add this new composable at the end of GraphsScreen.kt

@Composable
fun IncomeExpenseGroupedBarChart(
    data: List<IncomeExpensePoint>,
    onBarGroupClick: (IncomeExpensePoint?) -> Unit,
    modifier: Modifier = Modifier
) {
    // --- Define Colors ---
    val incomeColor = Color(0xFF2E7D32) // Dark green for income
    val expenseColor = MaterialTheme.colorScheme.error
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    val selectionHighlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)

    // --- Define Paints ---
    val density = LocalDensity.current
    val labelPaint = remember(density) {
        Paint().apply {
            textAlign = Paint.Align.CENTER
            textSize = 10.sp.toPx(density)
            isAntiAlias = true
            color = Color.White.toArgb() // ✨ FIX: White text for high contrast on both bars
        }
    }
    val axisLabelPaint = remember(density, axisColor) {
        Paint().apply { color = axisColor.toArgb()
            textAlign = Paint.Align.RIGHT
            textSize = 12.sp.toPx(density); isAntiAlias = true }
    }
    val monthLabelPaint = remember(density, axisColor) {
        Paint().apply { color = axisColor.toArgb()
            textAlign = Paint.Align.CENTER; textSize = 10.sp.toPx(density); isAntiAlias = true }
    }

    // --- Other State ---
    val barRegions = remember(data) { mutableStateListOf<RectF>() }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    if (data.isEmpty()) return

    val absoluteMax = calculateNiceMax(data.flatMap
    { listOf(it.totalIncome, it.totalExpense) }.maxOrNull() ?: 0.0)

    Column(modifier = modifier) {
        // Legend
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 56.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).background(incomeColor))
                Spacer(Modifier.width(4.dp))
                Text("Income", style = MaterialTheme.typography.labelSmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).background(expenseColor))
                Spacer(Modifier.width(4.dp))
                Text("Expense", style = MaterialTheme.typography.labelSmall)
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(data) {
                    detectTapGestures { tapOffset ->
                        val tappedIndex = barRegions.indexOfFirst { region -> region.contains(tapOffset.x, tapOffset.y) }
                        if (tappedIndex != -1) {
                            selectedIndex = if (selectedIndex == tappedIndex) null else tappedIndex
                            onBarGroupClick(selectedIndex?.let { data[it] })
                        }
                    }
                }
        ) {
            // Initialize or clear the regions list to match the data size
            if (barRegions.size != data.size) {
                barRegions.clear()
                repeat(data.size) { barRegions.add(RectF()) }
            }

            val yAxisLabelHorizontalPadding = 8.dp.toPx()
            val yAxisTextWidthApproximation = axisLabelPaint.measureText(formatYAxisLabel(absoluteMax)) + yAxisLabelHorizontalPadding
            val leftPadding = yAxisTextWidthApproximation + 12.dp.toPx()

            val bottomPadding = 30.dp.toPx()
            val topPadding = 24.dp.toPx()
            val rightPadding = 16.dp.toPx()
            val chartWidth = size.width - leftPadding - rightPadding
            val chartHeight = size.height - bottomPadding - topPadding

            if (chartHeight <= 0f || chartWidth <= 0f) return@Canvas

            // Draw Y-Axis lines and labels
            val yAxisSegments = 5
            for (i in 0..yAxisSegments) {
                val value = absoluteMax / yAxisSegments * i
                val yPos = topPadding + chartHeight - (value / absoluteMax * chartHeight).toFloat()
                drawLine(
                    color = axisColor.copy(alpha = 0.2f),
                    start = Offset(leftPadding - 4.dp.toPx(), yPos),
                    end = Offset(leftPadding + chartWidth, yPos)
                )
                val labelText = formatYAxisLabel(value)
                drawContext.canvas.nativeCanvas.drawText(labelText, leftPadding - yAxisLabelHorizontalPadding, yPos
                        + axisLabelPaint.textSize / 3, axisLabelPaint)
            }

            // Draw Bars and Labels
            val groupWidth = chartWidth / data.size
            data.forEachIndexed { index, point ->
                val groupLeft = leftPadding + (index * groupWidth)
                barRegions[index].set(groupLeft, topPadding, groupLeft + groupWidth, topPadding + chartHeight)

                val barPairPadding = groupWidth * 0.2f
                val barPairWidth = groupWidth - barPairPadding
                val barWidth = barPairWidth / 2
                val groupHorizontalPadding = (groupWidth - barPairWidth) / 2

                // --- Income Bar and Label ---
                val incomeBarHeight = if (absoluteMax > 0)
                        (point.totalIncome / absoluteMax).toFloat() * chartHeight else 0f
                val incomeBarLeft = groupLeft + groupHorizontalPadding
                val incomeBarTop = topPadding + chartHeight - incomeBarHeight
                drawRect(color = incomeColor,
                    topLeft = Offset(incomeBarLeft, incomeBarTop),
                    size = Size(barWidth, incomeBarHeight))

                labelPaint.textAlign = Paint.Align.CENTER
                val incomeValueText = point.totalIncome.roundToInt().toString()
                if (incomeBarHeight > (labelPaint.textSize + 4.dp.toPx())) {
                    labelPaint.color = Color.White.toArgb() // Set color for INSIDE
                    drawContext.canvas.nativeCanvas.drawText(incomeValueText, incomeBarLeft + barWidth / 2, incomeBarTop + labelPaint.textSize + 2.dp.toPx(), labelPaint)
                } else if (point.totalIncome > 0) {
                    labelPaint.color = axisColor.toArgb() // Set color for ABOVE (using axisColor for contrast)
                    drawContext.canvas.nativeCanvas.drawText(incomeValueText, incomeBarLeft + barWidth / 2, incomeBarTop - 4.dp.toPx(), labelPaint)
                }


                // --- Expense Bar and Label ---
                val expenseBarHeight = if (absoluteMax > 0)
                        (point.totalExpense / absoluteMax).toFloat() * chartHeight else 0f
                val expenseBarLeft = incomeBarLeft + barWidth
                val expenseBarTop = topPadding + chartHeight - expenseBarHeight
                drawRect(color = expenseColor,
                    topLeft = Offset(expenseBarLeft, expenseBarTop),
                    size = Size(barWidth, expenseBarHeight))

                val expenseValueText = point.totalExpense.roundToInt().toString()
                if (expenseBarHeight > (labelPaint.textSize + 4.dp.toPx())) {
                    labelPaint.color = Color.White.toArgb() // Set color for INSIDE
                    drawContext.canvas.nativeCanvas.drawText(expenseValueText, expenseBarLeft + barWidth / 2,
                        expenseBarTop + labelPaint.textSize + 2.dp.toPx(), labelPaint)
                } else if (point.totalExpense > 0) {
                    labelPaint.color = axisColor.toArgb() // Set color for ABOVE
                    drawContext.canvas.nativeCanvas.drawText(expenseValueText, expenseBarLeft + barWidth / 2,
                        expenseBarTop - 4.dp.toPx(), labelPaint)
                }

                // Selection Highlight
                if (index == selectedIndex) {
                    drawRect(color = selectionHighlightColor,
                        topLeft = Offset(groupLeft, topPadding),
                        size = Size(groupWidth, chartHeight))
                }

                // Month Label
                drawContext.canvas.nativeCanvas.drawText(point.yearMonth, groupLeft + groupWidth / 2,
                    topPadding + chartHeight + bottomPadding - 8.dp.toPx(), monthLabelPaint)
            }
        }
    }
}

@Composable
private fun StatsCard(
    stats: List<Triple<String, String, ImageVector>>,
    currencyFormatter: NumberFormat
) {
    // The StatItem helper composable does not need any changes
    @Composable
    fun StatItem(label: String, value: String, icon: ImageVector) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.secondary)
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(), // Removed top padding here to be controlled by the Spacer outside
        shape = MaterialTheme.shapes.large
    ) {
        // The root layout is now a Column to arrange items vertically
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp) // Space between the row and the centered item
        ) {
            // Top Row for the first two stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                stats.getOrNull(0)?.let { (label, value, icon) ->
                    StatItem(label = label, value = value, icon = icon)
                }
                stats.getOrNull(1)?.let { (label, value, icon) ->
                    StatItem(label = label, value = value, icon = icon)
                }
            }

            // Bottom, centered item for the third stat
            stats.getOrNull(2)?.let { (label, value, icon) ->
                StatItem(label = label, value = value, icon = icon)
            }
        }
    }
}

private fun formatYAxisLabel(value: Double): String {
    if (value < 1000) {
        return value.roundToInt().toString()
    }
    if (value < 1_00_000) {
        // Specify Locale.US for consistent formatting
        return String.format(Locale.US, "%.1fk", value / 1000).replace(".0k", "k")
    }
    if (value < 1_00_00_000) {
        // Specify Locale.US for consistent formatting
        return String.format(Locale.US, "%.1fL", value / 1_00_000).replace(".0L", "L")
    }
    // Specify Locale.US for consistent formatting
    return String.format(Locale.US, "%.1fCr", value / 1_00_00_000).replace(".0Cr", "Cr")
}
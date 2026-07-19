@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.upitracker.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.upitracker.util.CurrencyUtils
import com.canopas.lib.showcase.IntroShowcase
import com.canopas.lib.showcase.component.rememberIntroShowcaseState
import com.example.upitracker.R
import com.example.upitracker.data.Category
import com.example.upitracker.data.RecurringRule
import com.example.upitracker.ui.components.LottieEmptyState
import com.example.upitracker.ui.components.SpendingVelocityCard
import com.example.upitracker.ui.components.TransactionCardWithMenu
import com.example.upitracker.ui.components.expressive.ExpressiveSectionHeader
import com.example.upitracker.ui.components.expressive.ExpressiveTopBar
import com.example.upitracker.util.ExpressiveTokens
import com.example.upitracker.util.expressiveHeroGradient
import com.example.upitracker.util.OnboardingPreference
import com.example.upitracker.util.animateEnter
import com.example.upitracker.util.parseColor
import com.example.upitracker.viewmodel.MainViewModel
import com.example.upitracker.viewmodel.TransactionHistoryItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentMonthExpensesScreen(
    mainViewModel: MainViewModel,
    onViewAllClick: () -> Unit,
    onViewInsightsClick: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showAppIntro by remember { mutableStateOf(false) }

    val isTourCompleted by OnboardingPreference.isTourCompletedFlow(context)
        .collectAsState(initial = true)

    val velocityState by mainViewModel.spendingVelocityState.collectAsState()
    val currentMonthExpensesTotal by mainViewModel.currentMonthTotalExpenses.collectAsState()
    val currentMonthIncomeTotal by mainViewModel.currentMonthTotalIncome.collectAsState()
    val recentTransactions by mainViewModel.currentMonthExpenseItems.collectAsState()
    val recurringRules by mainViewModel.recurringRules.collectAsState()
    val isImporting by mainViewModel.isImportingSms.collectAsState()
    val isRefreshingSmsArchive by mainViewModel.isRefreshingSmsArchive.collectAsState()
    val allCategories by mainViewModel.allCategories.collectAsState()
    val isDashboardLoading by mainViewModel.isDashboardLoading.collectAsState()
    val latestBankBalances by mainViewModel.latestBankBalances.collectAsState()

    val smsSyncState by mainViewModel.smsSyncProgress.collectAsState()
    val isSyncingAllSms = smsSyncState.isSyncing
    val isSmsSyncing = isImporting || isRefreshingSmsArchive
    val smsSyncProgressFloat = smsSyncState.percentage

    val pullRefreshState = rememberPullToRefreshState()
    val scrollState = rememberScrollState()

    val transactionCount = remember(recentTransactions) {
        recentTransactions.filterIsInstance<TransactionHistoryItem>().size
    }

    val introShowcaseState = rememberIntroShowcaseState()

    LaunchedEffect(isTourCompleted, isDashboardLoading) {
        if (!isTourCompleted && !isDashboardLoading) {
            delay(1000.milliseconds) // Safe delay to allow layout coordinates to attach and settle
            showAppIntro = true
        } else {
            showAppIntro = false
        }
    }

    LaunchedEffect(introShowcaseState.currentTargetIndex) {
        if (showAppIntro && !isDashboardLoading) {
            if (introShowcaseState.currentTargetIndex == 1) {
                scrollState.animateScrollTo(scrollState.maxValue)
            } else if (introShowcaseState.currentTargetIndex == 0) {
                scrollState.animateScrollTo(0)
            }
        }
    }

    IntroShowcase(
        showIntroShowCase = showAppIntro && !isDashboardLoading,
        state = introShowcaseState,
        onShowCaseCompleted = {
            showAppIntro = false
            coroutineScope.launch {
                OnboardingPreference.setTourCompleted(context, true)
            }
        }
    ) {
        Scaffold(
            modifier = modifier,
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0),
            topBar = {
                ExpressiveTopBar(
                    title = "Today",
                    subtitle = "Your money, as it moves",
                    actions = {
                        TextButton(
                            onClick = onViewInsightsClick,
                            shape = ExpressiveTokens.corners.extraLarge,
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = "Insights",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Insights",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            // Root scaffolding box container wrapper allows for absolute structural overlay elements
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                PullToRefreshBox(
                    modifier = Modifier.fillMaxSize(),
                    isRefreshing = isSmsSyncing,
                    onRefresh = onRefresh,
                    state = pullRefreshState,
                    indicator = {
                        PullToRefreshDefaults.Indicator(
                            state = pullRefreshState,
                            isRefreshing = isSmsSyncing,
                            modifier = Modifier.align(Alignment.TopCenter),
                            color = MaterialTheme.colorScheme.primary,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    }
                ) {
                    if (isDashboardLoading) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        val dragPushOffset = remember(pullRefreshState.distanceFraction) {
                            derivedStateOf { (pullRefreshState.distanceFraction * 80).dp }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .graphicsLayer { translationY = dragPushOffset.value.toPx() }
                                .padding(
                                    start = ExpressiveTokens.spacing.lg,
                                    top = ExpressiveTokens.spacing.lg,
                                    end = ExpressiveTokens.spacing.lg,
                                    bottom = 120.dp
                                ),
                            verticalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.lg)
                        ) {
                            TotalExpensesHeroCard(
                                total = currentMonthExpensesTotal,
                                income = currentMonthIncomeTotal,
                                transactionCount = transactionCount,
                                modifier = Modifier.introShowCaseTarget(
                                    index = 0,
                                    content = {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "Monthly Snapshot",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Text(
                                                text = "Swipe down from the top to import old SMS messages. This card shows your total spending for the current month.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.White
                                            )
                                        }
                                    }
                                )
                            )

                            MonthlyQuickStatsSection(
                                transactionCount = transactionCount,
                                totalSpent = currentMonthExpensesTotal
                            )
                            
                            if (velocityState.totalBudget > 0) {
                                SpendingVelocityCard(
                                    totalBudget = velocityState.totalBudget,
                                    totalSpent = velocityState.totalSpent,
                                    daysRemaining = velocityState.daysRemaining
                                )
                            }

                            BankBalancesSection(balances = latestBankBalances)

                            UpcomingPaymentsSection(rules = recurringRules)

                            RecentTransactionsSection(
                                recentTransactions = recentTransactions,
                                allCategories = allCategories,
                                mainViewModel = mainViewModel,
                                onViewAllClick = onViewAllClick,
                                onRefresh = onRefresh,
                                isSyncing = isSmsSyncing,
                                modifier = Modifier.introShowCaseTarget(
                                    index = 1,
                                    content = {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "Full History",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Text(
                                                text = "Tap View All to see your complete transaction history with sorting and filtering options.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.White
                                            )
                                        }
                                    }
                                )
                            )
                        }
                    }
                }

                // ✨ GLOBAL PROGRESS OVERLAY CARD: Dynamically floats above content layers during a heavy parse execution
                if (isSyncingAllSms) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Synchronizing SMS Archive...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${(smsSyncProgressFloat * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            LinearProgressIndicator(
                                progress = { smsSyncProgressFloat },
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                strokeCap = StrokeCap.Round
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TotalExpensesHeroCard(
    total: Double,
    income: Double,
    transactionCount: Int,
    modifier: Modifier = Modifier
) {
    val currencyFormatter = remember {
        CurrencyUtils.getRupeeFormatter()
    }
    val currentMonthLabel = remember {
        SimpleDateFormat("MMMM", Locale.getDefault()).format(Date())
    }

    val heroShape = ExpressiveTokens.corners.hero

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 4.dp.value,
        targetValue = 8.dp.value,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseRadius"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = heroShape,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(expressiveHeroGradient(), heroShape)
                .padding(
                    horizontal = ExpressiveTokens.spacing.xl,
                    vertical = ExpressiveTokens.spacing.xl
                )
        ) {
            Column {
                Text(
                    text = "SPENT IN ${currentMonthLabel.uppercase()}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.5.sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(ExpressiveTokens.spacing.xs))

                AnimatedContent(
                    targetState = total,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInVertically { height -> height } togetherWith
                                    slideOutVertically { height -> -height }
                        } else {
                            slideInVertically { height -> -height } togetherWith
                                    slideOutVertically { height -> height }
                        }
                    },
                    label = "totalAmountAnimation"
                ) { targetTotal ->
                    Text(
                        text = currencyFormatter.format(targetTotal),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = 40.sp,
                            letterSpacing = (-1.5).sp
                        ),
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        lineHeight = 44.sp
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Across $transactionCount transactions",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(100.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowDownward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "₹" + String.format(Locale.US, "%,.0f", total),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                    Row(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(100.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowUpward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "₹" + String.format(Locale.US, "%,.0f", income),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }

                // Sparkline Canvas with a live pulsing dot at the end
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .padding(top = 8.dp)
                ) {
                    val width = size.width
                    val height = size.height

                    val path = Path().apply {
                        moveTo(0f, height * 0.85f)
                        cubicTo(
                            width * 0.15f, height * 0.8f,
                            width * 0.25f, height * 0.7f,
                            width * 0.4f, height * 0.55f
                        )
                        cubicTo(
                            width * 0.55f, height * 0.45f,
                            width * 0.65f, height * 0.65f,
                            width * 0.8f, height * 0.35f
                        )
                        cubicTo(
                            width * 0.9f, height * 0.15f,
                            width * 0.95f, height * 0.25f,
                            width, height * 0.1f
                        )
                    }

                    // Stroke
                    drawPath(
                        path = path,
                        color = Color.White.copy(alpha = 0.6f),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Fill gradient under path
                    val fillPath = Path().apply {
                        addPath(path)
                        lineTo(width, height)
                        lineTo(0f, height)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.White.copy(alpha = 0.18f), Color.Transparent)
                        )
                    )

                    // Pulse glow circle
                    drawCircle(
                        color = Color.White.copy(alpha = pulseAlpha),
                        radius = pulseRadius.dp.toPx(),
                        center = Offset(width, height * 0.1f)
                    )

                    // Center solid circle
                    drawCircle(
                        color = Color.White,
                        radius = 4.dp.toPx(),
                        center = Offset(width, height * 0.1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthlyQuickStatsSection(
    transactionCount: Int,
    totalSpent: Double
) {
    val currencyFormatter = remember {
        CurrencyUtils.getRupeeFormatter()
    }

    val averageAmount = if (transactionCount > 0) {
        totalSpent / transactionCount
    } else {
        0.0
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.md)
    ) {
        MonthlyStatCard(
            title = "Transactions",
            value = transactionCount.toString(),
            icon = Icons.AutoMirrored.Filled.ReceiptLong,
            modifier = Modifier.weight(1f)
        )

        MonthlyStatCard(
            title = "Average Spend",
            value = currencyFormatter.format(averageAmount),
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MonthlyStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = ExpressiveTokens.corners.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.88f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = ExpressiveTokens.elevation.card
        )
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = ExpressiveTokens.spacing.md,
                vertical = ExpressiveTokens.spacing.md
            )
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(ExpressiveTokens.corners.small)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(ExpressiveTokens.spacing.sm))

            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun UpcomingPaymentsSection(
    rules: List<RecurringRule>
) {
    val upcomingRules = rules.take(3)

    Column {
        ExpressiveSectionHeader(
            title = "Upcoming Payments",
            subtitle = if (upcomingRules.isEmpty()) {
                "No upcoming recurring payments"
            } else {
                "Next recurring payments coming up"
            }
        )

        Spacer(modifier = Modifier.height(ExpressiveTokens.spacing.sm))

        if (upcomingRules.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = ExpressiveTokens.corners.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = ExpressiveTokens.elevation.card
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = ExpressiveTokens.spacing.lg,
                            vertical = ExpressiveTokens.spacing.lg
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = "No upcoming payments",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(ExpressiveTokens.spacing.md))

                    Column {
                        Text(
                            text = "None upcoming",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Add recurring payments from Budgets to track bills here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.sm)
            ) {
                upcomingRules.forEach { rule ->
                    UpcomingPaymentRow(rule = rule)
                }
            }
        }
    }
}

@Composable
private fun UpcomingPaymentRow(
    rule: RecurringRule
) {
    val currencyFormatter = remember {
        CurrencyUtils.getRupeeFormatter()
    }

    val dateFormat = remember {
        SimpleDateFormat("dd MMM", Locale.getDefault())
    }

    val dueText = remember(rule.nextDueDate) {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val dueDate = Calendar.getInstance().apply {
            timeInMillis = rule.nextDueDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val diff = dueDate.timeInMillis - today.timeInMillis
        val daysUntil = TimeUnit.MILLISECONDS.toDays(diff)

        when {
            daysUntil < 0 -> "Overdue"
            daysUntil == 0L -> "Today"
            daysUntil == 1L -> "Tomorrow"
            else -> "in $daysUntil days"
        }
    }

    ListItem(
        modifier = Modifier.clip(ExpressiveTokens.corners.large),
        leadingContent = {
            Icon(
                imageVector = Icons.Filled.CalendarMonth,
                contentDescription = "Upcoming payment",
                tint = MaterialTheme.colorScheme.primary
            )
        },
        headlineContent = {
            Text(
                text = rule.description,
                fontWeight = FontWeight.SemiBold
            )
        },
        supportingContent = {
            Text(
                text = "${currencyFormatter.format(rule.amount)} • Due ${
                    dateFormat.format(Date(rule.nextDueDate))
                }"
            )
        },
        trailingContent = {
            Text(
                text = dueText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    )
}

@Composable
private fun RecentTransactionsSection(
    recentTransactions: List<*>,
    allCategories: List<Category>,
    mainViewModel: MainViewModel,
    onViewAllClick: () -> Unit,
    onRefresh: () -> Unit,
    isSyncing: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        RecentTransactionsHeader(onViewAllClick = onViewAllClick)

        Spacer(modifier = Modifier.height(ExpressiveTokens.spacing.sm))

        if (recentTransactions.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = ExpressiveTokens.spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                LottieEmptyState(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    message = "No transactions this month yet.",
                    lottieResourceId = R.raw.empty_box_animation
                )
                
                Spacer(modifier = Modifier.height(ExpressiveTokens.spacing.md))
                
                androidx.compose.material3.Button(
                    onClick = onRefresh,
                    enabled = !isSyncing,
                    shape = ExpressiveTokens.corners.large
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Sync",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isSyncing) "Syncing..." else "Sync SMS")
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.md)
            ) {
                recentTransactions.take(3).forEachIndexed { index, item ->
                    if (item is TransactionHistoryItem) {
                        val transaction = item.transaction

                        val categoryDetails = remember(transaction.category, allCategories) {
                            allCategories.find { category ->
                                category.name.equals(
                                    transaction.category,
                                    ignoreCase = true
                                )
                            }
                        }

                        val categoryColor = remember(categoryDetails) {
                            parseColor(categoryDetails?.colorHex ?: "#808080")
                        }

                        TransactionCardWithMenu(
                            modifier = Modifier.animateEnter(index),
                            transaction = transaction,
                            isSelectionMode = false,
                            isSelected = false,
                            showCheckbox = false,
                            onToggleSelection = {},
                            onShowDetails = {},
                            onDelete = { mainViewModel.deleteTransaction(it) },
                            onArchiveAction = {
                                mainViewModel.toggleTransactionArchiveStatus(
                                    it,
                                    true
                                )
                            },
                            archiveActionText = "Archive",
                            archiveActionIcon = Icons.Default.Archive,
                            categoryColor = categoryColor,
                            onCategoryClick = { categoryName ->
                                mainViewModel.filterHistoryByCategory(categoryName)
                                onViewAllClick()
                            }
                        )
                    }
                }
            }
            }
        }
    }

@Composable
private fun RecentTransactionsHeader(
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = ExpressiveTokens.spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Recent Transactions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Latest activity from this month",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        TextButton(onClick = onViewAllClick) {
            Text("View All")

            Spacer(modifier = Modifier.width(4.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "View All Transactions",
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun BankBalancesSection(balances: List<com.example.upitracker.data.TransactionDao.BankBalance>) {
    val currencyFormatter = remember {
        CurrencyUtils.getRupeeFormatter()
    }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        ExpressiveSectionHeader(
            title = "Bank Balances",
            subtitle = "Latest known balances from SMS"
        )
        
        Spacer(modifier = Modifier.height(ExpressiveTokens.spacing.sm))
        
        Column(verticalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.sm)) {
            if (balances.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ExpressiveTokens.corners.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(ExpressiveTokens.spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.xs)
                    ) {
                        Text(
                            text = "No Bank Balances Detected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Bank balances are automatically updated when you receive transaction SMS messages containing your available balance.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ExpressiveTokens.corners.extraLarge,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.88f)
                    )
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        balances.forEachIndexed { index, balance ->
                            if (index > 0) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(ExpressiveTokens.corners.medium)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = balance.bankName.firstOrNull()?.uppercase() ?: "B",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(ExpressiveTokens.spacing.md))
                                    Text(
                                        text = balance.bankName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = currencyFormatter.format(balance.latestBalance),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        val totalBalance = remember(balances) { balances.sumOf { it.latestBalance } }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total balance",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = currencyFormatter.format(totalBalance),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

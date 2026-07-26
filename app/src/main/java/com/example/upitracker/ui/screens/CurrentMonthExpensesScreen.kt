@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.upitracker.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.upitracker.ui.components.BankIcon
import com.example.upitracker.ui.components.SpendingVelocityCard
import com.example.upitracker.ui.components.expressive.ExpressiveSectionHeader
import com.example.upitracker.ui.components.expressive.ExpressiveTopBar
import com.example.upitracker.util.ExpressiveTokens
import com.example.upitracker.util.expressiveHeroGradient
import com.example.upitracker.util.OnboardingPreference
import com.example.upitracker.util.animateEnter
import com.example.upitracker.util.parseColor
import com.example.upitracker.viewmodel.MainViewModel
import com.example.upitracker.viewmodel.HistoryListItem
import com.example.upitracker.viewmodel.SummaryHistoryItem
import com.example.upitracker.viewmodel.TransactionHistoryItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentMonthExpensesScreen(
    mainViewModel: MainViewModel,
    onViewAllClick: () -> Unit,
    onViewBudgetsClick: () -> Unit,
    onViewUncategorizedClick: () -> Unit,
    onViewInsightsClick: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showAppIntro by remember { mutableStateOf(false) }

    val isTourCompleted by OnboardingPreference.isTourCompletedFlow(context)
        .collectAsStateWithLifecycle(initialValue = true)

    val velocityState by mainViewModel.spendingVelocityState.collectAsStateWithLifecycle()
    val currentMonthExpensesTotal by mainViewModel.currentMonthTotalExpenses.collectAsStateWithLifecycle()
    val recentTransactions by mainViewModel.currentMonthExpenseItems.collectAsStateWithLifecycle()
    val recurringRules by mainViewModel.recurringRules.collectAsStateWithLifecycle()
    val detectedSubscriptions by mainViewModel.detectedSubscriptions.collectAsStateWithLifecycle()
    val subscriptionPriceChanges by mainViewModel.subscriptionPriceChanges.collectAsStateWithLifecycle()
    val isImporting by mainViewModel.isImportingSms.collectAsStateWithLifecycle()
    val isRefreshingSmsArchive by mainViewModel.isRefreshingSmsArchive.collectAsStateWithLifecycle()
    val allCategories by mainViewModel.allCategories.collectAsStateWithLifecycle()
    val isDashboardLoading by mainViewModel.isDashboardLoading.collectAsStateWithLifecycle()

    val smsSyncState by mainViewModel.smsSyncProgress.collectAsStateWithLifecycle()
    val isSyncingAllSms = smsSyncState.isSyncing
    val isSmsSyncing = isImporting || isRefreshingSmsArchive
    val smsSyncProgressFloat = smsSyncState.percentage

    val pullRefreshState = rememberPullToRefreshState()
    val scrollState = rememberScrollState()
    val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDetailSheet by remember { mutableStateOf(false) }

    val transactionCount = remember(recentTransactions) {
        recentTransactions.sumOf { item ->
            when (item) {
                is TransactionHistoryItem -> 1
                is SummaryHistoryItem -> item.summary.transactionCount
            }
        }
    }
    val todayRange = remember {
        val start = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        start until (start + TimeUnit.DAYS.toMillis(1))
    }
    val todaySpend = remember(recentTransactions, todayRange) {
        recentTransactions
            .filter { it.displayDate in todayRange }
            .sumOf { item ->
                when (item) {
                    is TransactionHistoryItem -> item.transaction.amount
                    is SummaryHistoryItem -> item.summary.totalAmount
                }
            }
    }
    val largestTransaction = remember(recentTransactions) {
        recentTransactions
            .filterIsInstance<TransactionHistoryItem>()
            .maxByOrNull { it.transaction.amount }
            ?.transaction
    }
    val activeSpendingDays = remember(recentTransactions) {
        val calendar = Calendar.getInstance()
        recentTransactions
            .map { item ->
                calendar.timeInMillis = item.displayDate
                calendar.get(Calendar.YEAR) to calendar.get(Calendar.DAY_OF_YEAR)
            }
            .distinct()
            .size
    }
    val uncategorizedCount = remember(recentTransactions) {
        recentTransactions
            .filterIsInstance<TransactionHistoryItem>()
            .count { item ->
                item.transaction.category.isNullOrBlank()
            }
    }
    val attentionItems = remember(
        subscriptionPriceChanges,
        detectedSubscriptions,
        recurringRules,
        velocityState
    ) {
        buildHomeAttentionItems(
            subscriptionPriceChanges = subscriptionPriceChanges,
            detectedSubscriptions = detectedSubscriptions,
            recurringRules = recurringRules,
            hasOverallBudget = velocityState.hasOverallBudget,
            totalBudget = velocityState.totalBudget,
            totalSpent = velocityState.totalSpent
        )
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

    if (showDetailSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showDetailSheet = false
                mainViewModel.selectTransaction(null)
            },
            sheetState = detailSheetState
        ) {
            TransactionDetailSheetContent(
                mainViewModel = mainViewModel,
                onDismiss = {
                    coroutineScope.launch {
                        detailSheetState.hide()
                        showDetailSheet = false
                        mainViewModel.selectTransaction(null)
                    }
                }
            )
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
                        val dragPushOffset = remember(pullRefreshState) {
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
                                transactionCount = transactionCount,
                                todaySpend = todaySpend,
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

                            MoneyPulseSection(
                                activeSpendingDays = activeSpendingDays,
                                largestTransaction = largestTransaction,
                                uncategorizedCount = uncategorizedCount,
                                allCategories = allCategories,
                                onUncategorizedClick = onViewUncategorizedClick
                            )

                            if (attentionItems.isNotEmpty()) {
                                HomeAttentionSection(
                                    items = attentionItems,
                                    onBudgetsClick = onViewBudgetsClick
                                )
                            }

                            if (velocityState.hasOverallBudget) {
                                SpendingVelocityCard(
                                    totalBudget = velocityState.totalBudget,
                                    totalSpent = velocityState.totalSpent,
                                    daysRemaining = velocityState.daysRemaining,
                                    upcomingBillsTotal = velocityState.upcomingBillsTotal,
                                    currentDayOfMonth = velocityState.currentDayOfMonth,
                                    totalDaysInMonth = velocityState.totalDaysInMonth
                                )
                            }

                            if (recurringRules.isNotEmpty()) {
                                UpcomingPaymentsSection(rules = recurringRules)
                            }

                            RecentTransactionsSection(
                                recentTransactions = recentTransactions,
                                allCategories = allCategories,
                                onViewAllClick = onViewAllClick,
                                onTransactionClick = { transactionId ->
                                    mainViewModel.selectTransaction(transactionId)
                                    showDetailSheet = true
                                },
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
    transactionCount: Int,
    todaySpend: Double,
    modifier: Modifier = Modifier
) {
    val currencyFormatter = remember {
        CurrencyUtils.getRupeeFormatter()
    }
    val currentMonthLabel = remember {
        SimpleDateFormat("MMMM", Locale.getDefault()).format(Date())
    }

    val heroShape = ExpressiveTokens.corners.hero

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

                Spacer(modifier = Modifier.height(ExpressiveTokens.spacing.md))

                Surface(
                    shape = ExpressiveTokens.corners.extraLarge,
                    color = Color.White.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Today",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.72f)
                        )
                        Text(
                            text = currencyFormatter.format(todaySpend),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

private enum class HomeAttentionKind {
    PRICE_CHANGE,
    BUDGET_RISK,
    OVERDUE_PAYMENT,
    SUBSCRIPTION_FOUND
}

private data class HomeAttentionItem(
    val title: String,
    val subtitle: String,
    val kind: HomeAttentionKind
)

private fun buildHomeAttentionItems(
    subscriptionPriceChanges: List<MainViewModel.SubscriptionPriceChange>,
    detectedSubscriptions: List<MainViewModel.DetectedSubscription>,
    recurringRules: List<RecurringRule>,
    hasOverallBudget: Boolean,
    totalBudget: Double,
    totalSpent: Double
): List<HomeAttentionItem> {
    val currencyFormatter = CurrencyUtils.getRupeeFormatter()
    val items = mutableListOf<HomeAttentionItem>()

    subscriptionPriceChanges.firstOrNull()?.let { change ->
        items += HomeAttentionItem(
            title = "${change.merchant.toDisplayName()} changed price",
            subtitle = "${currencyFormatter.format(change.previousAmount)} to ${
                currencyFormatter.format(change.latestAmount)
            }",
            kind = HomeAttentionKind.PRICE_CHANGE
        )
    }

    if (hasOverallBudget && totalBudget <= 0.0) {
        items += HomeAttentionItem(
            title = "No overall budget remaining",
            subtitle = "The previous rollover deficit used this month’s limit",
            kind = HomeAttentionKind.BUDGET_RISK
        )
    } else if (hasOverallBudget) {
        val usage = totalSpent / totalBudget
        if (usage >= 0.80) {
            items += HomeAttentionItem(
                title = if (usage >= 1.0) "Monthly budget exceeded" else "Budget needs attention",
                subtitle = "${(usage * 100).toInt()}% used with ${
                    currencyFormatter.format((totalBudget - totalSpent).coerceAtLeast(0.0))
                } remaining",
                kind = HomeAttentionKind.BUDGET_RISK
            )
        }
    }

    val now = System.currentTimeMillis()
    recurringRules.count { it.nextDueDate < now }.takeIf { it > 0 }?.let { overdueCount ->
        items += HomeAttentionItem(
            title = "$overdueCount overdue ${if (overdueCount == 1) "payment" else "payments"}",
            subtitle = "Review recurring payment dates",
            kind = HomeAttentionKind.OVERDUE_PAYMENT
        )
    }

    detectedSubscriptions.firstOrNull()?.let { subscription ->
        items += HomeAttentionItem(
            title = "Possible subscription found",
            subtitle = "${subscription.merchant.toDisplayName()} • ${
                currencyFormatter.format(subscription.amount)
            }",
            kind = HomeAttentionKind.SUBSCRIPTION_FOUND
        )
    }

    return items.take(4)
}

private fun String.toDisplayName(): String {
    return trim()
        .lowercase(Locale.getDefault())
        .replaceFirstChar { character ->
            if (character.isLowerCase()) character.titlecase(Locale.getDefault()) else character.toString()
        }
}

@Composable
private fun MoneyPulseSection(
    activeSpendingDays: Int,
    largestTransaction: com.example.upitracker.data.Transaction?,
    uncategorizedCount: Int,
    allCategories: List<Category>,
    onUncategorizedClick: () -> Unit
) {
    val currencyFormatter = remember { CurrencyUtils.getRupeeFormatter() }
    val largestCategoryColor = remember(largestTransaction?.category, allCategories) {
        val category = allCategories.firstOrNull {
            it.name.equals(largestTransaction?.category, ignoreCase = true)
        }
        parseColor(category?.colorHex ?: "#808080")
    }

    Column(verticalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.sm)) {
        ExpressiveSectionHeader(
            title = "Money pulse",
            subtitle = "What is shaping this month"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.sm)
        ) {
            PulseMetricCard(
                modifier = Modifier
                    .weight(0.88f)
                    .height(132.dp),
                eyebrow = "ACTIVE DAYS",
                value = "$activeSpendingDays",
                supportingText = if (activeSpendingDays == 1) "Day with spending" else "Days with spending",
                icon = Icons.Filled.CalendarMonth,
                accentColor = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier = Modifier
                    .weight(1.12f)
                    .height(132.dp),
                shape = ExpressiveTokens.corners.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(ExpressiveTokens.spacing.md),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "BIGGEST",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        largestTransaction?.let { transaction ->
                            BankIcon(
                                bankName = transaction.bankName,
                                fallbackLabel = transaction.category,
                                fallbackColor = largestCategoryColor,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = largestTransaction?.let {
                                currencyFormatter.format(it.amount)
                            } ?: "—",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Text(
                            text = largestTransaction?.senderOrReceiver
                                ?.takeIf { it.isNotBlank() }
                                ?: "No transaction yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (uncategorizedCount > 0) {
                        Modifier.clickable(onClick = onUncategorizedClick)
                    } else {
                        Modifier
                    }
                ),
            shape = ExpressiveTokens.corners.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = ExpressiveTokens.corners.medium,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Label,
                        contentDescription = null,
                        modifier = Modifier.padding(9.dp).size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (uncategorizedCount) {
                            0 -> {
                                "Everything is categorised"
                            }
                            1 -> {
                                "1 needs categorising"
                            }
                            else -> {
                                "$uncategorizedCount need categorising"
                            }
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (uncategorizedCount == 0) {
                            "Your spending breakdown is up to date"
                        } else {
                            "Clean up your data for sharper insights"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (uncategorizedCount > 0) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "View uncategorized transactions",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun PulseMetricCard(
    modifier: Modifier,
    eyebrow: String,
    value: String,
    supportingText: String,
    icon: ImageVector,
    accentColor: Color
) {
    Card(
        modifier = modifier,
        shape = ExpressiveTokens.corners.large,
        colors = CardDefaults.cardColors(
            containerColor = accentColor.copy(alpha = 0.12f)
        ),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.24f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(ExpressiveTokens.spacing.md),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = eyebrow,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = accentColor
                )
            }
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun HomeAttentionSection(
    items: List<HomeAttentionItem>,
    onBudgetsClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.sm)) {
        ExpressiveSectionHeader(
            title = "Needs attention",
            subtitle = "Useful signals from your transactions"
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = ExpressiveTokens.corners.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            items.forEachIndexed { index, item ->
                HomeAttentionRow(
                    item = item,
                    onClick = onBudgetsClick
                )
                if (index < items.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeAttentionRow(
    item: HomeAttentionItem,
    onClick: () -> Unit
) {
    val accentColor = when (item.kind) {
        HomeAttentionKind.PRICE_CHANGE -> MaterialTheme.colorScheme.secondary
        HomeAttentionKind.BUDGET_RISK -> MaterialTheme.colorScheme.error
        HomeAttentionKind.OVERDUE_PAYMENT -> MaterialTheme.colorScheme.error
        HomeAttentionKind.SUBSCRIPTION_FOUND -> MaterialTheme.colorScheme.tertiary
    }
    val icon = when (item.kind) {
        HomeAttentionKind.PRICE_CHANGE -> Icons.AutoMirrored.Filled.TrendingUp
        HomeAttentionKind.BUDGET_RISK -> Icons.Filled.Warning
        HomeAttentionKind.OVERDUE_PAYMENT -> Icons.Filled.Schedule
        HomeAttentionKind.SUBSCRIPTION_FOUND -> Icons.Filled.AutoAwesome
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = ExpressiveTokens.corners.medium,
            color = accentColor.copy(alpha = 0.14f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(9.dp).size(18.dp),
                tint = accentColor
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
    recentTransactions: List<HistoryListItem>,
    allCategories: List<Category>,
    onViewAllClick: () -> Unit,
    onTransactionClick: (Int) -> Unit,
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = ExpressiveTokens.corners.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                recentTransactions.take(5).forEachIndexed { index, item ->
                    val categoryColor = remember(item, allCategories) {
                        val categoryName = (item as? TransactionHistoryItem)
                            ?.transaction
                            ?.category
                        val category = allCategories.firstOrNull {
                            it.name.equals(categoryName, ignoreCase = true)
                        }
                        parseColor(category?.colorHex ?: "#808080")
                    }

                    HomeRecentActivityRow(
                        item = item,
                        categoryColor = categoryColor,
                        onClick = (item as? TransactionHistoryItem)?.let { transactionItem ->
                            { onTransactionClick(transactionItem.transaction.id) }
                        },
                        modifier = Modifier.animateEnter(index)
                    )

                    if (index < minOf(4, recentTransactions.lastIndex)) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 58.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeRecentActivityRow(
    item: HistoryListItem,
    categoryColor: Color,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val currencyFormatter = remember { CurrencyUtils.getRupeeFormatter() }
    val dateFormatter = remember { SimpleDateFormat("dd MMM, h:mm a", Locale.getDefault()) }

    val title: String
    val supportingText: String
    val amount: Double
    val bankName: String?
    val fallbackLabel: String?

    when (item) {
        is TransactionHistoryItem -> {
            val transaction = item.transaction
            title = transaction.senderOrReceiver
                .takeIf { it.isNotBlank() && it != "Manual Entry" }
                ?: transaction.description
            supportingText = buildString {
                append(dateFormatter.format(Date(transaction.date)))
                transaction.category?.takeIf { it.isNotBlank() }?.let {
                    append(" • ")
                    append(it)
                }
            }
            amount = transaction.amount
            bankName = transaction.bankName
            fallbackLabel = transaction.category
        }

        is SummaryHistoryItem -> {
            val summary = item.summary
            title = "${summary.bank} UPI Lite"
            supportingText = "${summary.transactionCount} transactions • ${
                dateFormatter.format(Date(summary.date))
            }"
            amount = summary.totalAmount
            bankName = summary.bank
            fallbackLabel = "UPI Lite"
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BankIcon(
            bankName = bankName,
            fallbackLabel = fallbackLabel,
            fallbackColor = categoryColor,
            modifier = Modifier.size(34.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "-${currencyFormatter.format(amount)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
            maxLines = 1
        )
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

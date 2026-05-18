@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.upitracker.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.canopas.lib.showcase.IntroShowcase
import com.example.upitracker.R
import com.example.upitracker.data.Category
import com.example.upitracker.data.RecurringRule
import com.example.upitracker.ui.components.LottieEmptyState
import com.example.upitracker.ui.components.SpendingVelocityCard
import com.example.upitracker.ui.components.TransactionCardWithMenu
import com.example.upitracker.ui.components.expressive.ExpressiveSectionHeader
import com.example.upitracker.ui.components.expressive.ExpressiveTopBar
import com.example.upitracker.util.ExpressiveTokens
import com.example.upitracker.util.OnboardingPreference
import com.example.upitracker.util.animateEnter
import com.example.upitracker.util.getCategoryIcon
import com.example.upitracker.util.parseColor
import com.example.upitracker.viewmodel.MainViewModel
import com.example.upitracker.viewmodel.TransactionHistoryItem
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentMonthExpensesScreen(
    mainViewModel: MainViewModel,
    onViewAllClick: () -> Unit,
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
    val recentTransactions by mainViewModel.currentMonthExpenseItems.collectAsState()
    val recurringRules by mainViewModel.recurringRules.collectAsState()
    val isImporting by mainViewModel.isImportingSms.collectAsState()
    val allCategories by mainViewModel.allCategories.collectAsState()
    val isDashboardLoading by mainViewModel.isDashboardLoading.collectAsState()

    val pullRefreshState = rememberPullToRefreshState()
    val listState = rememberLazyListState()

    val transactionCount = remember(recentTransactions) {
        recentTransactions.filterIsInstance<TransactionHistoryItem>().size
    }

    LaunchedEffect(isTourCompleted) {
        if (!isTourCompleted) {
            showAppIntro = true
        }
    }

    IntroShowcase(
        showIntroShowCase = showAppIntro,
        onShowCaseCompleted = {
            showAppIntro = false
            coroutineScope.launch {
                OnboardingPreference.setTourCompleted(context, true)
            }
        }
    ) {
        Scaffold(
            modifier = modifier,
            contentWindowInsets = WindowInsets(0),
            topBar = {
                ExpressiveTopBar(
                    title = "UPI Expense Tracker"
                )
            }
        ) { paddingValues ->
            PullToRefreshBox(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                isRefreshing = isImporting,
                onRefresh = onRefresh,
                state = pullRefreshState,
                indicator = {
                    if (isImporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    }
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
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = ExpressiveTokens.spacing.lg,
                            top = ExpressiveTokens.spacing.lg,
                            end = ExpressiveTokens.spacing.lg,
                            bottom = 120.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.lg)
                    ) {
                        item {
                            TotalExpensesHeroCard(
                                total = currentMonthExpensesTotal,
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
                        }

                        item {
                            MonthlyQuickStatsSection(
                                transactionCount = transactionCount,
                                upcomingCount = recurringRules.size,
                                totalSpent = currentMonthExpensesTotal
                            )
                        }

                        if (velocityState.totalBudget > 0) {
                            item {
                                SpendingVelocityCard(
                                    totalBudget = velocityState.totalBudget,
                                    totalSpent = velocityState.totalSpent,
                                    daysRemaining = velocityState.daysRemaining
                                )
                            }
                        }

                        if (recurringRules.isNotEmpty()) {
                            item {
                                UpcomingPaymentsSection(rules = recurringRules)
                            }
                        }

                        item {
                            RecentTransactionsSection(
                                recentTransactions = recentTransactions,
                                allCategories = allCategories,
                                mainViewModel = mainViewModel,
                                onViewAllClick = onViewAllClick,
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
            }
        }
    }
}

@Composable
private fun TotalExpensesHeroCard(
    total: Double,
    modifier: Modifier = Modifier
) {
    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(
            Locale.Builder()
                .setLanguage("en")
                .setRegion("IN")
                .build()
        )
    }

    val heroShape = RoundedCornerShape(
        topStart = 28.dp,
        topEnd = 28.dp,
        bottomStart = 28.dp,
        bottomEnd = 10.dp
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = heroShape,
        elevation = CardDefaults.cardElevation(
            defaultElevation = ExpressiveTokens.elevation.card
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = ExpressiveTokens.spacing.xl,
                    vertical = ExpressiveTokens.spacing.xl
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Current Month's Expenses",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.height(ExpressiveTokens.spacing.sm))

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
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        lineHeight = 40.sp
                    )
                }

                Spacer(modifier = Modifier.height(ExpressiveTokens.spacing.xs))

                Text(
                    text = "Simple monthly view",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                )
            }

            Icon(
                imageVector = Icons.Filled.AccountBalanceWallet,
                contentDescription = "Monthly Expenses",
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .padding(8.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun MonthlyQuickStatsSection(
    transactionCount: Int,
    upcomingCount: Int,
    totalSpent: Double
) {
    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(
            Locale.Builder()
                .setLanguage("en")
                .setRegion("IN")
                .build()
        )
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
            title = "Average",
            value = currencyFormatter.format(averageAmount),
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            modifier = Modifier.weight(1f)
        )

        MonthlyStatCard(
            title = "Upcoming",
            value = upcomingCount.toString(),
            icon = Icons.Filled.Schedule,
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
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
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )

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

    if (upcomingRules.isNotEmpty()) {
        Column {
            ExpressiveSectionHeader(
                title = "Upcoming Payments",
                subtitle = "Next recurring payments coming up"
            )

            Spacer(modifier = Modifier.height(ExpressiveTokens.spacing.sm))

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
        NumberFormat.getCurrencyInstance(
            Locale.Builder()
                .setLanguage("en")
                .setRegion("IN")
                .build()
        )
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        RecentTransactionsHeader(onViewAllClick = onViewAllClick)

        Spacer(modifier = Modifier.height(ExpressiveTokens.spacing.sm))

        if (recentTransactions.isEmpty()) {
            LottieEmptyState(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                message = "No transactions this month yet.",
                lottieResourceId = R.raw.empty_box_animation
            )
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

                        val categoryIcon = getCategoryIcon(categoryDetails)

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
                            categoryIcon = categoryIcon,
                            onCategoryClick = { categoryName ->
                                mainViewModel.toggleCategoryFilter(categoryName)
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
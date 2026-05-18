@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.example.upitracker.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.upitracker.R
import com.example.upitracker.data.BudgetPeriod
import com.example.upitracker.data.RecurringRule
import com.example.upitracker.ui.components.AddEditBudgetDialog
import com.example.upitracker.ui.components.AddEditRecurringRuleDialog
import com.example.upitracker.ui.components.BudgetCard
import com.example.upitracker.ui.components.LottieEmptyState
import com.example.upitracker.ui.components.RecurringRuleCard
import com.example.upitracker.ui.components.expressive.ExpressiveTopBar
import com.example.upitracker.util.ExpressiveTokens
import com.example.upitracker.util.getCategoryIcon
import com.example.upitracker.viewmodel.BudgetStatus
import com.example.upitracker.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun BudgetScreen(
    mainViewModel: MainViewModel
) {
    val pagerState = rememberPagerState { 2 }
    val coroutineScope = rememberCoroutineScope()
    val tabTitles = listOf("Spending Budgets", "Recurring Payments")

    var showAddBudgetDialog by remember { mutableStateOf(false) }
    var budgetToEdit by remember { mutableStateOf<BudgetStatus?>(null) }

    var showAddRecurringDialog by remember { mutableStateOf(false) }
    var ruleToEdit by remember { mutableStateOf<RecurringRule?>(null) }
    var showRecurringHelpDialog by remember { mutableStateOf(false) }

    var ruleForForecast by remember { mutableStateOf<RecurringRule?>(null) }

    if (showAddBudgetDialog) {
        AddEditBudgetDialog(
            budgetStatus = null,
            onDismiss = { showAddBudgetDialog = false },
            onConfirm = { category, amount, period, allowRollover ->
                mainViewModel.addOrUpdateBudget(
                    category,
                    amount,
                    period,
                    allowRollover
                )
                showAddBudgetDialog = false
            }
        )
    }

    if (budgetToEdit != null) {
        AddEditBudgetDialog(
            budgetStatus = budgetToEdit,
            onDismiss = { budgetToEdit = null },
            onConfirm = { category, amount, period, allowRollover ->
                mainViewModel.addOrUpdateBudget(
                    category,
                    amount,
                    period,
                    allowRollover,
                    budgetToEdit!!.budgetId
                )
                budgetToEdit = null
            }
        )
    }

    if (showAddRecurringDialog) {
        AddEditRecurringRuleDialog(
            ruleToEdit = null,
            onDismiss = { showAddRecurringDialog = false },
            onConfirm = { description, amount, category, period, day ->
                mainViewModel.addRecurringRule(
                    description,
                    amount,
                    category,
                    period,
                    day
                )
                showAddRecurringDialog = false
            }
        )
    }

    if (ruleToEdit != null) {
        AddEditRecurringRuleDialog(
            ruleToEdit = ruleToEdit,
            onDismiss = { ruleToEdit = null },
            onConfirm = { description, amount, category, period, day ->
                mainViewModel.updateRecurringRule(
                    ruleToEdit!!.id,
                    description,
                    amount,
                    category,
                    period,
                    day
                )
                ruleToEdit = null
            }
        )
    }

    if (showRecurringHelpDialog) {
        AlertDialog(
            onDismissRequest = { showRecurringHelpDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = "Help"
                )
            },
            title = {
                Text("About Recurring Payments")
            },
            text = {
                Text(
                    "This feature allows you to automatically track fixed payments like subscriptions, rent, EMIs, and bills. The app creates a transaction for you on the date you specify."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showRecurringHelpDialog = false }
                ) {
                    Text("Got it")
                }
            }
        )
    }

    if (ruleForForecast != null) {
        ForecastDialog(
            rule = ruleForForecast!!,
            onDismiss = { ruleForForecast = null }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            ExpressiveTopBar(
                title = "Budgets",
                subtitle = if (pagerState.currentPage == 0) {
                    "Control category-wise spending limits"
                } else {
                    "Track subscriptions, rent, EMIs, and fixed bills"
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                modifier = Modifier.navigationBarsPadding(),
                onClick = {
                    if (pagerState.currentPage == 0) {
                        showAddBudgetDialog = true
                    } else {
                        showAddRecurringDialog = true
                    }
                },
                shape = RoundedCornerShape(18.dp),
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null
                    )
                },
                text = {
                    Text(
                        text = if (pagerState.currentPage == 0) {
                            "Add Budget"
                        } else {
                            "Add Payment"
                        }
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            PrimaryTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (index == 0) {
                                        Icons.Filled.Savings
                                    } else {
                                        Icons.Filled.EventRepeat
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleSmall
                                )

                                if (index == 1) {
                                    Spacer(modifier = Modifier.width(8.dp))

                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                        contentDescription = "Help about Recurring Payments",
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable {
                                                showRecurringHelpDialog = true
                                            },
                                        tint = if (pagerState.currentPage == index) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> {
                        BudgetList(
                            mainViewModel = mainViewModel,
                            onEditBudget = { budgetToEdit = it }
                        )
                    }

                    1 -> {
                        RecurringList(
                            mainViewModel = mainViewModel,
                            onEditRule = { ruleToEdit = it },
                            onViewForecast = { ruleForForecast = it }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetList(
    mainViewModel: MainViewModel,
    onEditBudget: (BudgetStatus) -> Unit
) {
    val budgetStatuses by mainViewModel.budgetStatuses.collectAsState()
    val allCategories by mainViewModel.allCategories.collectAsState()

    if (budgetStatuses.isEmpty()) {
        LottieEmptyState(
            modifier = Modifier
                .fillMaxSize()
                .padding(ExpressiveTokens.spacing.lg),
            message = "You haven't set any budgets yet.\nTap 'Add Budget' to create one.",
            lottieResourceId = R.raw.empty_box_animation
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = ExpressiveTokens.spacing.lg,
                top = ExpressiveTokens.spacing.lg,
                end = ExpressiveTokens.spacing.lg,
                bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.md)
        ) {
            items(
                items = budgetStatuses,
                key = { it.budgetId }
            ) { status ->
                val categoryDetails = remember(status.categoryName, allCategories) {
                    allCategories.find { category ->
                        category.name.equals(status.categoryName, ignoreCase = true)
                    }
                }

                val categoryIcon = getCategoryIcon(categoryDetails)

                BudgetCard(
                    status = status,
                    onEdit = { onEditBudget(status) },
                    onDelete = { mainViewModel.deleteBudget(status.budgetId) },
                    categoryIcon = categoryIcon
                )
            }
        }
    }
}

@Composable
private fun RecurringList(
    mainViewModel: MainViewModel,
    onEditRule: (RecurringRule) -> Unit,
    onViewForecast: (RecurringRule) -> Unit
) {
    val recurringRules by mainViewModel.recurringRules.collectAsState()

    if (recurringRules.isEmpty()) {
        LottieEmptyState(
            modifier = Modifier
                .fillMaxSize()
                .padding(ExpressiveTokens.spacing.lg),
            message = "You have no recurring transactions.\nTap 'Add Payment' to add subscriptions, rent, or bills.",
            lottieResourceId = R.raw.empty_box_animation
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = ExpressiveTokens.spacing.lg,
                top = ExpressiveTokens.spacing.lg,
                end = ExpressiveTokens.spacing.lg,
                bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.md)
        ) {
            items(
                items = recurringRules,
                key = { it.id }
            ) { rule ->
                RecurringRuleCard(
                    rule = rule,
                    onDelete = { mainViewModel.deleteRecurringRule(rule) },
                    onEdit = { onEditRule(rule) },
                    onViewForecast = { onViewForecast(rule) }
                )
            }
        }
    }
}

@Composable
private fun ForecastDialog(
    rule: RecurringRule,
    onDismiss: () -> Unit
) {
    val dateFormat = remember {
        SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
    }

    val upcomingDates = remember(rule) {
        List(6) { index ->
            val calendar = Calendar.getInstance()

            calendar.timeInMillis = if (index == 0) {
                rule.nextDueDate
            } else {
                val previousDateCalendar = Calendar.getInstance().apply {
                    timeInMillis = rule.nextDueDate
                }

                repeat(index - 1) {
                    when (rule.periodType) {
                        BudgetPeriod.MONTHLY -> previousDateCalendar.add(Calendar.MONTH, 1)
                        BudgetPeriod.WEEKLY -> previousDateCalendar.add(Calendar.WEEK_OF_YEAR, 1)
                        BudgetPeriod.YEARLY -> previousDateCalendar.add(Calendar.YEAR, 1)
                    }
                }

                when (rule.periodType) {
                    BudgetPeriod.MONTHLY -> previousDateCalendar.add(Calendar.MONTH, 1)
                    BudgetPeriod.WEEKLY -> previousDateCalendar.add(Calendar.WEEK_OF_YEAR, 1)
                    BudgetPeriod.YEARLY -> previousDateCalendar.add(Calendar.YEAR, 1)
                }

                previousDateCalendar.timeInMillis
            }

            val finalCalendar = Calendar.getInstance().apply {
                timeInMillis = calendar.timeInMillis
            }

            val maxDayInMonth = finalCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

            finalCalendar.set(
                Calendar.DAY_OF_MONTH,
                rule.dayOfPeriod.coerceAtMost(maxDayInMonth)
            )

            dateFormat.format(Date(finalCalendar.timeInMillis))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Upcoming Payments for \"${rule.description}\"")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                upcomingDates.forEach { dateString ->
                    Text(
                        text = "• $dateString",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Close")
            }
        }
    )
}
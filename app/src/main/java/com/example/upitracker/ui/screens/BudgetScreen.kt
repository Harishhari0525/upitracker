@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.example.upitracker.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import com.example.upitracker.R
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.upitracker.data.RecurringRule
import com.example.upitracker.ui.components.AddEditBudgetDialog
import com.example.upitracker.ui.components.AddEditRecurringRuleDialog
import com.example.upitracker.ui.components.BudgetCard
import com.example.upitracker.ui.components.RecurringRuleCard
import com.example.upitracker.viewmodel.BudgetStatus
import com.example.upitracker.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import com.example.upitracker.data.BudgetPeriod
import com.example.upitracker.ui.components.LottieEmptyState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.example.upitracker.util.getCategoryIcon

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BudgetScreen(mainViewModel: MainViewModel) {
    // --- STATE MANAGEMENT (HOISTED) ---
    // All state that controls dialogs is owned by the parent `BudgetScreen`.
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
                mainViewModel.addOrUpdateBudget(category, amount, period, allowRollover)
                showAddBudgetDialog = false
            }
        )
    }
    if (budgetToEdit != null) {
        AddEditBudgetDialog(
            budgetStatus = budgetToEdit,
            onDismiss = { budgetToEdit = null },
            onConfirm = { category, amount, period, allowRollover ->
                mainViewModel.addOrUpdateBudget(category, amount, period, allowRollover, budgetId = budgetToEdit!!.budgetId)
                budgetToEdit = null
            }
        )
    }
    if (showAddRecurringDialog) {
        AddEditRecurringRuleDialog(
            ruleToEdit = null,
            onDismiss = { showAddRecurringDialog = false },
            onConfirm = { description, amount, category, period, day ->
                mainViewModel.addRecurringRule(description, amount, category, period, day)
                showAddRecurringDialog = false
            }
        )
    }
    if (ruleToEdit != null) {
        AddEditRecurringRuleDialog(
            ruleToEdit = ruleToEdit,
            onDismiss = { ruleToEdit = null },
            onConfirm = { description, amount, category, period, day ->
                mainViewModel.updateRecurringRule(ruleToEdit!!.id, description, amount, category, period, day)
                ruleToEdit = null
            }
        )
    }

    if (showRecurringHelpDialog) {
        AlertDialog(
            onDismissRequest = { showRecurringHelpDialog = false },
            icon = { Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Help") },
            title = { Text("About Recurring Payments") },
            text = { Text("This feature allows you to automatically track fixed payments like subscriptions (e.g., Netflix) or rent. The app will create a new transaction for you on the date you specify each month.") },
            confirmButton = {
                TextButton(onClick = { showRecurringHelpDialog = false }) {
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (pagerState.currentPage == 0) {
                        showAddBudgetDialog = true
                    } else {
                        showAddRecurringDialog = true
                    }
                }
            ) { Icon(Icons.Filled.Add, contentDescription = "Add new item") }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text( text = title, style = MaterialTheme.typography.titleSmallEmphasized)
                                if (index == 1) {
                                    Spacer(Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                        contentDescription = "Help about Recurring Payments",
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clickable { showRecurringHelpDialog = true },
                                        tint = if (pagerState.currentPage == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    )
                }
            }

            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                when (page) {
                    // KEY FIX: We pass a lambda down to each child, so they can tell the parent to change the state.
                    0 -> BudgetList(mainViewModel = mainViewModel, onEditBudget = { budgetToEdit = it })
                    1 -> RecurringList(mainViewModel = mainViewModel,
                        onEditRule = { ruleToEdit = it },
                        onViewForecast = { ruleForForecast = it })
                }
            }
        }
    }
}

/**
 * BudgetList is now a "dumb" component. It only displays data and emits events.
 * It does NOT own any dialog state.
 */
@Composable
private fun BudgetList(mainViewModel: MainViewModel, onEditBudget: (BudgetStatus) -> Unit) {
    val budgetStatuses by mainViewModel.budgetStatuses.collectAsState()

    val allCategories by mainViewModel.allCategories.collectAsState()

    if (budgetStatuses.isEmpty()) {
        LottieEmptyState(
            message = "You haven't set any budgets yet.\nTap '+' to create one!",
            lottieResourceId = R.raw.empty_box_animation
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(budgetStatuses, key = { it.budgetId }) { status ->

                val categoryDetails = remember(status.categoryName, allCategories) {
                    allCategories.find { c -> c.name.equals(status.categoryName, ignoreCase = true) }
                }
                val categoryIcon = getCategoryIcon(categoryDetails)

                BudgetCard(
                    status = status,
                    onEdit = { onEditBudget(status) }, // This calls the lambda passed from the parent.
                    onDelete = { mainViewModel.deleteBudget(status.budgetId) },
                    categoryIcon = categoryIcon
                )
            }
        }
    }
}

/**
 * RecurringList is now a "dumb" component. It only displays data and emits events.
 * It does NOT own any dialog state.
 */
@Composable
private fun RecurringList(
    mainViewModel: MainViewModel,
    onEditRule: (RecurringRule) -> Unit,
    onViewForecast: (RecurringRule) -> Unit
) {
    val recurringRules by mainViewModel.recurringRules.collectAsState()

    if (recurringRules.isEmpty()) {
        LottieEmptyState(
            message = "You have no recurring transactions.\nTap '+' to add subscriptions, rent, or other regular payments.",
            lottieResourceId = R.raw.empty_box_animation
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(recurringRules, key = { it.id }) { rule ->
                RecurringRuleCard(
                    rule = rule,
                    onDelete = { mainViewModel.deleteRecurringRule(rule) },
                    onEdit = { onEditRule(rule) },
                    onViewForecast = { onViewForecast(rule) } // Use the new clickable card
                )
            }
        }
    }
}

@Composable
private fun ForecastDialog(rule: RecurringRule, onDismiss: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault()) }

    val upcomingDates = remember(rule) {
        List(6) { i ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = if (i == 0) {
                rule.nextDueDate
            } else {
                // To get the next date, we must start from the *previous* date in the list
                val previousDateCalendar = Calendar.getInstance().apply { timeInMillis = rule.nextDueDate }
                repeat(i - 1) {
                    when (rule.periodType) {
                        BudgetPeriod.MONTHLY -> previousDateCalendar.add(Calendar.MONTH, 1)
                        BudgetPeriod.WEEKLY -> previousDateCalendar.add(Calendar.WEEK_OF_YEAR, 1)
                        BudgetPeriod.YEARLY -> previousDateCalendar.add(Calendar.YEAR, 1)
                    }
                }
                // Now calculate the next one
                when (rule.periodType) {
                    BudgetPeriod.MONTHLY -> previousDateCalendar.add(Calendar.MONTH, 1)
                    BudgetPeriod.WEEKLY -> previousDateCalendar.add(Calendar.WEEK_OF_YEAR, 1)
                    BudgetPeriod.YEARLY -> previousDateCalendar.add(Calendar.YEAR, 1)
                }
                previousDateCalendar.timeInMillis
            }

            // Correct for month-end dates
            val finalCalendar = Calendar.getInstance().apply { timeInMillis = calendar.timeInMillis }
            val maxDayInMonth = finalCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            finalCalendar.set(Calendar.DAY_OF_MONTH, rule.dayOfPeriod.coerceAtMost(maxDayInMonth))

            dateFormat.format(Date(finalCalendar.timeInMillis))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Upcoming Payments for \"${rule.description}\"") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                upcomingDates.forEach { dateString ->
                    Text("• $dateString", style = MaterialTheme.typography.bodyMedium)
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
@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.example.upitracker.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.upitracker.ui.components.AddEditBudgetDialog
import com.example.upitracker.ui.components.AddEditRecurringRuleDialog
import com.example.upitracker.ui.components.BudgetCard
import com.example.upitracker.ui.components.RecurringRuleCard
import com.example.upitracker.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun BudgetScreen(mainViewModel: MainViewModel) {
    // State for the tabs
    val pagerState = rememberPagerState { 2 }
    val coroutineScope = rememberCoroutineScope()
    val tabTitles = listOf("Spending Budgets", "Recurring Payments")

    // State for the dialogs
    var showAddBudgetDialog by remember { mutableStateOf(false) }
    var showAddRecurringDialog by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // The FAB's action depends on the selected tab
                    if (pagerState.currentPage == 0) {
                        showAddBudgetDialog = true
                    } else {
                        showAddRecurringDialog = true
                    }
                }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add new item")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // TabRow to switch between Budgets and Recurring
            PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
                        },
                        text = { Text(title) }
                    )
                }
            }

            // HorizontalPager to hold the content for each tab
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> BudgetList(mainViewModel)
                    1 -> RecurringList(mainViewModel)
                }
            }
        }
    }

    // Show the appropriate dialog based on state
    if (showAddBudgetDialog) {
        AddEditBudgetDialog(
            budgetStatus = null, // Passing null for adding new
            onDismiss = { showAddBudgetDialog = false },
            onConfirm = { category, amount, period, allowRollover ->
                mainViewModel.addOrUpdateBudget(category, amount, period, allowRollover)
                showAddBudgetDialog = false
            }
        )
    }

    if (showAddRecurringDialog) {
        AddEditRecurringRuleDialog(
            onDismiss = { showAddRecurringDialog = false },
            onConfirm = { description, amount, category, period, day ->
                mainViewModel.addRecurringRule(description, amount, category, period, day)
                showAddRecurringDialog = false
            }
        )
    }
}

// Helper composable for the Budgets list
@Composable
private fun BudgetList(mainViewModel: MainViewModel) {
    val budgetStatuses by mainViewModel.budgetStatuses.collectAsState()

    if (budgetStatuses.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "You have no monthly budgets.\nTap the '+' button to create one.",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(budgetStatuses, key = { it.budgetId }) { status ->
                BudgetCard(
                    status = status,
                    onEdit = { /* Future edit functionality */ },
                    onDelete = { mainViewModel.deleteBudget(status.budgetId) }
                )
            }
        }
    }
}

// Helper composable for the Recurring Transactions list
@Composable
private fun RecurringList(mainViewModel: MainViewModel) {
    val recurringRules by mainViewModel.recurringRules.collectAsState()

    if (recurringRules.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "You have no recurring transactions.\nTap the '+' button to add one.",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(recurringRules, key = { it.id }) { rule ->
                RecurringRuleCard(
                    rule = rule,
                    onDelete = { mainViewModel.deleteRecurringRule(rule) }
                )
            }
        }
    }
}
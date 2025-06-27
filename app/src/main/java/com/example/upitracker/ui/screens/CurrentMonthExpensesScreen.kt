@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.upitracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.canopas.lib.showcase.IntroShowcase
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.upitracker.data.RecurringRule
import com.example.upitracker.ui.components.TransactionCardWithMenu
import com.example.upitracker.util.OnboardingPreference
import com.example.upitracker.viewmodel.BankMessageCount
import com.example.upitracker.viewmodel.MainViewModel
import com.example.upitracker.viewmodel.TransactionHistoryItem
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*
import java.util.concurrent.TimeUnit
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CurrentMonthExpensesScreen(
    mainViewModel: MainViewModel,
    onViewAllClick: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    // --- Showcase Setup ---
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showAppIntro by remember { mutableStateOf(false) }
    val isTourCompleted by OnboardingPreference.isTourCompletedFlow(context)
        .collectAsState(initial = true)

    LaunchedEffect(isTourCompleted) {
        if (!isTourCompleted) {
            showAppIntro = true
        }
    }

    // --- State Collection ---
    val currentMonthExpensesTotal by mainViewModel.currentMonthTotalExpenses.collectAsState()
    val bankMessageCounts by mainViewModel.bankMessageCounts.collectAsState()
    val recentTransactions by mainViewModel.currentMonthExpenseItems.collectAsState()
    val recurringRules by mainViewModel.recurringRules.collectAsState()
    val isImporting by mainViewModel.isImportingSms.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()


    // --- UI Layout with Showcase ---
    IntroShowcase(
        showIntroShowCase = showAppIntro,
        onShowCaseCompleted = {
            showAppIntro = false
            coroutineScope.launch {
                OnboardingPreference.setTourCompleted(context, true)
            }
        }
    ) { // This is the IntroShowcaseScope lambda

        PullToRefreshBox(
            modifier = Modifier.fillMaxSize(),
            isRefreshing = isImporting,
            onRefresh = onRefresh,
            state = pullRefreshState,
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    modifier = Modifier.align(Alignment.TopCenter),
                    state = pullRefreshState,
                    isRefreshing = isImporting
                )
            }
        ) {

            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = 10.dp)
            ) {
                stickyHeader {
                    Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                        TotalExpensesHeroCard(
                            total = currentMonthExpensesTotal,
                            // The .introShowcaseTarget modifier is now correctly used within the scope
                            modifier = Modifier.introShowCaseTarget(
                                index = 0,
                                content = {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Monthly Snapshot", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                                        Spacer(Modifier.height(8.dp))
                                        Text("This card shows your total spending for the current month at a glance.", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                    }
                                }
                            )
                        )
                        Spacer(Modifier.height(20.dp))
                    }
                }

                item {
                    SectionHeader(title = "Bank Activity")
                    Spacer(Modifier.height(8.dp))
                    BankActivityCard(counts = bankMessageCounts)
                }

                item {
                    UpcomingPaymentsSection(rules = recurringRules)
                }

                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        RecentTransactionsHeader(
                            onViewAllClick = onViewAllClick,
                            modifier = Modifier.introShowCaseTarget(
                                index = 1,
                                content = {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Full History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                                        Spacer(Modifier.height(8.dp))
                                        Text("Tap here to see your complete transaction history with powerful sorting and filtering options.", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                    }
                                }
                            )
                        )

                        Spacer(Modifier.height(12.dp))

                        if (recentTransactions.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No transactions this month yet.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                recentTransactions.take(3).forEach { item ->
                                    if (item is TransactionHistoryItem) {
                                        TransactionCardWithMenu(
                                            transaction = item.transaction,
                                            onClick = { /* ... */ },
                                            onArchive = {
                                                mainViewModel.toggleTransactionArchiveStatus(
                                                    it,
                                                    true
                                                )
                                            },
                                            onDelete = { mainViewModel.deleteTransaction(it) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun UpcomingPaymentsSection(rules: List<RecurringRule>) {
    val upcomingRules = rules.take(3)

    if (upcomingRules.isNotEmpty()) {
        Column {
            SectionHeader(title = "Upcoming Payments")
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    upcomingRules.forEach { rule ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(rule.description, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)

                            val daysUntil = TimeUnit.MILLISECONDS.toDays(rule.nextDueDate - System.currentTimeMillis())
                            val dueText = when {
                                daysUntil < 0 -> "Overdue"
                                daysUntil == 0L -> "Today"
                                daysUntil == 1L -> "Tomorrow"
                                else -> "in $daysUntil days"
                            }
                            Text(dueText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

// --- FIX: Added modifier parameter ---
@Composable
private fun TotalExpensesHeroCard(total: Double, modifier: Modifier = Modifier) {
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("en").setRegion("IN").build()) }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Current Month's Expenses", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text(
                    currencyFormatter.format(total),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 40.sp
                )
            }
            Icon(
                imageVector = Icons.Filled.TrackChanges,
                contentDescription = "Monthly Expenses",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
                    .padding(10.dp),
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
private fun BankActivityCard(counts: List<BankMessageCount>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
    ) {
        if (counts.isNotEmpty()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                counts.take(3).forEach { item ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(item.bankName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text("${item.count} messages", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
internal fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )
}

// --- FIX: Added modifier parameter ---
@Composable
private fun RecentTransactionsHeader(onViewAllClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionHeader(title = "Recent Transactions")
        TextButton(onClick = onViewAllClick) {
            Text("View All")
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "View All Transactions", modifier = Modifier.size(18.dp)
            )
        }
    }
}

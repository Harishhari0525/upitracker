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
import androidx.compose.material.icons.filled.AccountBalanceWallet
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
import com.example.upitracker.util.getCategoryIcon
import com.example.upitracker.util.parseColor
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import java.text.SimpleDateFormat


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

    val allCategories by mainViewModel.allCategories.collectAsState()


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
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 4.dp)
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
                                        Text("Please swipe down from the top to import old SMS's and this card shows your total" +
                                                " spending for the current month at a glance.", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                    }
                                }
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }

                item {
                    SectionHeader(title = "Bank Activity")
                    Spacer(Modifier.height(8.dp))
                    BankActivityCard(
                        counts = bankMessageCounts,
                        onBankClick = { bankName ->
                            mainViewModel.setBankFilter(bankName) // Set the filter
                            onViewAllClick() // Navigate to the history screen
                        }
                    )
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
                                        Text("Tap on View All to see your complete transaction history with powerful sorting and filtering options.", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                    }
                                }
                            )
                        )

                        Spacer(Modifier.height(8.dp))

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
                                        val transaction = item.transaction
                                        val categoryDetails = remember(transaction.category, allCategories) {
                                            allCategories.find { c -> c.name.equals(transaction.category, ignoreCase = true) }
                                        }
                                        val categoryColor = remember(categoryDetails) {
                                            parseColor(categoryDetails?.colorHex ?: "#808080")
                                        }
                                        val categoryIcon = getCategoryIcon(categoryDetails)

                                        TransactionCardWithMenu(
                                            transaction = item.transaction,
                                            onClick = { /* ... */ },
                                            onDelete = { mainViewModel.deleteTransaction(it) },
                                            onArchiveAction = {
                                                mainViewModel.toggleTransactionArchiveStatus(
                                                    it,
                                                    true
                                                )
                                            },
                                            // Add the missing parameters
                                            archiveActionText = "Archive",
                                            archiveActionIcon = Icons.Default.Archive,
                                            categoryColor = categoryColor,
                                            categoryIcon = categoryIcon,
                                            onCategoryClick = { categoryName -> // ✨ ADD THIS LAMBDA
                                                mainViewModel.toggleCategoryFilter(categoryName)
                                            }
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

            // The parent Card has been removed.
            // This Column now handles the spacing between each floating list item.
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                upcomingRules.forEach { rule ->
                    // Each ListItem is now styled individually to look like a card.
                    ListItem(
                        modifier = Modifier.clip(MaterialTheme.shapes.large),
                        headlineContent = {
                            Text(rule.description, fontWeight = FontWeight.SemiBold)
                        },
                        supportingContent = {
                            val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("en").setRegion("IN").build()) }
                            Text("${currencyFormatter.format(rule.amount)} • Due ${SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(rule.nextDueDate))}")
                        },
                        trailingContent = {
                            val daysUntil = TimeUnit.MILLISECONDS.toDays(rule.nextDueDate - System.currentTimeMillis())
                            val dueText = when {
                                daysUntil < 0 -> "Overdue"
                                daysUntil == 0L -> "Today"
                                daysUntil == 1L -> "Tomorrow"
                                else -> "in $daysUntil days"
                            }
                            Text(dueText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        },
                        // Apply the same colors as the Bank Activity items for consistency.
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                        )
                    )
                }
            }
        }
    }
}
@Composable
private fun TotalExpensesHeroCard(total: Double, modifier: Modifier = Modifier) {
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("en").setRegion("IN").build()) }

    // ✨ 1. Define a more expressive, asymmetrical shape.
    val modernShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 28.dp, bottomEnd = 8.dp)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = modernShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Current Month's Expenses",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer // Use theme-aware color
                )
                Spacer(Modifier.height(8.dp))

                // ✨ 3. Wrap the amount in AnimatedContent for the "ticking" animation.
                AnimatedContent(
                    targetState = total,
                    transitionSpec = {
                        if (targetState > initialState) {
                            // Animate up
                            slideInVertically { height -> height } togetherWith slideOutVertically { height -> -height }
                        } else {
                            // Animate down
                            slideInVertically { height -> -height } togetherWith slideOutVertically { height -> height }
                        }
                    },
                    label = "totalAmountAnimation"
                ) { targetTotal ->
                    Text(
                        currencyFormatter.format(targetTotal),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer, // Use theme-aware color
                        lineHeight = 40.sp
                    )
                }
            }
            Icon(
                imageVector = Icons.Filled.AccountBalanceWallet,
                contentDescription = "Monthly Expenses",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .padding(6.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun BankActivityCard(
    counts: List<BankMessageCount>,
    onBankClick: (String) -> Unit
) {
    // We no longer use a parent Card. The Column is now the root.
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp) // Adds space between each item
    ) {
        if (counts.isNotEmpty()) {
            counts.take(3).forEach { item ->
                ListItem(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.large) // Apply the shape directly to the item
                        .clickable { onBankClick(item.bankName) },
                    headlineContent = {
                        Text(item.bankName, fontWeight = FontWeight.SemiBold)
                    },
                    trailingContent = {
                        Text("${item.count} messages", style = MaterialTheme.typography.bodyMedium)
                    },
                    leadingContent = {
                        Icon(
                            Icons.Filled.AccountBalance,
                            contentDescription = "Bank",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    },
                    // This gives each item its own background and elevation color
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                    )
                )
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

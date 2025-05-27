@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.example.upitracker.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear // For clearing date filter
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.upitracker.R
import com.example.upitracker.ui.components.TransactionCard
import com.example.upitracker.ui.components.UpiLiteSummaryCard
import com.example.upitracker.viewmodel.MainViewModel
import com.example.upitracker.viewmodel.UpiTransactionTypeFilter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone // For UTC DatePicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    mainViewModel: MainViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val tabTitles = listOf(
        stringResource(R.string.tab_upi),
        stringResource(R.string.tab_upi_lite)
    )
    val tabIcons = listOf(Icons.Filled.AccountBalanceWallet, Icons.Filled.Summarize)

    val pagerState = rememberPagerState { tabTitles.size }
    val coroutineScope = rememberCoroutineScope()

    // Collect filtered lists and filter states from ViewModel
    val filteredUpiTransactions by mainViewModel.filteredUpiTransactions.collectAsState()
    val selectedUpiFilterType by mainViewModel.selectedUpiTransactionType.collectAsState()

    val filteredUpiLiteSummaries by mainViewModel.filteredUpiLiteSummaries.collectAsState() // ✨ Use new filtered list
    val selectedStartDate by mainViewModel.selectedDateRangeStart.collectAsState()
    val selectedEndDate by mainViewModel.selectedDateRangeEnd.collectAsState()

    // DatePickerDialog states
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    val datePickerStateStart = rememberDatePickerState()
    val datePickerStateEnd = rememberDatePickerState()

    // Date formatter for displaying selected dates
    val displayDateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    Column(modifier = modifier.fillMaxSize()) {
        // --- Date Range Filter UI ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.DateRange, contentDescription = stringResource(R.string.history_filter_date_range_desc)) // ✨ New String
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { showStartDatePicker = true }) {
                    Text(selectedStartDate?.let { displayDateFormat.format(Date(it)) } ?: stringResource(R.string.history_filter_start_date_label)) // ✨ New String
                }
                Text(" - ", modifier = Modifier.padding(horizontal = 4.dp))
                TextButton(onClick = { showEndDatePicker = true }) {
                    Text(selectedEndDate?.let { displayDateFormat.format(Date(it)) } ?: stringResource(R.string.history_filter_end_date_label)) // ✨ New String
                }
            }
            if (selectedStartDate != null || selectedEndDate != null) {
                IconButton(onClick = { mainViewModel.clearDateRangeFilter() }) {
                    Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.history_filter_clear_dates_desc)) // ✨ New String
                }
            }
        }
        HorizontalDivider()


        // --- Tabs ---
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
            indicator = { tabPositions ->
                if (pagerState.currentPage < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage])
                    )
                }
            }
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(title, style = MaterialTheme.typography.labelLarge) },
                    icon = { Icon(tabIcons[index], contentDescription = title) },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        HorizontalDivider()

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { pageIndex ->
            when (pageIndex) {
                0 -> { // UPI Transactions Tab
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Type FilterChips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp), // Reduced bottom padding
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            UpiTransactionTypeFilter.values().forEach { filterType ->
                                FilterChip(
                                    selected = selectedUpiFilterType == filterType,
                                    onClick = { mainViewModel.setUpiTransactionTypeFilter(filterType) },
                                    label = {
                                        Text(filterType.name.replace("_", " ")
                                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
                                    },
                                    leadingIcon = if (selectedUpiFilterType == filterType) {
                                        { Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.filter_chip_selected_desc)) } // ✨ New String
                                    } else null
                                )
                            }
                        }

                        if (filteredUpiTransactions.isEmpty()) {
                            EmptyStateHistoryView(message = stringResource(R.string.empty_state_no_upi_transactions_history_filtered))
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp), // Added top padding
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredUpiTransactions, key = { "txn-${it.id}" }) { transaction ->
                                    TransactionCard(transaction = transaction)
                                }
                            }
                        }
                    }
                }
                1 -> { // UPI Lite Summaries Tab
                    // UPI Lite summaries will be filtered by date range from ViewModel
                    if (filteredUpiLiteSummaries.isEmpty()) { // ✨ Use filtered list
                        EmptyStateHistoryView(message = stringResource(R.string.empty_state_no_upi_lite_summaries_history_filtered)) // ✨ New String
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(all = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredUpiLiteSummaries, key = { "lite-${it.id}" }) { summary -> // ✨ Use filtered list
                                UpiLiteSummaryCard(summary = summary)
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DatePicker Dialogs ---
    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showStartDatePicker = false
                    datePickerStateStart.selectedDateMillis?.let {
                        // Ensure start date is start of day in UTC for consistent filtering
                        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                        cal.timeInMillis = it
                        cal.set(Calendar.HOUR_OF_DAY, 0)
                        cal.set(Calendar.MINUTE, 0)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        mainViewModel.setDateRangeFilter(cal.timeInMillis, selectedEndDate)
                    }
                }) { Text(stringResource(R.string.dialog_button_ok)) } // ✨ New String
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text(stringResource(R.string.dialog_button_cancel)) }
            }
        ) {
            DatePicker(state = datePickerStateStart)
        }
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showEndDatePicker = false
                    datePickerStateEnd.selectedDateMillis?.let {
                        // Ensure end date is end of day in UTC for inclusive filtering
                        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                        cal.timeInMillis = it
                        cal.set(Calendar.HOUR_OF_DAY, 23)
                        cal.set(Calendar.MINUTE, 59)
                        cal.set(Calendar.SECOND, 59)
                        cal.set(Calendar.MILLISECOND, 999)
                        mainViewModel.setDateRangeFilter(selectedStartDate, cal.timeInMillis)
                    }
                }) { Text(stringResource(R.string.dialog_button_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text(stringResource(R.string.dialog_button_cancel)) }
            }
        ) {
            DatePicker(state = datePickerStateEnd)
        }
    }
}

// EmptyStateHistoryView (as defined before)
@Composable
fun EmptyStateHistoryView(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
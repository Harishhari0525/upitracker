@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.example.upitracker.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.* // For Edit, Clear, DateRange, Check, etc.
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
import com.example.upitracker.data.Transaction
import com.example.upitracker.data.UpiLiteSummary
import com.example.upitracker.ui.components.TransactionCard
import com.example.upitracker.ui.components.UpiLiteSummaryCard
import com.example.upitracker.viewmodel.MainViewModel
import com.example.upitracker.viewmodel.UpiTransactionTypeFilter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue // For 'by' delegation
import com.example.upitracker.ui.components.EditCategoryDialog
import com.example.upitracker.viewmodel.SortOrder // ✨ Import SortOrder
import com.example.upitracker.viewmodel.SortableTransactionField
import com.example.upitracker.viewmodel.SortableUpiLiteSummaryField

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

    // Collect Filter States from ViewModel
    val filteredUpiTransactions by mainViewModel.filteredUpiTransactions.collectAsState()
    val selectedUpiFilterType by mainViewModel.selectedUpiTransactionType.collectAsState()
    // val filteredUpiLiteSummaries by mainViewModel.filteredUpiLiteSummaries.collectAsState()
    // Around line 59 (or where you define it)
    val filteredUpiLiteSummaries: List<UpiLiteSummary> by mainViewModel.filteredUpiLiteSummaries.collectAsState()
    val selectedStartDate by mainViewModel.selectedDateRangeStart.collectAsState()
    val selectedEndDate by mainViewModel.selectedDateRangeEnd.collectAsState()
    val upiLiteSortField by mainViewModel.upiLiteSummarySortField.collectAsState()
    val upiLiteSortOrder by mainViewModel.upiLiteSummarySortOrder.collectAsState()

    // --- DatePickerDialog States (managed within this screen) ---
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    val datePickerStateStart = rememberDatePickerState(initialSelectedDateMillis = selectedStartDate)
    val datePickerStateEnd = rememberDatePickerState(initialSelectedDateMillis = selectedEndDate)

    LaunchedEffect(selectedStartDate) { datePickerStateStart.selectedDateMillis = selectedStartDate }
    LaunchedEffect(selectedEndDate) { datePickerStateEnd.selectedDateMillis = selectedEndDate }

    // ✨ --- State and Lambda for Category Edit Dialog - DEFINED HERE AT THE SCREEN LEVEL --- ✨
    var transactionToEditCategory by remember { mutableStateOf<Transaction?>(null) }
    var showCategoryEditDialog by remember { mutableStateOf(false) }

    // ✨ Collect Sort States from ViewModel ✨
    val upiSortField by mainViewModel.upiTransactionSortField.collectAsState()
    val upiSortOrder by mainViewModel.upiTransactionSortOrder.collectAsState()

    val openCategoryEditDialog = { transaction: Transaction ->
        transactionToEditCategory = transaction
        showCategoryEditDialog = true
    }
    // ✨ --- End of Category Edit Dialog State and Lambda --- ✨

    Column(modifier = modifier.fillMaxSize()) {
        DateFilterControls(
            selectedStartDate = selectedStartDate,
            selectedEndDate = selectedEndDate,
            onStartDateClick = { showStartDatePicker = true },
            onEndDateClick = { showEndDatePicker = true },
            onClearDates = { mainViewModel.clearDateRangeFilter() }
        )
        HorizontalDivider()

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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            UpiTransactionTypeFilter.entries.forEach { filterType ->
                                FilterChip(
                                    selected = selectedUpiFilterType == filterType,
                                    onClick = { mainViewModel.setUpiTransactionTypeFilter(filterType) },
                                    label = {
                                        Text(filterType.name.replace("_", " ")
                                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
                                    },
                                    leadingIcon = if (selectedUpiFilterType == filterType) {
                                        { Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.filter_chip_selected_desc)) }
                                    } else null
                                )
                            }
                        }
                        SortControls(
                            currentSortField = upiSortField,
                            currentSortOrder = upiSortOrder,
                            onSortFieldSelected = { field -> mainViewModel.setUpiTransactionSort(field) }
                        )

                        if (filteredUpiTransactions.isEmpty()) {
                            EmptyStateHistoryView(message = stringResource(R.string.empty_state_no_upi_transactions_history_filtered))
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredUpiTransactions, key = { "txn-${it.id}" }) { transaction ->
                                    TransactionCard(
                                        transaction = transaction,
                                        onClick = { openCategoryEditDialog(transaction) } // ✨ USING the lambda here ✨
                                    )
                                }
                            }
                        }
                    }
                }
                1 -> { // UPI Lite Summaries Tab
                    Column(modifier = Modifier.fillMaxSize()) {
                        // ✨ Sorting Controls for UPI Lite Summaries ✨
                        UpiLiteSummarySortControls(
                            currentSortField = upiLiteSortField,
                            currentSortOrder = upiLiteSortOrder,
                            onSortFieldSelected = { field -> mainViewModel.setUpiLiteSummarySort(field) }
                        )

                        if (filteredUpiLiteSummaries.isEmpty()) {
                            EmptyStateHistoryView(message = stringResource(R.string.empty_state_no_upi_lite_summaries_history_filtered))
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(all = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredUpiLiteSummaries, key = { "lite-${it.id}" }) { summary ->
                                    UpiLiteSummaryCard(summary = summary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Category Edit Dialog Invocation - Placed within the scope of TransactionHistoryScreen ---
    if (showCategoryEditDialog && transactionToEditCategory != null) {
        EditCategoryDialog(
            transaction = transactionToEditCategory!!,
            onDismiss = {
                showCategoryEditDialog = false
                transactionToEditCategory = null
            },
            onSaveCategory = { transactionId, newCategory ->
                mainViewModel.updateTransactionCategory(transactionId, newCategory)
                showCategoryEditDialog = false
                transactionToEditCategory = null
            }
        )
    }

    // --- DatePicker Dialogs ---
    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showStartDatePicker = false
                    datePickerStateStart.selectedDateMillis?.let {
                        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                        cal.timeInMillis = it
                        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                        mainViewModel.setDateRangeFilter(cal.timeInMillis, selectedEndDate)
                    }
                }) { Text(stringResource(R.string.dialog_button_ok)) }
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
                        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                        cal.timeInMillis = it
                        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
                        cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
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

// --- Helper Composables for TransactionHistoryScreen ---

@Composable
fun DateFilterControls(
    selectedStartDate: Long?,
    selectedEndDate: Long?,
    onStartDateClick: () -> Unit,
    onEndDateClick: () -> Unit,
    onClearDates: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayDateFormat = remember { SimpleDateFormat("dd MMM yy", Locale.getDefault()) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Filled.DateRange,
                contentDescription = stringResource(R.string.history_filter_date_range_desc),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onStartDateClick) {
                Text(
                    selectedStartDate?.let { displayDateFormat.format(Date(it)) }
                        ?: stringResource(R.string.history_filter_start_date_label)
                )
            }
            Text(" - ", modifier = Modifier.padding(horizontal = 4.dp))
            TextButton(onClick = onEndDateClick) {
                Text(
                    selectedEndDate?.let { displayDateFormat.format(Date(it)) }
                        ?: stringResource(R.string.history_filter_end_date_label)
                )
            }
        }
        if (selectedStartDate != null || selectedEndDate != null) {
            IconButton(onClick = onClearDates) {
                Icon(
                    Icons.Filled.Clear,
                    contentDescription = stringResource(R.string.history_filter_clear_dates_desc),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Spacer(Modifier.width(48.dp)) // Placeholder for consistent height
        }
    }
}

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
@Composable
fun SortButton(
    text: String,
    isSelected: Boolean,
    sortOrder: SortOrder,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp) // Compact button
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium, // Smaller text for sort buttons
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (isSelected) {
            Icon(
                imageVector = if (sortOrder == SortOrder.ASCENDING) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                contentDescription = stringResource(if (sortOrder == SortOrder.ASCENDING) R.string.history_sort_order_ascending_desc else R.string.history_sort_order_descending_desc),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp).padding(start = 2.dp) // Smaller icon
            )
        }
    }
}

@Composable
fun SortControls(
    currentSortField: SortableTransactionField,
    currentSortOrder: SortOrder,
    onSortFieldSelected: (SortableTransactionField) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 0.dp), // Reduced vertical padding
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End), // Align to end, less space
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.history_sort_by_label), style = MaterialTheme.typography.labelSmall) // Smaller label

        SortButton(
            text = stringResource(R.string.history_sort_by_date),
            isSelected = currentSortField == SortableTransactionField.DATE,
            sortOrder = if (currentSortField == SortableTransactionField.DATE) currentSortOrder else SortOrder.DESCENDING, // Default for non-active
            onClick = { onSortFieldSelected(SortableTransactionField.DATE) }
        )
        SortButton(
            text = stringResource(R.string.history_sort_by_amount),
            isSelected = currentSortField == SortableTransactionField.AMOUNT,
            sortOrder = if (currentSortField == SortableTransactionField.AMOUNT) currentSortOrder else SortOrder.DESCENDING,
            onClick = { onSortFieldSelected(SortableTransactionField.AMOUNT) }
        )
        SortButton(
            text = stringResource(R.string.history_sort_by_category),
            isSelected = currentSortField == SortableTransactionField.CATEGORY,
            sortOrder = if (currentSortField == SortableTransactionField.CATEGORY) currentSortOrder else SortOrder.ASCENDING, // Default category to Asc
            onClick = { onSortFieldSelected(SortableTransactionField.CATEGORY) }
        )
    }
}

@Composable
fun UpiTransactionSortControls(
    currentSortField: SortableTransactionField,
    currentSortOrder: SortOrder,
    onSortFieldSelected: (SortableTransactionField) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.history_sort_by_label), style = MaterialTheme.typography.labelSmall)
        SortableTransactionField.entries.forEach { field ->
            SortButton(
                text = stringResource( // Use specific string resources for each field
                    when(field) {
                        SortableTransactionField.DATE -> R.string.history_sort_by_date
                        SortableTransactionField.AMOUNT -> R.string.history_sort_by_amount
                        SortableTransactionField.CATEGORY -> R.string.history_sort_by_category
                    }
                ),
                isSelected = currentSortField == field,
                sortOrder = if (currentSortField == field) currentSortOrder else SortOrder.DESCENDING, // Default for non-active
                onClick = { onSortFieldSelected(field) }
            )
        }
    }
}

// ✨ New: SortControls for UPI Lite Summaries ✨
@Composable
fun UpiLiteSummarySortControls(
    currentSortField: SortableUpiLiteSummaryField,
    currentSortOrder: SortOrder,
    onSortFieldSelected: (SortableUpiLiteSummaryField) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.history_sort_by_label), style = MaterialTheme.typography.labelSmall)
        // Define display names or string resources for SortableUpiLiteSummaryField values
        SortableUpiLiteSummaryField.entries.forEach { field ->
            SortButton(
                text = field.name.replace("_", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }, // Or use stringResource
                isSelected = currentSortField == field,
                sortOrder = if (currentSortField == field) currentSortOrder else SortOrder.DESCENDING,
                onClick = { onSortFieldSelected(field) }
            )
        }
    }
}

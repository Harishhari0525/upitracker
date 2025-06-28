@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.example.upitracker.ui.screens

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.upitracker.R
import com.example.upitracker.ui.components.TransactionCardWithMenu
import com.example.upitracker.ui.components.UpiLiteSummaryCard
import com.example.upitracker.viewmodel.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TransactionHistoryScreen(
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    // State for the main view (tabs, detail sheet)
    val isUpiLiteEnabled by mainViewModel.isUpiLiteEnabled.collectAsState()
    val pageCount = if (isUpiLiteEnabled) 2 else 1
    val pagerState = rememberPagerState { pageCount }
    val coroutineScope = rememberCoroutineScope()
    val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDetailSheet by remember { mutableStateOf(false) }
    val showFilterSheet by mainViewModel.showHistoryFilterSheet.collectAsState()

    // State for our new Filter Bottom Sheet
    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val tabIcons = if (isUpiLiteEnabled) listOf(Icons.Filled.AccountBalanceWallet, Icons.Filled.Summarize) else listOf(Icons.Filled.AccountBalanceWallet)

    val tabTitles = if (isUpiLiteEnabled) {
        listOf("UPI", "UPI Lite")
    } else {
        listOf("UPI")
    }

    // Collect all necessary state from ViewModel
    val filters by mainViewModel.filters.collectAsState()

    Column(
            modifier = modifier
                .fillMaxSize()
        ) {
            // Search bar remains at the top
            OutlinedTextField(
                value = filters.searchQuery,
                onValueChange = { mainViewModel.setSearchQuery(it) },
                label = { Text(stringResource(R.string.search_hint)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // A new row to display currently active filters
            ActiveFiltersRow(
                filters = filters,
                onClearDateFilter = { mainViewModel.clearDateRangeFilter() },
                onClearAmountFilter = { mainViewModel.setAmountFilter(AmountFilterType.ALL, null, null) },
                onClearUncategorizedFilter = { mainViewModel.toggleUncategorizedFilter(false) },
                onDateFilterClick = { mainViewModel.onFilterClick() },
                onAmountFilterClick = { mainViewModel.onFilterClick() },
                onUncategorizedFilterClick = { mainViewModel.onFilterClick() }
            )

            // Tabs and Pager for content
        if (isUpiLiteEnabled) {
            SecondaryTabRow(selectedTabIndex = pagerState.currentPage) {
                // ✨ FIX: The code that uses the 'tabTitles' variable ✨
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(title) },
                        icon = { Icon(tabIcons[index], contentDescription = title) }
                    )
                }
            }
            HorizontalDivider()
        }


        HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { pageIndex ->
                when (pageIndex) {
                    0 -> UpiTransactionsList(mainViewModel, onShowDetails = { showDetailSheet = true })
                    1 -> if (isUpiLiteEnabled) UpiLiteSummariesList(mainViewModel)
                }
            }
        }

    // The BottomSheet for Filters
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { mainViewModel.onFilterSheetDismiss() },
            sheetState = filterSheetState,
        ) {
            FilterSheetContent(
                mainViewModel = mainViewModel,
                filters = filters
            )
        }
    }

    // The BottomSheet for Transaction Details (remains unchanged)
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
                    coroutineScope.launch { detailSheetState.hide() }.invokeOnCompletion {
                        if (!detailSheetState.isVisible) {
                            showDetailSheet = false
                            mainViewModel.selectTransaction(null)
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun UpiTransactionsList(mainViewModel: MainViewModel, onShowDetails: () -> Unit) {
    val filteredUpiTransactions by mainViewModel.filteredUpiTransactions.collectAsState()
    val selectedUpiFilterType by mainViewModel.selectedUpiTransactionType.collectAsState()
    val upiSortField by mainViewModel.upiTransactionSortField.collectAsState()
    val upiSortOrder by mainViewModel.upiTransactionSortOrder.collectAsState()

    val listState = rememberLazyListState()

// And change it to this (just add the new key):
    LaunchedEffect(upiSortField, upiSortOrder, selectedUpiFilterType) {
        listState.animateScrollToItem(index = 0)
    }


    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp)
        ) {
            Row(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                UpiTransactionTypeFilter.entries.forEach { filterType ->
                    FilterChip(
                        selected = selectedUpiFilterType == filterType,
                        onClick = { mainViewModel.setUpiTransactionTypeFilter(filterType) },
                        label = { Text(filterType.name.replace("_", " ").replaceFirstChar { it.titlecase(Locale.getDefault()) }) },
                        leadingIcon = if (selectedUpiFilterType == filterType) {
                            { Icon(Icons.Filled.Check, contentDescription = null) }
                        } else null
                    )
                }
            }

            // This IconButton is aligned to the end of the Box
            IconButton(
                onClick = { mainViewModel.onFilterClick() },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Show Filters"
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
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredUpiTransactions, key = { "txn-${it.id}" }) { transaction ->
                    TransactionCardWithMenu(
                        modifier = Modifier.animateItem(tween(300)),
                        transaction = transaction,
                        onClick = {
                            mainViewModel.selectTransaction(it.id)
                            onShowDetails()
                        },
                        onDelete = {
                            mainViewModel.deleteTransaction(it)
                        },
                        onArchiveAction = {
                            mainViewModel.toggleTransactionArchiveStatus(
                                it,
                                true
                            )
                        },
                        archiveActionText = "Archive",
                        archiveActionIcon = Icons.Default.Archive
                    )
                }
            }
        }
    }
}

@Composable
private fun UpiLiteSummariesList(mainViewModel: MainViewModel) {
    val filteredUpiLiteSummaries by mainViewModel.filteredUpiLiteSummaries.collectAsState()
    val upiLiteSortField by mainViewModel.upiLiteSummarySortField.collectAsState()
    val upiLiteSortOrder by mainViewModel.upiLiteSummarySortOrder.collectAsState()

    val listState = rememberLazyListState()

    LaunchedEffect(upiLiteSortField, upiLiteSortOrder) {
        // Scroll to the top of the list
        listState.animateScrollToItem(index = 0)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        UpiLiteSummarySortControls(
            currentSortField = upiLiteSortField,
            currentSortOrder = upiLiteSortOrder,
            onSortFieldSelected = { field -> mainViewModel.setUpiLiteSummarySort(field) }
        )

        if (filteredUpiLiteSummaries.isEmpty()) {
            EmptyStateHistoryView(message = stringResource(R.string.empty_state_no_upi_lite_summaries_history_filtered))
        } else {
            LazyColumn(
                state = listState,
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

@Composable
private fun FilterSheetContent(
    mainViewModel: MainViewModel,
    filters: TransactionFilters
) {
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    val datePickerStateStart = rememberDatePickerState(initialSelectedDateMillis = filters.startDate)
    val datePickerStateEnd = rememberDatePickerState(initialSelectedDateMillis = filters.endDate)

    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Filters", style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(16.dp))

        DateFilterControls(
            selectedStartDate = filters.startDate,
            selectedEndDate = filters.endDate,
            onStartDateClick = { showStartDatePicker = true },
            onEndDateClick = { showEndDatePicker = true },
            onClearDates = { mainViewModel.clearDateRangeFilter() }
        )
        AdvancedFilterControls(
            filters = filters,
            onToggleUncategorized = { mainViewModel.toggleUncategorizedFilter(it) },
            onSetAmountFilter = { type, val1, val2 -> mainViewModel.setAmountFilter(type, val1, val2) }
        )
        Spacer(modifier = Modifier.navigationBarsPadding())

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
                            mainViewModel.setDateRangeFilter(cal.timeInMillis, filters.endDate)
                        }
                    }) { Text(stringResource(R.string.dialog_button_ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { showStartDatePicker = false }) { Text(stringResource(R.string.dialog_button_cancel)) }
                }
            ) { DatePicker(state = datePickerStateStart) }
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
                            mainViewModel.setDateRangeFilter(filters.startDate, cal.timeInMillis)
                        }
                    }) { Text(stringResource(R.string.dialog_button_ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { showEndDatePicker = false }) { Text(stringResource(R.string.dialog_button_cancel)) }
                }
            ) { DatePicker(state = datePickerStateEnd) }
        }
    }
}

@Composable
private fun ActiveFiltersRow(
    filters: TransactionFilters,
    onClearDateFilter: () -> Unit,
    onClearAmountFilter: () -> Unit,
    onClearUncategorizedFilter: () -> Unit,
    onDateFilterClick: () -> Unit,
    onAmountFilterClick: () -> Unit,
    onUncategorizedFilterClick: () -> Unit
) {
    val showRow = filters.startDate != null || filters.amountType != AmountFilterType.ALL || filters.showUncategorized
    if (showRow) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (filters.startDate != null) {
                val formatter = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }
                val start = formatter.format(Date(filters.startDate))
                val end = filters.endDate?.let { formatter.format(Date(it)) } ?: "Now"
                FilterChip(
                    selected = false,
                    onClick = onDateFilterClick,
                    label = { Text("$start - $end") },
                    trailingIcon = {
                        Icon(
                            Icons.Default.Cancel,
                            contentDescription = "Clear date filter",
                            modifier = Modifier
                                .size(FilterChipDefaults.IconSize)
                                .clickable(onClick = onClearDateFilter)
                        )
                    }
                )
            }
            if (filters.amountType != AmountFilterType.ALL) {
                FilterChip(
                    selected = false,
                    onClick = onAmountFilterClick,
                    label = { Text("Amount") },
                    trailingIcon = {
                        Icon(
                            Icons.Default.Cancel,
                            contentDescription = "Clear amount filter",
                            modifier = Modifier
                                .size(FilterChipDefaults.IconSize)
                                .clickable(onClick = onClearAmountFilter)
                        )
                    }
                )
            }
            if (filters.showUncategorized) {
                FilterChip(
                    selected = false,
                    onClick = onUncategorizedFilterClick,
                    label = { Text("Uncategorized") },
                    trailingIcon = {
                        Icon(
                            Icons.Default.Cancel,
                            contentDescription = "Clear uncategorized filter",
                            modifier = Modifier
                                .size(FilterChipDefaults.IconSize)
                                .clickable(onClick = onClearUncategorizedFilter)
                        )
                    }
                )
            }
        }
    }
}

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
// In TransactionHistoryScreen.kt

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
// In TransactionHistoryScreen.kt, replace the old AdvancedFilterControls with this:

@Composable
fun AdvancedFilterControls(
    filters: TransactionFilters, // Pass the data object directly
    onToggleUncategorized: (Boolean) -> Unit, // Pass the specific lambda
    onSetAmountFilter: (AmountFilterType, Double?, Double?) -> Unit // Pass the specific lambda
) {
    // Local states for the TextFields remain the same
    var amountValue1 by remember { mutableStateOf("") }
    var amountValue2 by remember { mutableStateOf("") }
    var showAmountDropdown by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // --- Uncategorized Filter Chip ---
        FilterChip(
            selected = filters.showUncategorized,
            onClick = { onToggleUncategorized(!filters.showUncategorized) }, // Use the lambda
            label = { Text("Uncategorized") },
            leadingIcon = if (filters.showUncategorized) {
                { Icon(Icons.Filled.Check, contentDescription = "Uncategorized filter selected") }
            } else null
        )

        // --- Amount Filter Dropdown ---
        Box {
            OutlinedButton(onClick = { showAmountDropdown = true }) {
                Text(filters.amountType.name.replace('_', ' '))
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Open amount filter options")
            }

            DropdownMenu(
                expanded = showAmountDropdown,
                onDismissRequest = { showAmountDropdown = false }
            ) {
                AmountFilterType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.name.replace('_', ' ')) },
                        onClick = {
                            onSetAmountFilter(type, null, null) // Use the lambda
                            amountValue1 = ""
                            amountValue2 = ""
                            showAmountDropdown = false
                        }
                    )
                }
            }
        }
    }

    // --- Conditionally display TextFields for Amount Filter ---
    if (filters.amountType != AmountFilterType.ALL) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = amountValue1,
                onValueChange = {
                    amountValue1 = it
                    onSetAmountFilter( // Use the lambda
                        filters.amountType,
                        it.toDoubleOrNull(),
                        amountValue2.toDoubleOrNull()
                    )
                },
                label = {
                    val label = when (filters.amountType) {
                        AmountFilterType.GREATER_THAN -> "Greater Than"
                        AmountFilterType.LESS_THAN -> "Less Than"
                        AmountFilterType.RANGE -> "Min Amount"
                        else -> ""
                    }
                    Text(label)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )

            if (filters.amountType == AmountFilterType.RANGE) {
                OutlinedTextField(
                    value = amountValue2,
                    onValueChange = {
                        amountValue2 = it
                        onSetAmountFilter( // Use the lambda
                            filters.amountType,
                            amountValue1.toDoubleOrNull(),
                            it.toDoubleOrNull()
                        )
                    },
                    label = { Text("Max Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
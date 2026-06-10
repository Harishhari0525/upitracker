@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.example.upitracker.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.upitracker.R
import com.example.upitracker.ui.components.TransactionCardWithMenu
import com.example.upitracker.ui.components.UpiLiteSummaryCard
import com.example.upitracker.viewmodel.*
import com.example.upitracker.ui.components.expressive.ExpressiveTopBar
import com.example.upitracker.util.ExpressiveTokens
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.text.font.FontWeight
import com.example.upitracker.data.Category
import com.example.upitracker.ui.components.CategoryIconView
import com.example.upitracker.ui.components.FilteredTotalsBar
import com.example.upitracker.ui.components.LottieEmptyState
import com.example.upitracker.util.DecimalInputVisualTransformation
import com.example.upitracker.util.getCategoryIcon
import com.example.upitracker.util.parseColor
import kotlinx.coroutines.flow.drop
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val isUpiLiteEnabled by mainViewModel.isUpiLiteEnabled.collectAsState()
    val pageCount = if (isUpiLiteEnabled) { 2 } else { 1 }
    val pagerState = rememberPagerState { pageCount }
    val coroutineScope = rememberCoroutineScope()

    val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDetailSheet by remember { mutableStateOf(false) }

    val showFilterSheet by mainViewModel.showHistoryFilterSheet.collectAsState()
    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showCategorizeDialog by remember { mutableStateOf(false) }

    val filters by mainViewModel.filters.collectAsState()
    val isSelectionMode by mainViewModel.isSelectionModeActive.collectAsState()
    val selectedIds by mainViewModel.selectedTransactionIds.collectAsState()

    // ✨ Streams real-time progress calculations directly into the layout tree
    val syncProgress by mainViewModel.smsSyncProgress.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            mainViewModel.clearAllHistoryFilters()
        }
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            AnimatedContent(
                targetState = isSelectionMode,
                transitionSpec = {
                    (slideInVertically { height -> -height } + fadeIn()) togetherWith
                            (slideOutVertically { height -> -height } + fadeOut())
                },
                label = "history_top_bar_animation"
            ) { selectionMode ->
                if (selectionMode) {
                    SelectionModeTopAppBar(
                        selectionCount = selectedIds.size,
                        onCancelClick = { mainViewModel.clearSelection() },
                        onCategorizeClick = { showCategorizeDialog = true }
                    )
                } else {
                    ExpressiveTopBar(
                        title = "History",
                        subtitle = "Search, filter, and manage transactions"
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ✨ Real-Time SMS Processing Status Dashboard Card Overlay Banner
            AnimatedVisibility(
                visible = syncProgress.isSyncing,
                enter = fadeIn() + slideInVertically { -it / 3 },
                exit = fadeOut() + slideOutVertically { -it / 3 }
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = ExpressiveTokens.spacing.lg,
                            end = ExpressiveTokens.spacing.lg,
                            bottom = ExpressiveTokens.spacing.sm
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (syncProgress.isInitialImport) "Importing SMS Ledger..." else "Refreshing Transactions...",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Text(
                                text = "${syncProgress.currentProgress} / ${syncProgress.totalMessages}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        LinearProgressIndicator(
                            progress = { syncProgress.percentage },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = pagerState.currentPage == 0,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    OutlinedTextField(
                        value = filters.searchQuery,
                        onValueChange = { mainViewModel.setSearchQuery(it) },
                        placeholder = {
                            Text(text = stringResource(R.string.search_hint))
                        },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        },
                        trailingIcon = {
                            if (filters.searchQuery.isNotBlank()) {
                                IconButton(
                                    onClick = { mainViewModel.setSearchQuery("") }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear search"
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(ExpressiveTokens.corners.extraLarge.topStart),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = ExpressiveTokens.spacing.lg,
                                end = ExpressiveTokens.spacing.lg,
                                bottom = ExpressiveTokens.spacing.sm
                            ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )

                    ActiveFiltersRow(
                        filters = filters,
                        onClearDateFilter = { mainViewModel.clearDateRangeFilter() },
                        onClearAmountFilter = {
                            mainViewModel.setAmountFilter(
                                AmountFilterType.ALL,
                                null,
                                null
                            )
                        },
                        onClearUncategorizedFilter = {
                            mainViewModel.toggleUncategorizedFilter(false)
                        },
                        onDateFilterClick = { mainViewModel.onFilterClick() },
                        onAmountFilterClick = { mainViewModel.onFilterClick() },
                        onUncategorizedFilterClick = { mainViewModel.onFilterClick() },
                        onCategoryChipClick = { categoryName ->
                            mainViewModel.toggleCategoryFilter(categoryName)
                        },
                        onClearAllCategories = {
                            mainViewModel.clearCategoryFilter()
                        },
                        onClearBankFilter = {
                            mainViewModel.setBankFilter(null)
                        }
                    )
                }
            }


            val tabTitles = if (isUpiLiteEnabled) {
                listOf("UPI", "UPI Lite")
            } else {
                listOf("UPI")
            }

            val tabIcons = if (isUpiLiteEnabled) {
                listOf(
                    Icons.Filled.AccountBalanceWallet,
                    Icons.Filled.Summarize
                )
            } else {
                listOf(Icons.Filled.AccountBalanceWallet)
            }

            if (isUpiLiteEnabled) {
                SecondaryTabRow(
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
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = tabIcons[index],
                                    contentDescription = title
                                )
                            }
                        )
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { pageIndex ->
                when (pageIndex) {
                    0 -> {
                        UpiTransactionsList(
                            mainViewModel = mainViewModel,
                            onShowDetails = { showDetailSheet = true },
                            isSelectionMode = isSelectionMode,
                            selectedIds = selectedIds
                        )
                    }

                    1 -> {
                        if (isUpiLiteEnabled) {
                            UpiLiteSummariesList(mainViewModel)
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { mainViewModel.onFilterSheetDismiss() },
            sheetState = filterSheetState
        ) {
            FilterSheetContent(
                mainViewModel = mainViewModel,
                filters = filters
            )
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
                    }.invokeOnCompletion {
                        if (!detailSheetState.isVisible) {
                            showDetailSheet = false
                            mainViewModel.selectTransaction(null)
                        }
                    }
                }
            )
        }
    }

    if (showCategorizeDialog) {
        val allCategories by mainViewModel.allCategories.collectAsState()

        BulkCategorizeDialog(
            categories = allCategories,
            onDismiss = { showCategorizeDialog = false },
            onCategorySelected = { categoryName ->
                mainViewModel.categorizeSelectedTransactions(categoryName)
                showCategorizeDialog = false
            }
        )
    }
}

@Composable
private fun UpiTransactionsList(
    mainViewModel: MainViewModel,
    onShowDetails: () -> Unit,
    isSelectionMode: Boolean,
    selectedIds: Set<Int>
) {
    val selectedUpiFilterType by mainViewModel.selectedUpiTransactionType.collectAsState()
    val upiSortField by mainViewModel.upiTransactionSortField.collectAsState()
    val upiSortOrder by mainViewModel.upiTransactionSortOrder.collectAsState()
    val groupedTransactions by mainViewModel.filteredUpiTransactions.collectAsState()
    val allCategories by mainViewModel.allCategories.collectAsState()

    val totals by mainViewModel.filteredTotals.collectAsState()
    val isLoading by mainViewModel.isHistoryLoading.collectAsState()

    val filters by mainViewModel.filters.collectAsState()
    val areFiltersActive = remember(filters) {
        filters.searchQuery.isNotBlank() ||
                filters.type != UpiTransactionTypeFilter.ALL ||
                filters.startDate != null ||
                filters.endDate != null ||
                filters.amountType != AmountFilterType.ALL ||
                filters.showUncategorized ||
                filters.showOnlyLinked ||
                filters.selectedCategories.isNotEmpty() ||
                filters.bankNameFilter != null
    }

    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        snapshotFlow { Triple(filters, upiSortField, upiSortOrder) }
            .drop(1)
            .collect {
                listState.animateScrollToItem(0)
            }
    }

    LaunchedEffect(Unit) {
        mainViewModel.uiEvents.collect { event ->
            if (event is MainViewModel.UiEvent.ScrollToTop) {
                listState.animateScrollToItem(index = 0)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = !isSelectionMode) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UpiTransactionTypeFilter.entries.forEach { filterType ->
                        // ✨ FIX 2: Safely formatting the text title string configuration map cleanly
                        val filterLabelText = remember(filterType) {
                            filterType.name.lowercase().replaceFirstChar { it.uppercase() }
                        }
                        FilterChip(
                            modifier = Modifier.padding(end = 8.dp),
                            selected = selectedUpiFilterType == filterType,
                            onClick = { mainViewModel.setUpiTransactionTypeFilter(filterType) },
                            label = { Text(filterLabelText) },
                            leadingIcon = if (selectedUpiFilterType == filterType) { { Icon(Icons.Filled.Check, null) } } else null
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { mainViewModel.enterSelectionMode() }) {
                        Icon(Icons.Default.Checklist, contentDescription = "Enter Selection Mode")
                    }
                    IconButton(onClick = { mainViewModel.onFilterClick() }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Show Filters")
                    }
                }
                SortControls(
                    currentSortField = upiSortField,
                    currentSortOrder = upiSortOrder,
                    onSortFieldSelected = { field -> mainViewModel.setUpiTransactionSort(field) }
                )
            }
        }

        AnimatedVisibility(visible = groupedTransactions.isNotEmpty() && areFiltersActive) {
            FilteredTotalsBar(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                totalDebit = totals.totalDebit,
                totalCredit = totals.totalCredit
            )
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (isLoading) {
                CircularProgressIndicator()
            } else if (groupedTransactions.isEmpty()) {
                LottieEmptyState(
                    message = stringResource(R.string.empty_state_no_upi_transactions_history_filtered),
                    lottieResourceId = R.raw.empty_box_animation
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    groupedTransactions.forEach { (monthYear, transactionsInMonth) ->
                        stickyHeader(key = monthYear) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    tonalElevation = 3.dp
                                ) {
                                    Text(
                                        text = monthYear,
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.padding(
                                            vertical = 9.dp,
                                            horizontal = 100.dp
                                        ),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                        itemsIndexed(
                            items = transactionsInMonth,
                            key = { _, txn -> "txn-${txn.id}" },
                            contentType = { _, _ -> "TransactionItem" }
                        ) { _, transaction ->
                            val isSelected = selectedIds.contains(transaction.id)
                            val categoryDetails = allCategories.find { c ->
                                c.name.equals(transaction.category, ignoreCase = true)
                            }
                            val categoryColor = parseColor(categoryDetails?.colorHex ?: "#808080")
                            val categoryIcon = getCategoryIcon(categoryDetails)

                            TransactionCardWithMenu(
                                modifier = Modifier
                                    .animateItem(
                                        fadeInSpec = tween(durationMillis = 200),
                                        placementSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        ),
                                        fadeOutSpec = tween(durationMillis = 150)
                                    ),
                                transaction = transaction,
                                isSelectionMode = isSelectionMode,
                                isSelected = isSelected,
                                showCheckbox = isSelectionMode,
                                onToggleSelection = { mainViewModel.toggleSelection(transaction.id) },
                                onShowDetails = {
                                    mainViewModel.selectTransaction(transaction.id)
                                    onShowDetails()
                                },
                                onDelete = { mainViewModel.deleteTransaction(it) },
                                onArchiveAction = {
                                    mainViewModel.toggleTransactionArchiveStatus(it, true)
                                },
                                archiveActionText = "Archive",
                                archiveActionIcon = Icons.Default.Archive,
                                categoryColor = categoryColor,
                                categoryIcon = categoryIcon,
                                onCategoryClick = { categoryName ->
                                    if (!isSelectionMode) mainViewModel.toggleCategoryFilter(categoryName)
                                }
                            )
                        }
                    }
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
    val groupedSummaries by mainViewModel.groupedUpiLiteSummaries.collectAsState()
    val filters by mainViewModel.filters.collectAsState()

    // ✨ FIX 1: Explicitly allocate and instantiate the state within the remember block so it doesn't return Unit
    val listState = remember(filters, upiLiteSortField, upiLiteSortOrder) {
        LazyListState()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        UpiLiteSummarySortControls(
            currentSortField = upiLiteSortField,
            currentSortOrder = upiLiteSortOrder,
            onSortFieldSelected = { field -> mainViewModel.setUpiLiteSummarySort(field) }
        )

        if (filteredUpiLiteSummaries.isEmpty()) {
            LottieEmptyState(
                message = stringResource(R.string.empty_state_no_upi_lite_summaries_history_filtered),
                lottieResourceId = R.raw.empty_box_animation
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(all = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                groupedSummaries.forEach { group ->
                    stickyHeader(key = group.monthYear) {
                        MonthlyHeader(
                            title = group.monthYear,
                            total = group.monthlyTotal,
                            count = group.summaries.size
                        )
                    }
                    items(group.summaries, key = { "lite-${it.id}" }) { summary ->
                        UpiLiteSummaryCard(summary = summary)
                    }
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
    var showDateRangePicker by remember { mutableStateOf(false) }
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = filters.startDate,
        initialSelectedEndDateMillis = filters.endDate
    )

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
            onStartDateClick = { showDateRangePicker = true },
            onEndDateClick = { showDateRangePicker = true },
            onClearDates = { mainViewModel.clearDateRangeFilter() }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Show only linked transactions", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = filters.showOnlyLinked,
                onCheckedChange = { mainViewModel.toggleShowOnlyLinked(it) }
            )
        }

        AdvancedFilterControls(
            filters = filters,
            onToggleUncategorized = { mainViewModel.toggleUncategorizedFilter(it) },
            onSetAmountFilter = { type, val1, val2 -> mainViewModel.setAmountFilter(type, val1, val2) }
        )
        Spacer(modifier = Modifier.navigationBarsPadding())

        if (showDateRangePicker) {
            DatePickerDialog(
                onDismissRequest = { showDateRangePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        showDateRangePicker = false
                        val start = dateRangePickerState.selectedStartDateMillis
                        val end = dateRangePickerState.selectedEndDateMillis
                        
                        val processedStart = start?.let {
                            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                            cal.timeInMillis = it
                            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                            cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                            cal.timeInMillis
                        }
                        
                        val processedEnd = end?.let {
                            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                            cal.timeInMillis = it
                            cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
                            cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
                            cal.timeInMillis
                        }
                        
                        mainViewModel.setDateRangeFilter(processedStart, processedEnd)
                    }) { Text(stringResource(R.string.dialog_button_ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { showDateRangePicker = false }) { Text(stringResource(R.string.dialog_button_cancel)) }
                }
            ) {
                DateRangePicker(
                    state = dateRangePickerState,
                    modifier = Modifier.weight(1f).padding(16.dp),
                    title = {
                        Text(
                            text = "Select Date Range",
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp),
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    headline = {
                        Text(
                            text = "Choose statement period",
                            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                )
            }
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
    onUncategorizedFilterClick: () -> Unit,
    onCategoryChipClick: (String) -> Unit,
    onClearAllCategories: () -> Unit,
    onClearBankFilter: () -> Unit
) {
    val showRow = filters.startDate != null ||
            filters.amountType != AmountFilterType.ALL ||
            filters.showUncategorized ||
            filters.selectedCategories.isNotEmpty() ||
            filters.bankNameFilter != null

    AnimatedVisibility(visible = showRow) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (filters.startDate != null) {
                item {
                    val formatter = remember {
                        SimpleDateFormat("dd MMM", Locale.getDefault()).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }
                    }
                    val start = formatter.format(Date(filters.startDate))
                    val end = filters.endDate?.let { formatter.format(Date(it)) } ?: "Now"
                    FilterChip(
                        selected = true,
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
            }

            if (filters.amountType != AmountFilterType.ALL) {
                item {
                    val amountFilterText =
                        remember(filters.amountType, filters.amountValue1, filters.amountValue2) {
                            val val1 = filters.amountValue1?.toInt()
                            val val2 = filters.amountValue2?.toInt()
                            // ✨ FIX 3: Fully exhaustive branch match tracking criteria options
                            when (filters.amountType) {
                                AmountFilterType.GREATER_THAN -> "> ₹${val1 ?: ""}"
                                AmountFilterType.LESS_THAN -> "< ₹${val1 ?: ""}"
                                AmountFilterType.RANGE -> "₹${val1 ?: ""} - ₹${val2 ?: ""}"
                                AmountFilterType.ALL -> "All Amounts"
                            }
                        }

                    FilterChip(
                        selected = true,
                        onClick = onAmountFilterClick,
                        label = { Text(amountFilterText) },
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
            }

            if (filters.showUncategorized) {
                item {
                    FilterChip(
                        selected = true,
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
            items(filters.selectedCategories.toList()) { categoryName ->
                FilterChip(
                    selected = true,
                    onClick = { },
                    label = { Text(categoryName) },
                    trailingIcon = {
                        Icon(
                            Icons.Default.Cancel,
                            contentDescription = "Clear $categoryName filter",
                            modifier = Modifier
                                .size(FilterChipDefaults.IconSize)
                                .clickable { onCategoryChipClick(categoryName) }
                        )
                    }
                )
            }

            if (filters.bankNameFilter != null) {
                item {
                    FilterChip(
                        selected = true,
                        onClick = { },
                        label = { Text(filters.bankNameFilter) },
                        leadingIcon = { Icon(Icons.Filled.AccountBalance, contentDescription = "Bank Filter") },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Cancel,
                                contentDescription = "Clear bank filter",
                                modifier = Modifier
                                    .size(FilterChipDefaults.IconSize)
                                    .clickable(onClick = onClearBankFilter)
                            )
                        }
                    )
                }
            }

            if (filters.selectedCategories.size > 1) {
                item {
                    InputChip(
                        selected = false,
                        onClick = onClearAllCategories,
                        label = { Text("Clear All") },
                        enabled = true,
                        colors = InputChipDefaults.inputChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    )
                }
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
    val displayDateFormat = remember {
        SimpleDateFormat("dd MMM yy", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
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
            Spacer(Modifier.width(48.dp))
        }
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
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (isSelected) {
            Icon(
                imageVector = if (sortOrder == SortOrder.ASCENDING) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                contentDescription = stringResource(if (sortOrder == SortOrder.ASCENDING) R.string.history_sort_order_ascending_desc else R.string.history_sort_order_descending_desc),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp).padding(start = 2.dp)
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
            .padding(horizontal = 16.dp, vertical = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.history_sort_by_label), style = MaterialTheme.typography.labelSmall)

        SortButton(
            text = stringResource(R.string.history_sort_by_date),
            isSelected = currentSortField == SortableTransactionField.DATE,
            sortOrder = if (currentSortField == SortableTransactionField.DATE) currentSortOrder else SortOrder.DESCENDING,
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
            sortOrder = if (currentSortField == SortableTransactionField.CATEGORY) currentSortOrder else SortOrder.ASCENDING,
            onClick = { onSortFieldSelected(SortableTransactionField.CATEGORY) }
        )
    }
}

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
        SortableUpiLiteSummaryField.entries.forEach { field ->
            SortButton(
                text = field.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                isSelected = currentSortField == field,
                sortOrder = if (currentSortField == field) currentSortOrder else SortOrder.DESCENDING,
                onClick = { onSortFieldSelected(field) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedFilterControls(
    filters: TransactionFilters,
    onToggleUncategorized: (Boolean) -> Unit,
    onSetAmountFilter: (AmountFilterType, Double?, Double?) -> Unit
) {
    var amountValue1 by remember { mutableStateOf(filters.amountValue1?.toString() ?: "") }
    var amountValue2 by remember { mutableStateOf(filters.amountValue2?.toString() ?: "") }
    var showAmountDropdown by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = filters.showUncategorized,
            onClick = { onToggleUncategorized(!filters.showUncategorized) },
            label = { Text("Uncategorized") },
            leadingIcon = if (filters.showUncategorized) {
                { Icon(Icons.Filled.Check, contentDescription = "Uncategorized filter selected") }
            } else null
        )

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
                            onSetAmountFilter(type, null, null)
                            amountValue1 = ""
                            amountValue2 = ""
                            showAmountDropdown = false
                        }
                    )
                }
            }
        }
    }

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
                onValueChange = { newValue ->
                    val cleaned = newValue.filter { it.isDigit() || it == '.' }
                    if (cleaned.count { it == '.' } <= 1) {
                        amountValue1 = cleaned
                        onSetAmountFilter(
                            filters.amountType,
                            cleaned.toDoubleOrNull(),
                            amountValue2.toDoubleOrNull()
                        )
                    }
                },
                label = {
                    // ✨ FIX 3: Fully exhaustive when branch condition mapping parameters safely
                    val label = when (filters.amountType) {
                        AmountFilterType.GREATER_THAN -> "Greater Than"
                        AmountFilterType.LESS_THAN -> "Less Than"
                        AmountFilterType.RANGE -> "Min Amount"
                        AmountFilterType.ALL -> "Amount"
                    }
                    Text(label)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                prefix = { Text("₹") },
                visualTransformation = DecimalInputVisualTransformation(),
                modifier = Modifier.weight(1f)
            )

            if (filters.amountType == AmountFilterType.RANGE) {
                OutlinedTextField(
                    value = amountValue2,
                    onValueChange = { newValue ->
                        val cleaned = newValue.filter { it.isDigit() || it == '.' }
                        if (cleaned.count { it == '.' } <= 1) {
                            amountValue2 = cleaned
                            onSetAmountFilter(
                                filters.amountType,
                                amountValue1.toDoubleOrNull(),
                                cleaned.toDoubleOrNull()
                            )
                        }
                    },
                    label = { Text("Max Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    prefix = { Text("₹") },
                    visualTransformation = DecimalInputVisualTransformation(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BulkCategorizeDialog(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onCategorySelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Apply a Category") },
        text = {
            LazyColumn {
                items(categories) { category ->
                    ListItem(
                        headlineContent = { Text(category.name) },
                        modifier = Modifier.clickable { onCategorySelected(category.name) },
                        leadingContent = {
                            val categoryIcon = getCategoryIcon(category)
                            CategoryIconView(
                                categoryIcon = categoryIcon,
                                size = FilterChipDefaults.IconSize,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                iconTint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SelectionModeTopAppBar(selectionCount: Int, onCancelClick: () -> Unit, onCategorizeClick: () -> Unit) {
    TopAppBar(
        title = { Text("$selectionCount selected") },
        navigationIcon = { IconButton(onClick = onCancelClick) { Icon(Icons.Filled.Close, contentDescription = "Cancel selection") } },
        actions = { TextButton(onClick = onCategorizeClick) { Text("CATEGORIZE") } },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        windowInsets = WindowInsets(0)
    )
}

@Composable
private fun MonthlyHeader(title: String, total: Double, count: Int) {
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.Builder()
        .setLanguage("en")
        .setRegion("IN")
        .build()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = currencyFormatter.format(total),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$count items",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}
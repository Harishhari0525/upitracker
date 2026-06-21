@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.upitracker.ui.screens

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.upitracker.R
import com.example.upitracker.ui.components.LottieEmptyState
import com.example.upitracker.ui.components.TransactionCard
import com.example.upitracker.ui.components.expressive.ExpressiveSectionHeader
import com.example.upitracker.ui.components.expressive.ExpressiveTopBar
import com.example.upitracker.util.ExpressiveTokens
import com.example.upitracker.util.getCategoryIcon
import com.example.upitracker.util.parseColor
import com.example.upitracker.viewmodel.PassbookTransactionType
import com.example.upitracker.viewmodel.PassbookViewModel
import com.example.upitracker.viewmodel.PassbookSummaryState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems

@Composable
fun PassbookScreen(
    onBack: () -> Unit,
    passbookViewModel: PassbookViewModel = viewModel()
) {
    val transactionType by passbookViewModel.transactionType.collectAsState()
    val transactions = passbookViewModel.filteredTransactions.collectAsLazyPagingItems()
    val transactionCount by passbookViewModel.passbookTransactionCount.collectAsState()
    val allCategories by passbookViewModel.allCategories.collectAsState()
    val startDate by passbookViewModel.startDate.collectAsState()
    val endDate by passbookViewModel.endDate.collectAsState()

    // ✨ NEW OBSERVER: Connect directly to the calculated on-screen ledger sums
    val accountingSummary by passbookViewModel.passbookSummary.collectAsState()

    val showBankName by passbookViewModel.showBankName.collectAsState()
    val includeCategoryBreakdown by passbookViewModel.includeCategoryBreakdown.collectAsState()

    var isPeriodMenuExpanded by remember { mutableStateOf(false) }
    var selectedPeriodLabel by remember { mutableStateOf("This Month") }
    val context = LocalContext.current

    var showDateRangePicker by remember { mutableStateOf(false) }

    val dateRangePickerState = key(startDate, endDate) {
        rememberDateRangePickerState(
            initialSelectedStartDateMillis = startDate,
            initialSelectedEndDateMillis = endDate
        )
    }

    val coroutineScope = rememberCoroutineScope()
    var isExportingPdf by remember { mutableStateOf(false) }

    val pdfSaverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
        onResult = { uri ->
            uri?.let { safeUri ->
                isExportingPdf = true
                coroutineScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            // ✨ CLEAN CALL: Invoking the unified background PDF compiler method
                            passbookViewModel.generateAndSavePdf(context, safeUri)
                        }
                    } catch (e: Exception) {
                        Log.e("PdfExportError", "Error compiling PDF passbook document context lines", e)
                    } finally {
                        isExportingPdf = false
                    }
                }
            }
        }
    )

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.Builder()
        .setLanguage("en")
        .setRegion("IN")
        .build()).apply {
        maximumFractionDigits = 2
    }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            ExpressiveTopBar(
                title = "Statement",
                subtitle = "Generate a PDF passbook for selected transactions",
                showBackButton = true,
                onBackClick = onBack
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    val filename = "Passbook_${dateFormat.format(Date())}.pdf"
                    pdfSaverLauncher.launch(filename)
                },
                enabled = !isExportingPdf && transactionCount > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(
                        start = ExpressiveTokens.spacing.lg,
                        end = ExpressiveTokens.spacing.lg,
                        top = ExpressiveTokens.spacing.sm,
                        bottom = ExpressiveTokens.spacing.md
                    )
                    .height(54.dp),
                shape = ExpressiveTokens.corners.large
            ) {
                if (isExportingPdf) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Compiling Passbook Ledger...")
                } else {
                    Icon(imageVector = Icons.Filled.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                    Text("Generate Passbook PDF")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = ExpressiveTokens.spacing.lg,
                top = ExpressiveTokens.spacing.lg,
                end = ExpressiveTokens.spacing.lg,
                bottom = ExpressiveTokens.spacing.xl
            ),
            verticalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.md)
        ) {
            item {
                ExpressiveSectionHeader(
                    title = "Statement Options",
                    subtitle = "Choose period, custom dates, and transaction type"
                )
            }

            item {
                ExposedDropdownMenuBox(
                    expanded = isPeriodMenuExpanded,
                    onExpandedChange = { isPeriodMenuExpanded = !isPeriodMenuExpanded }
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        readOnly = true,
                        value = selectedPeriodLabel,
                        onValueChange = {},
                        label = { Text("Statement Period") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPeriodMenuExpanded) },
                        shape = ExpressiveTokens.corners.large
                    )

                    ExposedDropdownMenu(
                        expanded = isPeriodMenuExpanded,
                        onDismissRequest = { isPeriodMenuExpanded = false }
                    ) {
                        listOf(
                            "This Month" to { passbookViewModel.setThisMonth() },
                            "Last Month" to { passbookViewModel.setLastMonth() },
                            "This Year" to { passbookViewModel.setThisYear() },
                            "Current Financial Year" to { passbookViewModel.setFinancialYear() },
                            "Previous Financial Year" to { passbookViewModel.setPreviousFinancialYear() }
                        ).forEach { (label, action) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    action()
                                    selectedPeriodLabel = label
                                    isPeriodMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DateChip(
                        label = "Start Date",
                        timestamp = startDate,
                        onClick = { showDateRangePicker = true },
                        modifier = Modifier.weight(1f)
                    )

                    DateChip(
                        label = "End Date",
                        timestamp = endDate,
                        onClick = { showDateRangePicker = true },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PassbookTransactionType.entries.forEachIndexed { index, type ->
                        SegmentedButton(
                            selected = transactionType == type,
                            onClick = { passbookViewModel.setTransactionType(type) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = PassbookTransactionType.entries.size
                            )
                        ) {
                            Text(type.name)
                        }
                    }
                }
            }

            // ✨ NEW HIGH-DENSITY FEATURE: Live Account Passbook Summary Card Block
            item {
                OnScreenSummaryCard(
                    summary = accountingSummary,
                    formatter = currencyFormatter
                )
            }

            item {
                Card(
                    shape = ExpressiveTokens.corners.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "PDF CUSTOMIZATION OPTIONS",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Show Bank Names",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Prefix transactions with [Bank] in statement",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = showBankName,
                                onCheckedChange = { passbookViewModel.setShowBankName(it) }
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Category Breakdown Table",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Include spending totals per category at the end",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = includeCategoryBreakdown,
                                onCheckedChange = { passbookViewModel.setIncludeCategoryBreakdown(it) }
                            )
                        }
                    }
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = ExpressiveTokens.spacing.xs))
            }

            item {
                ExpressiveSectionHeader(
                    title = "Ledger Preview",
                    subtitle = "$transactionCount transactions matching query bounds"
                )
            }

            when (val refreshState = transactions.loadState.refresh) {
                is LoadState.Loading -> item {
                    Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is LoadState.Error -> item {
                    Column(
                        Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Could not load statement: ${refreshState.error.message ?: "Unknown error"}")
                        Button(onClick = { transactions.retry() }) { Text("Retry") }
                    }
                }
                is LoadState.NotLoading -> if (transactions.itemCount == 0) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LottieEmptyState(
                            message = "No transactions found for the selected parameters.",
                            lottieResourceId = R.raw.empty_box_animation
                        )
                    }
                }
                } else {
                items(
                    count = transactions.itemCount,
                    key = { index -> transactions.peek(index)?.let { "passbook-txn-${it.id}" } ?: "passbook-placeholder-$index" }
                ) { index ->
                    val transaction = transactions[index] ?: return@items
                    val categoryDetails = allCategories.find { category ->
                        category.name.equals(transaction.category, ignoreCase = true)
                    }

                    TransactionCard(
                        transaction = transaction,
                        categoryColor = parseColor(categoryDetails?.colorHex ?: "#808080"),
                        categoryIcon = getCategoryIcon(categoryDetails),
                        onCategoryClick = {},
                        isSelected = false,
                        showCheckbox = false
                    )
                }
                if (transactions.loadState.append is LoadState.Loading) {
                    item { Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                }
                if (transactions.loadState.append is LoadState.Error) {
                    item { TextButton(onClick = { transactions.retry() }) { Text("Retry loading more") } }
                }
                }
            }
        }
    }

    // --- Date Picker Dialog Management Windows ---
    if (showDateRangePicker) {
        DatePickerDialog(
            onDismissRequest = { showDateRangePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDateRangePicker = false
                        val start = dateRangePickerState.selectedStartDateMillis
                        val end = dateRangePickerState.selectedEndDateMillis
                        
                        val processedStart = start?.let {
                            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = it
                                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                        }
                        
                        val processedEnd = end?.let {
                            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = it
                                set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
                                set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
                            }.timeInMillis
                        }
                        
                        passbookViewModel.setDateRange(processedStart, processedEnd)
                        selectedPeriodLabel = "Custom"
                    }
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDateRangePicker = false }) { Text("Cancel") } }
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

// ✨ COMPONENT DESIGN: High-Density Premium Accounting Aggregate Card View Block
@Composable
private fun OnScreenSummaryCard(
    summary: PassbookSummaryState,
    formatter: NumberFormat
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "ACCOUNT FLOW DIAGNOSTICS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = "Total Credits (+)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = formatter.format(summary.totalCredit),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A) // Handled dynamically via text color tokens
                    )
                }

                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = "Total Debits (-)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = formatter.format(summary.totalDebit),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Net Velocity Shift",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${if (summary.netBalance >= 0) "+" else ""}${formatter.format(summary.netBalance)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (summary.netBalance >= 0) Color(0xFF16A34A) else Color(0xFFDC2626)
                )
            }
        }
    }
}

@Composable
private fun DateChip(
    label: String,
    timestamp: Long?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayDateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val displayText = timestamp?.let { displayDateFormat.format(Date(it)) } ?: label

    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = ExpressiveTokens.corners.large
    ) {
        Icon(
            imageVector = Icons.Default.DateRange,
            contentDescription = label,
            modifier = Modifier.size(ButtonDefaults.IconSize)
        )
        Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
        Text(displayText)
    }
}

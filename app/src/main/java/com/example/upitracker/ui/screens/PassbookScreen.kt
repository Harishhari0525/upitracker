@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.upitracker.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun PassbookScreen(
    onBack: () -> Unit,
    passbookViewModel: PassbookViewModel = viewModel()
) {
    val transactionType by passbookViewModel.transactionType.collectAsState()
    val transactions by passbookViewModel.filteredTransactions.collectAsState()
    val allCategories by passbookViewModel.allCategories.collectAsState()
    val startDate by passbookViewModel.startDate.collectAsState()
    val endDate by passbookViewModel.endDate.collectAsState()

    var isPeriodMenuExpanded by remember { mutableStateOf(false) }
    var selectedPeriodLabel by remember { mutableStateOf("This Month") }

    val context = LocalContext.current

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val startDatePickerState = rememberDatePickerState(initialSelectedDateMillis = startDate)
    val endDatePickerState = rememberDatePickerState(initialSelectedDateMillis = endDate)

    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()

    val pdfSaverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
        onResult = { uri ->
            uri?.let {
                passbookViewModel.generateAndSavePdf(
                    context,
                    it,
                    primaryColor,
                    textColor
                )
            }
        }
    )

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
                    val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                    val filename = "Passbook_${dateFormat.format(Date())}.pdf"
                    pdfSaverLauncher.launch(filename)
                },
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
                Icon(
                    imageVector = Icons.Filled.PictureAsPdf,
                    contentDescription = null
                )

                Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))

                Text("Generate PDF")
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
                    onExpandedChange = {
                        isPeriodMenuExpanded = !isPeriodMenuExpanded
                    }
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        readOnly = true,
                        value = selectedPeriodLabel,
                        onValueChange = {},
                        label = { Text("Statement Period") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = isPeriodMenuExpanded
                            )
                        },
                        shape = ExpressiveTokens.corners.large
                    )

                    ExposedDropdownMenu(
                        expanded = isPeriodMenuExpanded,
                        onDismissRequest = { isPeriodMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("This Month") },
                            onClick = {
                                passbookViewModel.setThisMonth()
                                selectedPeriodLabel = "This Month"
                                isPeriodMenuExpanded = false
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Last Month") },
                            onClick = {
                                passbookViewModel.setLastMonth()
                                selectedPeriodLabel = "Last Month"
                                isPeriodMenuExpanded = false
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("This Year") },
                            onClick = {
                                passbookViewModel.setThisYear()
                                selectedPeriodLabel = "This Year"
                                isPeriodMenuExpanded = false
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Previous Financial Year") },
                            onClick = {
                                passbookViewModel.setPreviousFinancialYear()
                                selectedPeriodLabel = "Previous Financial Year"
                                isPeriodMenuExpanded = false
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Current Financial Year") },
                            onClick = {
                                passbookViewModel.setFinancialYear()
                                selectedPeriodLabel = "Current Financial Year"
                                isPeriodMenuExpanded = false
                            }
                        )
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
                        onClick = { showStartDatePicker = true },
                        modifier = Modifier.weight(1f)
                    )

                    DateChip(
                        label = "End Date",
                        timestamp = endDate,
                        onClick = { showEndDatePicker = true },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Text(
                    text = "Transaction Type",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
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

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = ExpressiveTokens.spacing.sm)
                )
            }

            item {
                ExpressiveSectionHeader(
                    title = "Preview",
                    subtitle = "${transactions.size} transactions found"
                )
            }

            if (transactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LottieEmptyState(
                            message = "No transactions found for the selected period.",
                            lottieResourceId = R.raw.empty_box_animation
                        )
                    }
                }
            } else {
                items(
                    items = transactions,
                    key = { it.id }
                ) { transaction ->
                    val categoryDetails = allCategories.find { category ->
                        category.name.equals(
                            transaction.category,
                            ignoreCase = true
                        )
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
            }
        }
    }

    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showStartDatePicker = false

                        val selectedMillis = startDatePickerState.selectedDateMillis?.let {
                            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                            calendar.timeInMillis = it
                            calendar.set(Calendar.HOUR_OF_DAY, 0)
                            calendar.set(Calendar.MINUTE, 0)
                            calendar.set(Calendar.SECOND, 0)
                            calendar.set(Calendar.MILLISECOND, 0)
                            calendar.timeInMillis
                        }

                        passbookViewModel.setDateRange(selectedMillis, endDate)
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showStartDatePicker = false }
                ) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = startDatePickerState)
        }
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEndDatePicker = false

                        val selectedMillis = endDatePickerState.selectedDateMillis?.let {
                            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                            calendar.timeInMillis = it
                            calendar.set(Calendar.HOUR_OF_DAY, 23)
                            calendar.set(Calendar.MINUTE, 59)
                            calendar.set(Calendar.SECOND, 59)
                            calendar.set(Calendar.MILLISECOND, 999)
                            calendar.timeInMillis
                        }

                        passbookViewModel.setDateRange(startDate, selectedMillis)
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEndDatePicker = false }
                ) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = endDatePickerState)
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
    val displayDateFormat = remember {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    }

    val displayText = timestamp?.let {
        displayDateFormat.format(Date(it))
    } ?: label

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
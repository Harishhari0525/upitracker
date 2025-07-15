@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.upitracker.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
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
import com.example.upitracker.ui.components.LottieEmptyState
import com.example.upitracker.ui.components.TransactionCard
import com.example.upitracker.util.getCategoryIcon
import com.example.upitracker.util.parseColor
import com.example.upitracker.viewmodel.PassbookTransactionType
import com.example.upitracker.viewmodel.PassbookViewModel
import com.example.upitracker.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun PassbookScreen(
    onBack: () -> Unit,
    // ✅ Get an instance of our new ViewModel
    passbookViewModel: PassbookViewModel = viewModel()
) {
    // ✅ Collect the state from the ViewModel
    val transactionType by passbookViewModel.transactionType.collectAsState()
    val transactions by passbookViewModel.filteredTransactions.collectAsState()
    val allCategories by passbookViewModel.allCategories.collectAsState()
    val startDate by passbookViewModel.startDate.collectAsState()
    val endDate by passbookViewModel.endDate.collectAsState()

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
                passbookViewModel.generateAndSavePdf(context, it, primaryColor, textColor)
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = { Text("Generate Statement") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- Date Range Section ---
                Text("Select Period", style = MaterialTheme.typography.titleMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { passbookViewModel.setThisMonth() }, modifier = Modifier.weight(1f)) { Text("This Month") }
                    OutlinedButton(onClick = { passbookViewModel.setLastMonth() }, modifier = Modifier.weight(1f)) { Text("Last Month") }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { passbookViewModel.setThisYear() }, modifier = Modifier.weight(1f)) { Text("This Year") }
                    // ✅ NEW "Previous Year" Button
                    OutlinedButton(onClick = { passbookViewModel.setPreviousYear() }, modifier = Modifier.weight(1f)) { Text("Previous Year") }
                }
                OutlinedButton(onClick = { passbookViewModel.setFinancialYear() }, modifier = Modifier.fillMaxWidth()) { Text("Financial Year") }

                // ✅ NEW Custom Date Picker UI
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceAround) {
                    DateChip(label = "Start Date", timestamp = startDate, onClick = { showStartDatePicker = true })
                    DateChip(label = "End Date", timestamp = endDate, onClick = { showEndDatePicker = true })
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Transaction Type", style = MaterialTheme.typography.titleMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    PassbookTransactionType.entries.forEachIndexed { index, type ->
                        SegmentedButton(
                            selected = transactionType == type,
                            onClick = { passbookViewModel.setTransactionType(type) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = PassbookTransactionType.entries.size)
                        ) { Text(type.name) }
                    }
                }

                // --- Preview Area ---
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                if (transactions.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LottieEmptyState(message = "No transactions found for the selected period.", lottieResourceId = R.raw.empty_box_animation)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(transactions, key = { it.id }) { transaction ->
                            val categoryDetails = allCategories.find { c -> c.name.equals(transaction.category, ignoreCase = true) }
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

            // --- Generate Button ---
            Button(
                onClick = {
                    val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                    val filename = "Passbook_${dateFormat.format(Date())}.pdf"
                    pdfSaverLauncher.launch(filename)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .height(50.dp)
            ) {
                Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text("Generate PDF")
            }
        }
    }
    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showStartDatePicker = false
                    // Set time to the beginning of the selected day
                    val selectedMillis = startDatePickerState.selectedDateMillis?.let {
                        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                        cal.timeInMillis = it
                        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
                        cal.timeInMillis
                    }
                    passbookViewModel.setDateRange(selectedMillis, endDate)
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = startDatePickerState)
        }
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showEndDatePicker = false
                    // Set time to the end of the selected day
                    val selectedMillis = endDatePickerState.selectedDateMillis?.let {
                        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                        cal.timeInMillis = it
                        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(
                        Calendar.SECOND, 59)
                        cal.timeInMillis
                    }
                    passbookViewModel.setDateRange(startDate, selectedMillis)
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = endDatePickerState)
        }
    }
}
@Composable
private fun DateChip(
    label: String,
    timestamp: Long?,
    onClick: () -> Unit
) {
    val displayDateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val displayText = timestamp?.let { displayDateFormat.format(Date(it)) } ?: label

    OutlinedButton(onClick = onClick) {
        Icon(Icons.Default.DateRange, contentDescription = label, modifier = Modifier.size(ButtonDefaults.IconSize))
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text(displayText)
    }
}
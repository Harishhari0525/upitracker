package com.example.upitracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.upitracker.data.AppDatabase
import com.example.upitracker.data.Transaction
import com.example.upitracker.data.UpiLiteSummary
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.example.upitracker.util.ThemePreference
import com.example.upitracker.util.OnboardingPreference
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Date

// ... (MonthlyExpense, HistoryListItem, TransactionHistoryItem, SummaryHistoryItem data/sealed classes remain the same)
// ... (UpiTransactionTypeFilter enum remains the same)
// Data class for MonthlyExpense
data class MonthlyExpense(
    val yearMonth: String,
    val totalAmount: Double,
    val timestamp: Long
)
// Sealed interface for combined history list items
sealed interface HistoryListItem {
    val displayDate: Long
    val itemType: String
}
data class TransactionHistoryItem(val transaction: Transaction) : HistoryListItem {
    override val displayDate: Long get() = transaction.date
    override val itemType: String get() = "Transaction"
}
data class SummaryHistoryItem(val summary: UpiLiteSummary) : HistoryListItem {
    override val displayDate: Long get() = summary.date
    override val itemType: String get() = "UpiLiteSummary"
}

// Enum for UPI Transaction Type Filter
enum class UpiTransactionTypeFilter {
    ALL, DEBIT, CREDIT
}


class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val transactionDao = db.transactionDao()
    private val upiLiteSummaryDao = db.upiLiteSummaryDao()

    // Base data flows
    private val _transactions: StateFlow<List<Transaction>> = transactionDao.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _upiLiteSummaries: StateFlow<List<UpiLiteSummary>> = // Renamed to underscore for private base
        upiLiteSummaryDao.getAllSummaries()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ... (snackbarEvents, isImportingSms, isDarkMode, isOnboardingCompleted flows remain) ...
    private val _snackbarEvents = MutableSharedFlow<String>()
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()
    private val _isImportingSms = MutableStateFlow(false)
    val isImportingSms: StateFlow<Boolean> = _isImportingSms.asStateFlow()
    val isDarkMode: StateFlow<Boolean> = ThemePreference.isDarkModeFlow(application)
        .stateIn(viewModelScope, SharingStarted.Lazily, false)
    val isOnboardingCompleted: StateFlow<Boolean> =
        OnboardingPreference.isOnboardingCompletedFlow(application)
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)


    // --- Filters State ---
    private val _selectedUpiTransactionType = MutableStateFlow(UpiTransactionTypeFilter.ALL)
    val selectedUpiTransactionType: StateFlow<UpiTransactionTypeFilter> = _selectedUpiTransactionType.asStateFlow()

    // ✨ New: StateFlows for selected date range filter (timestamps in milliseconds) ✨
    private val _selectedDateRangeStart = MutableStateFlow<Long?>(null)
    val selectedDateRangeStart: StateFlow<Long?> = _selectedDateRangeStart.asStateFlow()

    private val _selectedDateRangeEnd = MutableStateFlow<Long?>(null)
    val selectedDateRangeEnd: StateFlow<Long?> = _selectedDateRangeEnd.asStateFlow()


    // --- Derived (Filtered) Data for UI ---
    // ✨ Updated: filteredUpiTransactions now also considers date range ✨
    val filteredUpiTransactions: StateFlow<List<Transaction>> =
        combine(
            _transactions,
            _selectedUpiTransactionType,
            _selectedDateRangeStart,
            _selectedDateRangeEnd
        ) { transactions, filterType, startDate, endDate ->
            val typeFilteredTransactions = when (filterType) {
                UpiTransactionTypeFilter.ALL -> transactions
                UpiTransactionTypeFilter.DEBIT -> transactions.filter { it.type.equals("DEBIT", ignoreCase = true) }
                UpiTransactionTypeFilter.CREDIT -> transactions.filter { it.type.equals("CREDIT", ignoreCase = true) }
            }
            // Apply date filter
            if (startDate != null && endDate != null) {
                typeFilteredTransactions.filter { it.date in startDate..endDate }
            } else if (startDate != null) {
                typeFilteredTransactions.filter { it.date >= startDate }
            } else if (endDate != null) {
                // Ensure endDate includes the whole day if it's just a date
                val endOfDay = Calendar.getInstance().apply {
                    timeInMillis = endDate
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis
                typeFilteredTransactions.filter { it.date <= endOfDay }
            }
            else {
                typeFilteredTransactions // No date filter
            }
        }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ✨ New: StateFlow for filtered UPI Lite summaries (applies date range) ✨
    val filteredUpiLiteSummaries: StateFlow<List<UpiLiteSummary>> =
        combine(
            _upiLiteSummaries, // Use the base private flow
            _selectedDateRangeStart,
            _selectedDateRangeEnd
        ) { summaries, startDate, endDate ->
            // Apply date filter
            if (startDate != null && endDate != null) {
                summaries.filter { it.date in startDate..endDate }
            } else if (startDate != null) {
                summaries.filter { it.date >= startDate }
            } else if (endDate != null) {
                val endOfDay = Calendar.getInstance().apply {
                    timeInMillis = endDate
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis
                summaries.filter { it.date <= endOfDay }
            } else {
                summaries // No date filter
            }
        }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())


    // Current month data (remains based on all transactions for home screen for now)
    // ... (getCurrentMonthDateRange, currentMonthDebitTransactions, currentMonthTotalExpenses as before, using _transactions) ...
    private fun getCurrentMonthDateRange(): Pair<Long, Long> { /* ... */
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
        val monthStartTimestamp = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1); calendar.add(Calendar.MILLISECOND, -1)
        val monthEndTimestamp = calendar.timeInMillis
        return Pair(monthStartTimestamp, monthEndTimestamp)
    }
    val currentMonthDebitTransactions: StateFlow<List<Transaction>> = _transactions
        .map { allTransactions ->
            val (monthStart, monthEnd) = getCurrentMonthDateRange()
            allTransactions.filter {
                it.type.equals("DEBIT", ignoreCase = true) && it.date in monthStart..monthEnd
            }.sortedByDescending { it.date }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val currentMonthTotalExpenses: StateFlow<Double> = currentMonthDebitTransactions
        .map { debitTransactions -> debitTransactions.sumOf { it.amount } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)


    // Graph data (remains based on all transactions for now)
    // ... (lastNMonthsExpenses, calculateLastNMonthsExpenses as before, using _transactions) ...
    val lastNMonthsExpenses: StateFlow<List<MonthlyExpense>> = _transactions
        .map { allTransactions -> calculateLastNMonthsExpenses(allTransactions, 6) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private fun calculateLastNMonthsExpenses(transactions: List<Transaction>, N: Int): List<MonthlyExpense> { /* ... */
        if (transactions.isEmpty()) return emptyList()
        val monthFormat = SimpleDateFormat("MMM yy", Locale.getDefault())
        val yearMonthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val expensesByYearMonth = transactions
            .filter { it.type.equals("DEBIT", ignoreCase = true) }
            .groupBy { transaction -> yearMonthFormat.format(Date(transaction.date)) }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
        val calendar = Calendar.getInstance()
        val monthlyExpensesData = mutableListOf<MonthlyExpense>()
        for (i in 0 until N) {
            val targetCalendar = Calendar.getInstance().apply { time = calendar.time; add(Calendar.MONTH, -i) }
            val yearMonthKey = yearMonthFormat.format(targetCalendar.time)
            val displayLabel = monthFormat.format(targetCalendar.time)
            targetCalendar.set(Calendar.DAY_OF_MONTH, 1); targetCalendar.set(Calendar.HOUR_OF_DAY, 0); targetCalendar.set(Calendar.MINUTE, 0); targetCalendar.set(Calendar.SECOND, 0); targetCalendar.set(Calendar.MILLISECOND, 0)
            val monthStartTimestamp = targetCalendar.timeInMillis
            monthlyExpensesData.add(MonthlyExpense(yearMonth = displayLabel, totalAmount = expensesByYearMonth[yearMonthKey] ?: 0.0, timestamp = monthStartTimestamp))
        }
        return monthlyExpensesData.reversed()
    }


    // Combined History List is NOT USED by TransactionHistoryScreen if it has tabs.
    // If you need it elsewhere, it should also use the most broadly filtered lists.
    // For now, I'll comment it out to avoid confusion, assuming TransactionHistoryScreen
    // will use filteredUpiTransactions and filteredUpiLiteSummaries directly for its tabs.
    /*
    val combinedHistoryList: StateFlow<List<HistoryListItem>> =
        combine(filteredUpiTransactions, filteredUpiLiteSummaries) { transactionList, summaryList ->
            val combinedItems = mutableListOf<HistoryListItem>()
            combinedItems.addAll(transactionList.map { TransactionHistoryItem(it) })
            combinedItems.addAll(summaryList.map { SummaryHistoryItem(it) })
            combinedItems.sortedByDescending { it.displayDate }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    */

    // --- Action Methods ---
    fun setUpiTransactionTypeFilter(filter: UpiTransactionTypeFilter) {
        _selectedUpiTransactionType.value = filter
    }

    // ✨ New: Method to set the date range filter ✨
    // Timestamps should be for the start of the day for startDate and end of the day for endDate
    fun setDateRangeFilter(startDate: Long?, endDate: Long?) {
        _selectedDateRangeStart.value = startDate
        _selectedDateRangeEnd.value = endDate // Ensure this is the end of the selected day (e.g., 23:59:59.999)
    }

    // ✨ New: Method to clear date range filter ✨
    fun clearDateRangeFilter() {
        _selectedDateRangeStart.value = null
        _selectedDateRangeEnd.value = null
    }

    // --- Action Methods ---
    fun addUpiLiteSummary(summary: UpiLiteSummary) {
        viewModelScope.launch {
            upiLiteSummaryDao.insert(summary)
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            ThemePreference.setDarkMode(getApplication(), enabled)
        }
    }

    fun insertTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionDao.insert(transaction)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionDao.delete(transaction)
        }
    }

    fun deleteAllTransactions() {
        viewModelScope.launch {
            transactionDao.deleteAll()
        }
    }

    fun deleteAllUpiLiteSummaries() {
        viewModelScope.launch {
            upiLiteSummaryDao.deleteAll()
        }
    }

    fun postSnackbarMessage(message: String) {
        viewModelScope.launch {
            _snackbarEvents.emit(message)
        }
    }

    fun setSmsImportingState(isImporting: Boolean) {
        _isImportingSms.value = isImporting
    }

    fun markOnboardingComplete() {
        viewModelScope.launch {
            OnboardingPreference.setOnboardingCompleted(getApplication(), true)
        }
    }
}
package com.example.upitracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.upitracker.data.AppDatabase
import com.example.upitracker.data.Transaction
import com.example.upitracker.data.UpiLiteSummary
import com.example.upitracker.util.OnboardingPreference
import com.example.upitracker.util.ThemePreference
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// --- Data class for Graph Data ---
data class MonthlyExpense(
    val yearMonth: String, // e.g., "May '23"
    val totalAmount: Double,
    val timestamp: Long // Start of the month timestamp for sorting/reference
)

// --- Sealed interface for Combined History List ---
sealed interface HistoryListItem {
    val displayDate: Long
    val itemType: String
}

data class TransactionHistoryItem(val transaction: Transaction) : HistoryListItem {
    override val displayDate: Long get() = transaction.date
    override val itemType: String get() = "Transaction"
}

data class SummaryHistoryItem(val summary: UpiLiteSummary) : HistoryListItem {
    override val displayDate: Long get() = summary.date // Assumes UpiLiteSummary.date is a Long timestamp
    override val itemType: String get() = "UpiLiteSummary"
}

// --- Enum for UPI Transaction Type Filter ---
enum class UpiTransactionTypeFilter {
    ALL, DEBIT, CREDIT
}

// --- Enum for Graph Period Selection ---
enum class GraphPeriod(val months: Int, val displayName: String) {
    THREE_MONTHS(3, "3M"),
    SIX_MONTHS(6, "6M"),
    TWELVE_MONTHS(12, "12M")
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val transactionDao = db.transactionDao()
    private val upiLiteSummaryDao = db.upiLiteSummaryDao()

    // --- Base Data Flows (Private) ---
    private val _transactions: StateFlow<List<Transaction>> = transactionDao.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _upiLiteSummaries: StateFlow<List<UpiLiteSummary>> =
        upiLiteSummaryDao.getAllSummaries() // Assumes DAO sorts by date (Long) DESC
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // --- UI State Flows (Public) ---
    private val _snackbarEvents = MutableSharedFlow<String>()
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    private val _isImportingSms = MutableStateFlow(false)
    val isImportingSms: StateFlow<Boolean> = _isImportingSms.asStateFlow()

    val isDarkMode: StateFlow<Boolean> = ThemePreference.isDarkModeFlow(application)
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val isOnboardingCompleted: StateFlow<Boolean> =
        OnboardingPreference.isOnboardingCompletedFlow(application)
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // --- Filter States (Private backing, Public immutable exposure) ---
    private val _selectedUpiTransactionType = MutableStateFlow(UpiTransactionTypeFilter.ALL)
    val selectedUpiTransactionType: StateFlow<UpiTransactionTypeFilter> = _selectedUpiTransactionType.asStateFlow()

    private val _selectedDateRangeStart = MutableStateFlow<Long?>(null)
    val selectedDateRangeStart: StateFlow<Long?> = _selectedDateRangeStart.asStateFlow()

    private val _selectedDateRangeEnd = MutableStateFlow<Long?>(null)
    val selectedDateRangeEnd: StateFlow<Long?> = _selectedDateRangeEnd.asStateFlow()

    private val _selectedGraphPeriod = MutableStateFlow(GraphPeriod.SIX_MONTHS) // Default to 6 months
    val selectedGraphPeriod: StateFlow<GraphPeriod> = _selectedGraphPeriod.asStateFlow()


    // --- Derived (Filtered) Data for UI (Public) ---
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
                val endOfDay = Calendar.getInstance().apply {
                    timeInMillis = endDate
                    set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
                }.timeInMillis
                typeFilteredTransactions.filter { it.date <= endOfDay }
            } else {
                typeFilteredTransactions
            }
        }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val filteredUpiLiteSummaries: StateFlow<List<UpiLiteSummary>> =
        combine(
            _upiLiteSummaries,
            _selectedDateRangeStart,
            _selectedDateRangeEnd
        ) { summaries, startDate, endDate ->
            if (startDate != null && endDate != null) {
                summaries.filter { it.date in startDate..endDate }
            } else if (startDate != null) {
                summaries.filter { it.date >= startDate }
            } else if (endDate != null) {
                val endOfDay = Calendar.getInstance().apply {
                    timeInMillis = endDate
                    set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
                }.timeInMillis
                summaries.filter { it.date <= endOfDay }
            } else {
                summaries
            }
        }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // --- Current Month Data (Public) ---
    private fun getCurrentMonthDateRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
        val monthStartTimestamp = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1); calendar.add(Calendar.MILLISECOND, -1)
        val monthEndTimestamp = calendar.timeInMillis
        return Pair(monthStartTimestamp, monthEndTimestamp)
    }

    val currentMonthDebitTransactions: StateFlow<List<Transaction>> = _transactions // Based on all transactions
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

    // --- Graph Data (Public) ---
    val lastNMonthsExpenses: StateFlow<List<MonthlyExpense>> =
        combine(_transactions, _selectedGraphPeriod) { allTransactions, period ->
            calculateLastNMonthsExpenses(allTransactions, period.months)
        }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private fun calculateLastNMonthsExpenses(transactions: List<Transaction>, N: Int): List<MonthlyExpense> {
        if (transactions.isEmpty() || N <= 0) return emptyList()
        val monthDisplayFormat = SimpleDateFormat("MMM yy", Locale.getDefault())
        val yearMonthKeyFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())

        val expensesByYearMonth = transactions
            .filter { it.type.equals("DEBIT", ignoreCase = true) }
            .groupBy { transaction -> yearMonthKeyFormat.format(Date(transaction.date)) }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val calendar = Calendar.getInstance()
        val monthlyExpensesData = mutableListOf<MonthlyExpense>()

        for (i in 0 until N) {
            val targetCalendar = Calendar.getInstance().apply {
                time = calendar.time
                add(Calendar.MONTH, -i)
            }
            val yearMonthKey = yearMonthKeyFormat.format(targetCalendar.time)
            val displayLabel = monthDisplayFormat.format(targetCalendar.time)

            targetCalendar.set(Calendar.DAY_OF_MONTH, 1)
            targetCalendar.set(Calendar.HOUR_OF_DAY, 0); targetCalendar.set(Calendar.MINUTE, 0); targetCalendar.set(Calendar.SECOND, 0); targetCalendar.set(Calendar.MILLISECOND, 0)
            val monthStartTimestamp = targetCalendar.timeInMillis

            monthlyExpensesData.add(
                MonthlyExpense(
                    yearMonth = displayLabel,
                    totalAmount = expensesByYearMonth[yearMonthKey] ?: 0.0,
                    timestamp = monthStartTimestamp
                )
            )
        }
        return monthlyExpensesData.reversed()
    }

    // --- Action Methods (Public) ---
    fun setUpiTransactionTypeFilter(filter: UpiTransactionTypeFilter) {
        _selectedUpiTransactionType.value = filter
    }

    fun setDateRangeFilter(startDate: Long?, endDate: Long?) {
        _selectedDateRangeStart.value = startDate
        _selectedDateRangeEnd.value = endDate
    }

    fun clearDateRangeFilter() {
        _selectedDateRangeStart.value = null
        _selectedDateRangeEnd.value = null
    }

    fun setSelectedGraphPeriod(period: GraphPeriod) {
        _selectedGraphPeriod.value = period
    }

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
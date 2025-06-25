package com.example.upitracker.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.upitracker.R
import com.example.upitracker.data.AppDatabase
import com.example.upitracker.data.BudgetPeriod
import com.example.upitracker.data.Transaction
import com.example.upitracker.data.UpiLiteSummary
import com.example.upitracker.util.CsvExporter
import com.example.upitracker.util.OnboardingPreference
import com.example.upitracker.util.ThemePreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.util.Log
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableSharedFlow // Add this import
import kotlinx.coroutines.flow.asSharedFlow
import com.example.upitracker.data.RecurringRule
import com.example.upitracker.data.CategorySuggestionRule
import com.example.upitracker.data.RuleField
import com.example.upitracker.data.RuleMatcher
import com.example.upitracker.util.AppTheme


// --- Data classes and Enums (should be defined here or imported if in separate files) ---
data class MonthlyExpense(
    val yearMonth: String,
    val totalAmount: Double,
    val timestamp: Long
)

data class DailyExpensePoint(
    val dayTimestamp: Long,
    val totalAmount: Double,
    val dayLabel: String
)

data class CategoryExpense(
    val categoryName: String,
    val totalAmount: Double
)

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

data class MonthlyDebitSummaryStats(
    val totalAmount: Double = 0.0,
    val averageAmount: Double = 0.0,
    val highestMonth: MonthlyExpense? = null
)

data class SpendingTrend(
    val title: String,
    val value: String,
    val subtitle: String
)

enum class UpiTransactionTypeFilter {
    ALL, DEBIT, CREDIT
}

enum class GraphPeriod(val months: Int, val displayName: String) {
    THREE_MONTHS(3, "3M"),
    SIX_MONTHS(6, "6M"),
    TWELVE_MONTHS(12, "12M")
}

enum class SortableTransactionField {
    DATE, AMOUNT, CATEGORY
}

enum class SortOrder {
    ASCENDING, DESCENDING
}

enum class AmountFilterType {
    ALL,
    GREATER_THAN,
    LESS_THAN,
    RANGE
}

data class BudgetStatus(
    val budgetId: Int,
    val periodType: BudgetPeriod,
    val categoryName: String,
    val budgetAmount: Double,
    val spentAmount: Double,
    val progress: Float,      // A value from 0.0f to 1.0f for progress bars
    val remainingAmount: Double,
    val allowRollover: Boolean, // ✨ NEW
    val rolloverAmount: Double,  // ✨ NEW
    val effectiveBudget: Double // ✨ NEW
)

data class IncomeExpensePoint(
    val yearMonth: String, // e.g., "Jun 25"
    val totalIncome: Double,
    val totalExpense: Double,
    val timestamp: Long
)

enum class SortableUpiLiteSummaryField {
    DATE,
    TOTAL_AMOUNT,
    TRANSACTION_COUNT,
    BANK
}

data class DailyTrendSummaryStats(
    val totalAmount: Double = 0.0,
    val dailyAverage: Double = 0.0,
    val highestDay: DailyExpensePoint? = null
)

data class IncomeExpenseSummaryStats(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netSavings: Double = 0.0
)

data class SnackbarMessage(
    val message: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null, // Action to perform if Undo is clicked
    val onDismiss: (() -> Unit)? = null  // Action to perform if Snackbar is dismissed (e.g., timeout)
)

data class TransactionFilters(
    val type: UpiTransactionTypeFilter,
    val startDate: Long?,
    val endDate: Long?,
    val searchQuery: String,
    val showUncategorized: Boolean = false,
    val amountType: AmountFilterType,
    val amountValue1: Double?,
    val amountValue2: Double?
)

data class BankMessageCount(val bankName: String, val count: Int)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val transactionDao = db.transactionDao()
    private val upiLiteSummaryDao = db.upiLiteSummaryDao()
    private val budgetDao = db.budgetDao()
    private val categorySuggestionRuleDao = db.categorySuggestionRuleDao()
    private val recurringRuleDao = db.recurringRuleDao()

    // --- Base Data Flows (Private) ---
    private val _transactions: StateFlow<List<Transaction>> = transactionDao.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _budgets = budgetDao.getAllActiveBudgets()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

   // private val _selectedUpiTransactionType = MutableStateFlow(UpiTransactionTypeFilter.ALL)
   private val _selectedUpiTransactionType = MutableStateFlow(UpiTransactionTypeFilter.ALL)
    val selectedUpiTransactionType: StateFlow<UpiTransactionTypeFilter> =
        _selectedUpiTransactionType.asStateFlow()

    val isUpiLiteEnabled: StateFlow<Boolean> = ThemePreference.isUpiLiteEnabledFlow(application)
        .stateIn(viewModelScope, SharingStarted.Lazily, true)


    private val _upiLiteSummaries: StateFlow<List<UpiLiteSummary>> =
        upiLiteSummaryDao.getAllSummaries() // Assumes DAO sorts by date (Long) DESC
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val refundCategory = "Refund"

    private val _isBackingUp = MutableStateFlow(false)
    val isBackingUp: StateFlow<Boolean> = _isBackingUp.asStateFlow()

    private val _isRestoring = MutableStateFlow(false)
    val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()

    private val _selectedDateRangeStart = MutableStateFlow<Long?>(null)
    private val _selectedDateRangeEnd   = MutableStateFlow<Long?>(null)

    private val _selectedTransactionId = MutableStateFlow<Int?>(null)

    private val _amountFilterType = MutableStateFlow(AmountFilterType.ALL)
    private val _amountFilterValue1 = MutableStateFlow<Double?>(null)
    private val _amountFilterValue2 = MutableStateFlow<Double?>(null)

    private val _showUncategorized = MutableStateFlow(false)

    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    // --- UI State Flows (Public) ---
    private val _snackbarEvents = MutableSharedFlow<SnackbarMessage>()
    val snackbarEvents: SharedFlow<SnackbarMessage> = _snackbarEvents.asSharedFlow()

    private val _isImportingSms = MutableStateFlow(false)
    val isImportingSms: StateFlow<Boolean> = _isImportingSms.asStateFlow()

    private val _isRefreshingSmsArchive = MutableStateFlow(false)

    private val _showHistoryFilterSheet = MutableStateFlow(false)
    val showHistoryFilterSheet: StateFlow<Boolean> = _showHistoryFilterSheet.asStateFlow()

    val isRefreshingSmsArchive: StateFlow<Boolean> = _isRefreshingSmsArchive.asStateFlow()

    val isDarkMode: StateFlow<Boolean> = ThemePreference.isDarkModeFlow(application)
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val recurringRules: StateFlow<List<RecurringRule>> = recurringRuleDao.getAllRules()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedTransaction: StateFlow<Transaction?> = _selectedTransactionId.flatMapLatest { id ->
        if (id == null) {
            flowOf(null) // Emit null if no ID is selected
        } else {
            transactionDao.getTransactionById(id)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val categorySuggestionRules: StateFlow<List<CategorySuggestionRule>> =
        categorySuggestionRuleDao.getAllRules()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _searchQuery = MutableStateFlow("")

    private val _dateAndSearch = combine(
        _selectedDateRangeStart,
        _selectedDateRangeEnd,
        _searchQuery
    ) { start, end, query -> Triple(start, end, query) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Triple(null, null, ""))

    private val archivedSmsDao = db.archivedSmsMessageDao()

    val bankMessageCounts: StateFlow<List<BankMessageCount>> =
        archivedSmsDao.getAllArchivedSms() // Assuming this returns Flow<List<ArchivedSmsMessage>>
            .map { allMessages ->
                allMessages
                    .mapNotNull { msg ->
                        // Heuristically determine bank name from sender address
                        val sender = msg.originalSender.uppercase()
                        when {
                            sender.contains("HDFC") -> "HDFC Bank"
                            sender.contains("ICICI") -> "ICICI Bank"
                            sender.contains("SBI") || sender.contains("SBIN") -> "State Bank of India"
                            sender.contains("AXIS") -> "Axis Bank"
                            sender.contains("KOTAK") -> "Kotak Mahindra Bank"
                            sender.contains("PNB") -> "Punjab National Bank"
                            sender.contains("CANARA") -> "Canara Bank"
                            sender.contains("IDBI") -> "IDBI Bank"
                            sender.contains("UNION") -> "Union Bank of India"
                            sender.contains("BANK OF BARODA") -> "Bank of Baroda"
                            sender.contains("YES") -> "Yes Bank"
                            sender.contains("FEDERAL") -> "Federal Bank"
                            sender.contains("CITI") -> "Citi Bank"
                            sender.contains("RBL") -> "RBL Bank"
                            else -> null // Ignore senders we don't recognize
                        }
                    }
                    .groupingBy { it }
                    .eachCount()
                    .map { (bankName, count) -> BankMessageCount(bankName, count) }
                    .sortedByDescending { it.count }
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _filters = combine(
        _selectedUpiTransactionType,
        _dateAndSearch,
        _showUncategorized,
        _amountFilterType,
        _amountFilterValue1
    ) { type, dateAndSearchTriple, uncategorized, amountType, amount1 ->
        quintuple(type, dateAndSearchTriple, uncategorized, amountType, amount1)
    }.combine(_amountFilterValue2) { quint, amount2 ->
        val (type, dateAndSearchTriple, uncategorized, amountType, amount1) = quint
        val (startDate, endDate, query) = dateAndSearchTriple
        TransactionFilters(type, startDate, endDate, query, uncategorized, amountType, amount1, amount2)
    }.stateIn(viewModelScope, SharingStarted.Lazily, TransactionFilters(UpiTransactionTypeFilter.ALL, null, null, "", false, AmountFilterType.ALL, null, null))

    // Helper function and data class
    private fun <A, B, C, D, E> quintuple(a: A, b: B, c: C, d: D, e: E): Quint<A, B, C, D, E> = Quint(a, b, c, d, e)
    private data class Quint<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)

    val userCategories: StateFlow<List<String>> = _transactions
        .map { allTransactions ->
            allTransactions
                .filter { !it.category.isNullOrBlank() } // Only consider transactions with a category
                .groupingBy { it.category!! }            // Group by the category name
                .eachCount()                             // Count occurrences of each category
                .toList()                                // Convert map to a list of pairs
                .sortedByDescending { it.second }        // Sort by count, most frequent first
                .take(7)                                 // Take the top 7 most used categories
                .map { it.first }                        // Get just the category name
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _selectedGraphPeriod = MutableStateFlow(GraphPeriod.SIX_MONTHS)
    val selectedGraphPeriod: StateFlow<GraphPeriod> = _selectedGraphPeriod.asStateFlow()

    // In MainViewModel.kt, add these new helper functions

    private fun getPreviousWeekRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.WEEK_OF_YEAR, -1) // Go to previous week
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
        val weekStart = calendar.timeInMillis
        calendar.add(Calendar.WEEK_OF_YEAR, 1); calendar.add(Calendar.MILLISECOND, -1)
        val weekEnd = calendar.timeInMillis
        return weekStart to weekEnd
    }

    private fun getPreviousMonthRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -1) // Go to previous month
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
        val monthStart = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1); calendar.add(Calendar.MILLISECOND, -1)
        val monthEnd = calendar.timeInMillis
        return monthStart to monthEnd
    }

    private fun getPreviousYearRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -1) // Go to previous year
        calendar.set(Calendar.DAY_OF_YEAR, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
        val yearStart = calendar.timeInMillis
        calendar.add(Calendar.YEAR, 1); calendar.add(Calendar.MILLISECOND, -1)
        val yearEnd = calendar.timeInMillis
        return yearStart to yearEnd
    }

    fun backupDatabase(targetUri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch(Dispatchers.IO) {
            _isBackingUp.value = true
            try {
                val sourceDbFile = getApplication<Application>().getDatabasePath("upi_tracker_db")

                contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                    sourceDbFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                } ?: throw IOException("Failed to open output stream for URI: $targetUri")

                postPlainSnackbarMessage("Backup successful!")

            } catch (e: Exception) {
                Log.e("BackupRestore", "Error during database backup", e)
                postPlainSnackbarMessage("Error: Backup failed. ${e.message}")
            } finally {
                _isBackingUp.value = false
            }
        }
    }

    // In MainViewModel.kt, add this new public function

    fun restoreDatabase(sourceUri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch(Dispatchers.IO) {
            _isRestoring.value = true
            try {
                val targetDbFile = getApplication<Application>().getDatabasePath("upi_tracker_db")

                contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                    targetDbFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                } ?: throw IOException("Failed to open input stream for URI: $sourceUri")

                _uiEvents.emit(UiEvent.RestartRequired("Restore successful! The app must now restart to apply changes."))

            } catch (e: Exception) {
                Log.e("BackupRestore", "Error during database restore", e)
                postPlainSnackbarMessage("Error: Restore failed. Invalid file? ${e.message}")
            } finally {
                _isRestoring.value = false
            }
        }
    }

    val appTheme: StateFlow<AppTheme> = ThemePreference.getAppThemeFlow(application)
        .stateIn(viewModelScope, SharingStarted.Lazily, AppTheme.DEFAULT)

    val filters: StateFlow<TransactionFilters> = _filters

    val isOnboardingCompleted: StateFlow<Boolean> =
        OnboardingPreference.isOnboardingCompletedFlow(application)
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val archivedUpiTransactions: StateFlow<List<Transaction>> =
        transactionDao.getArchivedTransactions() // Uses the new DAO method
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val incomeVsExpenseData: StateFlow<List<IncomeExpensePoint>> =
        combine(_transactions, _upiLiteSummaries, _selectedGraphPeriod) { transactions, summaries, period ->
            if (transactions.isEmpty()) {
                return@combine emptyList()
            }
            val monthDisplayFormat = SimpleDateFormat("MMM yy", Locale.getDefault())
            val yearMonthKeyFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())

            val summariesByMonth = summaries.groupBy { summary ->
                yearMonthKeyFormat.format(Date(summary.date))
            }
            // Group all transactions by month key ("yyyy-MM")
            val transactionsByMonth = transactions.groupBy { transaction ->
                yearMonthKeyFormat.format(Date(transaction.date))
            }

            val reportData = mutableListOf<IncomeExpensePoint>()
            val calendar = Calendar.getInstance()

            // Iterate through the last N months based on the selected period
            for (i in 0 until period.months) {
                val targetCalendar =
                    Calendar.getInstance().apply { time = calendar.time; add(Calendar.MONTH, -i) }
                val yearMonthKey = yearMonthKeyFormat.format(targetCalendar.time)
                val displayLabel = monthDisplayFormat.format(targetCalendar.time)

                targetCalendar.set(Calendar.DAY_OF_MONTH, 1)
                targetCalendar.set(Calendar.HOUR_OF_DAY, 0); targetCalendar.set(Calendar.MINUTE, 0)
                targetCalendar.set(Calendar.SECOND, 0); targetCalendar.set(Calendar.MILLISECOND, 0)
                val monthStartTimestamp = targetCalendar.timeInMillis

                // Get transactions for the current month key, or an empty list if none
                val monthTransactions = transactionsByMonth[yearMonthKey] ?: emptyList()
                val monthSummaries = summariesByMonth[yearMonthKey] ?: emptyList()

                // Calculate totals for this month
                val income = monthTransactions.filter { it.type.equals("CREDIT", ignoreCase = true) }.sumOf { it.amount }

                val regularExpense = monthTransactions.filter { it.type.equals("DEBIT", ignoreCase = true) }.sumOf { it.amount }
                val liteExpense = monthSummaries.sumOf { it.totalAmount }
                val totalExpense = regularExpense + liteExpense

                reportData.add(
                    IncomeExpensePoint(
                        yearMonth = displayLabel,
                        totalIncome = income,
                        totalExpense = totalExpense,
                        timestamp = monthStartTimestamp
                    )
                )
            }
            // Return the list reversed to have the oldest month first for the chart
            reportData.reversed()
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val budgetStatuses: StateFlow<List<BudgetStatus>> =
        combine(_transactions, _budgets) { transactions, budgets ->
            if (budgets.isEmpty()) return@combine emptyList()

            val debitTransactions = transactions.filter {
                it.type.equals("DEBIT", ignoreCase = true) &&
                        !it.category.equals(refundCategory, ignoreCase = true)
            }

            budgets.map { budget ->
                // --- Rollover Calculation ---
                val rolloverAmount = if (budget.allowRollover) {
                    val (prevPeriodStart, prevPeriodEnd) = when (budget.periodType) {
                        BudgetPeriod.WEEKLY -> getPreviousWeekRange()
                        BudgetPeriod.MONTHLY -> getPreviousMonthRange()
                        BudgetPeriod.YEARLY -> getPreviousYearRange()
                    }
                    val spentInPrevPeriod = debitTransactions
                        .filter { it.category.equals(budget.categoryName, true) && it.date in prevPeriodStart..prevPeriodEnd }
                        .sumOf { it.amount }
                    budget.budgetAmount - spentInPrevPeriod // Leftover or debt from last period
                } else {
                    0.0 // Rollover is disabled
                }

                // --- Current Period Calculation ---
                val (currentPeriodStart, currentPeriodEnd) = when (budget.periodType) {
                    BudgetPeriod.WEEKLY -> getCurrentWeekRange()
                    BudgetPeriod.MONTHLY -> getCurrentMonthDateRange()
                    BudgetPeriod.YEARLY -> getCurrentYearRange()
                }

                val spentInCurrentPeriod = debitTransactions
                    .filter { it.category.equals(budget.categoryName, true) && it.date in currentPeriodStart..currentPeriodEnd }
                    .sumOf { it.amount }

                val effectiveBudget = budget.budgetAmount + rolloverAmount
                val progress = if (effectiveBudget > 0) (spentInCurrentPeriod / effectiveBudget).toFloat().coerceIn(0f, 1f) else 0f

                BudgetStatus(
                    budgetId = budget.id,
                    periodType = budget.periodType,
                    categoryName = budget.categoryName,
                    budgetAmount = budget.budgetAmount,
                    spentAmount = spentInCurrentPeriod,
                    progress = progress,
                    remainingAmount = effectiveBudget - spentInCurrentPeriod,
                    allowRollover = budget.allowRollover,
                    rolloverAmount = rolloverAmount,
                    effectiveBudget = effectiveBudget
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isExportingCsv = MutableStateFlow(false)
    val isExportingCsv: StateFlow<Boolean> = _isExportingCsv.asStateFlow()

    // --- Filter & Sort States (Private backing, Public immutable exposure) ---

    private val _upiTransactionSortField = MutableStateFlow(SortableTransactionField.DATE)
    val upiTransactionSortField: StateFlow<SortableTransactionField> =
        _upiTransactionSortField.asStateFlow()

    private val _upiTransactionSortOrder = MutableStateFlow(SortOrder.DESCENDING)
    val upiTransactionSortOrder: StateFlow<SortOrder> = _upiTransactionSortOrder.asStateFlow()

    private val _upiLiteSummarySortField = MutableStateFlow(SortableUpiLiteSummaryField.DATE)
    val upiLiteSummarySortField: StateFlow<SortableUpiLiteSummaryField> =
        _upiLiteSummarySortField.asStateFlow()

    private val _upiLiteSummarySortOrder = MutableStateFlow(SortOrder.DESCENDING)
    val upiLiteSummarySortOrder: StateFlow<SortOrder> = _upiLiteSummarySortOrder.asStateFlow()

    val setSearchQuery: (String) -> Unit = { _searchQuery.value = it }

    val filteredUpiTransactions: StateFlow<List<Transaction>> =
        combine(
            _transactions,
            _filters, // ✨ USE the new combined filters flow
            _upiTransactionSortField,
            _upiTransactionSortOrder
        ) { transactions, filters, sortField, sortOrder ->
            // 1) Type filter
            val byType = when (filters.type) {
                UpiTransactionTypeFilter.ALL -> transactions
                UpiTransactionTypeFilter.DEBIT -> transactions.filter { it.type.equals("DEBIT", ignoreCase = true) }
                UpiTransactionTypeFilter.CREDIT -> transactions.filter { it.type.equals("CREDIT", ignoreCase = true) }
            }

            // 2) Date filter
            val byDate = when {
                filters.startDate != null && filters.endDate != null -> byType.filter { it.date in filters.startDate..filters.endDate }
                filters.startDate != null -> byType.filter { it.date >= filters.startDate }
                filters.endDate != null -> byType.filter { it.date <= filters.endDate }
                else -> byType
            }

            // 3) Search filter
            val bySearch = if (filters.searchQuery.isBlank()) byDate else byDate.filter { tx ->
                tx.description.contains(filters.searchQuery, ignoreCase = true) ||
                        tx.senderOrReceiver.contains(filters.searchQuery, ignoreCase = true) ||
                        (tx.category?.contains(filters.searchQuery, ignoreCase = true) == true)
            }

            // 4) Uncategorized Filter
            val byUncategorized = if (filters.showUncategorized) {
                bySearch.filter { it.category.isNullOrBlank() }
            } else {
                bySearch
            }

            // 5) Amount Filter
            val byAmount = when (filters.amountType) {
                AmountFilterType.GREATER_THAN -> filters.amountValue1?.let { limit -> byUncategorized.filter { it.amount > limit } } ?: byUncategorized
                AmountFilterType.LESS_THAN -> filters.amountValue1?.let { limit -> byUncategorized.filter { it.amount < limit } } ?: byUncategorized
                AmountFilterType.RANGE -> if (filters.amountValue1 != null && filters.amountValue2 != null) {
                    byUncategorized.filter { it.amount in filters.amountValue1..filters.amountValue2 }
                } else byUncategorized
                AmountFilterType.ALL -> byUncategorized
            }

            // 6) Sort
            when (sortField) {
                SortableTransactionField.DATE ->
                    if (sortOrder == SortOrder.ASCENDING) byAmount.sortedBy { it.date }
                    else byAmount.sortedByDescending { it.date }

                SortableTransactionField.AMOUNT ->
                    if (sortOrder == SortOrder.ASCENDING) byAmount.sortedBy { it.amount }
                    else byAmount.sortedByDescending { it.amount }

                SortableTransactionField.CATEGORY ->
                    if (sortOrder == SortOrder.ASCENDING) byAmount.sortedBy { it.category ?: "" }
                    else byAmount.sortedByDescending { it.category ?: "" }
            }
        }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // 3) You can do the same two-into-one trick for summaries if you like:
    val filteredUpiLiteSummaries: StateFlow<List<UpiLiteSummary>> =
        combine(
            _upiLiteSummaries,
            _selectedDateRangeStart,
            _selectedDateRangeEnd,
            _upiLiteSummarySortField, // ✨ Add sort field
            _upiLiteSummarySortOrder  // ✨ Add sort order
        ) { summaries, startDate, endDate, sortField, sortOrder ->
            // 1. Apply Date Filter
            val dateFilteredSummaries = if (startDate != null && endDate != null) {
                summaries.filter { it.date in startDate..endDate }
            } else if (startDate != null) {
                summaries.filter { it.date >= startDate }
            } else if (endDate != null) {
                val endOfDay = Calendar.getInstance().apply {
                    timeInMillis = endDate; set(Calendar.HOUR_OF_DAY, 23); set(
                    Calendar.MINUTE, 59
                ); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
                }.timeInMillis
                summaries.filter { it.date <= endOfDay }
            } else {
                summaries
            }
            // 2. Apply Sorting
            when (sortField) {
                SortableUpiLiteSummaryField.DATE -> {
                    if (sortOrder == SortOrder.ASCENDING) dateFilteredSummaries.sortedBy { it.date }
                    else dateFilteredSummaries.sortedByDescending { it.date }
                }

                SortableUpiLiteSummaryField.TOTAL_AMOUNT -> {
                    if (sortOrder == SortOrder.ASCENDING) dateFilteredSummaries.sortedBy { it.totalAmount }
                    else dateFilteredSummaries.sortedByDescending { it.totalAmount }
                }

                SortableUpiLiteSummaryField.TRANSACTION_COUNT -> {
                    if (sortOrder == SortOrder.ASCENDING) dateFilteredSummaries.sortedBy { it.transactionCount }
                    else dateFilteredSummaries.sortedByDescending { it.transactionCount }
                }

                SortableUpiLiteSummaryField.BANK -> {
                    if (sortOrder == SortOrder.ASCENDING) dateFilteredSummaries.sortedBy { it.bank }
                    else dateFilteredSummaries.sortedByDescending { it.bank }
                }
            }
        }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())


    // --- Current Month Data (Public) ---
    private fun getCurrentMonthDateRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(
            Calendar.SECOND,
            0
        ); calendar.set(Calendar.MILLISECOND, 0)
        val monthStartTimestamp = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1); calendar.add(Calendar.MILLISECOND, -1)
        val monthEndTimestamp = calendar.timeInMillis
        return Pair(monthStartTimestamp, monthEndTimestamp)
    }

    val currentMonthExpenseItems: StateFlow<List<HistoryListItem>> =
        combine(_transactions, _upiLiteSummaries,  isUpiLiteEnabled) { transactions, summaries, upiLiteEnabled ->
            val (monthStart, monthEnd) = getCurrentMonthDateRange()
            val combinedItems = mutableListOf<HistoryListItem>()

            // Filter and map debit transactions for the current month
            val debitTransactions = transactions.filter {
                it.type.equals("DEBIT", ignoreCase = true)
                        && !it.category.equals(refundCategory, ignoreCase = true)
                        && it.date in monthStart..monthEnd
            }
            combinedItems.addAll(debitTransactions.map { TransactionHistoryItem(it) })

            if (upiLiteEnabled) {
                val liteSummaries = summaries.filter { it.date in monthStart..monthEnd }
                combinedItems.addAll(liteSummaries.map { SummaryHistoryItem(it) })
            }

            // Filter and map UPI Lite summaries for the current month
            val liteSummaries = summaries.filter { it.date in monthStart..monthEnd }
            val sortedList = combinedItems.sortedByDescending { it.displayDate }
            combinedItems.addAll(liteSummaries.map { SummaryHistoryItem(it) })

            // Sort the combined list by date, most recent first
            combinedItems.sortedByDescending { it.displayDate }
            sortedList.distinctBy { item ->
                when (item) {
                    is TransactionHistoryItem -> "txn-${item.transaction.id}"
                    is SummaryHistoryItem -> "summary-${item.summary.id}"
                }
            }
        }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setUpiLiteEnabled(enabled: Boolean) {
        viewModelScope.launch {
            ThemePreference.setUpiLiteEnabled(getApplication(), enabled)
        }
    }

    val currentMonthTotalExpenses: StateFlow<Double> = currentMonthExpenseItems
        .map { expenseItems ->
            expenseItems.sumOf { item ->
                when (item) {
                    is TransactionHistoryItem -> item.transaction.amount
                    is SummaryHistoryItem -> item.summary.totalAmount
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    // --- Graph Data (Public) ---
    val lastNMonthsExpenses: StateFlow<List<MonthlyExpense>> =
        combine(_transactions, _selectedGraphPeriod) { allTransactions, period ->
            // Log.d("ViewModelDebug", "Recalculating lastNMonthsExpenses. Input transactions: ${allTransactions.size}, Period: ${period.displayName} (${period.months} months)")
            val result = calculateLastNMonthsExpenses(allTransactions, period.months, refundCategory)
            // Log.d("ViewModelDebug", "Resulting monthly expenses for graph: ${result.size} items")
            result
        }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val monthlyDebitSummaryStats: StateFlow<MonthlyDebitSummaryStats> = lastNMonthsExpenses
        .map { monthlyExpenses ->
            if (monthlyExpenses.isEmpty()) {
                MonthlyDebitSummaryStats() // Return default empty stats if there's no data
            } else {
                val total = monthlyExpenses.sumOf { it.totalAmount }
                val average = if (monthlyExpenses.isNotEmpty()) total / monthlyExpenses.size else 0.0
                val highest = monthlyExpenses.maxByOrNull { it.totalAmount }

                MonthlyDebitSummaryStats(
                    totalAmount = total,
                    averageAmount = average,
                    highestMonth = highest
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, MonthlyDebitSummaryStats())

    val dailyExpensesTrend: StateFlow<List<DailyExpensePoint>> = _transactions
        .map { allTransactions ->
            val (rangeStart, rangeEnd) = getDailyTrendDateRange(7) // Last 30 days
            val relevantTransactions = allTransactions.filter {
                it.type.equals("DEBIT", ignoreCase = true) &&
                        !it.category.equals(refundCategory, ignoreCase = true) &&
                        it.date in rangeStart..rangeEnd
            }
            val expensesByDayTimestamp = relevantTransactions
                .groupBy { transaction ->
                    val cal = Calendar.getInstance(); cal.timeInMillis = transaction.date
                    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(
                    Calendar.MINUTE,
                    0
                ); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                    cal.timeInMillis
                }.mapValues { entry -> entry.value.sumOf { it.amount } }
            val trendData = mutableListOf<DailyExpensePoint>()
            val dayLabelFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
            val currentDayCal = Calendar.getInstance(); currentDayCal.timeInMillis = rangeStart
            while (currentDayCal.timeInMillis <= rangeEnd) {
                val dayTimestamp = currentDayCal.timeInMillis
                trendData.add(
                    DailyExpensePoint(
                        dayTimestamp = dayTimestamp,
                        totalAmount = expensesByDayTimestamp[dayTimestamp] ?: 0.0,
                        dayLabel = dayLabelFormat.format(Date(dayTimestamp))
                    )
                )
                currentDayCal.add(Calendar.DAY_OF_YEAR, 1)
            }
            trendData
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val dailyTrendSummaryStats: StateFlow<DailyTrendSummaryStats> = dailyExpensesTrend
        .map { dailyPoints ->
            if (dailyPoints.isEmpty()) {
                DailyTrendSummaryStats()
            } else {
                val total = dailyPoints.sumOf { it.totalAmount }
                val average = if (dailyPoints.isNotEmpty()) total / dailyPoints.size else 0.0
                val highest = dailyPoints.maxByOrNull { it.totalAmount }

                DailyTrendSummaryStats(
                    totalAmount = total,
                    dailyAverage = average,
                    highestDay = highest
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, DailyTrendSummaryStats())

    val incomeExpenseSummaryStats: StateFlow<IncomeExpenseSummaryStats> = incomeVsExpenseData
        .map { incomeExpensePoints ->
            if (incomeExpensePoints.isEmpty()) {
                IncomeExpenseSummaryStats()
            } else {
                val totalIncome = incomeExpensePoints.sumOf { it.totalIncome }
                val totalExpense = incomeExpensePoints.sumOf { it.totalExpense }

                IncomeExpenseSummaryStats(
                    totalIncome = totalIncome,
                    totalExpense = totalExpense,
                    netSavings = totalIncome - totalExpense
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, IncomeExpenseSummaryStats())

    private fun calculateLastNMonthsExpenses(
        transactions: List<Transaction>,
        n: Int, categoryToExclude: String?
    ): List<MonthlyExpense> {
        if (transactions.isEmpty() || n <= 0) {
            return emptyList()
        }
        val monthDisplayFormat = SimpleDateFormat("MMM yy", Locale.getDefault())
        val yearMonthKeyFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())

        val debitTransactions = transactions.filter {
            it.type.equals("DEBIT", ignoreCase = true) &&
            !it.category.equals(categoryToExclude, ignoreCase = true)
        }
        // Log.d("ViewModelDebug", "calculateLastNMonthsExpenses: Filtered DEBIT transactions count: ${debitTransactions.size}")
        if (debitTransactions.isEmpty()) {
            return emptyList()
        }

        val expensesByYearMonth = debitTransactions
            .groupBy { transaction -> yearMonthKeyFormat.format(Date(transaction.date)) }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val calendar = Calendar.getInstance()
        val monthlyExpensesData = mutableListOf<MonthlyExpense>()
        for (i in 0 until n) {
            val targetCalendar =
                Calendar.getInstance().apply { time = calendar.time; add(Calendar.MONTH, -i) }
            val yearMonthKey = yearMonthKeyFormat.format(targetCalendar.time)
            val displayLabel = monthDisplayFormat.format(targetCalendar.time)
            targetCalendar.set(Calendar.DAY_OF_MONTH, 1); targetCalendar.set(
                Calendar.HOUR_OF_DAY,
                0
            ); targetCalendar.set(Calendar.MINUTE, 0); targetCalendar.set(
                Calendar.SECOND,
                0
            ); targetCalendar.set(Calendar.MILLISECOND, 0)
            val monthStartTimestamp = targetCalendar.timeInMillis
            val amountForMonth = expensesByYearMonth[yearMonthKey] ?: 0.0
            monthlyExpensesData.add(
                MonthlyExpense(
                    yearMonth = displayLabel,
                    totalAmount = amountForMonth,
                    timestamp = monthStartTimestamp
                )
            )
            // Log.d("ViewModelDebug", "calculateLastNMonthsExpenses: For period $displayLabel ($yearMonthKey),  Timestamp: $monthStartTimestamp, Amount: $amountForMonth")
        }
        val result = monthlyExpensesData.reversed()
        // Log.d("ViewModelDebug", "calculateLastNMonthsExpenses: END. Final data size: ${result.size}, Data: $result")
        return result
    }

    private fun getDailyTrendDateRange(daysToShow: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance(); calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(
            Calendar.MINUTE,
            59
        ); calendar.set(Calendar.SECOND, 59); calendar.set(Calendar.MILLISECOND, 999)
        val endDate = calendar.timeInMillis
        calendar.timeInMillis = System.currentTimeMillis(); calendar.add(
            Calendar.DAY_OF_YEAR,
            -(daysToShow - 1)
        )
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(
            Calendar.SECOND,
            0
        ); calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.timeInMillis
        return Pair(startDate, endDate)
    }

    val categoryExpensesData: StateFlow<List<CategoryExpense>> =
        combine(
            _transactions, // Using all transactions as base for category expenses
            _selectedDateRangeStart,
            _selectedDateRangeEnd
        ) { transactions, startDate, endDate ->
            // Log.d("MainViewModel", "Calculating category expenses. Transactions: ${transactions.size}, Start: $startDate, End: $endDate")
            var filteredForDate = transactions
            if (startDate != null && endDate != null) {
                filteredForDate = transactions.filter { it.date in startDate..endDate }
            } else if (startDate != null) {
                filteredForDate = transactions.filter { it.date >= startDate }
            } else if (endDate != null) {
                val endOfDay = Calendar.getInstance().apply {
                    timeInMillis = endDate
                    set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(
                    Calendar.SECOND,
                    59
                ); set(Calendar.MILLISECOND, 999)
                }.timeInMillis
                filteredForDate = transactions.filter { it.date <= endOfDay }
            }

            val result = filteredForDate
                .filter {
                    it.type.equals(
                        "DEBIT",
                        ignoreCase = true
                    ) && !it.category.isNullOrBlank() && !it.category.equals(refundCategory, ignoreCase = true)
                }
                .groupBy { it.category!! }
                .map { (categoryName, transactionsInCategory) ->
                    CategoryExpense(
                        categoryName = categoryName,
                        totalAmount = transactionsInCategory.sumOf { it.amount }
                    )
                }
                .sortedByDescending { it.totalAmount }
            // Log.d("MainViewModel", "Category expenses calculated: $result")
            result
        }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

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

    fun addRecurringRule(
        description: String,
        amount: Double,
        category: String,
        period: BudgetPeriod,
        dayOfPeriod: Int
    ) {
        if (description.isBlank() || amount <= 0 || category.isBlank()) {
            postPlainSnackbarMessage("Invalid input. Please fill all fields.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            // --- Calculate the first due date ---
            val calendar = Calendar.getInstance()
            val today = calendar.get(Calendar.DAY_OF_MONTH)

            // For monthly rules
            if (period == BudgetPeriod.MONTHLY) {
                // If the day for this month has already passed, set it for next month
                if (today >= dayOfPeriod) {
                    calendar.add(Calendar.MONTH, 1)
                }
                calendar.set(Calendar.DAY_OF_MONTH, dayOfPeriod)
            }
            // TODO: Add logic for WEEKLY and YEARLY periods later

            // Set time to a consistent point in the day, e.g., noon
            calendar.set(Calendar.HOUR_OF_DAY, 12)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)

            val nextDueDate = calendar.timeInMillis

            val newRule = RecurringRule(
                amount = amount,
                description = description,
                categoryName = category,
                periodType = period,
                dayOfPeriod = dayOfPeriod,
                nextDueDate = nextDueDate
            )
            recurringRuleDao.insert(newRule)
            postPlainSnackbarMessage("Recurring transaction for '$description' saved.")
        }
    }

    fun deleteRecurringRule(rule: RecurringRule) {
        viewModelScope.launch(Dispatchers.IO) {
            recurringRuleDao.delete(rule)
            postPlainSnackbarMessage("Recurring transaction deleted.")
        }
    }

    fun updateRecurringRule(
        ruleId: Int,
        newDescription: String,
        newAmount: Double,
        newCategory: String,
        newPeriod: BudgetPeriod,
        newDay: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            // First, get the original rule from the database.
            // This is important to preserve non-editable fields like the creationDate.
            val originalRule = recurringRuleDao.getRuleById(ruleId)

            originalRule?.let {
                // Create a new rule object by copying the original and applying changes.
                val updatedRule = it.copy(
                    description = newDescription,
                    amount = newAmount,
                    categoryName = newCategory,
                    periodType = newPeriod,
                    dayOfPeriod = newDay
                    // Note: We are not changing the nextDueDate here,
                    // which is usually the desired behavior for an edit.
                )
                // Use the existing DAO function to update the database.
                recurringRuleDao.update(updatedRule)
                postPlainSnackbarMessage("Recurring rule updated.")
            }
        }
    }

    fun addOrUpdateBudget(categoryName: String, amount: Double, periodType: BudgetPeriod, allowRollover: Boolean, budgetId: Int? = null) { // ✨ ADD allowRollover
        viewModelScope.launch(Dispatchers.IO) {
            val trimmedCategory = categoryName.trim()
            if (trimmedCategory.isNotBlank() && amount > 0) {
                val (startDate, _) = when (periodType) {
                    BudgetPeriod.WEEKLY -> getCurrentWeekRange()
                    BudgetPeriod.MONTHLY -> getCurrentMonthDateRange()
                    BudgetPeriod.YEARLY -> getCurrentYearRange()
                }

                val budget = com.example.upitracker.data.Budget(
                    id = budgetId ?: 0,
                    categoryName = trimmedCategory,
                    budgetAmount = amount,
                    periodType = periodType,
                    startDate = startDate,
                    allowRollover = allowRollover // ✨ PASS the new value
                )
                budgetDao.insertOrUpdate(budget)
            }
        }
    }

    fun deleteBudget(budgetId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            budgetDao.deleteById(budgetId)
        }
    }

    fun selectTransaction(id: Int?) {
        _selectedTransactionId.value = id
    }

    fun setUpiLiteSummarySort(field: SortableUpiLiteSummaryField) {
        if (_upiLiteSummarySortField.value == field) {
            _upiLiteSummarySortOrder.value =
                if (_upiLiteSummarySortOrder.value == SortOrder.ASCENDING) SortOrder.DESCENDING else SortOrder.ASCENDING
        } else {
            _upiLiteSummarySortField.value = field
            _upiLiteSummarySortOrder.value = SortOrder.DESCENDING // Default new field sort
        }
    }

    fun setAmountFilter(type: AmountFilterType, value1: Double?, value2: Double? = null) {
        _amountFilterType.value = type
        _amountFilterValue1.value = value1
        _amountFilterValue2.value = value2
    }

    fun toggleUncategorizedFilter(showUncategorized: Boolean) {
        _showUncategorized.value = showUncategorized
    }

    fun setUpiTransactionSort(field: SortableTransactionField) {
        if (_upiTransactionSortField.value == field) {
            // same field → just flip the order
            _upiTransactionSortOrder.value = when (_upiTransactionSortOrder.value) {
                SortOrder.ASCENDING -> SortOrder.DESCENDING
                SortOrder.DESCENDING -> SortOrder.ASCENDING
            }
        } else {
            // new field → set it and pick a sensible default order
            _upiTransactionSortField.value = field
            _upiTransactionSortOrder.value =
                if (field == SortableTransactionField.CATEGORY) SortOrder.ASCENDING
                else SortOrder.DESCENDING
        }
    }

    fun onFilterClick() {
        _showHistoryFilterSheet.value = true
    }

    fun setAppTheme(theme: AppTheme) {
        viewModelScope.launch {
            ThemePreference.setAppTheme(getApplication(), theme)
        }
    }

    fun onFilterSheetDismiss() {
        _showHistoryFilterSheet.value = false
    }

    fun permanentlyDeleteTransaction(transaction: Transaction) {
        viewModelScope.launch(Dispatchers.IO) {
            // This is a hard delete that completely removes the row from the database.
            transactionDao.delete(transaction)
        }
    }

    fun updateTransactionDetails(
        transactionId: Int,
        newDescription: String,
        newAmount: Double,
        newCategory: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            // First, get the original transaction from our current list of transactions
            val originalTransaction = _transactions.value.find { it.id == transactionId }

            originalTransaction?.let {
                // Create a new transaction object by copying the original and applying changes
                val updatedTransaction = it.copy(
                    description = newDescription.trim(),
                    amount = newAmount,
                    category = newCategory?.trim().takeIf { cat -> cat?.isNotBlank() == true }
                )

                // Use the existing DAO function to update the database
                transactionDao.update(updatedTransaction)

                // Let the user know the update was successful
                postPlainSnackbarMessage("Transaction updated successfully!")
            }
        }
    }

    fun processAndInsertTransaction(transaction: Transaction) {
        viewModelScope.launch(Dispatchers.IO) {
            // Only try to categorize if it doesn't already have a category
            if (transaction.category.isNullOrBlank()) {
                val rules = categorySuggestionRules.first() // Get the current list of rules
                var categorizedTransaction = transaction // Start with the original

                for (rule in rules) {
                    val textToMatch = when (rule.fieldToMatch) {
                        RuleField.DESCRIPTION -> transaction.description
                        RuleField.SENDER_OR_RECEIVER -> transaction.senderOrReceiver
                    }.lowercase()

                    val keyword = rule.keyword.lowercase()

                    val isMatch = when (rule.matcher) {
                        RuleMatcher.CONTAINS -> textToMatch.contains(keyword)
                        RuleMatcher.EQUALS -> textToMatch == keyword
                        RuleMatcher.STARTS_WITH -> textToMatch.startsWith(keyword)
                        RuleMatcher.ENDS_WITH -> textToMatch.endsWith(keyword)
                    }

                    if (isMatch) {
                        // Rule matched! Apply the category and stop checking other rules.
                        categorizedTransaction = transaction.copy(category = rule.categoryName)
                        Log.d("AutoCategorize", "Rule matched: '${rule.keyword}' in ${rule.fieldToMatch}. Applying category '${rule.categoryName}'.")
                        break
                    }
                }
                // Insert the transaction, which is either the original or the newly categorized version
                transactionDao.insert(categorizedTransaction)

            } else {
                // Transaction already has a category, just insert it.
                transactionDao.insert(transaction)
            }
        }
    }


    // ✨ 4. ADD these functions at the bottom of your ViewModel for managing the rules ✨

    fun addCategoryRule(
        field: RuleField,
        matcher: RuleMatcher,
        keyword: String,
        category: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (keyword.isNotBlank() && category.isNotBlank()) {
                val newRule = CategorySuggestionRule(
                    fieldToMatch = field,
                    matcher = matcher,
                    keyword = keyword,
                    categoryName = category
                )
                categorySuggestionRuleDao.insert(newRule)
                postPlainSnackbarMessage("New categorization rule saved.")
            }
        }
    }

    fun deleteCategoryRule(rule: CategorySuggestionRule) {
        viewModelScope.launch(Dispatchers.IO) {
            categorySuggestionRuleDao.delete(rule)
            postPlainSnackbarMessage("Rule deleted.")
        }
    }

    fun setIsRefreshingSmsArchive(isRefreshing: Boolean) {
        _isRefreshingSmsArchive.value = isRefreshing
        if (isRefreshing) { // If ad-hoc refresh starts, general import is not also in progress separately
            _isImportingSms.value = false
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            ThemePreference.setDarkMode(getApplication(), enabled)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            // Create a copy of the transaction and set the deletion timestamp.
            val transactionToMark = transaction.copy(
                pendingDeletionTimestamp = System.currentTimeMillis()
            )

            transactionDao.update(transactionToMark)

            _snackbarEvents.emit(
                SnackbarMessage(
                    message = "Moved to Recycle Bin. Will be deleted in 24 hours.", // TODO: Use string resource
                    actionLabel = "Undo", // TODO: Use string resource
                    onAction = {
                        // The UNDO action simply sets the timestamp back to null, making it visible again.
                        viewModelScope.launch {
                            transactionDao.update(transaction.copy(pendingDeletionTimestamp = null))
                            postPlainSnackbarMessage("Transaction restored") // TODO: Use string resource
                        }
                    }
                )
            )
            Log.d("DeleteBug", "Snackbar event emitted successfully.")
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
            _snackbarEvents.emit(SnackbarMessage(message = message))
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

    fun addManualTransaction(
        amount: Double,
        type: String,
        description: String,
        category: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val newTransaction = Transaction(
                // id is 0 so Room will auto-generate a new one
                amount = amount,
                type = type,
                description = description.trim(),
                category = category.trim(),
                date = System.currentTimeMillis(), // Use the current date and time
                senderOrReceiver = "Manual Entry", // A placeholder for the party
                isArchived = false,
                note = ""
            )
            transactionDao.insert(newTransaction)
            // Post a confirmation message to the user
            postPlainSnackbarMessage("Transaction saved successfully!")
        }
    }

    fun exportTransactionsToCsv(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            _isExportingCsv.value = true
            try {
                val allTransactions = _transactions.value
                if (allTransactions.isEmpty()) {
                    postSnackbarMessage(getApplication<Application>().getString(R.string.csv_export_no_transactions))
                    _isExportingCsv.value = false
                    return@launch
                }
                val csvString = CsvExporter.exportTransactionsToCsvString(allTransactions)
                withContext(Dispatchers.IO) {
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(csvString.toByteArray())
                        outputStream.flush()
                    } ?: throw IOException("Failed to open output stream for URI: $uri")
                }
                postSnackbarMessage(getApplication<Application>().getString(R.string.csv_export_success))
            } catch (e: IOException) {
                Log.e("MainViewModelCSV", "Error exporting CSV: ${e.message}", e)
                postSnackbarMessage(
                    getApplication<Application>().getString(
                        R.string.csv_export_error,
                        e.message ?: "Unknown IO Error"
                    )
                )
            } catch (e: Exception) {
                Log.e("MainViewModelCSV", "Unexpected error exporting CSV: ${e.message}", e)
                postSnackbarMessage(getApplication<Application>().getString(R.string.csv_export_error_unexpected))
            } finally {
                _isExportingCsv.value = false
            }
        }
    }

    fun toggleTransactionArchiveStatus(transaction: Transaction, archive: Boolean = true) {
        viewModelScope.launch {
            if (archive) {
                // --- ARCHIVING A TRANSACTION ---
                val transactionToArchive = transaction
                val originalCategory = transaction.category

                val archivedTransaction = transactionToArchive.copy(isArchived = true)
                transactionDao.update(archivedTransaction)

                _snackbarEvents.emit(
                    SnackbarMessage(
                        message = getApplication<Application>().getString(
                            R.string.transaction_archived_snackbar,
                            transactionToArchive.description.take(20)
                        ),
                        actionLabel = getApplication<Application>().getString(R.string.snackbar_action_undo),
                        onAction = {
                            // This self-contained Undo action restores the correct transaction
                            viewModelScope.launch {
                                val restoredTransaction = transactionToArchive.copy(
                                    isArchived = false,
                                    category = originalCategory
                                )
                                transactionDao.update(restoredTransaction)
                            }
                        }
                    )
                )
            } else {
                // --- RESTORING A TRANSACTION (from Archived screen) ---
                val unarchivedTransaction = transaction.copy(isArchived = false)
                transactionDao.update(unarchivedTransaction)
                postPlainSnackbarMessage(
                    getApplication<Application>().getString(
                        R.string.transaction_restored_snackbar,
                        transaction.description.take(20)
                    )
                )
            }
        }
    }

    fun postPlainSnackbarMessage(message: String) {
        viewModelScope.launch {
            _snackbarEvents.emit(SnackbarMessage(message = message))
        }
    }

    private fun getCurrentWeekRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
        val weekStart = calendar.timeInMillis
        calendar.add(Calendar.WEEK_OF_YEAR, 1); calendar.add(Calendar.MILLISECOND, -1)
        val weekEnd = calendar.timeInMillis
        return weekStart to weekEnd
    }

    private fun getCurrentYearRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_YEAR, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
        val yearStart = calendar.timeInMillis
        calendar.add(Calendar.YEAR, 1); calendar.add(Calendar.MILLISECOND, -1)
        val yearEnd = calendar.timeInMillis
        return yearStart to yearEnd
    }

    sealed class UiEvent {
        data class RestartRequired(val message: String) : UiEvent()
    }

    // Initialize base flows collection
    init {
        viewModelScope.launch { _transactions.collect() }
        viewModelScope.launch { _upiLiteSummaries.collect() }
    }
}
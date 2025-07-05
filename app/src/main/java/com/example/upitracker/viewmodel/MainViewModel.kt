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
import com.example.upitracker.data.Category
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
    val amountValue2: Double?,
    val showOnlyLinked: Boolean = false
)

data class BankMessageCount(val bankName: String, val count: Int)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val transactionDao = db.transactionDao()
    private val upiLiteSummaryDao = db.upiLiteSummaryDao()
    private val budgetDao = db.budgetDao()
    private val categorySuggestionRuleDao = db.categorySuggestionRuleDao()
    private val recurringRuleDao = db.recurringRuleDao()
    private val categoryDao = db.categoryDao()

    // --- Base Data Flows (Private) ---
    private val _transactions: StateFlow<List<Transaction>> = transactionDao.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _budgets = budgetDao.getAllActiveBudgets()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _selectedUpiTransactionType = MutableStateFlow(UpiTransactionTypeFilter.ALL)

    private val _refundKeywordUpdateInfo = MutableStateFlow<Pair<String, String>?>(null)
    val refundKeywordUpdateInfo: StateFlow<Pair<String, String>?> = _refundKeywordUpdateInfo.asStateFlow()
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
    private val _showOnlyLinked = MutableStateFlow(false)

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
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val recurringRules: StateFlow<List<RecurringRule>> = recurringRuleDao.getAllRules()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val refundKeyword: StateFlow<String> = ThemePreference.getRefundKeywordFlow(application)
        .stateIn(viewModelScope, SharingStarted.Eagerly, "Refund") // Default to "Refund"


    fun confirmRefundKeywordUpdate() {
        viewModelScope.launch(Dispatchers.IO) {
            _refundKeywordUpdateInfo.value?.let { (oldKeyword, newKeyword) ->
                // Save the new keyword to settings
                ThemePreference.setRefundKeyword(getApplication(), newKeyword)
                // Update the categories in the database
                transactionDao.updateCategoryName(oldKeyword, newKeyword)
                // Hide the dialog
                _refundKeywordUpdateInfo.value = null
                postPlainSnackbarMessage("Refund keyword and existing transactions updated.")
            }
        }
    }

    // 3. ADD this new function to dismiss the dialog
    fun dismissRefundKeywordUpdate() {
        viewModelScope.launch {
            // We still need to save the new keyword even if the user declines to update old transactions
            _refundKeywordUpdateInfo.value?.let { (_, newKeyword) ->
                ThemePreference.setRefundKeyword(getApplication(), newKeyword)
            }
            _refundKeywordUpdateInfo.value = null
        }
    }


    fun setRefundKeyword(newKeyword: String) {
        viewModelScope.launch {
            val oldKeyword = refundKeyword.first()
            // Only show the dialog if the keyword has actually changed to something new
            if (newKeyword.isNotBlank() && !newKeyword.equals(oldKeyword, ignoreCase = true)) {
                _refundKeywordUpdateInfo.value = Pair(oldKeyword, newKeyword)
            }
        }
    }

    private val _nonRefundDebits: StateFlow<List<Transaction>> =
        combine(_transactions, refundKeyword) { transactions, keyword ->
            transactions.filter {
                it.type.equals("DEBIT", ignoreCase = true) &&
                        !it.category.equals(keyword, ignoreCase = true) && it.linkedTransactionId == null
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedTransaction: StateFlow<Transaction?> = _selectedTransactionId.flatMapLatest { id ->
        if (id == null) {
            flowOf(null)
        } else {
            transactionDao.getTransactionById(id)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val categorySuggestionRules: StateFlow<List<CategorySuggestionRule>> =
        categorySuggestionRuleDao.getAllRules()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _searchQuery = MutableStateFlow("")

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

    val filters: StateFlow<TransactionFilters> = combine(
        _selectedUpiTransactionType,
        _selectedDateRangeStart,
        _selectedDateRangeEnd,
        _searchQuery,
        _showUncategorized
    ) { type, startDate, endDate, query, uncategorized ->
        // The first combine handles 5 flows and creates a base object
        TransactionFilters(
            type = type,
            startDate = startDate,
            endDate = endDate,
            searchQuery = query,
            showUncategorized = uncategorized,
            // Default values for the rest, which we will add below
            amountType = AmountFilterType.ALL,
            amountValue1 = null,
            amountValue2 = null,
            showOnlyLinked = false
        )
    }.combine(_amountFilterType) { currentFilters, amountType ->
        // Chain the next flow and update the object
        currentFilters.copy(amountType = amountType)
    }.combine(_amountFilterValue1) { currentFilters, amountVal1 ->
        // Chain the next flow...
        currentFilters.copy(amountValue1 = amountVal1)
    }.combine(_amountFilterValue2) { currentFilters, amountVal2 ->
        // and the next...
        currentFilters.copy(amountValue2 = amountVal2)
    }.combine(_showOnlyLinked) { currentFilters, showLinked ->
        // And finally, our new filter
        currentFilters.copy(showOnlyLinked = showLinked)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = TransactionFilters(
            UpiTransactionTypeFilter.ALL, null, null, "",
            false, AmountFilterType.ALL, null, null, false
        )
    )

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
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppTheme.DEFAULT)

   // val filters: StateFlow<TransactionFilters> = _filters

    val isOnboardingCompleted: StateFlow<Boolean> =
        OnboardingPreference.isOnboardingCompletedFlow(application)
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val archivedUpiTransactions: StateFlow<List<Transaction>> =
        transactionDao.getArchivedTransactions() // Uses the new DAO method
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val incomeVsExpenseData: StateFlow<List<IncomeExpensePoint>> =
        combine(_transactions, _upiLiteSummaries, _nonRefundDebits, _selectedGraphPeriod) { allTrans, summaries, nonRefundDebits, period ->
            if (allTrans.isEmpty()) return@combine emptyList()

            val monthDisplayFormat = SimpleDateFormat("MMM yy", Locale.getDefault())
            val yearMonthKeyFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())

            val incomeByMonth = allTrans.filter { it.type.equals("CREDIT", ignoreCase = true) }.groupBy { yearMonthKeyFormat.format(Date(it.date)) }
            val expenseByMonth = nonRefundDebits.groupBy { yearMonthKeyFormat.format(Date(it.date)) }
            val summariesByMonth = summaries.groupBy { yearMonthKeyFormat.format(Date(it.date)) }

            val reportData = mutableListOf<IncomeExpensePoint>()
            val calendar = Calendar.getInstance()

            for (i in 0 until period.months) {
                val targetCalendar = Calendar.getInstance().apply { time = calendar.time; add(Calendar.MONTH, -i) }
                val yearMonthKey = yearMonthKeyFormat.format(targetCalendar.time)
                val displayLabel = monthDisplayFormat.format(targetCalendar.time)
                targetCalendar.set(Calendar.DAY_OF_MONTH, 1)
                targetCalendar.set(Calendar.HOUR_OF_DAY, 0); targetCalendar.set(Calendar.MINUTE, 0); targetCalendar.set(Calendar.SECOND, 0); targetCalendar.set(Calendar.MILLISECOND, 0)
                val monthStartTimestamp = targetCalendar.timeInMillis

                val income = incomeByMonth[yearMonthKey]?.sumOf { it.amount } ?: 0.0
                val regularExpense = expenseByMonth[yearMonthKey]?.sumOf { it.amount } ?: 0.0
                val liteExpense = summariesByMonth[yearMonthKey]?.sumOf { it.totalAmount } ?: 0.0

                reportData.add(
                    IncomeExpensePoint(
                        yearMonth = displayLabel,
                        totalIncome = income,
                        totalExpense = regularExpense + liteExpense,
                        timestamp = monthStartTimestamp
                    )
                )
            }
            reportData.reversed()
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val budgetStatuses: StateFlow<List<BudgetStatus>> =
        combine(_nonRefundDebits, _budgets) { transactions, budgets ->
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

    // The return type is now StateFlow<Map<String, List<Transaction>>>
    val filteredUpiTransactions: StateFlow<Map<String, List<Transaction>>> =
        combine(
            _transactions,
            filters,
            _upiTransactionSortField,
            _upiTransactionSortOrder
        ) { transactions, filters, sortField, sortOrder ->
            // This 'filters' object is the complete, up-to-date TransactionFilters object

            // --- All of your existing filtering logic from Step 1 to 6 remains the same ---

            // 1) Linked Status Filter
            val byLinkedStatus = if (filters.showOnlyLinked) {
                transactions.filter { it.linkedTransactionId != null }
            } else {
                transactions
            }
            // 2) Type filter
            val byType = when (filters.type) {
                UpiTransactionTypeFilter.ALL -> byLinkedStatus
                UpiTransactionTypeFilter.DEBIT -> byLinkedStatus.filter { it.type.equals("DEBIT", ignoreCase = true) }
                UpiTransactionTypeFilter.CREDIT -> byLinkedStatus.filter { it.type.equals("CREDIT", ignoreCase = true) }
            }
            // 3) Date filter
            val byDate = when {
                filters.startDate != null && filters.endDate != null -> byType.filter { it.date in filters.startDate..filters.endDate }
                filters.startDate != null -> byType.filter { it.date >= filters.startDate }
                filters.endDate != null -> byType.filter { it.date <= filters.endDate }
                else -> byType
            }
            // 4) Search filter
            val bySearch = if (filters.searchQuery.isBlank()) byDate else byDate.filter { tx ->
                tx.description.contains(filters.searchQuery, ignoreCase = true) ||
                        tx.senderOrReceiver.contains(filters.searchQuery, ignoreCase = true) ||
                        (tx.category?.contains(filters.searchQuery, ignoreCase = true) == true)
            }
            // 5) Uncategorized Filter
            val byUncategorized = if (filters.showUncategorized) {
                bySearch.filter { it.category.isNullOrBlank() }
            } else {
                bySearch
            }
            // 6) Amount Filter
            val byAmount = when (filters.amountType) {
                AmountFilterType.GREATER_THAN -> filters.amountValue1?.let { limit -> byUncategorized.filter { it.amount > limit } } ?: byUncategorized
                AmountFilterType.LESS_THAN -> filters.amountValue1?.let { limit -> byUncategorized.filter { it.amount < limit } } ?: byUncategorized
                AmountFilterType.RANGE -> if (filters.amountValue1 != null && filters.amountValue2 != null) {
                    byUncategorized.filter { it.amount in filters.amountValue1..filters.amountValue2 }
                } else byUncategorized
                AmountFilterType.ALL -> byUncategorized
            }

            // 7) Sort
            val sortedTransactions = when (sortField) {
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

            // ✨ FINAL STEP: Group the sorted list into a Map before finishing ✨
            val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
            sortedTransactions.groupBy { transaction ->
                Instant.ofEpochMilli(transaction.date)
                    .atZone(ZoneId.systemDefault())
                    .format(formatter)
            }
        }
            .flowOn(Dispatchers.Default) // Run all this work on a background thread
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap()) // Initial value is an emptyMap()


    fun toggleShowOnlyLinked(isEnabled: Boolean) {
        _showOnlyLinked.value = isEnabled
    }

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
        combine(_nonRefundDebits, _upiLiteSummaries,  isUpiLiteEnabled) { transactions, summaries, upiLiteEnabled ->
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
            .flowOn(Dispatchers.Default)
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
        combine(_nonRefundDebits, _selectedGraphPeriod) { allTransactions, period ->
            val result = calculateLastNMonthsExpenses(allTransactions, period.months)
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

    val dailyExpensesTrend: StateFlow<List<DailyExpensePoint>> = _nonRefundDebits
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

    private fun calculateLastNMonthsExpenses(transactions: List<Transaction>, n: Int): List<MonthlyExpense> {
        if (transactions.isEmpty() || n <= 0) {
            return emptyList()
        }
        val monthDisplayFormat = SimpleDateFormat("MMM yy", Locale.getDefault())
        val yearMonthKeyFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())

        val debitTransactions = transactions.filter {
            it.type.equals("DEBIT", ignoreCase = true)
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
            _nonRefundDebits, // Use the pre-filtered list of expenses
            _selectedDateRangeStart,
            _selectedDateRangeEnd
        ) { nonRefundDebits, startDate, endDate ->

            // 1. Apply the date filter to our clean list of expenses
            val filteredForDate = when {
                startDate != null && endDate != null -> nonRefundDebits.filter { it.date in startDate..endDate }
                startDate != null -> nonRefundDebits.filter { it.date >= startDate }
                endDate != null -> {
                    val endOfDay = Calendar.getInstance().apply {
                        timeInMillis = endDate
                        set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
                    }.timeInMillis
                    nonRefundDebits.filter { it.date <= endOfDay }
                }
                else -> nonRefundDebits
            }

            // 2. Group the remaining transactions by category
            filteredForDate
                .filter { !it.category.isNullOrBlank() }
                .groupBy { it.category!! }
                .map { (categoryName, transactionsInCategory) ->
                    CategoryExpense(
                        categoryName = categoryName,
                        totalAmount = transactionsInCategory.sumOf { it.amount }
                    )
                }
                .sortedByDescending { it.totalAmount }
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allCategories: StateFlow<List<Category>> = categoryDao.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())


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

    private fun calculateNextDueDate(startFrom: Long, dayOfPeriod: Int, periodType: BudgetPeriod): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = startFrom
        }

        // Move to the next period
        when (periodType) {
            BudgetPeriod.MONTHLY -> calendar.add(Calendar.MONTH, 1)
            BudgetPeriod.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            BudgetPeriod.YEARLY -> calendar.add(Calendar.YEAR, 1)
        }

        // This is the key fix for end-of-month issues.
        // If the user wants the 31st, but the next month only has 30 days, this will correctly use the 30th.
        val maxDayInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        calendar.set(Calendar.DAY_OF_MONTH, dayOfPeriod.coerceAtMost(maxDayInMonth))

        // Set a consistent time of day
        calendar.set(Calendar.HOUR_OF_DAY, 12)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        return calendar.timeInMillis
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
            // ✨ FIX: Use the helper function to calculate the due date ✨
            // We start from a month ago to ensure the first date is in the correct upcoming month.
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MONTH, -1)
            val firstDueDate = calculateNextDueDate(calendar.timeInMillis, dayOfPeriod, period)

            val newRule = RecurringRule(
                amount = amount,
                description = description,
                categoryName = category,
                periodType = period,
                dayOfPeriod = dayOfPeriod,
                nextDueDate = firstDueDate
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
            val originalRule = recurringRuleDao.getRuleById(ruleId) ?: return@launch

            // ✨ FIX: Use the helper function to recalculate the due date if needed ✨
            val nextDueDate = if (originalRule.dayOfPeriod != newDay || originalRule.periodType != newPeriod) {
                // Start from a month ago to ensure the next date is calculated correctly based on today.
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.MONTH, -1)
                calculateNextDueDate(calendar.timeInMillis, newDay, newPeriod)
            } else {
                originalRule.nextDueDate
            }

            val updatedRule = originalRule.copy(
                description = newDescription,
                amount = newAmount,
                categoryName = newCategory,
                periodType = newPeriod,
                dayOfPeriod = newDay,
                nextDueDate = nextDueDate
            )
            recurringRuleDao.update(updatedRule)
            postPlainSnackbarMessage("Recurring rule updated.")
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

    private suspend fun findAndLinkRefund(refund: Transaction) {
        val potentialMatches = transactionDao.findPotentialDebitsForRefund(refund.senderOrReceiver)

        // Find the best match (same amount, closest in time before the refund)
        val bestMatch = potentialMatches.filter { it.amount == refund.amount && it.date <= refund.date }.minByOrNull { refund.date - it.date }

        if (bestMatch != null) {
            // Link them together
            val linkedRefund = refund.copy(linkedTransactionId = bestMatch.id)
            val linkedDebit = bestMatch.copy(linkedTransactionId = refund.id)

            transactionDao.update(linkedRefund)
            transactionDao.update(linkedDebit)

            postPlainSnackbarMessage("Successfully linked refund to a previous purchase.")
        } else {
            postPlainSnackbarMessage("Refund categorized, but no matching purchase was found to link.")
        }
    }


    fun updateTransactionDetails(
        transactionId: Int,
        newDescription: String,
        newAmount: Double,
        newCategory: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val originalTransaction = _transactions.value.find { it.id == transactionId }

            originalTransaction?.let {
                val isManualEntry = it.senderOrReceiver == "Manual Entry"

                // Create the updated transaction based on our new rule
                val updatedTransaction = if (isManualEntry) {
                    // If it's a manual entry, update everything.
                    it.copy(
                        description = newDescription.trim(),
                        amount = newAmount,
                        category = newCategory?.trim().takeIf { cat -> cat?.isNotBlank() == true }
                    )
                } else {
                    it.copy(
                        category = newCategory?.trim().takeIf { cat -> cat?.isNotBlank() == true }
                    )
                }
                transactionDao.update(updatedTransaction)

                val refundKeywordValue = refundKeyword.first()
                // If the category was just changed TO the refund keyword...
                if (updatedTransaction.type == "CREDIT" &&
                    newCategory?.equals(refundKeywordValue, ignoreCase = true) == true &&
                    !originalTransaction.category.equals(refundKeywordValue, ignoreCase = true)
                ) {
                    findAndLinkRefund(updatedTransaction)
                }
                // If the category was just changed AWAY FROM the refund keyword...
                else if (originalTransaction.category.equals(refundKeywordValue, ignoreCase = true) &&
                    !newCategory.equals(refundKeywordValue, ignoreCase = true) &&
                    originalTransaction.linkedTransactionId != null)
                {
                    // Unlink the pair
                    transactionDao.unlinkTransaction(originalTransaction.id)
                    transactionDao.unlinkTransaction(originalTransaction.linkedTransactionId)
                    postPlainSnackbarMessage("Unlinked refund from purchase.")
                }

                postPlainSnackbarMessage("Transaction updated successfully!")
            }
        }
    }

    fun processAndInsertTransaction(transaction: Transaction) {
        viewModelScope.launch(Dispatchers.IO) {
            if (transaction.category.isNullOrBlank()) {
                Log.d("AutoCategorize", "Processing new uncategorized transaction: '${transaction.description}'")

                val rules = categorySuggestionRules.first()
                Log.d("AutoCategorize", "Found ${rules.size} rules to check against.")

                var bestMatch: CategorySuggestionRule? = null

                // ✨ NEW, SMARTER LOGIC ✨
                // We now loop through all rules to find the best possible match.
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
                        // Check if this match is better than what we've already found.
                        if (bestMatch == null) {
                            // It's the first match, so it's the best so far.
                            bestMatch = rule
                        } else if (rule.priority > bestMatch.priority) {
                            // This rule has a higher priority, so it's better.
                            bestMatch = rule
                        } else if (rule.priority == bestMatch.priority && rule.keyword.length > bestMatch.keyword.length) {
                            // Priorities are the same, so choose the one with the longer, more specific keyword.
                            bestMatch = rule
                        }
                    }
                }

                val finalTransaction = if (bestMatch != null) {
                    Log.i("AutoCategorize", "SUCCESS: Best rule found! Keyword='${bestMatch.keyword}', Category='${bestMatch.categoryName}'. Applying to transaction.")
                    transaction.copy(category = bestMatch.categoryName)
                } else {
                    Log.d("AutoCategorize", "No matching rule found for this transaction.")
                    transaction
                }

                transactionDao.insert(finalTransaction)

            } else {
                Log.d("AutoCategorize", "Transaction already has category '${transaction.category}', skipping rule check.")
                transactionDao.insert(transaction)
            }
        }
    }


    // Add this new function to your MainViewModel
    fun reapplyRulesToTransaction(transaction: Transaction) {
        viewModelScope.launch(Dispatchers.IO) {
            if (transaction.category.isNullOrBlank()) {
                val rules = categorySuggestionRules.first()
                var bestMatch: CategorySuggestionRule? = null

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
                        if (bestMatch == null || (rule.priority > bestMatch.priority) ||
                            (rule.priority == bestMatch.priority && rule.keyword.length > bestMatch.keyword.length)) {
                            bestMatch = rule
                        }
                    }
                }

                if (bestMatch != null) {
                    val updatedTransaction = transaction.copy(category = bestMatch.categoryName)
                    transactionDao.update(updatedTransaction) // Update the transaction with the new category

                    postPlainSnackbarMessage("Transaction categorized as '${bestMatch.categoryName}'!")

                    val refundKeywordValue = refundKeyword.first()
                    if (updatedTransaction.type == "CREDIT" && updatedTransaction.category.equals(refundKeywordValue, ignoreCase = true)) {
                        // This will now correctly find a match and show the "Successfully linked..." snackbar.
                        findAndLinkRefund(updatedTransaction)
                    }
                } else {
                    postPlainSnackbarMessage("No matching rule found.")
                }
            }
        }
    }

    fun addCategoryRule(
        field: RuleField,
        matcher: RuleMatcher,
        keyword: String,
        category: String,
        priority: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (keyword.isNotBlank() && category.isNotBlank()) {
                val newRule = CategorySuggestionRule(
                    fieldToMatch = field,
                    matcher = matcher,
                    keyword = keyword,
                    categoryName = category,
                    priority = priority
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
                category = category.trim().takeIf { it.isNotEmpty() },
                date = System.currentTimeMillis(), // Use the current date and time
                senderOrReceiver = "Manual Entry", // A placeholder for the party
                isArchived = false,
                note = ""
            )

            processAndInsertTransaction(newTransaction)

            // Post a confirmation message to the user
            postPlainSnackbarMessage("Transaction saved successfully!")
            _uiEvents.emit(UiEvent.ScrollToTop)
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
        object ScrollToTop : UiEvent()
    }

    // Initialize base flows collection
    init {
        viewModelScope.launch { _transactions.collect() }
        viewModelScope.launch { _upiLiteSummaries.collect() }
    }
}
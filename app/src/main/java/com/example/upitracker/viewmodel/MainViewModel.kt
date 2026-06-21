package com.example.upitracker.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import android.database.sqlite.SQLiteDatabase
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.upitracker.R
import com.example.upitracker.data.AppDatabase
import com.example.upitracker.data.ArchivedSmsMessage
import com.example.upitracker.data.BudgetPeriod
import com.example.upitracker.data.Transaction
import com.example.upitracker.data.UpiLiteSummary
import com.example.upitracker.util.CsvExporter
import com.example.upitracker.util.OnboardingPreference
import com.example.upitracker.util.ThemePreference
import com.example.upitracker.sms.SmsProcessingService
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
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.upitracker.data.Category
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableSharedFlow // Add this import
import kotlinx.coroutines.flow.asSharedFlow
import com.example.upitracker.data.TransactionDao
import com.example.upitracker.util.CryptoManager
import com.example.upitracker.data.RecurringRule
import com.example.upitracker.data.CategorySuggestionRule
import com.example.upitracker.data.RuleField
import com.example.upitracker.data.RuleLogic
import com.example.upitracker.data.RuleMatcher
import com.example.upitracker.util.AppTheme
import com.example.upitracker.util.AutoLockDelay
import com.example.upitracker.util.BankIdentifier
import com.example.upitracker.util.HomeScreenStyle
import com.example.upitracker.util.NotificationHelper
import com.example.upitracker.util.PinStorage
import com.example.upitracker.util.PortableBackupCrypto
import com.example.upitracker.util.TagUtils
import com.example.upitracker.util.toPaise
import com.example.upitracker.util.toMajorUnits
import java.io.File
import java.io.FileOutputStream
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.math.abs


// --- Data classes and Enums (should be defined here or imported if in separate files) ---
data class MonthlyExpense(
    val yearMonth: String,
    val totalAmount: Double,
    val timestamp: Long
)

data class MerchantDna(
    val totalSpent: Double,
    val transactionCount: Int,
    val averageSpend: Double,
    val favoriteDay: String, // e.g., "Friday"
    val recentTrend: String,  // "↑ 10% vs last month" (Optional, we'll keep it simple for now)
    val loyaltyShare: Float // ✨ New Field: Percentage of category spend
)

data class VelocityState(
    val totalBudget: Double = 0.0,
    val totalSpent: Double = 0.0,
    val daysRemaining: Int = 0
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
    val allowRollover: Boolean,
    val rolloverAmount: Double,
    val effectiveBudget: Double
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
    TRANSACTION_COUNT
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
    val onAction: (() -> Unit)? = null,
    val onDismiss: (() -> Unit)? = null
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
    val showOnlyLinked: Boolean = false,
    val selectedCategories: Set<String> = emptySet(),
    val bankNameFilter: String? = null
)

data class FilteredTotals(
    val totalDebitPaise: Long = 0L,
    val totalCreditPaise: Long = 0L
)

data class GroupedUpiLiteSummaries(
    val monthYear: String,
    val summaries: List<UpiLiteSummary>,
    val monthlyTotal: Double,
    val count: Int
)

data class SmsSyncProgressState(
    val isSyncing: Boolean = false,
    val currentProgress: Int = 0,
    val totalMessages: Int = 0,
    val isInitialImport: Boolean = false
) {
    val percentage: Float
        get() = if (totalMessages > 0) currentProgress.toFloat() / totalMessages else 0f
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val transactionDao = db.transactionDao()
    private val upiLiteSummaryDao = db.upiLiteSummaryDao()
    private val budgetDao = db.budgetDao()
    private val categorySuggestionRuleDao = db.categorySuggestionRuleDao()
    private val recurringRuleDao = db.recurringRuleDao()
    private val categoryDao = db.categoryDao()

    // --- Base Data Flows (Private) ---
    private val _transactions: StateFlow<List<Transaction>> = transactionDao.getRecentTransactions(ANALYTICS_TRANSACTION_LIMIT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<Transaction>> = _transactions

    private val _budgets = budgetDao.getAllActiveBudgets()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _selectedUpiTransactionType = MutableStateFlow(UpiTransactionTypeFilter.ALL)

    private val _refundKeywordUpdateInfo = MutableStateFlow<Pair<String, String>?>(null)
    val refundKeywordUpdateInfo: StateFlow<Pair<String, String>?> = _refundKeywordUpdateInfo.asStateFlow()
    val selectedUpiTransactionType: StateFlow<UpiTransactionTypeFilter> =
        _selectedUpiTransactionType.asStateFlow()

    val isUpiLiteEnabled: StateFlow<Boolean> = ThemePreference.isUpiLiteEnabledFlow(application)
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    val homeScreenStyle: StateFlow<HomeScreenStyle> =
        ThemePreference.getHomeScreenStyleFlow(application)
            .stateIn(
                viewModelScope,
                SharingStarted.Lazily,
                HomeScreenStyle.CURRENT_MONTH
            )


    private val _upiLiteSummaries: StateFlow<List<UpiLiteSummary>> =
        upiLiteSummaryDao.getAllSummaries()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val refundCategory = "Refund"

    private val _isBackingUp = MutableStateFlow(false)
    val isBackingUp: StateFlow<Boolean> = _isBackingUp.asStateFlow()

    private val _isRestoring = MutableStateFlow(false)
    val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()

    private val _selectedDateRangeStart = MutableStateFlow<Long?>(null)
    private val _selectedDateRangeEnd   = MutableStateFlow<Long?>(null)

    private val _categoryFilter = MutableStateFlow<Set<String>>(emptySet())

    private val _selectedTransactionId = MutableStateFlow<Int?>(null)
    private val _showOnlyLinked = MutableStateFlow(false)

    private val _amountFilterType = MutableStateFlow(AmountFilterType.ALL)
    private val _amountFilterValue1 = MutableStateFlow<Double?>(null)
    private val _amountFilterValue2 = MutableStateFlow<Double?>(null)

    private val _showUncategorized = MutableStateFlow(false)

    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    private val _selectedTransactionIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedTransactionIds: StateFlow<Set<Int>> = _selectedTransactionIds.asStateFlow()

    private val _isSelectionModeActive = MutableStateFlow(false)
    val isSelectionModeActive: StateFlow<Boolean> = _isSelectionModeActive.asStateFlow()

    // --- UI State Flows (Public) ---
    private val _snackbarEvents = MutableSharedFlow<SnackbarMessage>()
    val snackbarEvents: SharedFlow<SnackbarMessage> = _snackbarEvents.asSharedFlow()

    private val _isImportingSms = MutableStateFlow(false)
    val isImportingSms: StateFlow<Boolean> = _isImportingSms.asStateFlow()

    private val _isRefreshingSmsArchive = MutableStateFlow(false)

    private val _smsSyncProgress = MutableStateFlow(SmsSyncProgressState())
    val smsSyncProgress: StateFlow<SmsSyncProgressState> = _smsSyncProgress.asStateFlow()

    private val _showHistoryFilterSheet = MutableStateFlow(false)
    val showHistoryFilterSheet: StateFlow<Boolean> = _showHistoryFilterSheet.asStateFlow()

    val isRefreshingSmsArchive: StateFlow<Boolean> = _isRefreshingSmsArchive.asStateFlow()

    val isDarkMode: StateFlow<Boolean> = ThemePreference.isDarkModeFlow(application)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val recurringRules: StateFlow<List<RecurringRule>> = recurringRuleDao.getAllRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestBankBalances: StateFlow<List<TransactionDao.BankBalance>> = transactionDao.getLatestBankBalances()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val balanceDrifts: StateFlow<List<TransactionDao.BalanceDrift>> = transactionDao.getBalanceDrifts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Smart Subscription Detector ---
    data class DetectedSubscription(val merchant: String, val amount: Double, val frequencyDays: Int, val confidenceScore: Float)
    private val _dismissedSubscriptions = MutableStateFlow<Set<String>>(emptySet())
    
    val detectedSubscriptions: StateFlow<List<DetectedSubscription>> = combine(
        _transactions,
        recurringRules,
        _dismissedSubscriptions
    ) { transactions, rules, dismissed ->
        val debits = transactions.filter { it.type == "DEBIT" }
        val potentialSubs = mutableListOf<DetectedSubscription>()
        
        // Group by Merchant and Amount
        val groups = debits.groupBy { Pair(it.senderOrReceiver.uppercase(), it.amount) }
        
        for ((key, txns) in groups) {
            val merchant = key.first
            val amount = key.second
            
            // Skip if already in rules
            if (rules.any { it.description.equals(merchant, ignoreCase = true) || it.amount == amount }) continue
            
            if (txns.size >= 3) {
                // Sort by date descending
                val sortedTxns = txns.sortedByDescending { it.date }
                val intervals = mutableListOf<Long>()
                
                for (i in 0 until sortedTxns.size - 1) {
                    val diff = sortedTxns[i].date - sortedTxns[i+1].date
                    val days = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff).toInt()
                    intervals.add(days.toLong())
                }
                
                val avgInterval = intervals.average()
                
                // If it happens roughly monthly (25-35 days) or weekly (6-8 days)
                if ((avgInterval in 25.0..35.0) || (avgInterval in 6.0..8.0)) {
                    val variance = intervals.map { abs(it - avgInterval) }.average()
                    val confidence = if (variance < 3) 0.9f else if (variance < 5) 0.7f else 0.5f
                    
                    if (confidence > 0.6f) {
                        potentialSubs.add(
                            DetectedSubscription(
                                merchant = merchant,
                                amount = amount,
                                frequencyDays = avgInterval.toInt(),
                                confidenceScore = confidence
                            )
                        )
                    }
                }
            }
        }
        
        potentialSubs.filterNot { "${it.merchant}|${it.amount}" in dismissed }.sortedByDescending { it.confidenceScore }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    data class SubscriptionPriceChange(val merchant: String, val previousAmount: Double, val latestAmount: Double)
    val subscriptionPriceChanges: StateFlow<List<SubscriptionPriceChange>> = combine(_transactions, recurringRules) { transactions, rules ->
        rules.mapNotNull { rule ->
            val latest = transactions.firstOrNull {
                it.type == "DEBIT" && it.senderOrReceiver.equals(rule.description, ignoreCase = true)
            } ?: return@mapNotNull null
            if (latest.amountPaise > (rule.amountPaise * 1.02).toLong()) {
                SubscriptionPriceChange(rule.description, rule.amount, latest.amount)
            } else null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun dismissDetectedSubscription(subscription: DetectedSubscription) {
        _dismissedSubscriptions.value += "${subscription.merchant}|${subscription.amount}"
    }

    val refundKeyword: StateFlow<String> = ThemePreference.getRefundKeywordFlow(application)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Refund")


    fun confirmRefundKeywordUpdate() {
        viewModelScope.launch(Dispatchers.IO) {
            _refundKeywordUpdateInfo.value?.let { (oldKeyword, newKeyword) ->
                ThemePreference.setRefundKeyword(getApplication(), newKeyword)
                transactionDao.updateCategoryName(oldKeyword, newKeyword)
                _refundKeywordUpdateInfo.value = null
                postPlainSnackbarMessage("Refund keyword and existing transactions updated.")
            }
        }
    }

    fun dismissRefundKeywordUpdate() {
        viewModelScope.launch {
            _refundKeywordUpdateInfo.value?.let { (_, newKeyword) ->
                ThemePreference.setRefundKeyword(getApplication(), newKeyword)
            }
            _refundKeywordUpdateInfo.value = null
        }
    }


    fun setRefundKeyword(newKeyword: String) {
        viewModelScope.launch {
            val oldKeyword = refundKeyword.first()
            if (newKeyword.isNotBlank() && !newKeyword.equals(oldKeyword, ignoreCase = true)) {
                _refundKeywordUpdateInfo.value = Pair(oldKeyword, newKeyword)
            }
        }
    }

    private val _bankFilter = MutableStateFlow<String?>(null)

    private val _merchantDna = MutableStateFlow<MerchantDna?>(null)
    val merchantDna = _merchantDna.asStateFlow()

    fun loadMerchantDna(sender: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val history = transactionDao.getHistoryForMerchant(sender)
            if (history.isEmpty()) {
                _merchantDna.value = null
                return@launch
            }

            val total = history.sumOf { it.amountPaise }.toMajorUnits()
            val count = history.size
            val avg = total / count

            // Calculate Favorite Day
            val calendar = Calendar.getInstance()
            val dayCounts = history.groupingBy {
                calendar.timeInMillis = it.date
                calendar.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon...
            }.eachCount()

            val topDayInt = dayCounts.maxByOrNull { it.value }?.key ?: Calendar.MONDAY
            val days = arrayOf("", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
            val favDay = days.getOrElse(topDayInt) { "Monday" }
            
            // Calculate Loyalty Share (Percentage of total spend in their primary category)
            val primaryCategory = history.groupingBy { it.category }.eachCount().maxByOrNull { it.value }?.key
            val loyaltyShare = if (!primaryCategory.isNullOrBlank()) {
                val categoryTotal = transactionDao.getCategoryDebitTotalPaise(primaryCategory).toMajorUnits()
                if (categoryTotal > 0) (total / categoryTotal).toFloat() else 1f
            } else {
                1f // If uncategorized, they have 100% share of "uncategorized" effectively
            }

            _merchantDna.value = MerchantDna(
                totalSpent = total,
                transactionCount = count,
                averageSpend = avg,
                favoriteDay = favDay,
                recentTrend = "",
                loyaltyShare = loyaltyShare
            )
        }
    }

    // Call this when clearing selection
    fun clearMerchantDna() {
        _merchantDna.value = null
    }

    private val _nonRefundDebits: StateFlow<List<Transaction>> =
        combine(_transactions, refundKeyword) { transactions, keyword ->
            transactions.filter {
                it.type.equals("DEBIT", ignoreCase = true) &&
                        !it.category.equals(keyword, ignoreCase = true) && it.linkedTransactionId == null
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedTransaction: StateFlow<Transaction?> = _selectedTransactionId.flatMapLatest { id ->
        if (id == null) {
            flowOf(null)
        } else {
            transactionDao.getTransactionById(id)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val linkedSelectedTransaction: StateFlow<Transaction?> = selectedTransaction.flatMapLatest { transaction ->
        when {
            transaction == null -> flowOf(null)
            transaction.linkedTransactionId != null -> transactionDao.getTransactionById(transaction.linkedTransactionId)
            else -> transactionDao.getTransactionLinkedToFlow(transaction.id)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val categorySuggestionRules: StateFlow<List<CategorySuggestionRule>> =
        categorySuggestionRuleDao.getAllRules()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")

    private val archivedSmsDao = db.archivedSmsMessageDao()
    val parserFailures: StateFlow<List<ArchivedSmsMessage>> = archivedSmsDao.getRecentParserFailures()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filters: StateFlow<TransactionFilters> = combine(
        _selectedUpiTransactionType,
        _selectedDateRangeStart,
        _selectedDateRangeEnd,
        _searchQuery,
        _showUncategorized
    ) { type, startDate, endDate, query, uncategorized ->
        TransactionFilters(
            type = type,
            startDate = startDate,
            endDate = endDate,
            searchQuery = query,
            showUncategorized = uncategorized,
            amountType = AmountFilterType.ALL,
            amountValue1 = null,
            amountValue2 = null,
            showOnlyLinked = false
        )
    }.combine(_amountFilterType) { currentFilters, amountType ->
        currentFilters.copy(amountType = amountType)
    }.combine(_amountFilterValue1) { currentFilters, amountVal1 ->
        currentFilters.copy(amountValue1 = amountVal1)
    }.combine(_amountFilterValue2) { currentFilters, amountVal2 ->
        currentFilters.copy(amountValue2 = amountVal2)
    }.combine(_showOnlyLinked) { currentFilters, showLinked ->
        currentFilters.copy(showOnlyLinked = showLinked)
    }.combine(_categoryFilter) { currentFilters, categories -> // ✨ ADD THIS
        currentFilters.copy(selectedCategories = categories)
    }.combine(_bankFilter) { currentFilters, bank -> // ✨ ADD THIS
        currentFilters.copy(bankNameFilter = bank)
    }
        .stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = TransactionFilters(
            UpiTransactionTypeFilter.ALL, null, null, "",
            false, AmountFilterType.ALL, null, null,
            false, emptySet(), null
        )
    )

    val allCategories: StateFlow<List<Category>> = categoryDao.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userCategories: StateFlow<List<Category>> =
        combine(transactionDao.getMostUsedCategoryNames(), allCategories) { frequentCategoryNames, allCategories ->
            // Create a map of category names to their full Category objects for easy lookup
            val categoryMap = allCategories.associateBy { it.name }

            // Count the frequency of each category name in the transactions
            // Map the frequent names back to their full Category objects
            frequentCategoryNames.mapNotNull { name -> categoryMap[name] }
        }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _selectedGraphPeriod = MutableStateFlow(GraphPeriod.SIX_MONTHS)
    val selectedGraphPeriod: StateFlow<GraphPeriod> = _selectedGraphPeriod.asStateFlow()

    private fun getPreviousWeekRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.WEEK_OF_YEAR, -1)
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
        calendar.add(Calendar.MONTH, -1)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
        val monthStart = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1); calendar.add(Calendar.MILLISECOND, -1)
        val monthEnd = calendar.timeInMillis
        return monthStart to monthEnd
    }

    // Add this new public function
    fun setBankFilter(bankName: String?) {
        _bankFilter.value = bankName
    }

    private fun getPreviousYearRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -1)
        calendar.set(Calendar.DAY_OF_YEAR, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
        val yearStart = calendar.timeInMillis
        calendar.add(Calendar.YEAR, 1); calendar.add(Calendar.MILLISECOND, -1)
        val yearEnd = calendar.timeInMillis
        return yearStart to yearEnd
    }

    fun backupDatabase(targetUri: Uri, contentResolver: ContentResolver, password: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            _isBackingUp.value = true
            val snapshotFile = File(getApplication<Application>().cacheDir, "upi_tracker_snapshot_${System.nanoTime()}.db")
            try {
                val escapedPath = snapshotFile.absolutePath.replace("'", "''")
                db.openHelper.writableDatabase.execSQL("VACUUM INTO '$escapedPath'")

                contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                    if (password.isBlank()) {
                        writeChunkedEncryptedBackup(snapshotFile, outputStream)
                    } else {
                        snapshotFile.inputStream().use { source ->
                            PortableBackupCrypto.encrypt(source, outputStream, password.toCharArray())
                        }
                    }
                } ?: throw IOException("Failed to open output stream for URI: $targetUri")

                postPlainSnackbarMessage("Backup successful (Encrypted)!")

            } catch (e: Exception) {
                Log.e("BackupRestore", "Error during database backup", e)
                postPlainSnackbarMessage("Error: Backup failed. ${e.message}")
            } finally {
                snapshotFile.delete()
                _isBackingUp.value = false
            }
        }
    }

    fun restoreDatabase(sourceUri: Uri, contentResolver: ContentResolver, password: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            _isRestoring.value = true
            val context = getApplication<Application>()
            val restoreFile = File(context.cacheDir, "upi_tracker_restore_${System.nanoTime()}.db")
            val targetDbFile = context.getDatabasePath("upi_tracker_db")
            val rollbackFile = File(targetDbFile.path + ".pre_restore")
            var databaseClosed = false
            try {
                contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                    restoreFile.outputStream().buffered().use { outputStream ->
                        val buffered = if (inputStream.markSupported()) inputStream else inputStream.buffered()
                        if (PortableBackupCrypto.isPortable(buffered)) {
                            require(password.isNotBlank()) { "This portable backup requires its password" }
                            PortableBackupCrypto.decrypt(buffered, outputStream, password.toCharArray())
                        } else {
                            readEncryptedBackup(buffered, outputStream)
                        }
                    }
                } ?: throw IOException("Failed to open input stream for URI: $sourceUri")

                validateSqliteFile(restoreFile)
                AppDatabase.closeDatabase()
                databaseClosed = true
                File(targetDbFile.path + "-wal").delete()
                File(targetDbFile.path + "-shm").delete()
                targetDbFile.parentFile?.mkdirs()
                rollbackFile.delete()
                if (targetDbFile.exists()) {
                    Files.move(
                        targetDbFile.toPath(),
                        rollbackFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                    )
                }
                Files.move(
                    restoreFile.toPath(),
                    targetDbFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
                rollbackFile.delete()

                _uiEvents.emit(UiEvent.RestartRequired("Restore successful! The app must now restart to apply changes."))

            } catch (e: Exception) {
                Log.e("BackupRestore", "Error during database restore (Decryption failed?)", e)
                if (databaseClosed) {
                    if (rollbackFile.exists()) {
                        try {
                            Files.move(
                                rollbackFile.toPath(),
                                targetDbFile.toPath(),
                                StandardCopyOption.REPLACE_EXISTING
                            )
                        } catch (rollbackError: Exception) {
                            Log.e("BackupRestore", "Failed to roll back database restore", rollbackError)
                        }
                    }
                    _uiEvents.emit(UiEvent.RestartRequired("Restore failed safely. Restart the app to reopen the existing database."))
                } else {
                    postPlainSnackbarMessage("Error: Restore failed. Is this a valid encrypted backup?")
                }
            } finally {
                restoreFile.delete()
                if (targetDbFile.exists()) rollbackFile.delete()
                _isRestoring.value = false
            }
        }
    }

    private fun writeChunkedEncryptedBackup(source: File, output: OutputStream) {
        val aead = CryptoManager.getAead()
        DataOutputStream(output.buffered()).use { dataOutput ->
            dataOutput.write(BACKUP_MAGIC)
            source.inputStream().buffered().use { input ->
                val buffer = ByteArray(BACKUP_CHUNK_SIZE)
                var index = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    val encrypted = aead.encrypt(buffer.copyOf(count), backupAssociatedData(index))
                    dataOutput.writeInt(encrypted.size)
                    dataOutput.write(encrypted)
                    index++
                }
                val endMarker = aead.encrypt(ByteArray(0), backupAssociatedData(index))
                dataOutput.writeInt(endMarker.size)
                dataOutput.write(endMarker)
            }
        }
    }

    private fun readEncryptedBackup(input: InputStream, output: OutputStream) {
        val bufferedInput = BufferedInputStream(input)
        bufferedInput.mark(BACKUP_MAGIC.size + 1)
        val header = ByteArray(BACKUP_MAGIC.size)
        val headerSize = bufferedInput.read(header)
        if (headerSize == BACKUP_MAGIC.size && header.contentEquals(BACKUP_MAGIC)) {
            val aead = CryptoManager.getAead()
            val dataInput = DataInputStream(bufferedInput)
            var index = 0
            while (true) {
                val encryptedSize = try {
                    dataInput.readInt()
                } catch (_: EOFException) {
                    throw IOException("Encrypted backup is truncated")
                }
                if (encryptedSize !in 1..BACKUP_MAX_ENCRYPTED_CHUNK_SIZE) {
                    throw IOException("Invalid encrypted backup chunk size")
                }
                val encrypted = ByteArray(encryptedSize)
                dataInput.readFully(encrypted)
                val decrypted = aead.decrypt(encrypted, backupAssociatedData(index))
                if (decrypted.isEmpty()) break
                output.write(decrypted)
                index++
            }
        } else {
            // Backward compatibility with the original one-shot encrypted backup format.
            bufferedInput.reset()
            output.write(CryptoManager.getAead().decrypt(bufferedInput.readBytes(), null))
        }
    }

    private fun validateSqliteFile(file: File) {
        val expectedHeader = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
        val actualHeader = file.inputStream().use { it.readNBytes(expectedHeader.size) }
        if (!actualHeader.contentEquals(expectedHeader)) {
            throw IOException("Decrypted backup is not a valid SQLite database")
        }
        val integrityOk = SQLiteDatabase.openDatabase(
            file.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY
        ).use { database ->
            database.rawQuery("PRAGMA quick_check", null).use { cursor ->
                cursor.moveToFirst() && cursor.getString(0).equals("ok", ignoreCase = true)
            }
        }
        if (!integrityOk) throw IOException("SQLite backup integrity check failed")
    }

    private fun backupAssociatedData(index: Int): ByteArray =
        "upi-tracker-backup-v2:$index".toByteArray(Charsets.UTF_8)

    private companion object {
        const val ANALYTICS_TRANSACTION_LIMIT = 5_000
        val BACKUP_MAGIC = "UPITRK2\n".toByteArray(Charsets.US_ASCII)
        const val BACKUP_CHUNK_SIZE = 1024 * 1024
        const val BACKUP_MAX_ENCRYPTED_CHUNK_SIZE = BACKUP_CHUNK_SIZE + 4096
    }

    val appTheme: StateFlow<AppTheme> = ThemePreference.getAppThemeFlow(application)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppTheme.DEFAULT)

    val isTransactionAlertsEnabled: StateFlow<Boolean> = ThemePreference.isTransactionAlertsEnabledFlow(application)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isNotificationActionsEnabled: StateFlow<Boolean> = ThemePreference.isNotificationActionsEnabledFlow(application)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isNotificationContentRedacted: StateFlow<Boolean> = ThemePreference.isNotificationContentRedactedFlow(application)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isWidgetAmountHidden: StateFlow<Boolean> = ThemePreference.isWidgetAmountHiddenFlow(application)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val autoLockDelay: StateFlow<AutoLockDelay> = ThemePreference.getAutoLockDelayFlow(application)
        .stateIn(viewModelScope, SharingStarted.Eagerly, AutoLockDelay.IMMEDIATE)

    val isOnboardingCompleted: StateFlow<Boolean> =
        OnboardingPreference.isOnboardingCompletedFlow(application)
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val archivedUpiTransactions: Flow<PagingData<Transaction>> = Pager(
        PagingConfig(pageSize = 50, initialLoadSize = 100, enablePlaceholders = false)
    ) { transactionDao.getArchivedTransactionsPaged() }
        .flow
        .cachedIn(viewModelScope)

    val archivedTransactionCount: StateFlow<Int> = transactionDao.getArchivedTransactionCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val incomeVsExpenseData: StateFlow<List<IncomeExpensePoint>> =
        combine(_transactions, _upiLiteSummaries, _nonRefundDebits, _selectedGraphPeriod, isUpiLiteEnabled) { allTrans, summaries, nonRefundDebits, period, upiLiteEnabled ->
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

                val income = incomeByMonth[yearMonthKey]?.sumOf { it.amountPaise }?.toMajorUnits() ?: 0.0
                val regularExpense = expenseByMonth[yearMonthKey]?.sumOf { it.amountPaise }?.toMajorUnits() ?: 0.0

                // ✅ STEP 2: Only include liteExpense if the feature is enabled
                val liteExpense = if (upiLiteEnabled) {
                    summariesByMonth[yearMonthKey]?.sumOf { it.totalAmountPaise }?.toMajorUnits() ?: 0.0
                } else {
                    0.0
                }

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

    @OptIn(ExperimentalCoroutinesApi::class)
    val budgetStatuses: StateFlow<List<BudgetStatus>> =
        combine(_budgets, transactionDao.observeTransactionCount(), refundKeyword) { budgets, _, refund -> budgets to refund }
        .mapLatest { (budgets, refund) ->
            budgets.map { budget ->
                val rolloverAmount = if (budget.allowRollover) {
                    val (prevPeriodStart, prevPeriodEnd) = when (budget.periodType) {
                        BudgetPeriod.WEEKLY -> getPreviousWeekRange()
                        BudgetPeriod.MONTHLY -> getPreviousMonthRange()
                        BudgetPeriod.YEARLY -> getPreviousYearRange()
                    }
                    val spentInPrevPeriod = transactionDao.getSpentAmountPaiseForCategoryInRangeSync(
                        budget.categoryName, prevPeriodStart, prevPeriodEnd, refund
                    )?.toMajorUnits() ?: 0.0
                    budget.budgetAmount - spentInPrevPeriod
                } else {
                    0.0
                }

                val (currentPeriodStart, currentPeriodEnd) = when (budget.periodType) {
                    BudgetPeriod.WEEKLY -> getCurrentWeekRange()
                    BudgetPeriod.MONTHLY -> getCurrentMonthDateRange()
                    BudgetPeriod.YEARLY -> getCurrentYearRange()
                }

                val spentInCurrentPeriod = transactionDao.getSpentAmountPaiseForCategoryInRangeSync(
                    budget.categoryName, currentPeriodStart, currentPeriodEnd, refund
                )?.toMajorUnits() ?: 0.0

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
        }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isExportingCsv = MutableStateFlow(false)
    val isExportingCsv: StateFlow<Boolean> = _isExportingCsv.asStateFlow()

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

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagedUpiTransactions: Flow<PagingData<Transaction>> =
        combine(
            filters,
            _upiTransactionSortField,
            _upiTransactionSortOrder
        ) { currentFilters, sortField, sortOrder ->
            Pair(currentFilters, Pair(sortField, sortOrder))
        }.flatMapLatest { (currentFilters, sortInfo) ->
            val (sortField, sortOrder) = sortInfo
            val (whereClause, args) = buildTransactionWhereClause(currentFilters)
            val queryBuilder = StringBuilder("SELECT * FROM transactions$whereClause")
            val sortColumn = when (sortField) {
                SortableTransactionField.DATE -> "date"
                SortableTransactionField.AMOUNT -> "amount"
                SortableTransactionField.CATEGORY -> "CASE WHEN category IS NULL THEN 1 ELSE 0 END, category"
            }
            val sortDirection = if (sortOrder == SortOrder.ASCENDING) "ASC" else "DESC"
            queryBuilder.append(" ORDER BY $sortColumn $sortDirection, id $sortDirection")
            val sqliteQuery = SimpleSQLiteQuery(queryBuilder.toString(), args.toTypedArray())
            Pager(
                config = PagingConfig(
                    pageSize = 50,
                    initialLoadSize = 100,
                    prefetchDistance = 10,
                    enablePlaceholders = false
                ),
                pagingSourceFactory = { transactionDao.getFilteredTransactionsPaged(sqliteQuery) }
            ).flow
        }.cachedIn(viewModelScope)

    private fun buildTransactionWhereClause(filters: TransactionFilters): Pair<String, MutableList<Any>> {
        val where = StringBuilder(" WHERE isArchived = 0 AND pendingDeletionTimestamp IS NULL")
        val args = mutableListOf<Any>()
        if (filters.type != UpiTransactionTypeFilter.ALL) {
            where.append(" AND type = ?")
            args += filters.type.name
        }
        filters.startDate?.let { where.append(" AND date >= ?"); args += it }
        filters.endDate?.let { where.append(" AND date <= ?"); args += it }
        if (filters.searchQuery.isNotBlank()) {
            where.append(" AND (description LIKE ? OR senderOrReceiver LIKE ? OR category LIKE ? OR tags LIKE ?)")
            val query = "%${filters.searchQuery}%"
            repeat(4) { args += query }
        }
        if (filters.showUncategorized) where.append(" AND (category IS NULL OR category = '')")
        if (filters.showOnlyLinked) where.append(" AND linkedTransactionId IS NOT NULL")
        if (filters.selectedCategories.isNotEmpty()) {
            where.append(" AND category IN (${filters.selectedCategories.joinToString { "?" }})")
            args.addAll(filters.selectedCategories)
        }
        filters.bankNameFilter?.let { where.append(" AND bankName = ?"); args += it }
        when (filters.amountType) {
            AmountFilterType.GREATER_THAN -> filters.amountValue1?.let { where.append(" AND amount > ?"); args += it.toPaise() }
            AmountFilterType.LESS_THAN -> filters.amountValue1?.let { where.append(" AND amount < ?"); args += it.toPaise() }
            AmountFilterType.RANGE -> if (filters.amountValue1 != null && filters.amountValue2 != null) {
                where.append(" AND amount BETWEEN ? AND ?")
                args += filters.amountValue1.toPaise()
                args += filters.amountValue2.toPaise()
            }
            AmountFilterType.ALL -> Unit
        }
        return where.toString() to args
    }


    fun toggleShowOnlyLinked(isEnabled: Boolean) {
        _showOnlyLinked.value = isEnabled
    }

    val filteredUpiLiteSummaries: StateFlow<List<UpiLiteSummary>> =
        combine(
            _upiLiteSummaries,
            _selectedDateRangeStart,
            _selectedDateRangeEnd,
            _upiLiteSummarySortField,
            _upiLiteSummarySortOrder
        ) { summaries, startDate, endDate, sortField, sortOrder ->
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
            }
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


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

            // Add debit transactions
            val debitTransactions = transactions.filter {
                it.type.equals("DEBIT", ignoreCase = true)
                        && !it.category.equals(refundCategory, ignoreCase = true)
                        && it.date in monthStart..monthEnd
            }
            combinedItems.addAll(debitTransactions.map { TransactionHistoryItem(it) })

            // Add lite summaries only if enabled
            if (upiLiteEnabled) {
                val liteSummaries = summaries.filter { it.date in monthStart..monthEnd }
                combinedItems.addAll(liteSummaries.map { SummaryHistoryItem(it) })
            }
            // ✅ FIX: Use the result of distinctBy and sort once at the end
            combinedItems.distinctBy { item ->
                when (item) {
                    is TransactionHistoryItem -> "txn-${item.transaction.id}"
                    is SummaryHistoryItem -> "summary-${item.summary.id}"
                }
            }.sortedByDescending { it.displayDate }
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
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private fun getPreviousMonthDateRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -1)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val monthStartTimestamp = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val monthEndTimestamp = calendar.timeInMillis
        return Pair(monthStartTimestamp, monthEndTimestamp)
    }

    val previousMonthTotalExpenses: StateFlow<Double> = combine(
        _nonRefundDebits,
        _upiLiteSummaries,
        isUpiLiteEnabled
    ) { transactions, summaries, upiLiteEnabled ->
        val (monthStart, monthEnd) = getPreviousMonthDateRange()
        var total = 0.0

        val debitTransactions = transactions.filter {
            it.type.equals("DEBIT", ignoreCase = true)
                    && !it.category.equals(refundCategory, ignoreCase = true)
                    && it.date in monthStart..monthEnd
        }
        total += debitTransactions.sumOf { it.amountPaise }.toMajorUnits()

        if (upiLiteEnabled) {
            val uLite = summaries.filter { it.date in monthStart..monthEnd }
            total += uLite.sumOf { it.totalAmountPaise }.toMajorUnits()
        }

        total
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val spendingVelocityState: StateFlow<VelocityState> = combine(
        _budgets,
        currentMonthTotalExpenses // This flow already exists in your ViewModel
    ) { budgets, spent ->
        val totalBudget = budgets.sumOf { it.budgetAmountPaise }.toMajorUnits()

        val calendar = Calendar.getInstance()
        val lastDayOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val daysRemaining = (lastDayOfMonth - currentDay).coerceAtLeast(1) // Avoid divide by zero

        VelocityState(totalBudget, spent, daysRemaining)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VelocityState())

    val lastNMonthsExpenses: StateFlow<List<MonthlyExpense>> =
        combine(
            _nonRefundDebits,
            _upiLiteSummaries,      // ✨ ADDED: UPI Lite summaries
            isUpiLiteEnabled,       // ✨ ADDED: The toggle to check if they should be included
            _selectedGraphPeriod
        ) { nonRefundDebits, summaries, upiLiteEnabled, period ->
            if (nonRefundDebits.isEmpty() && (!upiLiteEnabled || summaries.isEmpty())) {
                return@combine emptyList()
            }

            val monthDisplayFormat = SimpleDateFormat("MMM yy", Locale.getDefault())
            val yearMonthKeyFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())

            // Group both sources of expenses by month
            val regularExpensesByMonth = nonRefundDebits
                .groupBy { yearMonthKeyFormat.format(Date(it.date)) }
                .mapValues { entry -> entry.value.sumOf { it.amountPaise }.toMajorUnits() }

            val liteExpensesByMonth = if (upiLiteEnabled) {
                summaries
                    .groupBy { yearMonthKeyFormat.format(Date(it.date)) }
                    .mapValues { entry -> entry.value.sumOf { it.totalAmountPaise }.toMajorUnits() }
            } else {
                emptyMap()
            }

            val monthlyExpensesData = mutableListOf<MonthlyExpense>()
            val calendar = Calendar.getInstance()

            // Iterate through the last N months and build the final data
            for (i in 0 until period.months) {
                val targetCalendar = Calendar.getInstance().apply { time = calendar.time; add(Calendar.MONTH, -i) }
                val yearMonthKey = yearMonthKeyFormat.format(targetCalendar.time)
                val displayLabel = monthDisplayFormat.format(targetCalendar.time)

                targetCalendar.set(Calendar.DAY_OF_MONTH, 1)
                targetCalendar.set(Calendar.HOUR_OF_DAY, 0)
                targetCalendar.set(Calendar.MINUTE, 0)
                targetCalendar.set(Calendar.SECOND, 0)
                targetCalendar.set(Calendar.MILLISECOND, 0)
                val monthStartTimestamp = targetCalendar.timeInMillis

                // Sum the expenses from both maps for the current month
                val totalAmountForMonth = (regularExpensesByMonth[yearMonthKey] ?: 0.0) + (liteExpensesByMonth[yearMonthKey] ?: 0.0)

                monthlyExpensesData.add(
                    MonthlyExpense(
                        yearMonth = displayLabel,
                        totalAmount = totalAmountForMonth,
                        timestamp = monthStartTimestamp
                    )
                )
            }
            monthlyExpensesData.reversed() // Sort from oldest to newest for the chart
        }
            .flowOn(Dispatchers.Default) // Keep the work on a background thread
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    val dailyExpensesTrend: StateFlow<List<DailyExpensePoint>> =
        combine(
            _nonRefundDebits,
            _upiLiteSummaries,
            isUpiLiteEnabled
        ) { allTransactions, allSummaries, upiLiteEnabled ->
            val (rangeStart, rangeEnd) = getDailyTrendDateRange(7)
            val dayLabelFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
            val calendar = Calendar.getInstance()

            // 1. Group regular debit transactions by day
            val regularExpensesByDay = allTransactions
                .filter { it.date in rangeStart..rangeEnd }
                .groupBy { transaction ->
                    calendar.timeInMillis = transaction.date
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    calendar.timeInMillis
                }
                .mapValues { entry -> entry.value.sumOf { it.amountPaise }.toMajorUnits() }

            // 2. Group UPI Lite summaries by day (if enabled)
            val liteExpensesByDay = if (upiLiteEnabled) {
                allSummaries
                    .filter { it.date in rangeStart..rangeEnd }
                    .groupBy { summary ->
                        // The summary date is already at the start of the day
                        summary.date
                    }
                    .mapValues { entry -> entry.value.sumOf { it.totalAmountPaise }.toMajorUnits() }
            } else {
                emptyMap()
            }

            // 3. Build the final list of daily points
            val trendData = mutableListOf<DailyExpensePoint>()
            val currentDayCal = Calendar.getInstance().apply { timeInMillis = rangeStart }
            while (currentDayCal.timeInMillis <= rangeEnd) {
                val dayTimestamp = currentDayCal.timeInMillis

                // 4. Sum expenses from both sources for the current day
                val totalAmountForDay = (regularExpensesByDay[dayTimestamp] ?: 0.0) + (liteExpensesByDay[dayTimestamp] ?: 0.0)

                trendData.add(
                    DailyExpensePoint(
                        dayTimestamp = dayTimestamp,
                        totalAmount = totalAmountForDay,
                        dayLabel = dayLabelFormat.format(Date(dayTimestamp))
                    )
                )
                currentDayCal.add(Calendar.DAY_OF_YEAR, 1)
            }
            trendData
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    // Add this new public function to the ViewModel
    fun toggleCategoryFilter(categoryName: String) {
        val currentFilter = _categoryFilter.value.toMutableSet()
        if (currentFilter.contains(categoryName)) {
            currentFilter.remove(categoryName)
        } else {
            currentFilter.add(categoryName)
        }
        _categoryFilter.value = currentFilter
    }

    /** Opens history for exactly one category without carrying stale history filters. */
    fun filterHistoryByCategory(categoryName: String) {
        _selectedUpiTransactionType.value = UpiTransactionTypeFilter.ALL
        _selectedDateRangeStart.value = null
        _selectedDateRangeEnd.value = null
        _searchQuery.value = ""
        _showUncategorized.value = false
        _showOnlyLinked.value = false
        _amountFilterType.value = AmountFilterType.ALL
        _amountFilterValue1.value = null
        _amountFilterValue2.value = null
        _bankFilter.value = null
        _categoryFilter.value = setOf(categoryName)
    }

    // Add this function to clear the filter
    fun clearCategoryFilter() {
        _categoryFilter.value = emptySet()
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
            _nonRefundDebits,
            _selectedDateRangeStart,
            _selectedDateRangeEnd
        ) { nonRefundDebits, startDate, endDate ->

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

            filteredForDate
                .filter { !it.category.isNullOrBlank() }
                .groupBy { it.category!! }
                .map { (categoryName, transactionsInCategory) ->
                    CategoryExpense(
                        categoryName = categoryName,
                        totalAmount = transactionsInCategory.sumOf { it.amountPaise }.toMajorUnits()
                    )
                }
                .sortedByDescending { it.totalAmount }
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())




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

    // ✨ FIX: Removed the unused 'startFrom' parameter for a clean, clock-anchored signature
    private fun calculateNextDueDate(dayOfPeriod: Int, periodType: BudgetPeriod): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis() // Firm anchor on live execution time
        }

        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        when (periodType) {
            BudgetPeriod.MONTHLY -> {
                if (currentDay >= dayOfPeriod) {
                    calendar.add(Calendar.MONTH, 1)
                }
                val maxDayInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfPeriod.coerceAtMost(maxDayInMonth))
            }
            BudgetPeriod.WEEKLY -> {
                calendar.add(Calendar.WEEK_OF_YEAR, 1)
            }
            BudgetPeriod.YEARLY -> {
                if (calendar.get(Calendar.DAY_OF_YEAR) >= dayOfPeriod) {
                    calendar.add(Calendar.YEAR, 1)
                }
            }
        }

        calendar.set(Calendar.HOUR_OF_DAY, 9)
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
            // ✨ FIX: Matching the clean signature pass
            val firstDueDate = calculateNextDueDate(dayOfPeriod, period)

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

    // ✨ NEW METHOD: Updates the global progress frame data fields during asynchronous batch executions
    fun updateSmsSyncProgress(current: Int, total: Int, isInitial: Boolean) {
        _smsSyncProgress.value = SmsSyncProgressState(
            isSyncing = current < total,
            currentProgress = current,
            totalMessages = total,
            isInitialImport = isInitial
        )
    }

    // ✨ NEW METHOD: Safely terminates the syncing animation sequence state parameters once database passes finish writing
    fun clearSmsSyncProgress() {
        _smsSyncProgress.value = SmsSyncProgressState(isSyncing = false, currentProgress = 0, totalMessages = 0)
        _isImportingSms.value = false
        _isRefreshingSmsArchive.value = false
    }

    fun backfillBankNames() {
        viewModelScope.launch(Dispatchers.IO) {
            postPlainSnackbarMessage("Starting bank name sync...")
            var updatedCount = 0
            var offset = 0
            val pageSize = 250
            do {
                val page = transactionDao.getTransactionsPage(pageSize, offset)
                page.filter { it.bankName.isNullOrBlank() }.forEach { transaction ->
                    archivedSmsDao.findSenderNear(transaction.date)?.let { sender ->
                        transactionDao.update(transaction.copy(bankName = BankIdentifier.getBankName(sender)))
                        updatedCount++
                    }
                }
                offset += page.size
            } while (page.size == pageSize)
            postPlainSnackbarMessage("Sync complete. Updated $updatedCount transactions.")
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

            // ✨ FIX: Strip out the dead calendar instantiations and pass exactly 2 parameters
            val nextDueDate = if (originalRule.dayOfPeriod != newDay || originalRule.periodType != newPeriod) {
                calculateNextDueDate(newDay, newPeriod)
            } else {
                originalRule.nextDueDate
            }

            val updatedRule = originalRule.copy(
                description = newDescription,
                amountPaise = newAmount.toPaise(),
                categoryName = newCategory,
                periodType = newPeriod,
                dayOfPeriod = newDay,
                nextDueDate = nextDueDate
            )
            recurringRuleDao.update(updatedRule)
            postPlainSnackbarMessage("Recurring rule updated.")
        }
    }

    fun addOrUpdateBudget(categoryName: String, amount: Double, periodType: BudgetPeriod, allowRollover: Boolean, budgetId: Int? = null) {
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
                    allowRollover = allowRollover
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
            _upiLiteSummarySortOrder.value = SortOrder.DESCENDING
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
            _upiTransactionSortOrder.value = when (_upiTransactionSortOrder.value) {
                SortOrder.ASCENDING -> SortOrder.DESCENDING
                SortOrder.DESCENDING -> SortOrder.ASCENDING
            }
        } else {
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

    fun setTransactionAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            ThemePreference.setTransactionAlertsEnabled(getApplication(), enabled)
        }
    }

    fun setNotificationActionsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            ThemePreference.setNotificationActionsEnabled(getApplication(), enabled)
        }
    }

    fun setNotificationContentRedacted(enabled: Boolean) {
        viewModelScope.launch { ThemePreference.setNotificationContentRedacted(getApplication(), enabled) }
    }

    fun setWidgetAmountHidden(enabled: Boolean) {
        viewModelScope.launch {
            ThemePreference.setWidgetAmountHidden(getApplication(), enabled)
            SmsProcessingService.updateWidgets(getApplication())
        }
    }

    fun setAutoLockDelay(delay: AutoLockDelay) {
        viewModelScope.launch { ThemePreference.setAutoLockDelay(getApplication(), delay) }
    }

    fun onFilterSheetDismiss() {
        _showHistoryFilterSheet.value = false
    }

    fun permanentlyDeleteTransaction(transaction: Transaction) {
        viewModelScope.launch(Dispatchers.IO) {
            transactionDao.delete(transaction)
            SmsProcessingService.updateWidgets(getApplication())
        }
    }

    val groupedUpiLiteSummaries: StateFlow<List<GroupedUpiLiteSummaries>> = filteredUpiLiteSummaries
        .map { summaries ->
            val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
            summaries
                .groupBy { summary ->
                    Instant.ofEpochMilli(summary.date)
                        .atZone(ZoneId.systemDefault())
                        .format(formatter)
                }
                .map { (monthYear, summariesInMonth) ->
                    GroupedUpiLiteSummaries(
                        monthYear = monthYear,
                        summaries = summariesInMonth,
                        monthlyTotal = summariesInMonth.sumOf { it.totalAmountPaise }.toMajorUnits(),
                        count = summariesInMonth.sumOf { it.transactionCount }
                    )
                }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())


    private suspend fun findAndLinkRefund(refund: Transaction) {
        val potentialMatches = transactionDao.findPotentialDebitsForRefund(refund.senderOrReceiver)

        val refundPaise = refund.amountPaise
        val bestMatch = potentialMatches
            .filter { it.amountPaise == refundPaise && it.date <= refund.date }
            .minByOrNull { refund.date - it.date }

        if (bestMatch != null) {
            val linkedRefund = refund.copy(linkedTransactionId = bestMatch.id)
            val linkedDebit = bestMatch.copy(linkedTransactionId = refund.id)

            transactionDao.update(linkedRefund)
            transactionDao.update(linkedDebit)

            postPlainSnackbarMessage("Successfully linked refund to a previous purchase.")
        } else {
            postPlainSnackbarMessage("Refund categorized, but no matching purchase was found to link.")
        }
    }

    fun saveReceiptImage(uri: Uri): String? {
        return try {
            val context = getApplication<Application>().applicationContext
            val fileName = "receipt_${System.currentTimeMillis()}.jpg"
            val file = File(context.filesDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(file).use { outputStream -> inputStream.copyTo(outputStream) }
            } ?: throw IOException("Unable to open receipt image")
            file.absolutePath // Return the absolute path to the saved file
        } catch (e: Exception) {
            Log.e("ImageSave", "Failed to save receipt image", e)
            null
        }
    }


    fun updateTransactionDetails(
        transactionId: Int,
        newDescription: String,
        newAmount: Double,
        newCategory: String?,
        newNote: String, // Changed to non-nullable
        newReceiptPath: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val originalTransaction = transactionDao.getTransactionByIdSync(transactionId)

            originalTransaction?.let { it ->
                val isManualEntry = it.senderOrReceiver == "Manual Entry"

                val updatedTransaction = if (isManualEntry) {
                    it.copy(
                        description = newDescription.trim(),
                        amountPaise = newAmount.toPaise(),
                        category = newCategory?.trim().takeIf { cat -> cat?.isNotBlank() == true },
                        note = newNote.trim(),
                        receiptImagePath = newReceiptPath ?: it.receiptImagePath
                    )
                } else {
                    it.copy(
                        category = newCategory?.trim().takeIf { cat -> cat?.isNotBlank() == true },
                        note = newNote.trim(),
                        receiptImagePath = newReceiptPath ?: it.receiptImagePath
                    )
                }
                transactionDao.update(updatedTransaction)
                if (newReceiptPath != null && it.receiptImagePath != null && it.receiptImagePath != newReceiptPath) {
                    deleteManagedReceiptFile(it.receiptImagePath)
                }

                newCategory?.trim()?.takeIf { it.isNotBlank() }?.let { catName ->
                    if (categoryDao.getCategoryByName(catName) == null) {
                        categoryDao.insert(
                            Category(
                                name = catName,
                                iconName = "Category",
                                colorHex = "#808080"
                            )
                        )
                    }
                    checkAndRecommendAutoRule(updatedTransaction.senderOrReceiver, catName)
                }

                val refundKeywordValue = refundKeyword.first()
                if (updatedTransaction.type == "CREDIT" &&
                    newCategory?.equals(refundKeywordValue, ignoreCase = true) == true &&
                    !originalTransaction.category.equals(refundKeywordValue, ignoreCase = true)
                ) {
                    findAndLinkRefund(updatedTransaction)
                }
                else if (originalTransaction.category.equals(refundKeywordValue, ignoreCase = true) &&
                    !newCategory.equals(refundKeywordValue, ignoreCase = true) &&
                    originalTransaction.linkedTransactionId != null)
                {
                    transactionDao.unlinkTransaction(originalTransaction.id)
                    transactionDao.unlinkTransaction(originalTransaction.linkedTransactionId)
                    postPlainSnackbarMessage("Unlinked refund from purchase.")
                }

                postPlainSnackbarMessage("Transaction updated successfully!")
            }
        }
    }

    suspend fun processAndInsertTransactionSync(transaction: Transaction): Boolean {
        return withContext(Dispatchers.IO) {
            val rules = categorySuggestionRules.first()

            val finalTransaction = SmsProcessingService.prepareTransactionForInsert(
                transaction = transaction,
                rules = rules
            )

            val insertedId = transactionDao.insertIfNotDuplicate(finalTransaction)

            if (insertedId == -1L) {
                Log.d("DuplicateTxnFix", "Skipped duplicate transaction")
                return@withContext false
            }

            val savedTransaction = finalTransaction.copy(id = insertedId.toInt())
            checkBudgetForNewTransaction(savedTransaction)
            SmsProcessingService.updateWidgets(getApplication())

            true
        }
    }

    fun processAndInsertTransaction(transaction: Transaction) {
        viewModelScope.launch {
            processAndInsertTransactionSync(transaction)
        }
    }


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
                    transactionDao.update(updatedTransaction)

                    postPlainSnackbarMessage("Transaction categorized as '${bestMatch.categoryName}'!")

                    val refundKeywordValue = refundKeyword.first()
                    if (updatedTransaction.type == "CREDIT" && updatedTransaction.category.equals(refundKeywordValue, ignoreCase = true)) {
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
        priority: Int,
        logic: RuleLogic
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (keyword.isNotBlank() && category.isNotBlank()) {
                val newRule = CategorySuggestionRule(
                    fieldToMatch = field,
                    matcher = matcher,
                    keyword = keyword,
                    categoryName = category,
                    priority = priority,
                    logic = logic
                )
                categorySuggestionRuleDao.insert(newRule)
                val affectedIds = matchingRuleTransactionIds(field, matcher, keyword, logic)
                if (affectedIds.isNotEmpty()) {
                    categorySuggestionRuleDao.categorizeTransactions(affectedIds, category)
                    _lastRuleApplication.value = RuleApplicationUndo(affectedIds, category)
                }
                postPlainSnackbarMessage("Rule saved. ${affectedIds.size} existing transactions updated.")
            }
        }
    }

    data class RuleApplicationUndo(val transactionIds: List<Int>, val category: String)
    private val _lastRuleApplication = MutableStateFlow<RuleApplicationUndo?>(null)
    val lastRuleApplication: StateFlow<RuleApplicationUndo?> = _lastRuleApplication.asStateFlow()

    suspend fun previewRuleMatchingCount(
        field: RuleField,
        matcher: RuleMatcher,
        keyword: String,
        logic: RuleLogic
    ): Int = matchingRuleTransactionIds(field, matcher, keyword, logic).size

    private suspend fun matchingRuleTransactionIds(
        field: RuleField,
        matcher: RuleMatcher,
        keyword: String,
        logic: RuleLogic
    ): List<Int> {
        val terms = keyword.split(',').map(String::trim).filter(String::isNotBlank)
        if (terms.isEmpty()) return emptyList()
        val matches = terms.map { term ->
            categorySuggestionRuleDao.findMatchingTransactionIds(field.name, matcher.name, term).toSet()
        }
        return when (logic) {
            RuleLogic.ANY -> matches.flatten().distinct()
            RuleLogic.ALL -> matches.drop(1).fold(matches.first()) { result, ids -> result intersect ids }.toList()
        }
    }

    fun undoLastRuleApplication() {
        val undo = _lastRuleApplication.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            categorySuggestionRuleDao.undoCategorization(undo.transactionIds, undo.category)
            _lastRuleApplication.value = null
            postPlainSnackbarMessage("Rule application undone for ${undo.transactionIds.size} transactions.")
        }
    }

    fun deleteCategoryRule(rule: CategorySuggestionRule) {
        viewModelScope.launch(Dispatchers.IO) {
            categorySuggestionRuleDao.delete(rule)
            postPlainSnackbarMessage("Rule deleted.")
        }
    }

    // ✨✨✨ START: NEW CATEGORY MANAGEMENT FUNCTIONS ✨✨✨
    fun addCategory(name: String, iconName: String, colorHex: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val trimmedName = name.trim()
            if (trimmedName.isBlank()) {
                postPlainSnackbarMessage("Category name cannot be empty.")
                return@launch
            }
            // Check for duplicates
            if (categoryDao.getCategoryByName(trimmedName) != null) {
                postPlainSnackbarMessage("Category '$trimmedName' already exists.")
                return@launch
            }

            val newCategory = Category(name = trimmedName, iconName = iconName, colorHex = colorHex)
            categoryDao.insert(newCategory)
            postPlainSnackbarMessage("Category '$trimmedName' added.")
        }
    }

    fun updateCategory(category: Category, newName: String, newIconName: String, newColorHex: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val trimmedName = newName.trim()
            if (trimmedName.isBlank()) {
                postPlainSnackbarMessage("Category name cannot be empty.")
                return@launch
            }

            // If name is changed, check if the new name conflicts with another existing category
            val oldName = category.name
            if (!category.name.equals(trimmedName, ignoreCase = true)) {
                if (categoryDao.getCategoryByName(trimmedName) != null) {
                    postPlainSnackbarMessage("Category '$trimmedName' already exists.")
                    return@launch
                }
                // Also update the name in all associated transactions
                transactionDao.updateCategoryName(category.name, trimmedName)
                categorySuggestionRuleDao.updateCategoryNameInRules(oldName, trimmedName)
            }

            val updatedCategory = category.copy(
                name = trimmedName,
                iconName = newIconName,
                colorHex = newColorHex
            )
            categoryDao.update(updatedCategory)
            postPlainSnackbarMessage("Category updated.")
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch(Dispatchers.IO) {
            categorySuggestionRuleDao.deleteRulesForCategory(category.name)
            transactionDao.clearCategoryForTransactions(category.name)
            categoryDao.deleteCategoryAndCleanup(category)
            postPlainSnackbarMessage("Category '${category.name}' deleted.")
        }
    }

    fun setIsRefreshingSmsArchive(isRefreshing: Boolean) {
        _isRefreshingSmsArchive.value = isRefreshing
        if (isRefreshing) {
            _isImportingSms.value = false
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            ThemePreference.setDarkMode(getApplication(), enabled)
        }
    }

    fun toggleSelection(transactionId: Int) {
        val currentSelection = _selectedTransactionIds.value.toMutableSet()
        if (currentSelection.contains(transactionId)) {
            currentSelection.remove(transactionId)
        } else {
            currentSelection.add(transactionId)
        }
        _selectedTransactionIds.value = currentSelection

        // ✨ This is the fix: If the user deselects the last item, exit selection mode
        if (currentSelection.isEmpty()) {
            _isSelectionModeActive.value = false
        } else {
            // Ensure we are in selection mode if any item is selected
            _isSelectionModeActive.value = true
        }
    }

    fun clearSelection() {
        _selectedTransactionIds.value = emptySet()
        _isSelectionModeActive.value = false
    }

    fun categorizeSelectedTransactions(categoryName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val selectedIds = _selectedTransactionIds.value
            if (selectedIds.isEmpty()) return@launch

            // Fetch all transactions and find the ones that are selected
            val transactionsToUpdate = selectedIds.mapNotNull { transactionDao.getTransactionByIdSync(it) }

            transactionsToUpdate.forEach { transaction ->
                // Update each transaction with the new category
                val updatedTransaction = transaction.copy(category = categoryName)
                transactionDao.update(updatedTransaction)
            }

            // Post a message and clear the selection
            postPlainSnackbarMessage("${selectedIds.size} transactions categorized as '$categoryName'.")
            withContext(Dispatchers.Main) {
                clearSelection()
            }

            // Check for potential auto-rules for the merchants in this batch
            transactionsToUpdate.map { it.senderOrReceiver }.distinct().forEach { merchant ->
                checkAndRecommendAutoRule(merchant, categoryName)
            }
        }
    }

    fun createAutoRule(merchant: String, category: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val rule = CategorySuggestionRule(
                fieldToMatch = RuleField.SENDER_OR_RECEIVER,
                matcher = RuleMatcher.CONTAINS,
                keyword = merchant,
                categoryName = category,
                priority = 1,
                logic = RuleLogic.ANY
            )
            categorySuggestionRuleDao.insertRuleAndApplyRetroactively(rule)
            postPlainSnackbarMessage("Auto-rule created for '$merchant'!")
        }
    }

    fun linkTransactions(debitId: Int, creditId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val debitTxn = transactionDao.getTransactionByIdSync(debitId)
            if (debitTxn != null) {
                val updated = debitTxn.copy(linkedTransactionId = creditId)
                transactionDao.update(updated)
                postPlainSnackbarMessage("Refund linked successfully!")
            }
        }
    }

    fun unlinkRefund(transactionId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val debitTxn = transactionDao.getTransactionByIdSync(transactionId)
            if (debitTxn != null && debitTxn.linkedTransactionId != null) {
                val updated = debitTxn.copy(linkedTransactionId = null)
                transactionDao.update(updated)
                postPlainSnackbarMessage("Refund unlinked.")
                return@launch
            }
            val debitPointingToCredit = transactionDao.getTransactionLinkedTo(transactionId)
            if (debitPointingToCredit != null) {
                val updated = debitPointingToCredit.copy(linkedTransactionId = null)
                transactionDao.update(updated)
                postPlainSnackbarMessage("Refund unlinked.")
            }
        }
    }

    private suspend fun checkAndRecommendAutoRule(merchant: String, categoryName: String) {
        val cleanMerchant = merchant.trim()
        if (cleanMerchant.isBlank() || cleanMerchant == "Manual Entry" || cleanMerchant == "Recurring" || cleanMerchant == "Other Bank") return
        
        val existingRules = categorySuggestionRuleDao.getAllRules().first()
        val hasRule = existingRules.any { 
            it.fieldToMatch == RuleField.SENDER_OR_RECEIVER &&
            it.keyword.equals(cleanMerchant, ignoreCase = true) 
        }
        if (hasRule) return

        val count = transactionDao.countCategorizedMerchantTransactions(cleanMerchant, categoryName)

        if (count >= 3) {
            _uiEvents.emit(UiEvent.AutoRuleRecommendation(cleanMerchant, categoryName))
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            val transactionToMark = transaction.copy(
                pendingDeletionTimestamp = System.currentTimeMillis()
            )

            transactionDao.update(transactionToMark)
            SmsProcessingService.updateWidgets(getApplication())

            _snackbarEvents.emit(
                SnackbarMessage(
                    message = "Moved to Recycle Bin. Will be deleted in 24 hours.",
                    actionLabel = "Undo",
                    onAction = {
                        viewModelScope.launch {
                            transactionDao.update(transaction.copy(pendingDeletionTimestamp = null))
                            SmsProcessingService.updateWidgets(getApplication())
                            postPlainSnackbarMessage("Transaction restored")
                        }
                    }
                )
            )
            Log.d("DeleteBug", "Snackbar event emitted successfully.")
        }
    }

    private val _isDashboardLoading = MutableStateFlow(true)
    val isDashboardLoading: StateFlow<Boolean> = _isDashboardLoading.asStateFlow()

    fun deleteAllAppData() {
        viewModelScope.launch {
            transactionDao.deleteAll()
            upiLiteSummaryDao.deleteAll()
            budgetDao.deleteAll()
            categoryDao.deleteAll()
            AppDatabase.populateDefaultCategories(categoryDao)
            categorySuggestionRuleDao.deleteAll()
            recurringRuleDao.deleteAll()
            db.archivedSmsMessageDao().deleteAllArchivedSms()

            getApplication<Application>().filesDir.listFiles { file ->
                file.isFile && file.name.startsWith("receipt_")
            }?.forEach { it.delete() }

            ThemePreference.setLastProcessedSmsTimestamp(getApplication(), 0L)
            ThemePreference.setLastSyncExecutionTimestamp(getApplication(), 0L)

            // Update home screen widgets to show cleared totals immediately
            SmsProcessingService.updateWidgets(getApplication())
        }
    }

    private fun deleteManagedReceiptFile(path: String) {
        val file = File(path)
        if (file.parentFile == getApplication<Application>().filesDir && file.name.startsWith("receipt_")) {
            file.delete()
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

    fun enterSelectionMode() {
        _isSelectionModeActive.value = true
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
        category: String,
        date: Long
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val trimmedCategory = category.trim()
            if (trimmedCategory.isNotEmpty()) {
                if (categoryDao.getCategoryByName(trimmedCategory) == null) {
                    categoryDao.insert(
                        Category(
                            name = trimmedCategory,
                            iconName = "Category",
                            colorHex = "#808080"
                        )
                    )
                }
            }

            val extractedTags = TagUtils.extractTags(description)
            val newTransaction = Transaction(
                amount = amount,
                type = type,
                description = description.trim(),
                category = trimmedCategory.takeIf { it.isNotEmpty() },
                date = date,
                senderOrReceiver = "Manual Entry",
                isArchived = false,
                note = "",
                tags = extractedTags
            )

            processAndInsertTransaction(newTransaction)

            postPlainSnackbarMessage("Transaction saved successfully!")
            _uiEvents.emit(UiEvent.ScrollToTop)
        }
    }

    fun exportTransactionsToCsv(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            _isExportingCsv.value = true
            try {
                withContext(Dispatchers.IO) {
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                            CsvExporter.writeHeader(writer)
                            var offset = 0
                            val pageSize = 500
                            var exported = 0
                            do {
                                val page = transactionDao.getTransactionsPage(pageSize, offset)
                                page.forEach { CsvExporter.writeTransaction(writer, it) }
                                exported += page.size
                                offset += page.size
                            } while (page.size == pageSize)
                            if (exported == 0) {
                                throw NoSuchElementException("No transactions available for export")
                            }
                        }
                    } ?: throw IOException("Failed to open output stream for URI: $uri")
                }

                postSnackbarMessage(getApplication<Application>().getString(R.string.csv_export_success))
            } catch (_: NoSuchElementException) {
                postSnackbarMessage(getApplication<Application>().getString(R.string.csv_export_no_transactions))
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
                val originalCategory = transaction.category

                val archivedTransaction = transaction.copy(isArchived = true)
                transactionDao.update(archivedTransaction)
                SmsProcessingService.updateWidgets(getApplication())

                _snackbarEvents.emit(
                    SnackbarMessage(
                        message = getApplication<Application>().getString(
                            R.string.transaction_archived_snackbar,
                            transaction.description.take(20)
                        ),
                        actionLabel = getApplication<Application>().getString(R.string.snackbar_action_undo),
                        onAction = {
                            viewModelScope.launch {
                                val restoredTransaction = transaction.copy(
                                    isArchived = false,
                                    category = originalCategory
                                )
                                transactionDao.update(restoredTransaction)
                                SmsProcessingService.updateWidgets(getApplication())
                            }
                        }
                    )
                )
            } else {
                val unarchivedTransaction = transaction.copy(isArchived = false)
                transactionDao.update(unarchivedTransaction)
                SmsProcessingService.updateWidgets(getApplication())
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

    private suspend fun checkBudgetForNewTransaction(transaction: Transaction) {
        if (transaction.type != "DEBIT" || transaction.category.equals(refundKeyword.first(), ignoreCase = true)) {
            return
        }

        val activeBudgets = _budgets.first()
        val relevantBudget = activeBudgets.find { it.categoryName.equals(transaction.category, ignoreCase = true) }

        if (relevantBudget == null) {
            return
        }

        relevantBudget.let { budget ->
            val (periodStart, periodEnd) = when (budget.periodType) {
                BudgetPeriod.WEEKLY -> getCurrentWeekRange()
                BudgetPeriod.MONTHLY -> getCurrentMonthDateRange()
                BudgetPeriod.YEARLY -> getCurrentYearRange()
            }

            if (budget.lastNotificationTimestamp < periodStart) {

                // ✨ START OF THE FIX: Directly query the DAO for the most current data ✨
                val allDebitsForCategoryInPeriod = transactionDao.getTransactionsForBudgetCheck(
                    categoryName = budget.categoryName,
                    startDate = periodStart,
                    endDate = periodEnd,
                    refundCategory = refundKeyword.first()
                )
                // ✨ END OF THE FIX ✨

                val spentInCurrentPeriodPaise = allDebitsForCategoryInPeriod.sumOf { it.amountPaise }

                if (spentInCurrentPeriodPaise > budget.budgetAmountPaise) {
                    NotificationHelper.showBudgetExceededNotification(
                        context = getApplication(),
                        budget = budget,
                        spentAmount = spentInCurrentPeriodPaise.toMajorUnits()
                    )
                    val updatedBudget = budget.copy(lastNotificationTimestamp = System.currentTimeMillis())
                    budgetDao.update(updatedBudget)
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredTotals: StateFlow<FilteredTotals> = filters
        .flatMapLatest { currentFilters ->
            val (whereClause, args) = buildTransactionWhereClause(currentFilters)
            transactionDao.getFilteredTotals(
                SimpleSQLiteQuery(
                    "SELECT " +
                        "COALESCE(SUM(CASE WHEN type = 'DEBIT' THEN amount ELSE 0 END), 0) AS totalDebitPaise, " +
                        "COALESCE(SUM(CASE WHEN type = 'CREDIT' THEN amount ELSE 0 END), 0) AS totalCreditPaise " +
                        "FROM transactions$whereClause",
                    args.toTypedArray()
                )
            )
        }
        .map { totals ->
            FilteredTotals(
                totalDebitPaise = totals.totalDebitPaise,
                totalCreditPaise = totals.totalCreditPaise
            )
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, FilteredTotals())

    val latestTransactionTimestamp: StateFlow<Long> = _transactions
        .map { transactions -> transactions.maxOfOrNull { it.date } ?: 0L }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val lastSyncExecutionTimestamp: StateFlow<Long> = ThemePreference.getLastSyncExecutionTimestampFlow(application)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)


    private val _isDataReady = MutableStateFlow(false)
    val isDataReady: StateFlow<Boolean> = _isDataReady.asStateFlow()

    private val _isHistoryLoading = MutableStateFlow(true)

    private val _pinUnlocked = MutableStateFlow(false)
    val pinUnlocked: StateFlow<Boolean> = _pinUnlocked.asStateFlow()

    fun setPinUnlocked(unlocked: Boolean) {
        _pinUnlocked.value = unlocked
    }

    fun updateCategoryRule(
        ruleId: Int, // ✨ We need the ID to update the correct rule
        field: RuleField,
        matcher: RuleMatcher,
        keyword: String,
        category: String,
        priority: Int,
        logic: RuleLogic
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (keyword.isNotBlank() && category.isNotBlank()) {
                val updatedRule = CategorySuggestionRule(
                    id = ruleId, // ✨ Pass the ID here
                    fieldToMatch = field,
                    matcher = matcher,
                    keyword = keyword,
                    categoryName = category,
                    priority = priority,
                    logic = logic
                )
                categorySuggestionRuleDao.update(updatedRule)
                val affectedIds = matchingRuleTransactionIds(field, matcher, keyword, logic)
                if (affectedIds.isNotEmpty()) {
                    categorySuggestionRuleDao.categorizeTransactions(affectedIds, category)
                    _lastRuleApplication.value = RuleApplicationUndo(affectedIds, category)
                }
                postPlainSnackbarMessage("Rule updated. ${affectedIds.size} existing transactions categorized.")
            }
        }
    }

    fun clearAllHistoryFilters() {
        _selectedUpiTransactionType.value = UpiTransactionTypeFilter.ALL
        _selectedDateRangeStart.value = null
        _selectedDateRangeEnd.value = null
        _searchQuery.value = ""
        _showUncategorized.value = false
        _amountFilterType.value = AmountFilterType.ALL
        _amountFilterValue1.value = null
        _amountFilterValue2.value = null
        _showOnlyLinked.value = false
        _categoryFilter.value = emptySet()
        _bankFilter.value = null
    }

    sealed class UiEvent {
        data class RestartRequired(val message: String) : UiEvent()
        object ScrollToTop : UiEvent()
        data class AutoRuleRecommendation(val merchant: String, val category: String) : UiEvent()
    }

    init {
        // Pipeline 1: Independent, lightning-fast onboarding state evaluation
        viewModelScope.launch {
            try {
                val onboardingCompleted = OnboardingPreference.isOnboardingCompletedFlow(getApplication()).first()
                if (onboardingCompleted) {
                    PinStorage.isPinSet(getApplication())
                }
            } catch (e: Exception) {
                Log.e("MainViewModelInit", "Error checking onboarding preference state", e)
            } finally {
                _isDataReady.value = true
            }
        }

        viewModelScope.launch {
            _isHistoryLoading.value = true
            try {
                _transactions.first()
            } catch (e: Exception) {
                Log.e("MainViewModelInit", "Error waiting for primary data streams", e)
            } finally {
                _isHistoryLoading.value = false
            }
        }

        viewModelScope.launch {
            _isDashboardLoading.value = true
            try {
                _isHistoryLoading.first { !it }
            } catch (e: Exception) {
                Log.e("MainViewModelInit", "Error waiting for dashboard data streams", e)
            } finally {
                _isDashboardLoading.value = false
            }
        }
    }

}

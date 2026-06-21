package com.example.upitracker.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.upitracker.data.AppDatabase
import com.example.upitracker.data.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import java.util.Calendar
import com.example.upitracker.data.Category
import com.example.upitracker.util.PdfGenerator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.example.upitracker.util.toMajorUnits
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class PassbookTransactionType { ALL, DEBIT, CREDIT }

data class PassbookSummaryState(
    val totalDebit: Double = 0.0,
    val totalCredit: Double = 0.0,
    val netBalance: Double = 0.0
)

class PassbookViewModel(application: Application) : AndroidViewModel(application) {

    private val transactionDao = AppDatabase.getDatabase(application).transactionDao()
    private val categoryDao = AppDatabase.getDatabase(application).categoryDao()

    // Base filter state streams
    private val _startDate = MutableStateFlow<Long?>(null)
    val startDate: StateFlow<Long?> = _startDate.asStateFlow()

    private val _endDate = MutableStateFlow<Long?>(null)
    val endDate: StateFlow<Long?> = _endDate.asStateFlow()

    private val _transactionType = MutableStateFlow(PassbookTransactionType.ALL)
    val transactionType: StateFlow<PassbookTransactionType> = _transactionType.asStateFlow()

    private val _showBankName = MutableStateFlow(true)
    val showBankName: StateFlow<Boolean> = _showBankName.asStateFlow()

    private val _includeCategoryBreakdown = MutableStateFlow(true)
    val includeCategoryBreakdown: StateFlow<Boolean> = _includeCategoryBreakdown.asStateFlow()

    // 1. Core Transaction Database Stream Hook
    // 2. ✨ AIRTIGHT REACTIVE PIPELINE: Dynamically filters your transaction ledger list on the fly
    @OptIn(ExperimentalCoroutinesApi::class)
    private val passbookQuery = combine(
        _startDate,
        _endDate,
        _transactionType
    ) { start, end, type -> Triple(start, end, type) }

    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredTransactions: Flow<PagingData<Transaction>> = passbookQuery
        .flatMapLatest { (start, end, type) ->
            Pager(PagingConfig(pageSize = 50, prefetchDistance = 15, enablePlaceholders = false)) {
                transactionDao.getTransactionsForPassbookPaged(
                    startDate = start,
                    endDate = end,
                    type = type.takeUnless { it == PassbookTransactionType.ALL }?.name
                )
            }.flow
        }
        .cachedIn(viewModelScope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val passbookSummary: StateFlow<PassbookSummaryState> = passbookQuery
        .flatMapLatest { (start, end, type) ->
            transactionDao.getPassbookTotals(start, end, type.takeUnless { it == PassbookTransactionType.ALL }?.name)
        }
        .map { totals ->
            val debits = totals.totalDebitPaise.toMajorUnits()
            val credits = totals.totalCreditPaise.toMajorUnits()
            PassbookSummaryState(
                totalDebit = debits,
                totalCredit = credits,
                netBalance = credits - debits
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PassbookSummaryState())

    @OptIn(ExperimentalCoroutinesApi::class)
    val passbookTransactionCount: StateFlow<Int> = passbookQuery
        .flatMapLatest { (start, end, type) ->
            transactionDao.getPassbookCount(start, end, type.takeUnless { it == PassbookTransactionType.ALL }?.name)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allCategories: StateFlow<List<Category>> = categoryDao.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Set initialization boundary target to current calendar parameters
        setThisMonth()
    }

    fun setTransactionType(type: PassbookTransactionType) {
        _transactionType.value = type
    }

    fun setDateRange(start: Long?, end: Long?) {
        _startDate.value = start
        _endDate.value = end
    }

    fun setThisMonth() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        setDayToStart(calendar)
        val start = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        setDayToEnd(calendar)
        val end = calendar.timeInMillis
        setDateRange(start, end)
    }

    fun setThisYear() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_YEAR, 1)
        setDayToStart(calendar)
        val start = calendar.timeInMillis

        calendar.set(Calendar.MONTH, Calendar.DECEMBER)
        calendar.set(Calendar.DAY_OF_MONTH, 31)
        setDayToEnd(calendar)
        val end = calendar.timeInMillis
        setDateRange(start, end)
    }

    fun setLastMonth() {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -1)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        setDayToStart(calendar)
        val start = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        setDayToEnd(calendar)
        val end = calendar.timeInMillis
        setDateRange(start, end)
    }

    fun setFinancialYear() {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        val startYear = if (currentMonth >= Calendar.APRIL) currentYear else currentYear - 1
        val endYear = startYear + 1

        calendar.set(startYear, Calendar.APRIL, 1)
        setDayToStart(calendar)
        val start = calendar.timeInMillis

        calendar.set(endYear, Calendar.MARCH, 31)
        setDayToEnd(calendar)
        val end = calendar.timeInMillis
        setDateRange(start, end)
    }

    fun setPreviousFinancialYear() {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        val startYear = (if (currentMonth >= Calendar.APRIL) currentYear else currentYear - 1) - 1
        val endYear = startYear + 1

        calendar.set(startYear, Calendar.APRIL, 1)
        setDayToStart(calendar)
        val start = calendar.timeInMillis

        calendar.set(endYear, Calendar.MARCH, 31)
        setDayToEnd(calendar)
        val end = calendar.timeInMillis
        setDateRange(start, end)
    }

    private fun setDayToStart(calendar: Calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
    }

    private fun setDayToEnd(calendar: Calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
    }

    fun setShowBankName(show: Boolean) {
        _showBankName.value = show
    }

    fun setIncludeCategoryBreakdown(include: Boolean) {
        _includeCategoryBreakdown.value = include
    }

    suspend fun generateAndSavePdf(context: Context, uri: Uri) {
        val transactionsToExport = transactionDao.getTransactionsForPassbookSnapshot(
            startDate.value,
            endDate.value,
            transactionType.value.takeUnless { it == PassbookTransactionType.ALL }?.name
        )
        if (transactionsToExport.isEmpty()) return

        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val start = startDate.value?.let { dateFormat.format(Date(it)) } ?: "Start"
        val end = endDate.value?.let { dateFormat.format(Date(it)) } ?: "End"
        val statementPeriod = "For period: $start to $end"

        PdfGenerator.generatePassbookPdf(
            context = context,
            transactions = transactionsToExport,
            statementPeriod = statementPeriod,
            targetUri = uri,
            showBankName = _showBankName.value,
            includeCategoryBreakdown = _includeCategoryBreakdown.value
        )
    }
}

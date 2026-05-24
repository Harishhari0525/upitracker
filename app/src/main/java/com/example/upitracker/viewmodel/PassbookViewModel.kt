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

    // 1. Core Transaction Database Stream Hook
    private val _allTransactions = transactionDao.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 2. ✨ AIRTIGHT REACTIVE PIPELINE: Dynamically filters your transaction ledger list on the fly
    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        _allTransactions,
        _startDate,
        _endDate,
        _transactionType
    ) { txns, start, end, type ->
        txns.filter { txn ->
            if (txn.pendingDeletionTimestamp != null) return@filter false
            val dateMatch = (start == null || txn.date >= start) && (end == null || txn.date <= end)
            val typeMatch = when (type) {
                PassbookTransactionType.ALL -> true
                PassbookTransactionType.DEBIT -> txn.type.equals("DEBIT", ignoreCase = true)
                PassbookTransactionType.CREDIT -> txn.type.equals("CREDIT", ignoreCase = true)
            }
            dateMatch && typeMatch
        }.sortedByDescending { it.date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val passbookSummary: StateFlow<PassbookSummaryState> = filteredTransactions
        .map { txns ->
            val debits = txns.filter { it.type.equals("DEBIT", ignoreCase = true) }.sumOf { it.amount }
            val credits = txns.filter { it.type.equals("CREDIT", ignoreCase = true) }.sumOf { it.amount }
            PassbookSummaryState(
                totalDebit = debits,
                totalCredit = credits,
                netBalance = credits - debits
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PassbookSummaryState())

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

    fun generateAndSavePdf(context: Context, uri: Uri) {
        val transactionsToExport = filteredTransactions.value
        if (transactionsToExport.isEmpty()) return

        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val start = startDate.value?.let { dateFormat.format(Date(it)) } ?: "Start"
        val end = endDate.value?.let { dateFormat.format(Date(it)) } ?: "End"
        val statementPeriod = "For period: $start to $end"

        PdfGenerator.generatePassbookPdf(
            context = context,
            transactions = transactionsToExport,
            statementPeriod = statementPeriod,
            targetUri = uri
        )
    }
}
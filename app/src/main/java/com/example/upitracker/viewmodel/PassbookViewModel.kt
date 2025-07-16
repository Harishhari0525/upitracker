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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import com.example.upitracker.data.Category
import com.example.upitracker.util.PdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class PassbookTransactionType { ALL, DEBIT, CREDIT }

class PassbookViewModel(application: Application) : AndroidViewModel(application) {

    private val transactionDao = AppDatabase.getDatabase(application).transactionDao()
    private val categoryDao = AppDatabase.getDatabase(application).categoryDao()
    private val _filteredTransactions = MutableStateFlow<List<Transaction>>(emptyList())



    // Private state holders for the filters
    private val _startDate = MutableStateFlow<Long?>(null)
    val startDate: StateFlow<Long?> = _startDate.asStateFlow()

    private val _endDate = MutableStateFlow<Long?>(null)
    val endDate: StateFlow<Long?> = _endDate.asStateFlow()

    private val _transactionType = MutableStateFlow(PassbookTransactionType.ALL)
    val transactionType: StateFlow<PassbookTransactionType> = _transactionType.asStateFlow()

    // The final list of transactions to be displayed, reacts to filter changes
    val filteredTransactions: StateFlow<List<Transaction>> = _filteredTransactions.asStateFlow()

    val allCategories: StateFlow<List<Category>> = categoryDao.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    init {
        // Refresh the list whenever a filter changes
        viewModelScope.launch {
            combine(
                _startDate,
                _endDate,
                _transactionType
            ) { start, end, type ->
                updateTransactionList(start, end, type)
            }.collect {}
        }
        // Set initial date range to "This Month"
        setThisMonth()
    }

    private suspend fun updateTransactionList(start: Long?, end: Long?, type: PassbookTransactionType) {
        // This is a temporary, non-reactive way to fetch data. We will improve this later.
        val allTransactions = transactionDao.getAllTransactions().first() // Gets all transactions
        _filteredTransactions.value = allTransactions.filter { txn ->
            val dateMatch = (start == null || txn.date >= start) && (end == null || txn.date <= end)
            val typeMatch = when (type) {
                PassbookTransactionType.ALL -> true
                PassbookTransactionType.DEBIT -> txn.type.equals("DEBIT", ignoreCase = true)
                PassbookTransactionType.CREDIT -> txn.type.equals("CREDIT", ignoreCase = true)
            }
            dateMatch && typeMatch
        }.sortedByDescending { it.date }
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
        calendar.set(Calendar.DAY_OF_YEAR, 1) // First day of the current year
        setDayToStart(calendar)
        val start = calendar.timeInMillis

        calendar.set(Calendar.MONTH, Calendar.DECEMBER) // Go to December
        calendar.set(Calendar.DAY_OF_MONTH, 31) // Last day of December
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
        val currentMonth = calendar.get(Calendar.MONTH) // January is 0
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

    // --- Helper functions ---
    private fun setDayToStart(calendar: Calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
    }

    fun setPreviousFinancialYear() {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        // Go back one year from the current financial year's logic
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

    private fun setDayToEnd(calendar: Calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
    }
    /**
     * Generates a PDF of the passbook transactions and saves it to the specified URI.
     * This function runs in a background thread to avoid blocking the UI.
     *
     * @param context The application context.
     * @param uri The URI where the PDF should be saved.
     */
    fun generateAndSavePdf
                (context: Context,
                 uri: Uri,
                 primaryColor: Int,
                 textColor: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) { // Run PDF generation on a background thread
            val transactionsToExport = filteredTransactions.value
            if (transactionsToExport.isEmpty()) return@launch

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
}
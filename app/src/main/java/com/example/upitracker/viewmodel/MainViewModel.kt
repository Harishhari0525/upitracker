package com.example.upitracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.upitracker.data.AppDatabase
import com.example.upitracker.data.Transaction
import com.example.upitracker.data.UpiLiteSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.example.upitracker.util.ThemePreference

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val dao = db.transactionDao()
    val transactions = dao.getAllTransactions().stateIn(
        viewModelScope, SharingStarted.Eagerly, emptyList()
    )
    // UPI Lite Summaries (in-memory + observe DB)
    private val _upiLiteSummaries = MutableStateFlow<List<UpiLiteSummary>>(emptyList())
    val upiLiteSummaries: StateFlow<List<UpiLiteSummary>> = _upiLiteSummaries

    init {
        // Load existing UPI Lite summaries from DB on init
        viewModelScope.launch {
            db.upiLiteSummaryDao().getAllSummaries().collect { liteList ->
                _upiLiteSummaries.value = liteList
            }
        }
    }

    fun addUpiLiteSummary(summary: UpiLiteSummary) {
        // This is called when a new summary is inserted via MainActivity
        val current = _upiLiteSummaries.value.toMutableList()
        // Add or update if same date (or define your deduplication)
        current.add(0, summary) // latest at top
        _upiLiteSummaries.value = current
    }

    // Theme Preference
    val isDarkMode = ThemePreference.isDarkModeFlow(application)
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            ThemePreference.setDarkMode(getApplication(), enabled)
        }
    }

    fun insertTransaction(transaction: Transaction) = viewModelScope.launch {
        dao.insert(transaction)
    }

    fun deleteTransaction(transaction: Transaction) = viewModelScope.launch {
        dao.delete(transaction)
    }

    fun deleteAllTransactions() = viewModelScope.launch {
        dao.deleteAll()
    }
    fun deleteAllUpiLiteSummaries() = viewModelScope.launch {
        db.upiLiteSummaryDao().deleteAll()
        _upiLiteSummaries.value = emptyList() // if you use StateFlow/MutableStateFlow for UPI Lite
    }

}

package com.example.upitracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.upitracker.data.AppDatabase
import com.example.upitracker.data.Transaction
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
}
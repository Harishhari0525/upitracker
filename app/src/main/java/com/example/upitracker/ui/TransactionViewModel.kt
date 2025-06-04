package com.example.upitracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.upitracker.data.Transaction
import com.example.upitracker.data.TransactionDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TransactionViewModel(private val dao: TransactionDao) : ViewModel() {
    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions

    init {
        // Automatically collect from Flow and update StateFlow
        viewModelScope.launch {
            dao.getAllTransactions().collectLatest {
                _transactions.value = it
            }
        }
    }
}

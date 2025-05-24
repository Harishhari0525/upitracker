@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.upitracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.upitracker.viewmodel.MainViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    mainViewModel: MainViewModel,
    onImportOldSms: () -> Unit,
    onEditRegex: () -> Unit   // <-- NEW PARAM
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("App Preferences", style = MaterialTheme.typography.titleMedium)
            HorizontalDivider()

            // Dark mode toggle (for demonstration)
            var isDark by remember { mutableStateOf(false) }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Dark Mode")
                Switch(
                    checked = isDark,
                    onCheckedChange = { isDark = it /* Implement persistence in prod */ }
                )
            }

            // PIN/Biometric setup (placeholder)
            val isPinSet by remember { mutableStateOf(false) }
            Button(onClick = { /* Launch PIN set screen */ }) {
                Text(if (isPinSet) "Change PIN" else "Set PIN")
            }

            // Regex Editor (Now working!)
            Button(onClick = onEditRegex) {
                Text("Edit UPI SMS Regex")
            }

            // Export to CSV (stub)
            Button(onClick = { /* Implement CSV Export */ }) {
                Text("Export Transactions to CSV")
            }
            Button(onClick = onImportOldSms) {
                Text("Import Old UPI SMS")
            }
            // Danger zone: delete all transactions
            OutlinedButton(
                onClick = { mainViewModel.deleteAllTransactions() },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete All Transactions")
            }
        }
    }
}

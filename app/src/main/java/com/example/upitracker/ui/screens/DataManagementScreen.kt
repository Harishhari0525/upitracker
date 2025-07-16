@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.upitracker.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.upitracker.viewmodel.MainViewModel

private sealed interface DataDialog {
    data object None : DataDialog
    data object RefundKeyword : DataDialog
    data object RestoreConfirm : DataDialog
}

@Composable
fun DataManagementScreen(
    onBack: () -> Unit,
    mainViewModel: MainViewModel,
    onImportOldSms: () -> Unit,
    onRefreshSmsArchive: () -> Unit,
    onBackupDatabase: () -> Unit,
    onRestoreDatabase: () -> Unit,
    onNavigateToArchive: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToRules: () -> Unit,
    onNavigateToPassbook: () -> Unit,
    onExportToCsv: () -> Unit
) {
    val isImportingSms by mainViewModel.isImportingSms.collectAsState()
    val isExportingCsv by mainViewModel.isExportingCsv.collectAsState()
    val isRefreshingSmsArchive by mainViewModel.isRefreshingSmsArchive.collectAsState()
    val isBackingUp by mainViewModel.isBackingUp.collectAsState()
    val isRestoring by mainViewModel.isRestoring.collectAsState()
    val refundKeyword by mainViewModel.refundKeyword.collectAsState()

    var activeDialog by remember { mutableStateOf<DataDialog>(DataDialog.None) }
    val refundKeywordUpdateInfo by mainViewModel.refundKeywordUpdateInfo.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = { Text("Data & Sync") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item { SettingsSectionTitle("Manage") }
            item {
                SettingItemRow(
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    title = "Manage Rules",
                    summary = "Customize auto-categorization and parsing",
                    onClick = onNavigateToRules
                )
            }
            item {
                SettingItemRow(
                    icon = Icons.Filled.Category,
                    title = "Manage Categories",
                    summary = "Add, edit, or delete custom categories",
                    onClick = onNavigateToCategories
                )
            }
            item {
                SettingItemRow(
                    icon = Icons.Filled.Archive,
                    title = "View Archived Transactions",
                    summary = "View and restore archived items",
                    onClick = onNavigateToArchive
                )
            }
            item {
                SettingItemRow(
                    icon = Icons.AutoMirrored.Filled.Undo,
                    title = "Set Refund Keyword",
                    summary = "Current keyword: \"$refundKeyword\"",
                    onClick = { activeDialog = DataDialog.RefundKeyword }
                )
            }

            item { SettingsSectionTitle("Sync") }
            item {
                SettingItemRow(
                    icon = if (isRefreshingSmsArchive) Icons.Filled.SyncProblem else Icons.Filled.Sync,
                    title = "Sync SMS Archive",
                    summary = if (isRefreshingSmsArchive) "Sync in progress..." else "Find new transactions from all SMS",
                    onClick = { if (!isRefreshingSmsArchive && !isImportingSms) onRefreshSmsArchive() }
                ) { if (isRefreshingSmsArchive) CircularProgressIndicator(Modifier.size(24.dp)) }
            }
            item {
                SettingItemRow(
                    icon = Icons.Filled.SyncLock,
                    title = "Re-Sync Bank Names",
                    summary = "Update older transactions with bank data",
                    onClick = { mainViewModel.backfillBankNames() }
                )
            }

            item { SettingsSectionTitle("Import & Export") }
            item {
                SettingItemRow(
                    icon = Icons.Filled.Description,
                    title = "Passbook / Statements",
                    summary = "Generate transaction statements as PDF",
                    onClick = onNavigateToPassbook
                )
            }
            item {
                SettingItemRow(
                    icon = Icons.Filled.Download,
                    title = "Export to CSV",
                    summary = if (isExportingCsv) "Exporting..." else "Save all transactions to a CSV file",
                    onClick = onExportToCsv
                ) { if (isExportingCsv) CircularProgressIndicator(Modifier.size(24.dp)) }
            }
            item {
                SettingItemRow(
                    icon = Icons.Filled.Save,
                    title = "Backup Database",
                    summary = "Save all data to a single file",
                    onClick = { if (!isBackingUp) onBackupDatabase() }
                ) { if (isBackingUp) CircularProgressIndicator(Modifier.size(24.dp)) }
            }
            item {
                SettingItemRow(
                    icon = Icons.Filled.Restore,
                    title = "Restore Database",
                    summary = "Replace all data from a backup file",
                    onClick = { if (!isRestoring) activeDialog = DataDialog.RestoreConfirm }
                ) { if (isRestoring) CircularProgressIndicator(Modifier.size(24.dp)) }
            }
            item {
                SettingItemRow(
                    icon = if (isImportingSms) Icons.Filled.CloudDownload else Icons.Filled.CloudUpload,
                    title = "Import from All SMS",
                    summary = if (isImportingSms) "Import in progress..." else "One-time import from entire SMS inbox",
                    onClick = { if (!isImportingSms) onImportOldSms() }
                ) {
                    if (isImportingSms) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                }
            }
        }
    }
    when (activeDialog) {
        is DataDialog.RefundKeyword -> {
            var tempKeyword by remember { mutableStateOf(refundKeyword) }
            RefundKeywordDialog(
                tempKeyword = tempKeyword,
                onTempKeywordChange = { tempKeyword = it },
                onDismiss = { activeDialog = DataDialog.None },
                onConfirm = {
                    mainViewModel.setRefundKeyword(tempKeyword)
                    activeDialog = DataDialog.None
                }
            )
        }
        is DataDialog.RestoreConfirm -> {
            RestoreConfirmDialog(
                onDismiss = { activeDialog = DataDialog.None },
                onConfirm = {
                    activeDialog = DataDialog.None
                    onRestoreDatabase()
                }
            )
        }
        is DataDialog.None -> { /* Do nothing */ }
    }
    refundKeywordUpdateInfo?.let { (oldKeyword, newKeyword) ->
        AlertDialog(
            onDismissRequest = { mainViewModel.dismissRefundKeywordUpdate() },
            title = { Text("Update Existing Transactions?") },
            text = { Text("Would you like to rename all transactions currently categorized as '$oldKeyword' to '$newKeyword'?") },
            confirmButton = {
                Button(onClick = { mainViewModel.confirmRefundKeywordUpdate() }) {
                    Text(
                        "Yes, Update All"
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { mainViewModel.dismissRefundKeywordUpdate() }) {
                    Text(
                        "No, Just Save"
                    )
                }
            }
        )
    }
}
@Composable
private fun RefundKeywordDialog(tempKeyword: String, onTempKeywordChange: (String) -> Unit, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Set Refund Keyword") },
        text = {
            Column {
                Text("Transactions with this category will be excluded from spending totals.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = tempKeyword, onValueChange = onTempKeywordChange, label = { Text("Keyword") }, singleLine = true)
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun RestoreConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Warning, contentDescription = "Warning", tint = MaterialTheme.colorScheme.error) },
        title = { Text("Confirm Restore") },
        text = { Text("This will permanently overwrite all current app data. This action cannot be undone. Are you sure?") },
        confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Restore") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
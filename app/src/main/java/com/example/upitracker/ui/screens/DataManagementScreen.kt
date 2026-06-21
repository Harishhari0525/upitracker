@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.upitracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncLock
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.upitracker.ui.components.expressive.ExpressiveQuickActionCard
import com.example.upitracker.ui.components.expressive.ExpressiveSectionHeader
import com.example.upitracker.ui.components.expressive.ExpressiveTopBar
import com.example.upitracker.util.ExpressiveTokens
import com.example.upitracker.viewmodel.MainViewModel

private sealed interface DataDialog {
    data object None : DataDialog
    data object RefundKeyword : DataDialog
    data class RestoreConfirm(val password: String) : DataDialog
    data object BackupPassword : DataDialog
    data object RestorePassword : DataDialog
}

@Composable
fun DataManagementScreen(
    onBack: () -> Unit,
    mainViewModel: MainViewModel,
    onImportOldSms: () -> Unit,
    onRefreshSmsArchive: () -> Unit,
    onBackupDatabase: (String) -> Unit,
    onRestoreDatabase: (String) -> Unit,
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
    val refundKeywordUpdateInfo by mainViewModel.refundKeywordUpdateInfo.collectAsState()
    val balanceDrifts by mainViewModel.balanceDrifts.collectAsState()

    var activeDialog by remember { mutableStateOf<DataDialog>(DataDialog.None) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            ExpressiveTopBar(
                title = "Data & Sync",
                subtitle = "Imports, backups, rules, and exports",
                showBackButton = true,
                onBackClick = onBack
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = ExpressiveTokens.spacing.lg,
                top = ExpressiveTokens.spacing.md,
                end = ExpressiveTokens.spacing.lg,
                bottom = ExpressiveTokens.spacing.huge
            ),
            verticalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.md)
        ) {
            item {
                ExpressiveSectionHeader(
                    title = "Manage",
                    subtitle = "Control categories, rules, archive, and refund behavior"
                )
            }

            if (balanceDrifts.isNotEmpty()) {
                item {
                    ExpressiveQuickActionCard(
                        icon = Icons.Filled.Warning,
                        title = "Balance reconciliation",
                        subtitle = "${balanceDrifts.size} possible missing or inconsistent transactions detected",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val newest = balanceDrifts.first()
                            mainViewModel.selectTransaction(newest.transactionId)
                            mainViewModel.postPlainSnackbarMessage("Newest discrepancy selected in ${newest.bankName}.")
                        }
                    )
                }
            }

            item {
                ExpressiveQuickActionCard(
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    title = "Manage Rules",
                    subtitle = "Customize auto-categorization and parsing",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onNavigateToRules
                )
            }

            item {
                ExpressiveQuickActionCard(
                    icon = Icons.Filled.Category,
                    title = "Manage Categories",
                    subtitle = "Add, edit, or delete custom categories",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onNavigateToCategories
                )
            }

            item {
                ExpressiveQuickActionCard(
                    icon = Icons.Filled.Archive,
                    title = "View Archived Transactions",
                    subtitle = "View and restore archived items",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onNavigateToArchive
                )
            }

            item {
                ExpressiveQuickActionCard(
                    icon = Icons.AutoMirrored.Filled.Undo,
                    title = "Set Refund Keyword",
                    subtitle = "Current keyword: \"$refundKeyword\"",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { activeDialog = DataDialog.RefundKeyword }
                )
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = ExpressiveTokens.spacing.xs)
                )
            }

            item {
                ExpressiveSectionHeader(
                    title = "Sync",
                    subtitle = "Refresh SMS-based transaction data"
                )
            }

            item {
                val latestTimestamp by mainViewModel.latestTransactionTimestamp.collectAsState()
                val lastSyncTime by mainViewModel.lastSyncExecutionTimestamp.collectAsState()
                val latestSyncText = remember(lastSyncTime, latestTimestamp) {
                    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                    val syncStr = if (lastSyncTime > 0) sdf.format(Date(lastSyncTime)) else "Never"
                    val txnStr = if (latestTimestamp > 0) sdf.format(Date(latestTimestamp)) else "None"
                    "Synced: $syncStr • Last Txn: $txnStr"
                }
                ExpressiveQuickActionCard(
                    icon = if (isRefreshingSmsArchive) Icons.Filled.SyncProblem else Icons.Filled.Sync,
                    title = "Sync SMS Archive",
                    subtitle = if (isRefreshingSmsArchive) {
                        "Sync in progress..."
                    } else {
                        latestSyncText
                    },
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (!isRefreshingSmsArchive && !isImportingSms) {
                            onRefreshSmsArchive()
                        }
                    }
                )
            }

            item {
                ExpressiveQuickActionCard(
                    icon = Icons.Filled.SyncLock,
                    title = "Re-Sync Bank Names",
                    subtitle = "Update older transactions with bank data",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { mainViewModel.backfillBankNames() }
                )
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = ExpressiveTokens.spacing.xs)
                )
            }

            item {
                ExpressiveSectionHeader(
                    title = "Import & Export",
                    subtitle = "Move data in and out of the app"
                )
            }

            item {
                ExpressiveQuickActionCard(
                    icon = Icons.Filled.Description,
                    title = "Passbook / Statements",
                    subtitle = "Generate transaction statements as PDF",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onNavigateToPassbook
                )
            }

            item {
                ExpressiveQuickActionCard(
                    icon = Icons.Filled.Download,
                    title = "Export to CSV",
                    subtitle = if (isExportingCsv) {
                        "Exporting..."
                    } else {
                        "Save all transactions to a CSV file"
                    },
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (!isExportingCsv) {
                            onExportToCsv()
                        }
                    }
                )
            }

            item {
                ExpressiveQuickActionCard(
                    icon = Icons.Filled.Save,
                    title = "Backup Database",
                    subtitle = if (isBackingUp) {
                        "Backup in progress..."
                    } else {
                        "Save all data to a single file"
                    },
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (!isBackingUp) {
                            activeDialog = DataDialog.BackupPassword
                        }
                    }
                )
            }

            item {
                ExpressiveQuickActionCard(
                    icon = Icons.Filled.Restore,
                    title = "Restore Database",
                    subtitle = if (isRestoring) {
                        "Restore in progress..."
                    } else {
                        "Replace all data from a backup file"
                    },
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (!isRestoring) {
                            activeDialog = DataDialog.RestorePassword
                        }
                    }
                )
            }

            item {
                ExpressiveQuickActionCard(
                    icon = if (isImportingSms) Icons.Filled.CloudDownload else Icons.Filled.CloudUpload,
                    title = "Import from All SMS",
                    subtitle = if (isImportingSms) {
                        "Import in progress..."
                    } else {
                        "One-time import from entire SMS inbox"
                    },
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (!isImportingSms) {
                            onImportOldSms()
                        }
                    }
                )
            }

            if (isImportingSms || isRefreshingSmsArchive || isExportingCsv || isBackingUp || isRestoring) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = ExpressiveTokens.spacing.md)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp
                        )

                        Spacer(modifier = Modifier.height(ExpressiveTokens.spacing.sm))

                        Text(
                            text = "A background operation is running. Please keep the app open.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    when (activeDialog) {
        is DataDialog.RefundKeyword -> {
            var tempKeyword by remember(refundKeyword) { mutableStateOf(refundKeyword) }

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

        is DataDialog.BackupPassword -> {
            var password by remember { mutableStateOf("") }
            BackupPasswordDialog(
                title = "Backup protection",
                password = password,
                onPasswordChange = { password = it },
                onDismiss = { activeDialog = DataDialog.None },
                onConfirm = {
                    activeDialog = DataDialog.None
                    onBackupDatabase(password)
                }
            )
        }

        is DataDialog.RestorePassword -> {
            var password by remember { mutableStateOf("") }
            BackupPasswordDialog(
                title = "Backup password",
                password = password,
                onPasswordChange = { password = it },
                onDismiss = { activeDialog = DataDialog.None },
                onConfirm = { activeDialog = DataDialog.RestoreConfirm(password) }
            )
        }

        is DataDialog.RestoreConfirm -> {
            val restore = activeDialog as DataDialog.RestoreConfirm
            RestoreConfirmDialog(
                onDismiss = { activeDialog = DataDialog.None },
                onConfirm = {
                    activeDialog = DataDialog.None
                    onRestoreDatabase(restore.password)
                }
            )
        }

        is DataDialog.None -> Unit
    }

    refundKeywordUpdateInfo?.let { (oldKeyword, newKeyword) ->
        AlertDialog(
            onDismissRequest = { mainViewModel.dismissRefundKeywordUpdate() },
            title = {
                Text("Update Existing Transactions?")
            },
            text = {
                Text(
                    "Would you like to rename all transactions currently categorized as '$oldKeyword' to '$newKeyword'?"
                )
            },
            confirmButton = {
                Button(
                    onClick = { mainViewModel.confirmRefundKeywordUpdate() }
                ) {
                    Text("Yes, Update All")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { mainViewModel.dismissRefundKeywordUpdate() }
                ) {
                    Text("No, Just Save")
                }
            }
        )
    }
}

@Composable
private fun RefundKeywordDialog(
    tempKeyword: String,
    onTempKeywordChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Set Refund Keyword")
        },
        text = {
            Column {
                Text(
                    text = "Transactions with this category will be excluded from spending totals.",
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = tempKeyword,
                    onValueChange = onTempKeywordChange,
                    label = { Text("Keyword") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun RestoreConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = "Warning",
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text("Confirm Restore")
        },
        text = {
            Text(
                "This will permanently overwrite all current app data. This action cannot be undone. Are you sure?"
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Restore")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun BackupPasswordDialog(
    title: String,
    password: String,
    onPasswordChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Use at least 8 characters for a portable backup. Leave blank only for a device-bound legacy backup.")
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
            }

        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = password.isBlank() || password.length >= 8) { Text("Continue") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

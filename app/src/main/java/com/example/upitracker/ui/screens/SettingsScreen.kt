@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.upitracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.activity.compose.rememberLauncherForActivityResult // ✨
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.upitracker.R
import com.example.upitracker.ui.components.OldPinVerificationComponent
import com.example.upitracker.ui.components.PinSetupScreen
import com.example.upitracker.viewmodel.MainViewModel
import com.example.upitracker.util.PinStorage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.example.upitracker.util.AppTheme

private enum class PinChangeStep {
    NONE,
    VERIFY_OLD,
    SET_NEW
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    mainViewModel: MainViewModel,
    onImportOldSms: () -> Unit,
    onNavigateToRules: () -> Unit,
    onRefreshSmsArchive: () -> Unit,
    modifier: Modifier = Modifier,
    onBackupDatabase: () -> Unit, // ✨ ADD THIS
    onRestoreDatabase: () -> Unit,
    onNavigateToArchive: () -> Unit
) {
    val context = LocalContext.current
    var currentPinChangeStep by remember { mutableStateOf(PinChangeStep.NONE) }
    var isPinSet by remember { mutableStateOf(false) }
    var oldPinVerifiedSuccessfully by remember { mutableStateOf(false) }

    var showThemeDialog by remember { mutableStateOf(false) }
    val currentTheme by mainViewModel.appTheme.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val isImportingSms by mainViewModel.isImportingSms.collectAsState()
    val isExportingCsv by mainViewModel.isExportingCsv.collectAsState()
    val isRefreshingSmsArchive by mainViewModel.isRefreshingSmsArchive.collectAsState() // ✨ Collect new state
    val isBackingUp by mainViewModel.isBackingUp.collectAsState() // ✨ ADD THIS
    val isRestoring by mainViewModel.isRestoring.collectAsState() // ✨ ADD THIS
    var showAboutDialog by remember { mutableStateOf(false) }

    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var showRefundKeywordDialog by remember { mutableStateOf(false) }

    var showPrivacyDialog by remember { mutableStateOf(false) }

    // Re-compute isPinSet when no dialog is active
    LaunchedEffect(Unit, currentPinChangeStep) {
        if (currentPinChangeStep == PinChangeStep.NONE) {
            isPinSet = PinStorage.isPinSet(context)
            oldPinVerifiedSuccessfully = false
        }
    }

    val createCsvFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            mainViewModel.exportTransactionsToCsv(it, context.contentResolver)
        } ?: run {
            mainViewModel.postSnackbarMessage("CSV export cancelled.")
            mainViewModel.setSmsImportingState(false)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        // Appearance Section
        item { SettingsSectionTitle(stringResource(R.string.settings_section_appearance)) }
        item {
            val isDarkMode by mainViewModel.isDarkMode.collectAsState()
            SettingItemRow(
                icon = Icons.Filled.BrightnessMedium,
                title = stringResource(R.string.settings_dark_mode),
                summary = if (isDarkMode) stringResource(R.string.settings_dark_mode_enabled)
                else stringResource(R.string.settings_dark_mode_disabled),
                onClick = { mainViewModel.toggleDarkMode(!isDarkMode) }
            ) {
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { mainViewModel.toggleDarkMode(it) }
                )
            }
        }

        item {
            SettingItemRow(
                icon = Icons.Filled.Palette, // Example Icon
                title = "App Theme",
                summary = "Current: ${currentTheme.displayName}",
                onClick = { showThemeDialog = true }
            )
        }

        item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }

        // Security Section
        item { SettingsSectionTitle(stringResource(R.string.settings_section_security)) }
        item {
            SettingItemRow(
                icon = Icons.Filled.Lock,
                title = if (isPinSet)
                    stringResource(R.string.settings_change_pin)
                else
                    stringResource(R.string.settings_set_pin),
                summary = if (isPinSet)
                    stringResource(R.string.settings_pin_protected_summary)
                else
                    stringResource(R.string.settings_set_pin_summary),
                onClick = {
                    currentPinChangeStep = if (isPinSet) PinChangeStep.VERIFY_OLD
                    else PinChangeStep.SET_NEW
                }
            )
        }
        item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }

        // Data Management Section
        item { SettingsSectionTitle(stringResource(R.string.settings_section_data_management)) }
        item {
            SettingItemRow(
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                title = stringResource(R.string.settings_manage_rules_title),
                summary = stringResource(R.string.settings_manage_rules_summary),
                onClick = onNavigateToRules
            )
        }

        item {
            SettingItemRow(
                icon = Icons.Filled.Archive,
                title = stringResource(R.string.settings_view_archived_transactions), // Create this string resource
                summary = stringResource(R.string.settings_view_archived_summary),    // Create this string resource
                onClick = onNavigateToArchive
            )
        }

        item {
            val refundKeyword by mainViewModel.refundKeyword.collectAsState() // We will add this to the ViewModel next
            SettingItemRow(
                icon = Icons.AutoMirrored.Filled.Undo, // Example icon
                title = "Set Refund Keyword",
                summary = "Current keyword: \"$refundKeyword\"",
                onClick = { showRefundKeywordDialog = true }
            )
        }

        item {
            SettingItemRow(
                icon = if (isRefreshingSmsArchive) Icons.Filled.SyncProblem else Icons.Filled.Sync, // Example icons
                title = stringResource(R.string.settings_sync_sms_backup),
                summary = if (isRefreshingSmsArchive) stringResource(R.string.settings_sync_sms_backup_in_progress) else stringResource(R.string.settings_sync_sms_backup_summary), // ✨ New String
                onClick = {
                    if (!isRefreshingSmsArchive && !isImportingSms) { // Ensure no other SMS operation is running
                        // ✨ Call the new function/lambda ✨
                        // This should handle its own permission check if needed or assume it's granted
                        // For now, assuming MainActivity's requestSmsPermissionAndRefreshArchive handles permissions.
                        onRefreshSmsArchive() // This lambda needs to be passed down
                    }
                },
                titleColor = if (isRefreshingSmsArchive) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                iconTint = if (isRefreshingSmsArchive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            ) {
                if (isRefreshingSmsArchive) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            }
        }

        item {
            // CSV Export row
            SettingItemRow(
                icon = Icons.Filled.Download,
                title = stringResource(R.string.settings_export_csv),
                summary = if (isExportingCsv) "Exporting in progress..."
                else stringResource(R.string.settings_export_csv_summary),
                onClick = {
                    if (!isExportingCsv) {
                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                            .format(Date())
                        val fileName = "upi_tracker_export_$timestamp.csv"
                        createCsvFileLauncher.launch(fileName)
                    }
                },
                titleColor = if (isExportingCsv) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
                iconTint = if (isExportingCsv) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.secondary
            ) {
                if (isExportingCsv) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }

        item {
            SettingItemRow(
                icon = Icons.Filled.Save,
                title = "Backup Database", // TODO: Add to strings.xml
                summary = "Save all data (transactions, budgets) to a file",
                onClick = { if (!isBackingUp) onBackupDatabase() },
                titleColor = if (isBackingUp) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                iconTint = if (isBackingUp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            ) {
                if (isBackingUp) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            }
        }

        item {
            SettingItemRow(
                icon = Icons.Filled.Restore,
                title = "Restore Database", // TODO: Add to strings.xml
                summary = "Replace all current data from a backup file",
                onClick = { if (!isRestoring) showRestoreConfirmDialog = true },
                titleColor = if (isRestoring) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                iconTint = if (isRestoring) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            ) {
                if (isRestoring) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            }
        }

        item {
            SettingItemRow(
                icon = if (isImportingSms) Icons.Filled.CloudDownload else Icons.Filled.CloudUpload,
                title = stringResource(R.string.settings_import_old_sms),
                summary = if (isImportingSms)
                    stringResource(R.string.settings_import_old_sms_in_progress)
                else stringResource(R.string.settings_import_old_sms_summary),
                onClick = {
                    if (!isImportingSms) onImportOldSms()
                },
                titleColor = if (isImportingSms) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
                iconTint = if (isImportingSms) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.secondary
            ) {
                if (isImportingSms) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }
        item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }

        item {
            SettingItemRow(
                icon = Icons.Filled.PrivacyTip,
                title = "Privacy & Permissions",
                summary = "How your SMS data is used",
                onClick = { showPrivacyDialog = true }
            )
        }

        // Danger Zone Section
        item {
            SettingsSectionTitle(
                stringResource(R.string.settings_section_danger_zone),
                titleColor = MaterialTheme.colorScheme.error
            )
        }
        item {
            var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
            SettingItemRow(
                icon = Icons.Filled.DeleteForever,
                title = stringResource(R.string.settings_delete_all_data),
                summary = stringResource(R.string.settings_delete_all_data_summary),
                onClick = { showDeleteConfirmationDialog = true },
                titleColor = MaterialTheme.colorScheme.error,
                iconTint = MaterialTheme.colorScheme.error
            )
            if (showDeleteConfirmationDialog) {
                DeleteConfirmationDialog(
                    onConfirm = {
                        coroutineScope.launch {
                            mainViewModel.deleteAllTransactions()
                            mainViewModel.deleteAllUpiLiteSummaries()
                            mainViewModel.postSnackbarMessage(
                                context.getString(R.string.settings_delete_all_data_success)
                            )
                        }
                        showDeleteConfirmationDialog = false
                    },
                    onDismiss = { showDeleteConfirmationDialog = false }
                )
            }
        }
        item { Spacer(Modifier.height(20.dp)) }

        // About Section
        item { SettingsSectionTitle(stringResource(R.string.settings_section_about)) }
        item {
            val versionName = stringResource(R.string.settings_app_version_placeholder)
            SettingItemRow(
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                title = stringResource(R.string.settings_about_app),
                summary = stringResource(R.string.settings_app_version_summary, versionName),
                onClick = { showAboutDialog = true }
            )
        }
        item { Spacer(Modifier.height(16.dp)) }
    }

    // PIN-change dialogs
    if (currentPinChangeStep != PinChangeStep.NONE) {
        AlertDialog(
            onDismissRequest = { currentPinChangeStep = PinChangeStep.NONE },
            title = {
                Text(
                    when (currentPinChangeStep) {
                        PinChangeStep.VERIFY_OLD ->
                            stringResource(R.string.pin_change_enter_current_pin_title)
                        PinChangeStep.SET_NEW ->
                            if (isPinSet && oldPinVerifiedSuccessfully)
                                stringResource(R.string.dialog_set_pin_title_change)
                            else
                                stringResource(R.string.dialog_set_pin_title_new)
                        else -> ""
                    }
                )
            },
            text = {
                when (currentPinChangeStep) {
                    PinChangeStep.VERIFY_OLD -> {
                        OldPinVerificationComponent(
                            onOldPinVerified = {
                                oldPinVerifiedSuccessfully = true
                                currentPinChangeStep = PinChangeStep.SET_NEW
                            },
                            onCancel = { currentPinChangeStep = PinChangeStep.NONE }
                        )
                    }
                    PinChangeStep.SET_NEW -> {
                        PinSetupScreen(
                            onPinSet = {
                                coroutineScope.launch {
                                    isPinSet = PinStorage.isPinSet(context)
                                    mainViewModel.postSnackbarMessage(
                                        context.getString(
                                            if (oldPinVerifiedSuccessfully)
                                                R.string.pin_change_success_pin_changed
                                            else
                                                R.string.pin_setup_pin_set_success
                                        )
                                    )
                                }
                                currentPinChangeStep = PinChangeStep.NONE
                            },
                            onCancel = { currentPinChangeStep = PinChangeStep.NONE }
                        )
                    }
                    else -> {}
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }
    if (showRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            icon = { Icon(Icons.Filled.Warning, contentDescription = "Warning", tint = MaterialTheme.colorScheme.error) },
            title = { Text("Confirm Restore") },
            text = { Text("This will permanently overwrite all current app data with the contents of the backup file. This action cannot be undone. Are you sure you want to proceed?") },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreConfirmDialog = false
                        onRestoreDatabase()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    if (showThemeDialog) {
        ThemeChooserDialog(
            currentTheme = currentTheme,
            onDismiss = { showThemeDialog = false },
            onThemeSelected = { theme ->
                mainViewModel.setAppTheme(theme)
                showThemeDialog = false
            }
        )
    }
    if (showAboutDialog) {
        AboutDialog(
            onDismiss = { showAboutDialog = false }
        )
    }
    if (showRefundKeywordDialog) {
        val refundKeyword by mainViewModel.refundKeyword.collectAsState()
        var tempKeyword by remember { mutableStateOf(refundKeyword) }

        AlertDialog(
            onDismissRequest = { showRefundKeywordDialog = false },
            title = { Text("Set Refund Keyword") },
            text = {
                Column {
                    Text("Transactions categorized with this keyword will be excluded from spending totals.", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = tempKeyword,
                        onValueChange = { tempKeyword = it },
                        label = { Text("Keyword") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    mainViewModel.setRefundKeyword(tempKeyword)
                    showRefundKeywordDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRefundKeywordDialog = false }) { Text("Cancel") }
            }
        )
    }
    if (showPrivacyDialog) {
        PrivacyPolicyDialog(onDismiss = { showPrivacyDialog = false })
    }
}

@Composable
fun SettingsSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    titleColor: Color = MaterialTheme.colorScheme.primary
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = titleColor,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    )
}

@Composable
fun SettingItemRow(
    icon: ImageVector,
    title: String,
    summary: String? = null,
    onClick: () -> Unit,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    iconTint: Color = MaterialTheme.colorScheme.secondary,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = titleColor)
            if (summary != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailingContent != null) {
            Spacer(Modifier.width(16.dp))
            Box(contentAlignment = Alignment.CenterEnd) {
                trailingContent()
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Filled.DeleteForever,
                contentDescription = stringResource(R.string.settings_delete_all_data),
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text(stringResource(R.string.dialog_confirm_deletion_title)) },
        text = { Text(stringResource(R.string.dialog_confirm_deletion_message)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.dialog_button_delete_all))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_button_cancel))
            }
        }
    )
}
@Composable
private fun ThemeChooserDialog(
    currentTheme: AppTheme,
    onDismiss: () -> Unit,
    onThemeSelected: (AppTheme) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose a Theme") },
        text = {
            Column {
                AppTheme.entries.forEach { theme ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onThemeSelected(theme) }
                            .padding(horizontal = 8.dp, vertical = 8.dp), // Adjusted padding
                        verticalAlignment = Alignment.CenterVertically // This is the key fix
                    ) {
                        RadioButton(
                            selected = theme == currentTheme,
                            onClick = { onThemeSelected(theme) }
                        )
                        Spacer(Modifier.width(16.dp)) // Use a Spacer for consistent padding
                        Text(theme.displayName)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    val aboutText = buildAnnotatedString {
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append("UPI Expense Tracker - Version 1.5\n\n")
        }
        append("Effortlessly manage your spending with UPI Expense Tracker, " +
                "a powerful tool designed to give you a clear and complete picture of your finances.\n\n")
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append("Core Features:\n")
        }
        append("• Automatic Tracking\n")
        append("• Smart Categorization\n")
        append("• Insightful Reports\n")
        append("• Flexible Budgeting with Rollover\n")
        append("• Recurring Payments\n")
        append("• Data Backup, Restore & CSV Export\n")
        append("• Personalization & Themes\n\n")
        append("All data is stored privately and securely on your device.")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Info, contentDescription = "About App") },
        title = { Text("About") },
        text = {
            // Make the text scrollable in case it's too long for the screen
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(text = aboutText)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
@Composable
private fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    val policyText = buildAnnotatedString {
        append("This app is designed with your privacy as the top priority. Here’s how we handle your data:\n\n")

        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
            append("SMS Permission (READ_SMS)\n")
        }
        append("The app requests permission to read your SMS messages for one reason only: to automatically detect and parse UPI payment and expense messages. This allows the app to build your transaction history without any manual entry.\n\n")

        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
            append("Data Storage and Security\n")
        }
        append("All data processed from your SMS, including the transactions created, is stored exclusively on your device. This data is never uploaded, shared, or sent to any external server. Your financial information does not leave your phone.\n\n")

        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
            append("Your Control\n")
        }
        append("You are in complete control of your data. You can delete any individual transaction or use the 'Delete All Data' option in the settings to permanently erase all stored information from the app at any time.")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.PrivacyTip, contentDescription = "Privacy Policy") },
        title = { Text("Privacy & Data Policy") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(text = policyText, style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}
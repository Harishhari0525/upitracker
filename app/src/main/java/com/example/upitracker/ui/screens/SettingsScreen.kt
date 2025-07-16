@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.upitracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.upitracker.R
import com.example.upitracker.ui.components.OldPinVerificationComponent
import com.example.upitracker.ui.components.PinSetupScreen
import com.example.upitracker.util.AppTheme
import com.example.upitracker.util.PinStorage
import com.example.upitracker.viewmodel.MainViewModel
import kotlinx.coroutines.launch

private enum class PinChangeStep {
    NONE,
    VERIFY_OLD,
    SET_NEW
}

private sealed interface SettingsDialog {
    data object None : SettingsDialog
    data object ThemeChooser : SettingsDialog
    data object DeleteAllConfirm : SettingsDialog
    data object Privacy : SettingsDialog
    data object About : SettingsDialog
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    mainViewModel: MainViewModel,
    onNavigateToDataManagement: () -> Unit, // This is the key navigation action
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var activeDialog by remember { mutableStateOf<SettingsDialog>(SettingsDialog.None) }
    var currentPinChangeStep by remember { mutableStateOf(PinChangeStep.NONE) }
    var isPinSet by remember { mutableStateOf(false) }
    var oldPinVerifiedSuccessfully by remember { mutableStateOf(false) }

    LaunchedEffect(Unit, currentPinChangeStep) {
        if (currentPinChangeStep == PinChangeStep.NONE) {
            isPinSet = PinStorage.isPinSet(context)
            oldPinVerifiedSuccessfully = false
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        item { SettingsSectionTitle("Appearance") }
        item {
            val isDarkMode by mainViewModel.isDarkMode.collectAsState()
            SettingItemRow(
                icon = Icons.Filled.BrightnessMedium,
                title = "Dark Mode",
                summary = if (isDarkMode) "Enabled" else "Disabled",
                onClick = { mainViewModel.toggleDarkMode(!isDarkMode) }
            ) {
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { mainViewModel.toggleDarkMode(it) })
            }
        }
        item {
            val currentTheme by mainViewModel.appTheme.collectAsState()
            SettingItemRow(
                icon = Icons.Filled.Palette,
                title = "App Theme",
                summary = "Current: ${currentTheme.displayName}",
                onClick = { activeDialog = SettingsDialog.ThemeChooser }
            )
        }
        item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
        item { SettingsSectionTitle("Security") }
        item {
            SettingItemRow(
                icon = Icons.Filled.Lock,
                title = if (isPinSet) "Change PIN" else "Set PIN",
                summary = if (isPinSet) "PIN protection is active" else "Secure the app with a PIN",
                onClick = {
                    currentPinChangeStep =
                        if (isPinSet) PinChangeStep.VERIFY_OLD else PinChangeStep.SET_NEW
                }
            )
        }
        item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
        item { SettingsSectionTitle("Data & Sync") }
        item {
            SettingItemRow(
                icon = Icons.Filled.Storage,
                title = "Data & Sync",
                summary = "Manage rules, sync, backup, and export",
                onClick = onNavigateToDataManagement
            )
        }

        item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
        item { SettingsSectionTitle("About & Privacy") }
        item {
            SettingItemRow(
                icon = Icons.Filled.PrivacyTip,
                title = "Privacy & Permissions",
                summary = "How your SMS data is used",
                onClick = { activeDialog = SettingsDialog.Privacy }
            )
        }
        item {
            val versionName = stringResource(R.string.settings_app_version_placeholder)
            SettingItemRow(
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                title = "About App",
                summary = "Version $versionName",
                onClick = { activeDialog = SettingsDialog.About }
            )
        }
        item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
        item { SettingsSectionTitle("Danger Zone", titleColor = MaterialTheme.colorScheme.error) }
        item {
            SettingItemRow(
                icon = Icons.Filled.DeleteForever,
                title = "Delete All Data",
                summary = "Permanently erase all app data",
                onClick = { activeDialog = SettingsDialog.DeleteAllConfirm },
                titleColor = MaterialTheme.colorScheme.error,
                iconTint = MaterialTheme.colorScheme.error
            )
        }
    }

    when (activeDialog) {
        is SettingsDialog.ThemeChooser -> {
            val currentTheme by mainViewModel.appTheme.collectAsState()
            ThemeChooserDialog(
                currentTheme = currentTheme,
                onDismiss = { activeDialog = SettingsDialog.None },
                onThemeSelected = {
                    mainViewModel.setAppTheme(it); activeDialog = SettingsDialog.None
                }
            )
        }
        is SettingsDialog.DeleteAllConfirm -> {
            DeleteConfirmationDialog(
                onDismiss = { activeDialog = SettingsDialog.None },
                onConfirm = {
                    mainViewModel.deleteAllTransactions(); mainViewModel.deleteAllUpiLiteSummaries()
                    mainViewModel.postSnackbarMessage("All data has been deleted.")
                    activeDialog = SettingsDialog.None
                }
            )
        }
        is SettingsDialog.Privacy ->
            PrivacyPolicyDialog(
                onDismiss = {
                    activeDialog = SettingsDialog.None
                }
            )
        is SettingsDialog.About -> AboutDialog(onDismiss = { activeDialog = SettingsDialog.None })
        is SettingsDialog.None -> { /* Do nothing */ }
    }

    if (currentPinChangeStep != PinChangeStep.NONE) {
        Dialog(onDismissRequest = { currentPinChangeStep = PinChangeStep.NONE }) {
            Surface(
                shape = MaterialTheme.shapes.large,
                tonalElevation = 6.dp,
                modifier = Modifier.wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = when (currentPinChangeStep) {
                            PinChangeStep.VERIFY_OLD -> stringResource(R.string.pin_change_enter_current_pin_title)
                            PinChangeStep.SET_NEW -> if (isPinSet) stringResource(R.string.dialog_set_pin_title_change) else stringResource(R.string.dialog_set_pin_title_new)
                            PinChangeStep.NONE -> ""
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))

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
                                                if (oldPinVerifiedSuccessfully) R.string.pin_change_success_pin_changed
                                                else R.string.pin_setup_pin_set_success
                                            )
                                        )
                                    }
                                    currentPinChangeStep = PinChangeStep.NONE
                                },
                                onCancel = { currentPinChangeStep = PinChangeStep.NONE }
                            )
                        }
                        PinChangeStep.NONE -> {}
                    }
                }
            }
        }
    }
}


@Composable
fun SettingsSectionTitle(title: String, modifier: Modifier = Modifier, titleColor: Color = MaterialTheme.colorScheme.primary) {
    Text(
        text = title, style = MaterialTheme.typography.titleSmall, color = titleColor,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)
    )
}

@Composable
fun SettingItemRow(
    icon: ImageVector, title: String, summary: String? = null, onClick: () -> Unit,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    iconTint: Color = MaterialTheme.colorScheme.secondary,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = titleColor)
            if (summary != null) {
                Spacer(Modifier.height(2.dp))
                Text(text = summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (trailingContent != null) {
            Spacer(Modifier.width(16.dp))
            Box(contentAlignment = Alignment.CenterEnd) { trailingContent() }
        }
    }
}

@Composable
private fun ThemeChooserDialog(currentTheme: AppTheme, onDismiss: () -> Unit, onThemeSelected: (AppTheme) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Choose a Theme") },
        text = {
            Column {
                AppTheme.entries.forEach { theme ->
                    Row(Modifier.fillMaxWidth().clickable { onThemeSelected(theme) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = theme == currentTheme, onClick = { onThemeSelected(theme) })
                        Spacer(Modifier.width(16.dp))
                        Text(theme.displayName)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun DeleteConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.DeleteForever, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error) },
        title = { Text("Confirm Deletion") },
        text = { Text("Are you sure you want to permanently delete all transactions and data?") },
        confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete All") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    val policyText = buildAnnotatedString {
        append("This app is designed with your privacy as the top priority. Here’s how we handle your data:\n\n")
        withStyle(style = SpanStyle(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        ) {
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

    AlertDialog(onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.PrivacyTip, contentDescription = "Privacy Policy") },
        title = { Text("Privacy & Data Policy") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(text = policyText, style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
    )
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    val aboutText = buildAnnotatedString {
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append("UPI Expense Tracker - Version 1.9\n\n")
        }
        append("Effortlessly manage your spending with UPI Expense Tracker, a powerful tool designed to give you a clear and complete picture of your finances.\n\n")
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

    AlertDialog(onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Info, contentDescription = "About App") },
        title = { Text("About") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(text = aboutText)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.upitracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.example.upitracker.ui.components.expressive.ExpressiveSectionHeader
import com.example.upitracker.ui.components.expressive.ExpressiveTopBar
import com.example.upitracker.ui.components.expressive.PulseAmber
import com.example.upitracker.ui.components.expressive.PulseCyan
import com.example.upitracker.ui.components.expressive.PulseEmerald
import com.example.upitracker.ui.components.expressive.PulseRose
import com.example.upitracker.ui.components.expressive.PulseViolet
import com.example.upitracker.util.AutoLockDelay
import com.example.upitracker.util.ExpressiveTokens
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
    data object Notifications : SettingsDialog
    data object DeleteAllConfirm : SettingsDialog
    data object Privacy : SettingsDialog
    data object About : SettingsDialog
}

@Composable
fun SettingsScreen(
    mainViewModel: MainViewModel,
    onNavigateToDataManagement: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val pinChangedSuccessMessage = stringResource(R.string.pin_change_success_pin_changed)
    val pinSetSuccessMessage = stringResource(R.string.pin_setup_pin_set_success)

    var activeDialog by remember { mutableStateOf<SettingsDialog>(SettingsDialog.None) }
    var currentPinChangeStep by remember { mutableStateOf(PinChangeStep.NONE) }
    var isPinSet by remember { mutableStateOf(false) }
    var oldPinVerifiedSuccessfully by remember { mutableStateOf(false) }

    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "3.0.0"
        } catch (_: Exception) {
            "3.0.0"
        }
    }

    LaunchedEffect(Unit, currentPinChangeStep) {
        if (currentPinChangeStep == PinChangeStep.NONE) {
            isPinSet = PinStorage.isPinSet(context)
            oldPinVerifiedSuccessfully = false
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            ExpressiveTopBar(
                title = "Settings",
                subtitle = "Tools, privacy, and preferences"
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = ExpressiveTokens.spacing.lg,
                top = ExpressiveTokens.spacing.lg,
                end = ExpressiveTokens.spacing.lg,
                bottom = ExpressiveTokens.spacing.huge
            ),
            verticalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.md)
        ) {
            item {
                ExpressiveSectionHeader(
                    title = "Appearance",
                    subtitle = "Display and privacy preferences"
                )
            }

            item {
                val isDarkMode by mainViewModel.isDarkMode.collectAsState()

                SettingItemRow(
                    icon = Icons.Filled.BrightnessMedium,
                    title = "Dark Mode",
                    summary = if (isDarkMode) "Enabled" else "Disabled",
                    iconTint = PulseViolet,
                    onClick = { mainViewModel.toggleDarkMode(!isDarkMode) }
                ) {
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { mainViewModel.toggleDarkMode(it) }
                    )
                }
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item {
                ExpressiveSectionHeader(
                    title = "Notifications",
                    subtitle = "Manage new transaction alerts and quick categorization"
                )
            }

            item {
                val alertsEnabled by mainViewModel.isTransactionAlertsEnabled.collectAsState()
                val actionsEnabled by mainViewModel.isNotificationActionsEnabled.collectAsState()
                val redacted by mainViewModel.isNotificationContentRedacted.collectAsState()
                val summary = buildList {
                    add(if (alertsEnabled) "Alerts on" else "Alerts off")
                    if (alertsEnabled) add(if (actionsEnabled) "Quick actions on" else "Quick actions off")
                    add(if (redacted) "Details hidden" else "Details visible")
                }.joinToString(" · ")

                SettingItemRow(
                    icon = Icons.Filled.Notifications,
                    title = "Notifications",
                    summary = summary,
                    iconTint = PulseRose,
                    onClick = { activeDialog = SettingsDialog.Notifications }
                )
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item {
                ExpressiveSectionHeader(
                    title = "Security",
                    subtitle = "Protect access to your financial data"
                )
            }

            item {
                SettingItemRow(
                    icon = Icons.Filled.Lock,
                    title = if (isPinSet) "Change PIN" else "Set PIN",
                    summary = if (isPinSet) "PIN protection is active" else "Secure the app with a PIN",
                    iconTint = PulseEmerald,
                    onClick = {
                        currentPinChangeStep =
                            if (isPinSet) PinChangeStep.VERIFY_OLD else PinChangeStep.SET_NEW
                    }
                )
            }

            item {
                val delay by mainViewModel.autoLockDelay.collectAsState()
                SettingItemRow(
                    icon = Icons.Filled.Lock,
                    title = "Automatic lock",
                    summary = delay.displayName,
                    iconTint = PulseAmber,
                    onClick = {
                        val choices = AutoLockDelay.entries
                        mainViewModel.setAutoLockDelay(choices[(choices.indexOf(delay) + 1) % choices.size])
                    }
                )
            }

            item {
                val hidden by mainViewModel.isWidgetAmountHidden.collectAsState()
                SettingItemRow(
                    icon = Icons.Filled.PrivacyTip,
                    title = "Hide widget amounts",
                    summary = "Require opening the app to see spending totals",
                    iconTint = PulseViolet,
                    onClick = { mainViewModel.setWidgetAmountHidden(!hidden) }
                ) {
                    Switch(checked = hidden, onCheckedChange = mainViewModel::setWidgetAmountHidden)
                }
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item {
                ExpressiveSectionHeader(
                    title = "Data & Sync",
                    subtitle = "Manage backup, export, rules, and import tools"
                )
            }

            item {
                SettingItemRow(
                    icon = Icons.Filled.Storage,
                    title = "Data & Sync",
                    summary = "Manage rules, sync, backup, and export",
                    iconTint = PulseCyan,
                    onClick = onNavigateToDataManagement
                )
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item {
                ExpressiveSectionHeader(
                    title = "About & Privacy",
                    subtitle = "Understand data usage and app details"
                )
            }

            item {
                SettingItemRow(
                    icon = Icons.Filled.PrivacyTip,
                    title = "Privacy & Permissions",
                    summary = "How your SMS data is used",
                    iconTint = PulseRose,
                    onClick = { activeDialog = SettingsDialog.Privacy }
                )
            }

            item {
                SettingItemRow(
                    icon = Icons.AutoMirrored.Filled.HelpOutline,
                    title = "About App",
                    summary = "Version $versionName",
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { activeDialog = SettingsDialog.About }
                )
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item {
                ExpressiveSectionHeader(
                    title = "Danger Zone",
                    subtitle = "Permanent actions that cannot be undone"
                )
            }

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

            is SettingsDialog.Notifications -> {
                val alertsEnabled by mainViewModel.isTransactionAlertsEnabled.collectAsState()
                val actionsEnabled by mainViewModel.isNotificationActionsEnabled.collectAsState()
                val redacted by mainViewModel.isNotificationContentRedacted.collectAsState()

                NotificationSettingsDialog(
                    alertsEnabled = alertsEnabled,
                    actionsEnabled = actionsEnabled,
                    redacted = redacted,
                    onAlertsChanged = mainViewModel::setTransactionAlertsEnabled,
                    onActionsChanged = mainViewModel::setNotificationActionsEnabled,
                    onRedactedChanged = mainViewModel::setNotificationContentRedacted,
                    onDismiss = { activeDialog = SettingsDialog.None }
                )
            }

            is SettingsDialog.DeleteAllConfirm -> {
                DeleteConfirmationDialog(
                    onDismiss = { activeDialog = SettingsDialog.None },
                    onConfirm = {
                        mainViewModel.deleteAllAppData()
                        mainViewModel.postSnackbarMessage("All data has been deleted.")
                        activeDialog = SettingsDialog.None
                    }
                )
            }

            is SettingsDialog.Privacy -> {
                PrivacyPolicyDialog(
                    onDismiss = { activeDialog = SettingsDialog.None }
                )
            }

            is SettingsDialog.About -> {
                AboutDialog(
                    versionName = versionName,
                    onDismiss = { activeDialog = SettingsDialog.None }
                )
            }

            is SettingsDialog.None -> Unit
        }

        if (currentPinChangeStep != PinChangeStep.NONE) {
            Dialog(
                onDismissRequest = { currentPinChangeStep = PinChangeStep.NONE }
            ) {
                Surface(
                    shape = ExpressiveTokens.corners.large,
                    tonalElevation = ExpressiveTokens.elevation.floating,
                    modifier = Modifier.wrapContentHeight()
                ) {
                    Column(
                        modifier = Modifier.padding(ExpressiveTokens.spacing.xl),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = when (currentPinChangeStep) {
                                PinChangeStep.VERIFY_OLD -> {
                                    stringResource(R.string.pin_change_enter_current_pin_title)
                                }

                                PinChangeStep.SET_NEW -> {
                                    if (isPinSet) {
                                        stringResource(R.string.dialog_set_pin_title_change)
                                    } else {
                                        stringResource(R.string.dialog_set_pin_title_new)
                                    }
                                }

                                PinChangeStep.NONE -> ""
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(ExpressiveTokens.spacing.lg))

                        when (currentPinChangeStep) {
                            PinChangeStep.VERIFY_OLD -> {
                                OldPinVerificationComponent(
                                    onOldPinVerified = {
                                        oldPinVerifiedSuccessfully = true
                                        currentPinChangeStep = PinChangeStep.SET_NEW
                                    },
                                    onCancel = {
                                        currentPinChangeStep = PinChangeStep.NONE
                                    }
                                )
                            }

                            PinChangeStep.SET_NEW -> {
                                PinSetupScreen(
                                    onPinSet = {
                                        val successMessage = if (oldPinVerifiedSuccessfully) {
                                            pinChangedSuccessMessage
                                        } else {
                                            pinSetSuccessMessage
                                        }

                                        coroutineScope.launch {
                                            isPinSet = PinStorage.isPinSet(context)
                                            mainViewModel.postSnackbarMessage(successMessage)
                                        }

                                        currentPinChangeStep = PinChangeStep.NONE
                                    },
                                    onCancel = {
                                        currentPinChangeStep = PinChangeStep.NONE
                                    }
                                )
                            }

                            PinChangeStep.NONE -> Unit
                        }
                    }
                }
            }
        }
    }
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = ExpressiveTokens.corners.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.88f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = ExpressiveTokens.elevation.card,
            pressedElevation = ExpressiveTokens.elevation.cardPressed
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = ExpressiveTokens.compact.cardHorizontal,
                    vertical = ExpressiveTokens.spacing.md
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = ExpressiveTokens.corners.small,
                color = iconTint.copy(alpha = 0.12f),
                contentColor = iconTint
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(ExpressiveTokens.compact.itemGap))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor
                )

                if (summary != null) {
                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (trailingContent != null) {
                Spacer(modifier = Modifier.width(ExpressiveTokens.spacing.md))

                Box(
                    contentAlignment = Alignment.CenterEnd
                ) {
                    trailingContent()
                }
            }
        }
    }
}



@Composable
private fun NotificationSettingsDialog(
    alertsEnabled: Boolean,
    actionsEnabled: Boolean,
    redacted: Boolean,
    onAlertsChanged: (Boolean) -> Unit,
    onActionsChanged: (Boolean) -> Unit,
    onRedactedChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = ExpressiveTokens.corners.extraLarge,
        icon = {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = null
            )
        },
        title = {
            Text(
                text = "Notifications",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.md)
            ) {
                NotificationToggleRow(
                    title = "Transaction alerts",
                    summary = "Show a notification when a new transaction is detected.",
                    checked = alertsEnabled,
                    onCheckedChange = onAlertsChanged
                )

                NotificationToggleRow(
                    title = "Quick category actions",
                    summary = "Show suggested categories and custom category input on transaction alerts.",
                    checked = actionsEnabled && alertsEnabled,
                    enabled = alertsEnabled,
                    onCheckedChange = onActionsChanged
                )

                NotificationToggleRow(
                    title = "Hide notification details",
                    summary = "Hide amount and merchant details on the lock screen.",
                    checked = redacted,
                    onCheckedChange = onRedactedChanged
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun NotificationToggleRow(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(ExpressiveTokens.spacing.md))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = ExpressiveTokens.corners.extraLarge,
        icon = {
            Icon(
                imageVector = Icons.Filled.DeleteForever,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = "Confirm Deletion",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text("Are you sure you want to permanently delete all transactions and data?")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                shape = ExpressiveTokens.corners.medium
            ) {
                Text("Delete All")
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
private fun PrivacyPolicyDialog(
    onDismiss: () -> Unit
) {
    val policyText = buildAnnotatedString {
        append("This app is designed with your privacy as the top priority. Here’s how we handle your data:\n\n")

        withStyle(
            style = SpanStyle(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        ) {
            append("SMS Permission (READ_SMS)\n")
        }

        append("The app requests permission to read your SMS messages for one reason only: to automatically detect and parse UPI payment and expense messages. This allows the app to build your transaction history without any manual entry.\n\n")

        withStyle(
            style = SpanStyle(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        ) {
            append("Data Storage and Security\n")
        }

        append("All data processed from your SMS, including the transactions created, is stored exclusively on your device. This data is never uploaded, shared, or sent to any external server. Your financial information does not leave your phone.\n\n")

        withStyle(
            style = SpanStyle(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        ) {
            append("Your Control\n")
        }

        append("You are in complete control of your data. You can delete any individual transaction or use the 'Delete All Data' option in the settings to permanently erase all stored information from the app at any time.")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = ExpressiveTokens.corners.extraLarge,
        icon = {
            Icon(
                imageVector = Icons.Filled.PrivacyTip,
                contentDescription = "Privacy Policy"
            )
        },
        title = {
            Text(
                text = "Privacy & Data Policy",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = policyText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

@Composable
private fun AboutDialog(
    versionName: String,
    onDismiss: () -> Unit
) {
    val aboutText = buildAnnotatedString {
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append("UPI Expense Tracker - Version $versionName\n\n")
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

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = ExpressiveTokens.corners.extraLarge,
        icon = {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "About App"
            )
        },
        title = {
            Text(
                text = "About",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
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

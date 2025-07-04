package com.example.upitracker

import android.Manifest
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.biometric.BiometricPrompt
import android.animation.AnimatorListenerAdapter
import androidx.compose.foundation.layout.fillMaxSize
import android.animation.Animator
import android.animation.ObjectAnimator
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import android.content.Intent
import android.view.View
import android.view.animation.AnticipateInterpolator
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat // ✨ Import WindowCompat ✨
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.example.upitracker.data.AppDatabase
import com.example.upitracker.sms.SmsReceiver
import com.example.upitracker.sms.parseUpiLiteSummarySms
import com.example.upitracker.sms.parseUpiSms
import com.example.upitracker.ui.components.PinLockScreen
import com.example.upitracker.ui.screens.MainNavHost
import com.example.upitracker.ui.screens.OnboardingScreen
import com.example.upitracker.util.PinStorage
import com.example.upitracker.util.RegexPreference
import com.example.upitracker.util.Theme
import com.example.upitracker.viewmodel.MainViewModel
import com.example.upitracker.util.RestartUtil
import kotlinx.coroutines.Dispatchers
import com.example.upitracker.data.ArchivedSmsMessage
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.work.PeriodicWorkRequestBuilder
import com.example.upitracker.util.PermanentDeleteWorker // ✨ Import the new worker
import com.example.upitracker.util.RecurringTransactionWorker
import com.example.upitracker.util.BiometricHelper // ✨ Import BiometricHelper
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import com.example.upitracker.util.CleanupArchivedSmsWorker
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.TimeUnit
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import com.example.upitracker.util.CryptoManager

class MainActivity : FragmentActivity() {

    private var smsReceiver: SmsReceiver? = null
    private val mainViewModel: MainViewModel by viewModels()

    private val backupDatabaseLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
            uri?.let {
                // Persist write/read permission so this URI remains usable across reboots
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                mainViewModel.backupDatabase(it, contentResolver)
            }
        }
    private val restoreDatabaseLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                // Persist read/write permission so we can open this URI again if needed
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                mainViewModel.restoreDatabase(it, contentResolver)
            }
        }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (!isGranted) {
                mainViewModel.postSnackbarMessage("SMS permission is required to import transactions.")
            } else {
                importOldUpiSms()
            }
        }

    private val refreshArchivePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                performSmsArchiveRefresh()
            } else {
                mainViewModel.postSnackbarMessage("SMS permission is required to import transactions.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {

        CryptoManager.initialize(this)

        var keepSplashOnScreen = true
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Use a short delay to prevent the splash screen from disappearing too quickly.
        // In a real app, you might wait for initial data to load here.
        Handler(Looper.getMainLooper()).postDelayed({
            keepSplashOnScreen = false
        }, 500L) // A 500 millisecond delay

        // The exit animation listener remains the same.
        // It will now be triggered correctly after our condition becomes false.
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            val fadeOut = ObjectAnimator.ofFloat(
                splashScreenView.iconView,
                View.ALPHA,
                1f,
                0f
            )
            fadeOut.interpolator = AnticipateInterpolator()
            fadeOut.duration = 400L

            fadeOut.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    splashScreenView.remove()
                }
            })
            fadeOut.start()
        }

        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        scheduleArchivedSmsCleanup()
        schedulePermanentDeleteWorker()
        scheduleRecurringTransactionWorker()

        // ... (db, dao, smsReceiver setup remains the same)
        val db = AppDatabase.getDatabase(this); val liteDao = db.upiLiteSummaryDao()
        smsReceiver = SmsReceiver(
            onTransactionParsed = { transaction -> mainViewModel.processAndInsertTransaction(transaction) },
            onUpiLiteSummaryReceived = { newSummary ->
                lifecycleScope.launch {
                    val existingSummary = liteDao.getSummaryByDateAndBank(newSummary.date, newSummary.bank)
                    if (existingSummary == null) { liteDao.insert(newSummary)
                    } else {
                        if (existingSummary.transactionCount != newSummary.transactionCount || existingSummary.totalAmount != newSummary.totalAmount) {
                            liteDao.update(existingSummary.copy(transactionCount = newSummary.transactionCount, totalAmount = newSummary.totalAmount))
                        }
                    }
                }
            }
        )
        val filter = IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
        ContextCompat.registerReceiver(this, smsReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        setContent {
            Theme(mainViewModel = mainViewModel) {
                val snackbarHostState = remember { SnackbarHostState() }
                val coroutineScope = rememberCoroutineScope()

                var showRestartDialog by remember { mutableStateOf(false) }
                var restartDialogMessage by remember { mutableStateOf("") }

                LaunchedEffect(Unit) {
                    mainViewModel.snackbarEvents.collectLatest { snackbarMessageData ->
                        coroutineScope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = snackbarMessageData.message,
                                actionLabel = snackbarMessageData.actionLabel,
                                duration = SnackbarDuration.Short // Or .Long depending on importance
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                snackbarMessageData.onAction?.invoke()
                            } else if (result == SnackbarResult.Dismissed) {
                                snackbarMessageData.onDismiss?.invoke()
                            }
                        }
                    }
                }

                val onboardingCompleted by mainViewModel.isOnboardingCompleted.collectAsState()
                var pinUnlocked by rememberSaveable { mutableStateOf(false) }
                var pinIsActuallySet by rememberSaveable { mutableStateOf(false) }

                LaunchedEffect(onboardingCompleted, pinUnlocked) { // Re-check PIN status if onboarding or unlock status changes
                    if (onboardingCompleted) {
                        pinIsActuallySet = PinStorage.isPinSet(this@MainActivity)
                        if (!pinIsActuallySet && !pinUnlocked) { // If no PIN is set after onboarding, consider it "unlocked" for main app view
                            // Or, if PinLockScreen handles initial setup, this might not be needed
                            // Forcing initial setup if !pinIsActuallySet will be handled by PinLockScreen's internal logic
                        } else if (pinIsActuallySet && !pinUnlocked) {
                            // PIN is set but locked, do nothing here, PinLockScreen will handle
                        } else { // No PIN or PIN is set and unlocked
                            pinUnlocked = true
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    mainViewModel.uiEvents.collectLatest { event ->
                        when (event) {
                            // ✨ FIX: Add "MainViewModel." before UiEvent ✨
                            is MainViewModel.UiEvent.RestartRequired -> {
                                restartDialogMessage = event.message
                                showRestartDialog = true
                            }
                            is MainViewModel.UiEvent.ScrollToTop -> {
                                // Do nothing here
                            }
                        }
                    }
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    contentWindowInsets = WindowInsets(0)
                ) { innerPadding ->
                    if (!onboardingCompleted) {
                        OnboardingScreen(
                            modifier = Modifier.padding(innerPadding).fillMaxSize(),
                            // ✨ UPDATE the call to handle the new parameter ✨
                            onOnboardingComplete = { isUpiLiteEnabled ->
                                mainViewModel.markOnboardingComplete()
                                mainViewModel.setUpiLiteEnabled(isUpiLiteEnabled)
                            }
                        )
                    } else {
                        // Onboarding is complete, now handle PIN logic
                        if (!pinUnlocked && pinIsActuallySet) { // PIN is set AND app is locked
                            PinLockScreen(
                                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                                onUnlock = { pinUnlocked = true },
                                onSetPin = { // Called when PIN is set/changed from PinLockScreen
                                    coroutineScope.launch {
                                        pinIsActuallySet = PinStorage.isPinSet(this@MainActivity)
                                        pinUnlocked = true // Assume unlock after successful PIN set/change
                                    }
                                },
                                onAttemptBiometricUnlock = { // ✨ Handle biometric unlock attempt ✨
                                    if (BiometricHelper.isBiometricReady(this@MainActivity)) {
                                        val promptInfo = BiometricHelper.getPromptInfo(
                                            title = getString(R.string.biometric_prompt_title),
                                            subtitle = getString(R.string.biometric_prompt_subtitle),
                                            negativeButtonText = getString(R.string.biometric_prompt_use_pin) // Or "Cancel"
                                        )
                                        val biometricPrompt = BiometricHelper.getBiometricPrompt(
                                            activity = this@MainActivity, // Pass the Activity
                                            onAuthenticationSucceeded = {
                                                pinUnlocked = true // Unlock the app
                                            },
                                            onAuthenticationError = { errorCode, errString ->
                                                // Don't show PIN screen again if user cancelled or used negative button for PIN
                                                if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON && errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                                                    mainViewModel.postSnackbarMessage(getString(R.string.biometric_auth_error_generic, errString))
                                                }
                                            },
                                            onAuthenticationFailed = {
                                                mainViewModel.postSnackbarMessage(getString(R.string.biometric_auth_failed))
                                            }
                                        )
                                        biometricPrompt.authenticate(promptInfo)
                                    } else {
                                        mainViewModel.postSnackbarMessage(getString(R.string.biometric_not_available_or_enrolled))
                                    }
                                }
                            )
                        } else if (!pinIsActuallySet) { // No PIN set after onboarding, PinLockScreen handles setup mode
                            PinLockScreen(
                                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                                onUnlock = { /* Not expected */ },
                                onSetPin = {
                                    coroutineScope.launch {
                                        pinIsActuallySet = PinStorage.isPinSet(this@MainActivity)
                                        pinUnlocked = true
                                    }
                                },
                                onAttemptBiometricUnlock = { /* Biometrics typically for unlock, not initial setup */ }
                            )
                        }
                        else { // Onboarding complete, and ( (PIN is set AND unlocked) OR (no PIN set and initial setup handled) )
                            MainNavHost(
                                modifier = Modifier.padding(innerPadding),
                                onImportOldSms = { requestSmsPermissionAndImport() },
                                onRefreshSmsArchive = { requestSmsPermissionAndRefreshArchive() },
                                onBackupDatabase = { backupDatabaseLauncher.launch("upi_tracker_backup.db") },
                                onRestoreDatabase = { restoreDatabaseLauncher.launch(arrayOf("application/octet-stream")) },
                                mainViewModel = mainViewModel
                            )
                        }
                    }
                }
                if (showRestartDialog) {
                    ForceRestartDialog(
                        message = restartDialogMessage,
                        onConfirm = {
                            RestartUtil.restartApp(this@MainActivity)
                        }
                    )
                }
            }
        }
    }

    private fun scheduleArchivedSmsCleanup() {
        // Create a periodic work request to run once a day
        val cleanupRequest =
            PeriodicWorkRequestBuilder<CleanupArchivedSmsWorker>(1, TimeUnit.DAYS)
                // Optional: Add constraints like network type, charging, etc.
                // .setConstraints(Constraints.Builder().setRequiresCharging(true).build())
                .build()

        // Enqueue the work as unique periodic work
        // This ensures only one instance of this worker with this name is scheduled.
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            CleanupArchivedSmsWorker.WORK_NAME, // Unique name for the work
            ExistingPeriodicWorkPolicy.KEEP,    // Or REPLACE if you want to update it if it already exists
            cleanupRequest
        )
        Log.d("MainActivity", "Periodic cleanup worker for archived SMS scheduled.")
    }

    private fun requestSmsPermissionAndImport() {
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED -> {
                importOldUpiSms()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.READ_SMS)
            }
        }
    }

    // Add this new private function inside your MainActivity
    private fun processSmsInbox(
        setLoadingState: (Boolean) -> Unit,
        onComplete: (newTxnCount: Int, processedSummaries: Int, archivedCount: Int) -> Unit
    ) {
        setLoadingState(true)
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@MainActivity)
            val dao = db.transactionDao()
            val liteDao = db.upiLiteSummaryDao()
            val archivedSmsDao = db.archivedSmsMessageDao()

            val smsList = getAllSms()
            var newTxnCount = 0
            var processedSummaries = 0
            var archivedCount = 0

            val customRegexPatterns = RegexPreference.getRegexPatterns(this@MainActivity).firstOrNull()
                ?.mapNotNull { patternString ->
                    try {
                        Regex(patternString, RegexOption.IGNORE_CASE)
                    } catch (e: Exception) {
                        Log.w("MainActivityImport", "Skipping invalid regex: '$patternString'", e)
                        null
                    }
                } ?: emptyList()

            for ((sender, body, smsDate) in smsList) {
                var isUpiRelated = false

                parseUpiLiteSummarySms(body)?.let { summary ->
                    isUpiRelated = true
                    val existing = liteDao.getSummaryByDateAndBank(summary.date, summary.bank)
                    if (existing == null) {
                        liteDao.insert(summary)
                        processedSummaries++
                    } else if (existing.transactionCount != summary.transactionCount || existing.totalAmount != summary.totalAmount) {
                        liteDao.update(existing.copy(transactionCount = summary.transactionCount, totalAmount = summary.totalAmount))
                        processedSummaries++
                    }
                }

                parseUpiSms(body, sender, smsDate, customRegexPatterns)?.let { transaction ->
                    isUpiRelated = true
                    // Check if a transaction with the same core details already exists
                    if (dao.getTransactionByDetails(transaction.amount, transaction.date, transaction.description) == null) {
                        mainViewModel.processAndInsertTransaction(transaction)
                        newTxnCount++
                    }
                }

                if (isUpiRelated) {
                    val archivedSms = ArchivedSmsMessage(
                        originalSender = sender,
                        originalBody = body,
                        originalTimestamp = smsDate,
                        backupTimestamp = System.currentTimeMillis()
                    )
                    archivedSmsDao.insertArchivedSms(archivedSms)
                    archivedCount++
                }
            }

            withContext(Dispatchers.Main) {
                setLoadingState(false)
                onComplete(newTxnCount, processedSummaries, archivedCount)
            }
        }
    }

    private fun importOldUpiSms() {
        processSmsInbox(
            setLoadingState = { mainViewModel.setSmsImportingState(it) },
            onComplete = { txnCount, summaryCount, _ ->
                val message = if (txnCount > 0 || summaryCount > 0) {
                    "Imported $txnCount new transactions and processed $summaryCount UPI Lite summaries."
                } else {
                    "No new UPI transactions or Lite summaries found in old SMS."
                }
                mainViewModel.postSnackbarMessage(message)
            }
        )
    }

    private fun scheduleRecurringTransactionWorker() {
        // Run this check periodically (e.g., every 12 hours)
        val recurringRequest =
            PeriodicWorkRequestBuilder<RecurringTransactionWorker>(12, TimeUnit.HOURS)
                .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            RecurringTransactionWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            recurringRequest
        )
        Log.d("MainActivity", "Periodic recurring transaction worker scheduled.")
    }

    private suspend fun getAllSms(): List<Triple<String, String, Long>> = withContext(Dispatchers.IO) {
        val smsList = mutableListOf<Triple<String, String, Long>>()
        val uriSms = "content://sms/inbox".toUri()
        val projection = arrayOf("_id", "address", "date", "body")
        val sortOrder = "date DESC"

        try {
            contentResolver.query(uriSms, projection, null, null, sortOrder)?.use { cursor ->
                val addressIdx = cursor.getColumnIndexOrThrow("address") // Use getColumnIndexOrThrow for safety
                val bodyIdx = cursor.getColumnIndexOrThrow("body")
                val dateIdx = cursor.getColumnIndexOrThrow("date")
                while (cursor.moveToNext()) {
                    val address = cursor.getString(addressIdx) ?: ""
                    val body = cursor.getString(bodyIdx) ?: ""
                    val date = cursor.getLong(dateIdx)
                    smsList.add(Triple(address, body, date))
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivitySmsRead", "Error reading SMS", e)
            // Ensure mainViewModel is accessible here if not on Dispatchers.Main,
            // or pass the error message back to be shown on the main thread.
            // For simplicity, if postSnackbarMessage handles its own threading:
            mainViewModel.postSnackbarMessage("Error reading SMS: ${e.message ?: "Unknown error"}")
            // In case of error, smsList will be returned as is (potentially empty or partially filled)
        }
        smsList // This is the last expression in the withContext block, so it's implicitly returned.
    }

    private fun requestSmsPermissionAndRefreshArchive() { // ✨ New function ✨
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED -> {
                performSmsArchiveRefresh()
            }
            else -> {
                refreshArchivePermissionLauncher.launch(Manifest.permission.READ_SMS)
            }
        }
    }
    private fun performSmsArchiveRefresh() {
        processSmsInbox(
            setLoadingState = { mainViewModel.setIsRefreshingSmsArchive(it) },
            onComplete = { newTxns, newSummaries, totalArchived ->
                mainViewModel.postSnackbarMessage(
                    getString(
                        R.string.sms_archive_refreshed_message,
                        totalArchived,
                        newTxns,
                        newSummaries
                    )
                )
            }
        )
    }


    private fun schedulePermanentDeleteWorker() {
        val deleteRequest =
            PeriodicWorkRequestBuilder<PermanentDeleteWorker>(1, TimeUnit.DAYS)
                .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            PermanentDeleteWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            deleteRequest
        )
        Log.d("MainActivity", "Periodic permanent delete worker scheduled.")
    }

    override fun onDestroy() {
        super.onDestroy()
        smsReceiver?.let { unregisterReceiver(it) }
    }
}
@Composable
private fun ForceRestartDialog(message: String, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = { /* This dialog cannot be dismissed */ },
        icon = { Icon(Icons.Filled.SyncProblem, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(text = "Restore Successful") }, // Change title
        // Change the message to be more instructive
        // text = { Text(text = "Your data has been restored. Please manually close and reopen the app to see the changes.") },
        text = { Text(text = message) },
        confirmButton = {
            // Change the button to a simple "OK" that closes the app.
            Button(onClick = onConfirm) {
                Text(text = "OK, Close App")
            }
        }
    )
}
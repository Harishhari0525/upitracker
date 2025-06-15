package com.example.upitracker

import android.Manifest
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Telephony
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.WindowCompat // ✨ Import WindowCompat ✨
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.example.upitracker.data.AppDatabase
import com.example.upitracker.data.UpiLiteSummary
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
import kotlinx.coroutines.Dispatchers
import com.example.upitracker.data.ArchivedSmsMessage
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.upitracker.util.BiometricHelper // ✨ Import BiometricHelper
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.upitracker.util.CleanupArchivedSmsWorker
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.TimeUnit
import androidx.compose.material3.SnackbarResult

class MainActivity : FragmentActivity() {

    private var smsReceiver: SmsReceiver? = null
    private val mainViewModel: MainViewModel by viewModels()

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
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        scheduleArchivedSmsCleanup()

        // ... (db, dao, smsReceiver setup remains the same)
        val db = AppDatabase.getDatabase(this); val dao = db.transactionDao(); val liteDao = db.upiLiteSummaryDao()
        smsReceiver = SmsReceiver(
            onTransactionParsed = { transaction -> lifecycleScope.launch { dao.insert(transaction) } },
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
            val isDarkMode by mainViewModel.isDarkMode.collectAsState()
            Theme(darkTheme = isDarkMode) {
                val snackbarHostState = remember { SnackbarHostState() }
                val coroutineScope = rememberCoroutineScope()

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

                LaunchedEffect(pinIsActuallySet, onboardingCompleted) {
                    pinUnlocked = if (onboardingCompleted) {
                        !pinIsActuallySet // If onboarding is done, unlock if no PIN is set
                    } else {
                        false // If onboarding not done, assume locked (will show onboarding first)
                    }
                }
                Scaffold(
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    contentWindowInsets = WindowInsets(0)
                ) { innerPadding ->
                    if (!onboardingCompleted) {
                        OnboardingScreen(
                            modifier = Modifier.padding(innerPadding).fillMaxSize(),
                            onOnboardingComplete = { mainViewModel.markOnboardingComplete() }
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
                                mainViewModel = mainViewModel
                            )
                        }
                    }
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

    private fun importOldUpiSms() {
        val db = AppDatabase.getDatabase(this)
        val dao = db.transactionDao()
        val liteDao = db.upiLiteSummaryDao()
        val archivedSmsDao = db.archivedSmsMessageDao()

        mainViewModel.setSmsImportingState(true)
        lifecycleScope.launch(Dispatchers.IO) {
            val smsList = getAllSms()
            var txnCount = 0; var liteSummaryCount = 0
            var lastLiteSummaryForSnackbar: UpiLiteSummary? = null
            var customRegexPatterns: List<Regex> = emptyList()
            RegexPreference.getRegexPatterns(this@MainActivity).first().let { patternsSet ->
                customRegexPatterns = patternsSet.mapNotNull { patternString ->
                    try { Regex(patternString, RegexOption.IGNORE_CASE) }
                    catch (e: Exception) { Log.w("MainActivityImport", "Skipping invalid regex pattern: '$patternString'", e); null }
                }
            }
            for ((sender, body, smsDate) in smsList) {
                var isUpiRelatedForBackup = false // Flag for backup

                val liteSummary = parseUpiLiteSummarySms(body)
                if (liteSummary != null) {
                    isUpiRelatedForBackup = true // Mark as UPI related for backup
                    val existingSummary = liteDao.getSummaryByDateAndBank(liteSummary.date, liteSummary.bank)
                    if (existingSummary == null) {
                        liteDao.insert(liteSummary); liteSummaryCount++; lastLiteSummaryForSnackbar = liteSummary
                    } else {
                        if (existingSummary.transactionCount != liteSummary.transactionCount || existingSummary.totalAmount != liteSummary.totalAmount) {
                            val updatedSummary = existingSummary.copy(transactionCount = liteSummary.transactionCount, totalAmount = liteSummary.totalAmount)
                            liteDao.update(updatedSummary); lastLiteSummaryForSnackbar = updatedSummary
                        }
                    }
                    continue
                }
                val transaction = parseUpiSms(body, sender, smsDate, customRegexPatterns)
                if (transaction != null) {
                    isUpiRelatedForBackup = true // Mark as UPI related for backup
                    val exists = dao.getTransactionByDetails(transaction.amount, transaction.date, transaction.description)
                    if (exists == null) { dao.insert(transaction); txnCount++ }
                }
                if (isUpiRelatedForBackup) {
                    val archivedSms = ArchivedSmsMessage(
                        originalSender = sender,
                        originalBody = body,
                        originalTimestamp = smsDate,
                        backupTimestamp = System.currentTimeMillis()
                    )
                    archivedSmsDao.insertArchivedSms(archivedSms)
                    // Log.d("MainActivityImport", "SMS from $sender backed up during old import.") // Optional log
                }
            }
            withContext(Dispatchers.Main) {
                mainViewModel.setSmsImportingState(false)
                val snackbarDateFormat = SimpleDateFormat("dd MMM yy", Locale.getDefault())
                val liteMsgPart = lastLiteSummaryForSnackbar?.let {
                    val formattedDate = try { snackbarDateFormat.format(Date(it.date)) } catch (_:Exception) {"N/A"}
                    "\nLast UPI Lite: ${it.transactionCount} txns, ₹${"%.2f".format(it.totalAmount)} on $formattedDate"
                } ?: ""
                val mainMessage = if (txnCount > 0 || liteSummaryCount > 0) "Imported $txnCount new UPI SMS. Processed $liteSummaryCount UPI Lite summaries.$liteMsgPart"
                else "No new UPI transactions or Lite summaries found/updated in old SMS."
                mainViewModel.postSnackbarMessage(mainMessage)
            }
        }
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
    private fun performSmsArchiveRefresh() { // ✨ New function ✨
        // This function is essentially importOldUpiSms, but uses a different loading flag
        // and potentially different snackbar messages.
        val db = AppDatabase.getDatabase(this)
        val dao = db.transactionDao()
        val liteDao = db.upiLiteSummaryDao()
        val archivedSmsDao = db.archivedSmsMessageDao()

        mainViewModel.setIsRefreshingSmsArchive(true) // ✨ Use new loading state ✨

        lifecycleScope.launch(Dispatchers.IO) {
            val smsList = getAllSms() // getAllSms remains the same
            var transactionsProcessed = 0
            var summariesProcessed = 0
            var smsArchived = 0

            var customRegexPatterns: List<Regex> = emptyList()
            RegexPreference.getRegexPatterns(this@MainActivity).firstOrNull()?.let { patternsSet ->
                customRegexPatterns = patternsSet.mapNotNull { patternString ->
                    try {
                        Regex(patternString, RegexOption.IGNORE_CASE)
                    } catch (e: Exception) {
                        Log.w(
                            "MainActivityImport",
                            "Skipping invalid regex pattern: '$patternString'",
                            e
                        ); null
                    }
                }

                for ((sender, body, smsDate) in smsList) {
                    var needsArchiving = false
                    val liteSummary = parseUpiLiteSummarySms(body)
                    if (liteSummary != null) {
                        needsArchiving = true
                        val existingSummary =
                            liteDao.getSummaryByDateAndBank(liteSummary.date, liteSummary.bank)
                        if (existingSummary == null) {
                            liteDao.insert(liteSummary); summariesProcessed++
                        } else {
                            if (existingSummary.transactionCount != liteSummary.transactionCount || existingSummary.totalAmount != liteSummary.totalAmount) {
                                liteDao.update(
                                    existingSummary.copy(
                                        transactionCount = liteSummary.transactionCount,
                                        totalAmount = liteSummary.totalAmount
                                    )
                                )
                                summariesProcessed++ // Count updates as processed
                            }
                        }
                    }

                    val transaction = parseUpiSms(body, sender, smsDate, customRegexPatterns)
                    if (transaction != null) {
                        needsArchiving = true
                        val exists = dao.getTransactionByDetails(
                            transaction.amount,
                            transaction.date,
                            transaction.description
                        )
                        if (exists == null) {
                            dao.insert(transaction); transactionsProcessed++
                        }
                    }

                    if (needsArchiving) {
                        val archivedSms = ArchivedSmsMessage(
                            originalSender = sender,
                            originalBody = body,
                            originalTimestamp = smsDate,
                            backupTimestamp = System.currentTimeMillis()
                        )
                        // insertArchivedSms is suspend, ensure it's called within a coroutine or made non-suspend
                        // For now, assuming it's suspend and this IO scope is fine.
                        archivedSmsDao.insertArchivedSms(archivedSms)
                        smsArchived++
                    }
                }

                withContext(Dispatchers.Main) {
                    mainViewModel.setIsRefreshingSmsArchive(false) // ✨ Use new loading state ✨
                    // ✨ Different Snackbar message for archive refresh ✨
                    mainViewModel.postSnackbarMessage(
                        getString(
                            R.string.sms_archive_refreshed_message,
                            smsArchived,
                            transactionsProcessed,
                            summariesProcessed
                        ) // ✨ New String Resource
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        smsReceiver?.let { unregisterReceiver(it) }
    }
}

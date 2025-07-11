package com.example.upitracker

import android.Manifest
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Telephony
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.upitracker.data.AppDatabase
import com.example.upitracker.data.ArchivedSmsMessage
import com.example.upitracker.sms.SmsReceiver
import com.example.upitracker.sms.parseUpiLiteSummarySms
import com.example.upitracker.sms.parseUpiSms
import com.example.upitracker.ui.components.PinLockScreen
import com.example.upitracker.ui.screens.LottieSplashScreen
import com.example.upitracker.ui.screens.MainNavHost
import com.example.upitracker.ui.screens.OnboardingScreen
import com.example.upitracker.util.*
import com.example.upitracker.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import com.example.upitracker.network.GitHubRelease
import com.example.upitracker.network.UpdateService
import com.example.upitracker.ui.components.WhatsNewDialog

class MainActivity : FragmentActivity() {

    private var smsReceiver: SmsReceiver? = null
    private val mainViewModel: MainViewModel by viewModels()

    private val multiplePermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val readSmsGranted = permissions[Manifest.permission.READ_SMS] ?: false
            val receiveSmsGranted = permissions[Manifest.permission.RECEIVE_SMS] ?: false

            if (readSmsGranted && receiveSmsGranted) {
                importOldUpiSms()
            } else {
                mainViewModel.postSnackbarMessage("Both READ_SMS and RECEIVE_SMS permissions are required.")
            }
        }

    private val backupDatabaseLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
            uri?.let {
                contentResolver.takePersistableUriPermission(it, android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION or android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                mainViewModel.backupDatabase(it, contentResolver)
            }
        }

    private val restoreDatabaseLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                contentResolver.takePersistableUriPermission(it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                mainViewModel.restoreDatabase(it, contentResolver)
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Splash Screen Setup
        installSplashScreen().setKeepOnScreenCondition {
            !mainViewModel.isDataReady.value
        }

        // 2. App Initialization
        WindowCompat.setDecorFitsSystemWindows(window, false)
        CryptoManager.initialize(this)
        scheduleAllWorkers()
        registerSmsReceiver()
        NotificationHelper.createNotificationChannels(this)

        // 3. Automatic SMS Sync on App Resume
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                val lastTimestamp = mainViewModel.latestTransactionTimestamp.first()
                if (lastTimestamp > 0) {
                    val newSmsList = getAllSms(sinceTimestamp = lastTimestamp)
                    if (newSmsList.isNotEmpty()) {
                        processSmsInbox(
                            smsList = newSmsList,
                            setLoadingState = { /* Silent sync */ },
                            onComplete = { newTxnCount, _, _ ->
                                if (newTxnCount > 0) {
                                    mainViewModel.postSnackbarMessage("Found $newTxnCount new transaction(s).")
                                }
                            }
                        )
                    }
                }
            }
        }

        // 4. Setting UI Content
        setContent {
            var showLottieSplash by remember { mutableStateOf(true) }

            Theme(mainViewModel = mainViewModel) {
                if (showLottieSplash) {
                    LottieSplashScreen(onAnimationFinished = { showLottieSplash = false })
                } else {
                    MainAppContent()
                }
            }
        }
    }

    @Composable
    private fun MainAppContent() {
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
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) snackbarMessageData.onAction?.invoke()
                }
            }
        }

        val onboardingCompleted by mainViewModel.isOnboardingCompleted.collectAsState()
        var pinUnlocked by rememberSaveable { mutableStateOf(false) }
        var pinIsActuallySet by rememberSaveable { mutableStateOf(false) }
        var latestReleaseInfo by remember { mutableStateOf<GitHubRelease?>(null) }
        val context = LocalContext.current

        LaunchedEffect(onboardingCompleted, pinUnlocked) {
            if (onboardingCompleted) {
                pinIsActuallySet = PinStorage.isPinSet(this@MainActivity)
                if (!pinIsActuallySet) pinUnlocked = true
            }
        }

        LaunchedEffect(Unit) {
            mainViewModel.uiEvents.collectLatest { event ->
                if (event is MainViewModel.UiEvent.RestartRequired) {
                    restartDialogMessage = event.message
                    showRestartDialog = true
                }
            }
        }

        LaunchedEffect(Unit) {
            try {
                val currentVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName
                val lastSeenVersion = ThemePreference.getLastSeenVersionFlow(context).first()

                if (currentVersion != lastSeenVersion) {
                    // Fetch the release notes for the current version
                    val release = UpdateService.getLatestRelease()
                    // Make sure your GitHub release tag is "v" + versionName (e.g., "v1.7")
                    if (release?.tagName == "v$currentVersion") {
                        latestReleaseInfo = release
                    }
                }
            } catch (e: Exception) {
                // Handle exceptions, e.g., if package info is not found
                Log.e("VersionCheck", "Error checking for new version", e)
            }
        }


        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            if (!onboardingCompleted) {
                OnboardingScreen(
                    modifier = Modifier.padding(innerPadding).fillMaxSize(),
                    onOnboardingComplete = { isUpiLiteEnabled ->
                        mainViewModel.markOnboardingComplete()
                        mainViewModel.setUpiLiteEnabled(isUpiLiteEnabled)
                    }
                )
            } else {
                if (!pinUnlocked && pinIsActuallySet) {
                    PinLockScreen(
                        modifier = Modifier.padding(innerPadding).fillMaxSize(),
                        onUnlock = { pinUnlocked = true },
                        onSetPin = { coroutineScope.launch { pinIsActuallySet = PinStorage.isPinSet(this@MainActivity); pinUnlocked = true } },
                        onAttemptBiometricUnlock = {
                            if (BiometricHelper.isBiometricReady(this@MainActivity)) {
                                val promptInfo = BiometricHelper.getPromptInfo(
                                    title = getString(R.string.biometric_prompt_title),
                                    subtitle = getString(R.string.biometric_prompt_subtitle),
                                    negativeButtonText = getString(R.string.biometric_prompt_use_pin)
                                )
                                val biometricPrompt = BiometricHelper.getBiometricPrompt(
                                    activity = this@MainActivity,
                                    onAuthenticationSucceeded = {
                                        pinUnlocked = true // Unlock the app
                                    },
                                    onAuthenticationError = { errorCode, errString ->
                                        // Don't show PIN screen again if user cancelled
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
                } else if (!pinIsActuallySet) {
                    PinLockScreen(
                        modifier = Modifier.padding(innerPadding).fillMaxSize(),
                        onUnlock = { /* Not expected */ },
                        onSetPin = { coroutineScope.launch { pinIsActuallySet = PinStorage.isPinSet(this@MainActivity); pinUnlocked = true } },
                        onAttemptBiometricUnlock = {}
                    )
                }
                else {
                    MainNavHost(
                        modifier = Modifier.padding(innerPadding),
                        onImportOldSms = { requestSmsPermissionAndImport() },
                        onRefreshSmsArchive = { performSmsArchiveRefresh() },
                        onBackupDatabase = { backupDatabaseLauncher.launch("upi_tracker_backup.db") },
                        onRestoreDatabase = { restoreDatabaseLauncher.launch(arrayOf("application/octet-stream")) },
                        mainViewModel = mainViewModel
                    )
                }
            }
        }

        if (showRestartDialog) {
            ForceRestartDialog(message = restartDialogMessage, onConfirm = { RestartUtil.restartApp(this@MainActivity) })
        }

        if (latestReleaseInfo != null) {
            WhatsNewDialog(
                release = latestReleaseInfo!!,
                onDismiss = {
                    lifecycleScope.launch {
                        // Get the version name, which might be null
                        val currentVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName
                        // Only set the preference if the version name is not null
                        if (currentVersion != null) {
                            ThemePreference.setLastSeenVersion(context, currentVersion)
                        }
                    }
                    latestReleaseInfo = null
                }
            )
        }
    }

    private fun registerSmsReceiver() {
        smsReceiver = SmsReceiver(
            onTransactionParsed = { transaction -> mainViewModel.processAndInsertTransaction(transaction) },
            onUpiLiteSummaryReceived = { newSummary ->
                lifecycleScope.launch {
                    val liteDao = AppDatabase.getDatabase(this@MainActivity).upiLiteSummaryDao()
                    val existingSummary = liteDao.getSummaryByDateAndBank(newSummary.date, newSummary.bank)
                    if (existingSummary == null) {
                        liteDao.insert(newSummary)
                    } else if (existingSummary.transactionCount != newSummary.transactionCount || existingSummary.totalAmount != newSummary.totalAmount) {
                        liteDao.update(existingSummary.copy(transactionCount = newSummary.transactionCount, totalAmount = newSummary.totalAmount))
                    }
                }
            }
        )
        val filter = IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
        ContextCompat.registerReceiver(this, smsReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    private fun scheduleAllWorkers() {
        val workManager = WorkManager.getInstance(applicationContext)
        val budgetCheckRequest = PeriodicWorkRequestBuilder<BudgetCheckerWorker>(12, TimeUnit.HOURS).build()
        workManager.enqueueUniquePeriodicWork(BudgetCheckerWorker.WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, budgetCheckRequest)
        val deleteRequest = PeriodicWorkRequestBuilder<PermanentDeleteWorker>(1, TimeUnit.DAYS).build()
        workManager.enqueueUniquePeriodicWork(PermanentDeleteWorker.WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, deleteRequest)
        val recurringRequest = PeriodicWorkRequestBuilder<RecurringTransactionWorker>(12, TimeUnit.HOURS).build()
        workManager.enqueueUniquePeriodicWork(RecurringTransactionWorker.WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, recurringRequest)
        val cleanupRequest = PeriodicWorkRequestBuilder<CleanupArchivedSmsWorker>(1, TimeUnit.DAYS).build()
        workManager.enqueueUniquePeriodicWork(CleanupArchivedSmsWorker.WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, cleanupRequest)
    }

    private suspend fun getAllSms(sinceTimestamp: Long = 0L): List<Triple<String, String, Long>> = withContext(Dispatchers.IO) {
        val smsList = mutableListOf<Triple<String, String, Long>>()
        val uriSms = "content://sms/inbox".toUri()
        val projection = arrayOf("address", "date", "body")
        val selection = if (sinceTimestamp > 0) "date > ?" else null
        val selectionArgs = if (sinceTimestamp > 0) arrayOf(sinceTimestamp.toString()) else null

        try {
            contentResolver.query(uriSms, projection, selection, selectionArgs, "date DESC")?.use { cursor ->
                val addressIdx = cursor.getColumnIndexOrThrow("address")
                val bodyIdx = cursor.getColumnIndexOrThrow("body")
                val dateIdx = cursor.getColumnIndexOrThrow("date")
                while (cursor.moveToNext()) {
                    smsList.add(Triple(cursor.getString(addressIdx) ?: "", cursor.getString(bodyIdx) ?: "", cursor.getLong(dateIdx)))
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivitySmsRead", "Error reading SMS", e)
        }
        smsList
    }

    private fun processSmsInbox(
        smsList: List<Triple<String, String, Long>>,
        setLoadingState: (Boolean) -> Unit,
        onComplete: (newTxnCount: Int, processedSummaries: Int, archivedCount: Int) -> Unit
    ) {
        setLoadingState(true)
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@MainActivity)
            val dao = db.transactionDao()
            val liteDao = db.upiLiteSummaryDao()
            val archivedSmsDao = db.archivedSmsMessageDao()
            var newTxnCount = 0
            var processedSummaries = 0
            var archivedCount = 0

            val customRegexPatterns = RegexPreference.getRegexPatterns(this@MainActivity).firstOrNull()
                ?.mapNotNull { patternString -> try { Regex(patternString, RegexOption.IGNORE_CASE) } catch (_: Exception) { null } } ?: emptyList()

            for ((sender, body, smsDate) in smsList) {
                var isUpiRelated = false
                val bankName = getBankName(sender)

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

                parseUpiSms(body, sender, smsDate, customRegexPatterns, bankName)?.let { transaction ->
                    isUpiRelated = true
                    if (dao.getTransactionByDetails(transaction.amount, transaction.date, transaction.description) == null) {
                        mainViewModel.processAndInsertTransaction(transaction)
                        newTxnCount++
                    }
                }

                if (isUpiRelated) {
                    val archivedSms = ArchivedSmsMessage(originalSender = sender, originalBody = body, originalTimestamp = smsDate, backupTimestamp = System.currentTimeMillis())
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

    private fun getBankName(sender: String): String? {
        return when {
            sender.uppercase().contains("HDFC") -> "HDFC Bank"
            sender.uppercase().contains("ICICI") -> "ICICI Bank"
            sender.uppercase().contains("SBI") || sender.contains("SBIN") -> "State Bank of India"
            sender.uppercase().contains("AXIS") -> "Axis Bank"
            sender.uppercase().contains("KOTAK") -> "Kotak Mahindra Bank"
            // Add other banks as needed
            else -> null
        }
    }

    private fun importOldUpiSms() {
        lifecycleScope.launch {
            val allSms = getAllSms()
            processSmsInbox(
                smsList = allSms,
                setLoadingState = { mainViewModel.setSmsImportingState(it) },
                onComplete = { txnCount, summaryCount, _ ->
                    val message = if (txnCount > 0 || summaryCount > 0) "Imported $txnCount new transactions." else "No new transactions found."
                    mainViewModel.postSnackbarMessage(message)
                }
            )
        }
    }

    private fun performSmsArchiveRefresh() {
        lifecycleScope.launch {
            val allSms = getAllSms()
            processSmsInbox(
                smsList = allSms,
                setLoadingState = { mainViewModel.setIsRefreshingSmsArchive(it) },
                onComplete = { newTxns, newSummaries, totalArchived ->
                    mainViewModel.postSnackbarMessage(getString(R.string.sms_archive_refreshed_message, totalArchived, newTxns, newSummaries))
                }
            )
        }
    }

    private fun requestSmsPermissionAndImport() {
        val permissionsToRequest = arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
        multiplePermissionsLauncher.launch(permissionsToRequest)
    }

    override fun onDestroy() {
        super.onDestroy()
        smsReceiver?.let { unregisterReceiver(it) }
    }
}

@Composable
private fun ForceRestartDialog(message: String, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = { /* Cannot be dismissed */ },
        icon = { Icon(Icons.Default.SyncProblem, contentDescription = null) },
        title = { Text(text = "Restore Successful") },
        text = { Text(text = message) },
        confirmButton = { Button(onClick = onConfirm) { Text("OK, Close App") } }
    )
}
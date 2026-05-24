package com.example.upitracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.upitracker.data.AppDatabase
import com.example.upitracker.network.GitHubRelease
import com.example.upitracker.network.UpdateService
import com.example.upitracker.ui.components.AddTransactionDialog
import com.example.upitracker.ui.components.PinLockScreen
import com.example.upitracker.ui.components.WhatsNewDialog
import com.example.upitracker.ui.screens.LottieSplashScreen
import com.example.upitracker.ui.screens.MainNavHost
import com.example.upitracker.ui.screens.OnboardingScreen
import com.example.upitracker.util.*
import com.example.upitracker.viewmodel.MainViewModel
import com.example.upitracker.sms.SmsProcessingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class MainActivity : FragmentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    private var pendingUpiLiteEnabledState: Boolean = true

    // The single, trusted class-level launcher for tracking structural runtime permission rules
    private val smsPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val readSmsGranted = permissions[Manifest.permission.READ_SMS] ?: false
            val receiveSmsGranted = permissions[Manifest.permission.RECEIVE_SMS] ?: false
            val bothGranted = readSmsGranted && receiveSmsGranted

            if (!mainViewModel.isOnboardingCompleted.value) {
                mainViewModel.setUpiLiteEnabled(pendingUpiLiteEnabledState)
                mainViewModel.markOnboardingComplete()

                if (bothGranted) {
                    startFullSmsSync(isInitialImport = true)
                } else {
                    mainViewModel.postSnackbarMessage(
                        "SMS permission not granted. You can enable it later from Data & Sync."
                    )
                }
            } else {
                if (bothGranted) {
                    startFullSmsSync(isInitialImport = false)
                } else {
                    mainViewModel.postSnackbarMessage(
                        "Both READ_SMS and RECEIVE_SMS permissions are required."
                    )
                }
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

    private fun hasSmsPermissions(): Boolean {
        val readSmsGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        val receiveSmsGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED

        return readSmsGranted && receiveSmsGranted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen().setKeepOnScreenCondition { !mainViewModel.isDataReady.value }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        CryptoManager.initialize(this)
        NotificationHelper.createNotificationChannels(this)
        scheduleAllWorkers()

        setContent {
            Theme(mainViewModel = mainViewModel) {
                var showLottieSplash by remember { mutableStateOf(true) }

                // Enforced status bars handling padding right at structural layout root
                Surface(
                    modifier = Modifier
                        .fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Crossfade(
                        targetState = showLottieSplash,
                        animationSpec = tween(durationMillis = 350),
                        label = "SplashToHomeTransition"
                    ) { isSplashActive ->
                        if (isSplashActive) {
                            LottieSplashScreen(
                                onAnimationFinished = { showLottieSplash = false }
                            )
                        } else {
                            MainAppRouter()
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun MainAppRouter() {
        val onboardingCompleted by mainViewModel.isOnboardingCompleted.collectAsState()
        var pinUnlocked by rememberSaveable { mutableStateOf(false) }

        val pinIsActuallySet by produceState<Boolean?>(initialValue = null) {
            try {
                value = PinStorage.isPinSet(this@MainActivity)
            } catch (e: Exception) {
                Log.e("MainActivity", "Error reading PIN state", e)
                value = false
            }
        }

        val isDataReady by mainViewModel.isDataReady.collectAsState()

        LaunchedEffect(pinIsActuallySet) {
            if (pinIsActuallySet == false) {
                pinUnlocked = true
            }
        }

        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (!isDataReady) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator()
                }
            } else {
                when {
                    !onboardingCompleted -> {
                        OnboardingScreen(
                            modifier = Modifier.fillMaxSize(),
                            onOnboardingComplete = { isUpiLiteEnabled ->
                                pendingUpiLiteEnabledState = isUpiLiteEnabled

                                if (hasSmsPermissions()) {
                                    mainViewModel.setUpiLiteEnabled(isUpiLiteEnabled)
                                    mainViewModel.markOnboardingComplete()
                                    startFullSmsSync(isInitialImport = true)
                                } else {
                                    smsPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.READ_SMS,
                                            Manifest.permission.RECEIVE_SMS
                                        )
                                    )
                                }
                            }
                        )
                    }

                    pinIsActuallySet == null -> {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            CircularProgressIndicator()
                        }
                    }

                    !pinUnlocked && pinIsActuallySet == true -> {
                        PinLockScreen(
                            modifier = Modifier.fillMaxSize(),
                            onUnlock = { pinUnlocked = true },
                            onSetPin = { pinUnlocked = true },
                            onAttemptBiometricUnlock = {
                                handleBiometricUnlock { pinUnlocked = true }
                            }
                        )
                    }

                    else -> {
                        MainAppScreen()
                    }
                }
            }
        }
    }

    @Composable
    private fun MainAppScreen() {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }
        val rootNavController = rememberNavController()
        var latestReleaseInfo by remember { mutableStateOf<GitHubRelease?>(null) }
        var showRestartDialog by remember { mutableStateOf(false) }
        var restartDialogMessage by remember { mutableStateOf("") }
        var showAddTransactionDialog by remember { mutableStateOf(false) }

        // Core snackbar monitoring pipeline hook
        LaunchedEffect(Unit) {
            mainViewModel.snackbarEvents.collectLatest { snackbarMessageData ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = snackbarMessageData.message,
                        actionLabel = snackbarMessageData.actionLabel,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }

        // Processing incremental delta sync runs contextually ONLY when permission verification passes cleanly
        LaunchedEffect(Unit) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                val db = AppDatabase.getDatabase(context)
                val lastTimestamp = withContext(Dispatchers.IO) {
                    db.transactionDao().getLatestTransactionTimestampIncludingArchived() ?: 0L
                }
                if (lastTimestamp > 0) {
                    val newSmsList = getAllSms(sinceTimestamp = lastTimestamp)
                    if (newSmsList.isNotEmpty()) {
                        processSmsInbox(
                            smsList = newSmsList,
                            setLoadingState = { /* Silent */ },
                            onComplete = { newTxnCount, summaryCount, _ ->
                                val isUpiLiteEnabled = mainViewModel.isUpiLiteEnabled.value
                                val messageParts = mutableListOf<String>()

                                if (newTxnCount > 0) {
                                    messageParts.add("$newTxnCount new transaction(s)")
                                }
                                if (isUpiLiteEnabled && summaryCount > 0) {
                                    messageParts.add("$summaryCount new UPI Lite summary(s)")
                                }
                                if (messageParts.isNotEmpty()) {
                                    val message = "Silently synced " + messageParts.joinToString(" and ") + "."
                                    mainViewModel.postSnackbarMessage(message)
                                }
                            }
                        )
                    }
                }
            } else {
                Log.d("MainActivity", "Skipping background incremental check: permissions absent or loading onboarding pathways.")
            }

            try {
                val currentVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName
                val lastSeenVersion = ThemePreference.getLastSeenVersionFlow(context).first()
                if (currentVersion != null && currentVersion != lastSeenVersion) {
                    val release = UpdateService.getLatestRelease()
                    if (release?.tagName == "v$currentVersion") {
                        latestReleaseInfo = release
                    }
                }
            } catch (e: Exception) {
                Log.e("VersionCheck", "Error checking for new version", e)
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

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            MainNavHost(
                rootNavController = rootNavController,
                modifier = Modifier.padding(innerPadding),
                onImportOldSms = { requestSmsPermissionAndThenSync(isInitialImport = true) },
                onRefreshSmsArchive = { requestSmsPermissionAndThenSync(isInitialImport = false) },
                onBackupDatabase = { backupDatabaseLauncher.launch("upi_tracker_backup.db") },
                onRestoreDatabase = { restoreDatabaseLauncher.launch(arrayOf("application/octet-stream")) },
                onShowAddTransactionDialog = { showAddTransactionDialog = true },
                mainViewModel = mainViewModel
            )
        }

        if (showAddTransactionDialog) {
            AddTransactionDialog(
                onDismiss = { showAddTransactionDialog = false },
                onConfirm = { amount, type, description, category, date ->
                    mainViewModel.addManualTransaction(amount, type, description, category, date)
                    showAddTransactionDialog = false
                }
            )
        }

        if (latestReleaseInfo != null) {
            WhatsNewDialog(
                release = latestReleaseInfo!!,
                onDismiss = {
                    coroutineScope.launch {
                        val currentVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName
                        if (currentVersion != null) ThemePreference.setLastSeenVersion(context, currentVersion)
                    }
                    latestReleaseInfo = null
                }
            )
        }

        if (showRestartDialog) {
            ForceRestartDialog(message = restartDialogMessage, onConfirm = { RestartUtil.restartApp(this@MainActivity) })
        }
    }

    private fun handleBiometricUnlock(onSuccess: () -> Unit) {
        if (BiometricHelper.isBiometricReady(this@MainActivity)) {
            val promptInfo = BiometricHelper.getPromptInfo(
                title = getString(R.string.biometric_prompt_title),
                subtitle = getString(R.string.biometric_prompt_subtitle),
                negativeButtonText = getString(R.string.biometric_prompt_use_pin)
            )
            val biometricPrompt = BiometricHelper.getBiometricPrompt(
                activity = this@MainActivity,
                onAuthenticationSucceeded = { onSuccess() },
                onAuthenticationError = { errorCode, errString ->
                    if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON && errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                        mainViewModel.postSnackbarMessage(getString(R.string.biometric_auth_error_generic, errString))
                    }
                },
                onAuthenticationFailed = { mainViewModel.postSnackbarMessage(getString(R.string.biometric_auth_failed)) }
            )
            biometricPrompt.authenticate(promptInfo)
        } else {
            mainViewModel.postSnackbarMessage(getString(R.string.biometric_not_available_or_enrolled))
        }
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

        val updateCheckRequest = PeriodicWorkRequestBuilder<UpdateCheckWorker>(12, TimeUnit.HOURS)
            .setConstraints(androidx.work.Constraints.Builder().setRequiredNetworkType(androidx.work.NetworkType.CONNECTED).build())
            .build()
        workManager.enqueueUniquePeriodicWork(UpdateCheckWorker.WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, updateCheckRequest)
    }

    private suspend fun getAllSms(sinceTimestamp: Long = 0L): List<Triple<String, String, Long>> = withContext(Dispatchers.IO) {
        val smsList = mutableListOf<Triple<String, String, Long>>()

        val hasPermission = ContextCompat.checkSelfPermission(
            this@MainActivity,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            Log.w("MainActivitySmsRead", "Skipping history import sync loop: READ_SMS permission not granted yet.")
            return@withContext smsList
        }

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
                    smsList.add(
                        Triple(
                            cursor.getString(addressIdx) ?: "",
                            cursor.getString(bodyIdx) ?: "",
                            cursor.getLong(dateIdx)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivitySmsRead", "Error reading SMS from ContentResolver provider", e)
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
            val result = SmsProcessingService.processSmsBatch(
                context = this@MainActivity,
                smsList = smsList,
                updateWidget = false
            )

            withContext(Dispatchers.Main) {
                setLoadingState(false)
                onComplete(
                    result.newTxnCount,
                    result.processedSummaries,
                    result.archivedCount
                )
            }
        }
    }

    private fun startFullSmsSync(isInitialImport: Boolean) {
        if (mainViewModel.isImportingSms.value || mainViewModel.isRefreshingSmsArchive.value) {
            mainViewModel.postSnackbarMessage("Sync is already in progress.")
            return
        }

        lifecycleScope.launch {
            val allSms = getAllSms()
            val loadingStateSetter = if (isInitialImport) mainViewModel::setSmsImportingState else mainViewModel::setIsRefreshingSmsArchive
            val onCompleteMessage = if (isInitialImport) "Import" else "Refresh"

            processSmsInbox(
                smsList = allSms,
                setLoadingState = loadingStateSetter,
                onComplete = { txnCount, summaryCount, _ ->
                    val isUpiLiteEnabled = mainViewModel.isUpiLiteEnabled.value
                    val messageParts = mutableListOf<String>()

                    if (txnCount > 0) {
                        messageParts.add("$txnCount new transaction(s)")
                    }
                    if (isUpiLiteEnabled && summaryCount > 0) {
                        messageParts.add("$summaryCount UPI Lite summary(s)")
                    }
                    val finalMessage = if (messageParts.isEmpty()) {
                        "$onCompleteMessage complete: No new items found."
                    } else {
                        "$onCompleteMessage complete: Found " + messageParts.joinToString(" and ") + "."
                    }
                    mainViewModel.postSnackbarMessage(finalMessage)
                }
            )
        }
    }

    private fun requestSmsPermissionAndThenSync(isInitialImport: Boolean) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED) {
            startFullSmsSync(isInitialImport = isInitialImport)
        } else {
            val permissionsToRequest = arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
            smsPermissionLauncher.launch(permissionsToRequest)
        }
    }
}

@Composable
private fun ForceRestartDialog(message: String, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = { },
        icon = { Icon(Icons.Default.SyncProblem, contentDescription = null) },
        title = { Text(text = "Restore Successful") },
        text = { Text(text = message) },
        confirmButton = { Button(onClick = onConfirm) { Text("OK, Close App") } }
    )
}
package com.example.upitracker

import android.Manifest
import android.content.Intent
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
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
import com.example.upitracker.ui.components.PremiumFloatingSnackbarHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

class MainActivity : FragmentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private var pendingBackupPassword = ""
    private var pendingRestorePassword = ""
    private var autoLockJob: Job? = null

    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
            autoLockJob?.cancel()
            autoLockJob = lifecycleScope.launch {
                delay(mainViewModel.autoLockDelay.value.milliseconds)
                mainViewModel.setPinUnlocked(false)
            }
        }

        override fun onStart(owner: LifecycleOwner) {
            autoLockJob?.cancel()
        }
    }

    private var pendingUpiLiteEnabledState: Boolean = true

    // The single, trusted class-level launcher for tracking structural runtime permission rules
    private val smsPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val readSmsGranted = permissions[Manifest.permission.READ_SMS] ?: false
            val receiveSmsGranted = permissions[Manifest.permission.RECEIVE_SMS] ?: false
            val postNotificationsGranted = permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false
            val bothGranted = readSmsGranted && receiveSmsGranted

            Log.d("Permissions", "READ_SMS: $readSmsGranted, RECEIVE_SMS: $receiveSmsGranted, POST_NOTIFICATIONS: $postNotificationsGranted")

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
                contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                mainViewModel.backupDatabase(it, contentResolver, pendingBackupPassword)
                pendingBackupPassword = ""
            }
        }

    private val restoreDatabaseLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                mainViewModel.restoreDatabase(it, contentResolver, pendingRestorePassword)
                pendingRestorePassword = ""
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
        if (!isTaskRoot && intent.hasCategory(Intent.CATEGORY_LAUNCHER) && intent.action == Intent.ACTION_MAIN) {
            super.onCreate(savedInstanceState)
            finish()
            return
        }
        super.onCreate(savedInstanceState)
        installSplashScreen().setKeepOnScreenCondition { !mainViewModel.isDataReady.value }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        CryptoManager.initialize(this)
        NotificationHelper.createNotificationChannels(this)
        scheduleAllWorkers()

        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)

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
        val pinUnlocked by mainViewModel.pinUnlocked.collectAsState()

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
                mainViewModel.setPinUnlocked(true)
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

                                val permissionsToRequest = mutableListOf(
                                    Manifest.permission.READ_SMS,
                                    Manifest.permission.RECEIVE_SMS
                                )
                                if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                    permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                                }

                                if (hasSmsPermissions()) {
                                    mainViewModel.setUpiLiteEnabled(isUpiLiteEnabled)
                                    mainViewModel.markOnboardingComplete()
                                    if (permissionsToRequest.contains(Manifest.permission.POST_NOTIFICATIONS)) {
                                        smsPermissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                                    }
                                    startFullSmsSync(isInitialImport = true)
                                } else {
                                    smsPermissionLauncher.launch(permissionsToRequest.toTypedArray())
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
                            onUnlock = { mainViewModel.setPinUnlocked(true) },
                            onSetPin = { mainViewModel.setPinUnlocked(true) },
                            onAttemptBiometricUnlock = {
                                lifecycleScope.launch {
                                    val lockoutUntil = PinStorage.getPinLockoutUntil(this@MainActivity)
                                    if (lockoutUntil <= System.currentTimeMillis()) {
                                        handleBiometricUnlock { mainViewModel.setPinUnlocked(true) }
                                    } else {
                                        mainViewModel.postSnackbarMessage("App is locked out. Please wait.")
                                    }
                                }
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
        var showAutoRuleDialog by remember { mutableStateOf(false) }
        var recommendedMerchant by remember { mutableStateOf("") }
        var recommendedCategory by remember { mutableStateOf("") }

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
                    // Safe incremental background sync execution gated via startFullSmsSync to prevent concurrent thread collisions
                    startFullSmsSync(isInitialImport = false)
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
                when (event) {
                    is MainViewModel.UiEvent.RestartRequired -> {
                        restartDialogMessage = event.message
                        showRestartDialog = true
                    }
                    is MainViewModel.UiEvent.AutoRuleRecommendation -> {
                        recommendedMerchant = event.merchant
                        recommendedCategory = event.category
                        showAutoRuleDialog = true
                    }
                    else -> {}
                }
            }
        }

        Scaffold(
            snackbarHost = {
                PremiumFloatingSnackbarHost(
                    hostState = snackbarHostState
                ) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            MainNavHost(
                rootNavController = rootNavController,
                modifier = Modifier.padding(innerPadding),
                onImportOldSms = { requestSmsPermissionAndThenSync(isInitialImport = true) },
                onRefreshSmsArchive = { requestSmsPermissionAndThenSync(isInitialImport = false) },
                onBackupDatabase = { password ->
                    pendingBackupPassword = password
                    backupDatabaseLauncher.launch(if (password.isBlank()) "upi_tracker_backup.db" else "upi_tracker_portable.upibak")
                },
                onRestoreDatabase = { password ->
                    pendingRestorePassword = password
                    restoreDatabaseLauncher.launch(arrayOf("application/octet-stream", "application/x-upitracker-backup"))
                },
                onShowAddTransactionDialog = { showAddTransactionDialog = true },
                mainViewModel = mainViewModel
            )
        }

        if (showAddTransactionDialog) {
            val allCategories by mainViewModel.allCategories.collectAsState()
            val frequentCategories by mainViewModel.userCategories.collectAsState()
            AddTransactionDialog(
                userCategories = allCategories,
                initialCategory = frequentCategories.firstOrNull()?.name.orEmpty(),
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

        if (showAutoRuleDialog) {
            AlertDialog(
                onDismissRequest = { showAutoRuleDialog = false },
                title = { Text("Create Auto-Categorization Rule?") },
                text = { Text("We noticed you categorized transactions from \"$recommendedMerchant\" as \"$recommendedCategory\" multiple times.\n\nWould you like to automatically categorize all transactions from \"$recommendedMerchant\" as \"$recommendedCategory\" in the future?") },
                confirmButton = {
                    Button(onClick = {
                        mainViewModel.createAutoRule(recommendedMerchant, recommendedCategory)
                        showAutoRuleDialog = false
                    }) {
                        Text("Create Rule")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAutoRuleDialog = false }) {
                        Text("No, Thanks")
                    }
                }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleObserver)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error removing lifecycle observer", e)
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
        BudgetCheckerWorker.enqueue(applicationContext)

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
        
        // ✨ Schedule Monthly Statement Worker
        val statementRequest = PeriodicWorkRequestBuilder<MonthlyStatementWorker>(1, TimeUnit.DAYS).build()
        workManager.enqueueUniquePeriodicWork(MonthlyStatementWorker.WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, statementRequest)
    }

    private data class SmsPage(
        val messages: List<Triple<String, String, Long>>,
        val lastTimestamp: Long,
        val lastId: Long
    )

    private suspend fun countSmsForImport(fromTimestamp: Long): Int = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            return@withContext 0
        }
        contentResolver.query(
            "content://sms/inbox".toUri(),
            arrayOf("_id"),
            "date >= ?",
            arrayOf(fromTimestamp.toString()),
            null
        )?.use { it.count } ?: 0
    }

    private suspend fun readSmsBatch(
        afterTimestamp: Long,
        afterId: Long,
        limit: Int
    ): SmsPage = withContext(Dispatchers.IO) {
        val batch = ArrayList<Triple<String, String, Long>>(limit)
        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            return@withContext SmsPage(batch, afterTimestamp, afterId)
        }
        var lastTimestamp = afterTimestamp
        var lastId = afterId
        contentResolver.query(
            "content://sms/inbox".toUri(),
            arrayOf("_id", "address", "date", "body"),
            "date > ? OR (date = ? AND _id > ?)",
            arrayOf(afterTimestamp.toString(), afterTimestamp.toString(), afterId.toString()),
            "date ASC, _id ASC"
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow("_id")
            val addressIdx = cursor.getColumnIndexOrThrow("address")
            val bodyIdx = cursor.getColumnIndexOrThrow("body")
            val dateIdx = cursor.getColumnIndexOrThrow("date")
            while (batch.size < limit && cursor.moveToNext()) {
                lastId = cursor.getLong(idIdx)
                lastTimestamp = cursor.getLong(dateIdx)
                batch.add(Triple(cursor.getString(addressIdx).orEmpty(), cursor.getString(bodyIdx).orEmpty(), cursor.getLong(dateIdx)))
            }
        }
        SmsPage(batch, lastTimestamp, lastId)
    }

    private fun processSmsInbox(
        smsList: List<Triple<String, String, Long>>,
        isInitialImport: Boolean, // Pass context down to tell if this is a fresh database seed pass
        setLoadingState: (Boolean) -> Unit,
        onComplete: (newTxnCount: Int, processedSummaries: Int, archivedCount: Int) -> Unit
    ) {
        setLoadingState(true)

        lifecycleScope.launch(Dispatchers.IO) {
            val totalMessages = smsList.size
            var globalNewTxn = 0
            var globalSummaries = 0
            var globalArchived = 0

            if (totalMessages == 0) {
                withContext(Dispatchers.Main) {
                    mainViewModel.clearSmsSyncProgress()
                    setLoadingState(false)
                    onComplete(0, 0, 0)
                }
                return@launch
            }

            val config = SmsProcessingService.fetchProcessingConfig(this@MainActivity)

            // Chunk messages into sub-batches of 50 to allow the main thread UI state definitions to breathe
            val chunkSize = 50
            val chunks = smsList.chunked(chunkSize)
            var processedCount = 0

            for (chunk in chunks) {
                // Execute database writing pass for the current chunk
                val result = SmsProcessingService.processSmsBatch(
                    context = this@MainActivity,
                    smsList = chunk,
                    updateWidget = false,
                    config = config
                )

                globalNewTxn += result.newTxnCount
                globalSummaries += result.processedSummaries
                globalArchived += result.archivedCount
                processedCount += chunk.size

                // ✨ Update last processed timestamp to avoid re-scanning these in the future
                val latestInBatch = chunk.maxOf { it.third }
                ThemePreference.setLastProcessedSmsTimestamp(this@MainActivity, latestInBatch)

                // ✨ PROGRESS ACCUMULATION HOOK: Stream updated counts straight to our state engine flow parameters
                withContext(Dispatchers.Main) {
                    mainViewModel.updateSmsSyncProgress(
                        current = processedCount.coerceAtMost(totalMessages),
                        total = totalMessages,
                        isInitial = isInitialImport
                    )
                }
            }

            withContext(Dispatchers.Main) {
                // ✨ TERMINATION HOOK: Flush and reset states once all batch passes complete successfully
                mainViewModel.clearSmsSyncProgress()
                setLoadingState(false)
                onComplete(globalNewTxn, globalSummaries, globalArchived)
            }
        }
    }

    private fun startFullSmsSync(isInitialImport: Boolean) {
        if (mainViewModel.isImportingSms.value || mainViewModel.isRefreshingSmsArchive.value) {
            mainViewModel.postSnackbarMessage("Sync is already in progress.")
            return
        }

        lifecycleScope.launch {
            // ✨ Logic Fix: Use last processed timestamp with a 7-day lookback for incremental sync, or 0 for initial/full refresh
            val lastProcessed = if (isInitialImport) {
                0L
            } else {
                val lastTimestamp = ThemePreference.getLastProcessedSmsTimestampFlow(this@MainActivity).first()
                if (lastTimestamp > 0L) {
                    (lastTimestamp - 7 * 24 * 60 * 60 * 1000L).coerceAtLeast(0L)
                } else {
                    0L
                }
            }
            
            val loadingStateSetter = if (isInitialImport) mainViewModel::setSmsImportingState else mainViewModel::setIsRefreshingSmsArchive
            val onCompleteMessage = if (isInitialImport) "Import" else "Refresh"

            loadingStateSetter(true)
            lifecycleScope.launch(Dispatchers.IO) {
                var processedCount = 0
                var cursorTimestamp = lastProcessed
                var cursorId = -1L
                var txnCount = 0
                var summaryCount = 0
                var archivedCount = 0
                val pageSize = 50
                val config = SmsProcessingService.fetchProcessingConfig(this@MainActivity)
                val totalMessages = countSmsForImport(lastProcessed)
                try {
                    var batch: List<Triple<String, String, Long>>
                    do {
                        val page = readSmsBatch(cursorTimestamp, cursorId, pageSize)
                        batch = page.messages
                        if (batch.isNotEmpty()) {
                            val result = SmsProcessingService.processSmsBatch(
                                this@MainActivity, batch, updateWidget = false, config = config
                            )
                            txnCount += result.newTxnCount
                            summaryCount += result.processedSummaries
                            archivedCount += result.archivedCount
                            processedCount += batch.size
                            cursorTimestamp = page.lastTimestamp
                            cursorId = page.lastId
                            ThemePreference.setLastProcessedSmsTimestamp(this@MainActivity, batch.last().third)
                            withContext(Dispatchers.Main) {
                                mainViewModel.updateSmsSyncProgress(
                                    current = processedCount.coerceAtMost(totalMessages),
                                    total = totalMessages,
                                    isInitial = isInitialImport
                                )
                            }
                        }
                    } while (batch.size == pageSize)
                } catch (e: Exception) {
                    Log.e("MainActivitySmsRead", "Error processing paged SMS import", e)
                    withContext(Dispatchers.Main) {
                        mainViewModel.postSnackbarMessage("SMS sync stopped because an error occurred.")
                    }
                } finally {
                    withContext(Dispatchers.Main) {
                        mainViewModel.clearSmsSyncProgress()
                        loadingStateSetter(false)
                    }
                }
                withContext(Dispatchers.Main) {
                    lifecycleScope.launch {
                        ThemePreference.setLastSyncExecutionTimestamp(this@MainActivity, System.currentTimeMillis())
                    }
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
            }
        }
    }

    private fun requestSmsPermissionAndThenSync(isInitialImport: Boolean) {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS
        )
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED) {
            
            if (permissionsToRequest.contains(Manifest.permission.POST_NOTIFICATIONS)) {
                smsPermissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            }
            startFullSmsSync(isInitialImport = isInitialImport)
        } else {
            smsPermissionLauncher.launch(permissionsToRequest.toTypedArray())
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

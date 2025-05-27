package com.example.upitracker

import android.Manifest
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Telephony
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.WindowCompat // ✨ Import WindowCompat ✨
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✨ Enable edge-to-edge display BEFORE setContent ✨
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val db = AppDatabase.getDatabase(this)
        val dao = db.transactionDao()
        val liteDao = db.upiLiteSummaryDao()

        smsReceiver = SmsReceiver(
            onTransactionParsed = { transaction ->
                lifecycleScope.launch { dao.insert(transaction) }
            },
            onUpiLiteSummaryReceived = { newSummary ->
                lifecycleScope.launch {
                    val existingSummary = liteDao.getSummaryByDateAndBank(newSummary.date, newSummary.bank)
                    if (existingSummary == null) {
                        liteDao.insert(newSummary)
                    } else {
                        if (existingSummary.transactionCount != newSummary.transactionCount || existingSummary.totalAmount != newSummary.totalAmount) {
                            val updatedSummary = existingSummary.copy(
                                transactionCount = newSummary.transactionCount,
                                totalAmount = newSummary.totalAmount
                            )
                            liteDao.update(updatedSummary)
                        }
                    }
                }
            }
        )
        val filter = IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
        ContextCompat.registerReceiver(this, smsReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        setContent {
            val isDarkMode by mainViewModel.isDarkMode.collectAsState()
            Theme(darkTheme = isDarkMode) { // Theme now handles status bar color and icon appearance
                val snackbarHostState = remember { SnackbarHostState() }
                val coroutineScope = rememberCoroutineScope()

                LaunchedEffect(Unit) {
                    mainViewModel.snackbarEvents.collectLatest { message ->
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = message,
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                }

                val onboardingCompleted by mainViewModel.isOnboardingCompleted.collectAsState()
                var pinUnlocked by rememberSaveable { mutableStateOf(false) }
                var pinIsActuallySet by rememberSaveable { mutableStateOf(false) }

                LaunchedEffect(onboardingCompleted) {
                    if (onboardingCompleted) {
                        pinIsActuallySet = PinStorage.isPinSet(this@MainActivity)
                        pinUnlocked = !pinIsActuallySet
                    }
                }

                Scaffold(
                    // This Scaffold is now mainly for the SnackbarHost.
                    // The modifier.padding(innerPadding) will be passed to MainNavHost,
                    // but MainNavHost's children (like MainAppScreen) will handle their own insets
                    // if they have TopAppBars/BottomNavBars drawing in system bar areas.
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    // Set container color to transparent if you want content below to handle all backgrounds
                    // when edge-to-edge. This might be needed if MainActivity's Scaffold was opaque.
                    // containerColor = Color.Transparent
                ) { innerPadding -> // This padding is from MainActivity's Scaffold (mostly for Snackbar)
                    if (!onboardingCompleted) {
                        OnboardingScreen(
                            // Pass padding if OnboardingScreen itself doesn't have a Scaffold
                            // and needs to respect MainActivity's Scaffold (e.g., snackbar area)
                            modifier = Modifier.padding(innerPadding).fillMaxSize(),
                            onOnboardingComplete = {
                                mainViewModel.markOnboardingComplete()
                            }
                        )
                    } else {
                        if (!pinIsActuallySet) {
                            PinLockScreen(
                                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                                onUnlock = { /* Should not be called if no PIN is set */ },
                                onSetPin = {
                                    coroutineScope.launch {
                                        pinIsActuallySet = PinStorage.isPinSet(this@MainActivity)
                                        pinUnlocked = true
                                    }
                                }
                            )
                        } else if (!pinUnlocked) {
                            PinLockScreen(
                                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                                onUnlock = { pinUnlocked = true },
                                onSetPin = {
                                    coroutineScope.launch {
                                        pinIsActuallySet = PinStorage.isPinSet(this@MainActivity)
                                        pinUnlocked = true
                                    }
                                }
                            )
                        } else {
                            MainNavHost(
                                modifier = Modifier.padding(innerPadding), // Pass padding to MainNavHost
                                onImportOldSms = { requestSmsPermissionAndImport() },
                                mainViewModel = mainViewModel
                            )
                        }
                    }
                }
            }
        }
    }

    // ... requestSmsPermissionAndImport(), importOldUpiSms(), getAllSms(), onDestroy() methods remain the same ...
    // (Ensure they are present as provided in the last complete MainActivity.kt version)
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
                val liteSummary = parseUpiLiteSummarySms(body)
                if (liteSummary != null) {
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
                    val exists = dao.getTransactionByDetails(transaction.amount, transaction.date, transaction.description)
                    if (exists == null) { dao.insert(transaction); txnCount++ }
                }
            }
            withContext(Dispatchers.Main) {
                mainViewModel.setSmsImportingState(false)
                val snackbarDateFormat = SimpleDateFormat("dd MMM yy", Locale.getDefault())
                val liteMsgPart = lastLiteSummaryForSnackbar?.let {
                    val formattedDate = try { snackbarDateFormat.format(Date(it.date)) } catch (e:Exception) {"N/A"}
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

    override fun onDestroy() {
        super.onDestroy()
        smsReceiver?.let { unregisterReceiver(it) }
    }
}
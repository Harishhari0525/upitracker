package com.example.upitracker

import android.Manifest
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.core.net.toUri
import com.example.upitracker.data.AppDatabase
import com.example.upitracker.data.UpiLiteSummary
import com.example.upitracker.sms.SmsReceiver
import com.example.upitracker.sms.parseUpiLiteSummarySms
import com.example.upitracker.sms.parseUpiSms
import com.example.upitracker.ui.screens.MainNavHost
import com.example.upitracker.util.Theme
import com.example.upitracker.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private var smsReceiver: SmsReceiver? = null
    private val mainViewModel: MainViewModel by viewModels()

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (!isGranted) {
                Toast.makeText(this, "SMS permission required to import old UPI SMS.", Toast.LENGTH_LONG).show()
            } else {
                importOldUpiSms()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppDatabase.getDatabase(this)
        val dao = db.transactionDao()
        val liteDao = db.upiLiteSummaryDao()

        smsReceiver = SmsReceiver(
            onTransactionParsed = { transaction ->
                lifecycleScope.launch { dao.insert(transaction) }
            },
            onUpiLiteSummary = { summary ->
                lifecycleScope.launch {
                    // Avoid duplicates at runtime as well
                    val exists = liteDao.getSummaryByDateAndBank(summary.date, summary.bank)
                    if (exists == null) {
                        liteDao.insert(summary)
                        mainViewModel.addUpiLiteSummary(summary)
                    }
                }
            }
        )

        val filter = IntentFilter("android.provider.Telephony.SMS_RECEIVED")
        registerReceiver(smsReceiver, filter)

        setContent {
            Theme {
                MainNavHost(
                    onImportOldSms = {
                        if (ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.READ_SMS
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            importOldUpiSms()
                        } else {
                            requestPermission.launch(Manifest.permission.READ_SMS)
                        }
                    },
                    mainViewModel = mainViewModel
                )
            }
        }
    }

    // Now prevents UPI Lite duplicates by date & bank!
    private fun importOldUpiSms() {
        val db = AppDatabase.getDatabase(this)
        val dao = db.transactionDao()
        val liteDao = db.upiLiteSummaryDao()
        lifecycleScope.launch(Dispatchers.IO) {
            val smsList = getAllSms()
            var txnCount = 0
            var liteSummaryCount = 0
            var lastLiteSummary: UpiLiteSummary? = null
            for ((sender, body, smsDate) in smsList) {
                val transaction = parseUpiSms(body, sender, smsDate)
                if (transaction != null) {
                    dao.insert(transaction)
                    txnCount++
                } else {
                    val liteSummary = parseUpiLiteSummarySms(body)
                    if (liteSummary != null) {
                        // Check for duplicate before inserting
                        val exists = liteDao.getSummaryByDateAndBank(liteSummary.date, liteSummary.bank)
                        if (exists == null) {
                            liteDao.insert(liteSummary)
                            mainViewModel.addUpiLiteSummary(liteSummary)
                            liteSummaryCount++
                            lastLiteSummary = liteSummary
                        }
                    }
                }
            }
            withContext(Dispatchers.Main) {
                val liteMsg = lastLiteSummary?.let {
                    "\nLast UPI Lite: ${it.transactionCount} transactions, ₹${it.totalAmount} on ${it.date}"
                } ?: ""
                Toast.makeText(
                    this@MainActivity,
                    "Imported $txnCount UPI SMS!\nFound $liteSummaryCount new UPI Lite summaries.$liteMsg",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private suspend fun getAllSms(): List<Triple<String, String, Long>> = withContext(Dispatchers.IO) {
        val smsList = mutableListOf<Triple<String, String, Long>>()
        val uriSms = "content://sms/inbox".toUri()
        val cursor = contentResolver.query(
            uriSms, arrayOf("_id", "address", "date", "body"),
            null, null, "date DESC"
        )
        cursor?.use {
            val addressIdx = it.getColumnIndex("address")
            val bodyIdx = it.getColumnIndex("body")
            val dateIdx = it.getColumnIndex("date")
            while (it.moveToNext()) {
                val address = it.getString(addressIdx) ?: ""
                val body = it.getString(bodyIdx) ?: ""
                val date = it.getLong(dateIdx)
                smsList.add(Triple(address, body, date))
            }
        }
        smsList
    }

    override fun onDestroy() {
        super.onDestroy()
        smsReceiver?.let { unregisterReceiver(it) }
    }
}
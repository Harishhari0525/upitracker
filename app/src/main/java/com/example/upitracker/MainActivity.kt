package com.example.upitracker

import android.Manifest
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.upitracker.data.AppDatabase
import com.example.upitracker.data.UpiLiteSummary
import com.example.upitracker.sms.SmsReceiver
import com.example.upitracker.sms.parseUpiLiteSummarySms
import com.example.upitracker.sms.parseUpiSms
import com.example.upitracker.ui.screens.MainNavHost
import com.example.upitracker.util.Theme
import kotlinx.coroutines.launch
import androidx.core.net.toUri

class MainActivity : ComponentActivity() {

    private var smsReceiver: SmsReceiver? = null

    // Permission launcher for READ_SMS
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

        // Setup Room DB and DAO
        val db = AppDatabase.getDatabase(this)
        val dao = db.transactionDao()

        smsReceiver = SmsReceiver(
            onTransactionParsed = { transaction ->
                lifecycleScope.launch { dao.insert(transaction) }
            },
            onUpiLiteSummary = { summaryText ->
                runOnUiThread {
                    Toast.makeText(this, summaryText, Toast.LENGTH_LONG).show()
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
                    }
                )
            }
        }
    }

    // Reads, parses, and imports all old SMS from inbox (both UPI transactions & UPI Lite summaries)
    private fun importOldUpiSms() {
        val smsList = getAllSms()
        val db = AppDatabase.getDatabase(this)
        val dao = db.transactionDao()
        lifecycleScope.launch {
            var txnCount = 0
            var liteSummaryCount = 0
            var lastLiteSummary: UpiLiteSummary? = null
            smsList.forEach { (sender, body, smsDate) ->
                val transaction = parseUpiSms(body, sender, smsDate)
                if (transaction != null) {
                    dao.insert(transaction)
                    txnCount++
                } else {
                    // Try UPI Lite summary
                    val liteSummary = parseUpiLiteSummarySms(body)
                    if (liteSummary != null) {
                        lastLiteSummary = liteSummary
                        liteSummaryCount++
                    }
                }
            }
            runOnUiThread {
                val liteMsg = if (lastLiteSummary != null) {
                    "\nLast UPI Lite: ${lastLiteSummary!!.transactionCount} transactions, ₹${lastLiteSummary!!.totalAmount} on ${lastLiteSummary!!.date}"
                } else ""
                Toast.makeText(
                    this@MainActivity,
                    "Imported $txnCount UPI SMS!\nFound $liteSummaryCount UPI Lite summaries.$liteMsg",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // Reads all SMS from inbox as (sender, body, date)
    private fun getAllSms(): List<Triple<String, String, Long>> {
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
        return smsList
    }

    override fun onDestroy() {
        super.onDestroy()
        smsReceiver?.let { unregisterReceiver(it) }
    }
}
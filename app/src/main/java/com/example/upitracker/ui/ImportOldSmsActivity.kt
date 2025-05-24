package com.example.upitracker.ui

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import com.example.upitracker.data.AppDatabase
import com.example.upitracker.sms.parseUpiSms
import com.example.upitracker.sms.parseUpiLiteSummarySms
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.core.net.toUri

class ImportOldSmsActivity : Activity() {

    private val activityScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Start import process (optionally trigger this from a button)
        importOldUpiSms()
    }

    private fun importOldUpiSms() {
        val smsList = getAllSms()
        val db = AppDatabase.getDatabase(this)
        val dao = db.transactionDao()
        var txnCount = 0
        var liteSummaryCount = 0
        activityScope.launch {
            smsList.forEach { (sender, body, smsDate) ->
                val transaction = parseUpiSms(body, sender, smsDate)
                if (transaction != null) {
                    dao.insert(transaction)
                    txnCount++
                } else {
                    val liteSummary = parseUpiLiteSummarySms(body)
                    if (liteSummary != null) {
                        liteSummaryCount++
                        // Optionally handle summary (e.g., show Toast, save elsewhere)
                    }
                }
            }
            runOnUiThread {
                Toast.makeText(
                    this@ImportOldSmsActivity,
                    "Imported $txnCount UPI transactions!\nFound $liteSummaryCount UPI Lite summaries.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

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
}

package com.example.upitracker.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsMessage
import com.example.upitracker.data.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log

class SmsReceiver(
    private val onTransactionParsed: (Transaction) -> Unit,
    private val onUpiLiteSummary: ((summary: String) -> Unit)? = null
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val bundle = intent.extras ?: return
        val pdus = bundle.get("pdus") as? Array<*>
        val format = bundle.getString("format")

        pdus?.forEach { pdu ->
            val sms = createSmsMessageFromPdu(pdu, format)
            if (sms != null) {
                val body = sms.messageBody.orEmpty()
                val sender = sms.originatingAddress.orEmpty()
                val smsTimestamp = sms.timestampMillis

                // Try parsing as summary
                val summary = parseUpiLiteSummarySms(body)
                if (summary != null) {
                    onUpiLiteSummary?.invoke(
                        "UPI Lite Summary: ${summary.transactionCount} transactions, Rs. ${summary.totalAmount}, Date: ${summary.date}, Bank: ${summary.bank}"
                    )
                    Log.d("UPI_LITE_SUMMARY", summary.toString())
                    return@forEach
                }

                CoroutineScope(Dispatchers.IO).launch {
                    val transaction = parseUpiSms(body, sender, smsTimestamp)
                    if (transaction != null) {
                        onTransactionParsed(transaction)
                    }
                }
            }
        }
    }

    // Helper function: avoid deprecated warnings in main logic
    private fun createSmsMessageFromPdu(pdu: Any?, format: String?): SmsMessage? {
        return try {
            if (pdu is ByteArray) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && format != null) {
                    SmsMessage.createFromPdu(pdu, format)
                } else {
                    @Suppress("DEPRECATION")
                    SmsMessage.createFromPdu(pdu)
                }
            } else null
        } catch (e: Exception) {
            null
        }
    }
}

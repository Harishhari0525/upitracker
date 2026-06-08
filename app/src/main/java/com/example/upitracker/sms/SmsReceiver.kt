package com.example.upitracker.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.upitracker.data.AppDatabase
import com.example.upitracker.data.BudgetPeriod
import com.example.upitracker.data.Transaction
import com.example.upitracker.util.BankIdentifier
import com.example.upitracker.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            Log.w("SmsReceiver", "Rejected unexpected broadcast action signature: ${intent.action}")
            return
        }

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                if (messages.isNullOrEmpty()) return@launch

                val fullSmsBody = messages.joinToString(separator = "") { it.displayMessageBody ?: "" }
                val originalSender = messages.firstOrNull()?.originatingAddress ?: "Unknown"
                val smsDate = messages.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()

                SmsProcessingService.processSingleSms(
                    context = context,
                    sender = originalSender,
                    body = fullSmsBody,
                    smsDate = smsDate,
                    updateWidget = true
                )

            } catch (e: Exception) {
                Log.e("SmsReceiver", "Error processing received SMS", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
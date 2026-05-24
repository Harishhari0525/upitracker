package com.example.upitracker.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        val pendingResult = goAsync()
        val receiverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        receiverScope.launch {
            try {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return@launch

                for (sms in messages) {
                    val body = sms.displayMessageBody ?: continue
                    val sender = sms.displayOriginatingAddress ?: "Unknown"
                    val smsTimestamp = sms.timestampMillis

                    SmsProcessingService.processSingleSms(
                        context = context,
                        sender = sender,
                        body = body,
                        smsDate = smsTimestamp,
                        updateWidget = true
                    )
                }
            } catch (e: Exception) {
                Log.e("SmsReceiver", "Error processing background SMS tracker payload", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
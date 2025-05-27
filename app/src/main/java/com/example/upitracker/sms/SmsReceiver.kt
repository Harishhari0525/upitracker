package com.example.upitracker.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony // For Telephony.Sms.Intents
import android.telephony.SmsMessage
import com.example.upitracker.data.Transaction
import com.example.upitracker.data.UpiLiteSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import android.util.Log
import com.example.upitracker.util.RegexPreference // For accessing user-defined regex

class SmsReceiver(
    // Callbacks to notify about parsed data
    private val onTransactionParsed: (Transaction) -> Unit,
    private val onUpiLiteSummaryReceived: (UpiLiteSummary) -> Unit // Renamed for clarity
) : BroadcastReceiver() {

    // Create a dedicated CoroutineScope for this receiver
    // Using SupervisorJob so if one parsing job fails, it doesn't cancel the whole scope
    private val receiverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        // Ensure the intent action is SMS_RECEIVED
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        messages?.forEach { sms ->
            val body = sms.messageBody.orEmpty()
            val sender = sms.originatingAddress.orEmpty()
            val smsTimestamp = sms.timestampMillis

            // Log raw SMS for debugging if needed
            // Log.d("SmsReceiver", "Received SMS from $sender: $body")

            // Attempt to parse as UPI Lite Summary first
            val liteSummary = parseUpiLiteSummarySms(body)
            if (liteSummary != null) {
                Log.d("SmsReceiver", "Parsed UPI Lite Summary: $liteSummary")
                onUpiLiteSummaryReceived(liteSummary)
                // If it's a lite summary, we might not need to parse it as a regular transaction.
                // Depending on SMS content, some lite summaries might also look like regular transactions.
                // For now, if it's a lite summary, we return to avoid double processing.
                return@forEach
            }

            // If not a Lite Summary, try to parse as a regular UPI transaction
            receiverScope.launch {
                // Fetch custom regex patterns to pass to the parser
                var customRegexPatterns: List<Regex> = emptyList()
                RegexPreference.getRegexPatterns(context).collect { patternsSet ->
                    customRegexPatterns = patternsSet.mapNotNull {
                        try { Regex(it, RegexOption.IGNORE_CASE) } catch (e: Exception) { null }
                    }
                }
                // Log.d("SmsReceiver", "Using ${customRegexPatterns.size} custom regex patterns.")

                val transaction = parseUpiSms(
                    message = body,
                    sender = sender,
                    smsDate = smsTimestamp,
                    customRegexList = customRegexPatterns // Pass loaded custom regex
                )
                if (transaction != null) {
                    Log.d("SmsReceiver", "Parsed UPI Transaction: $transaction")
                    onTransactionParsed(transaction)
                } else {
                    // Log.d("SmsReceiver", "SMS from $sender not recognized as UPI transaction or Lite summary.")
                }
            }
        }
    }

    // createSmsMessageFromPdu is not needed if using Telephony.Sms.Intents.getMessagesFromIntent(intent)
    // which is the recommended way. I'm removing it for simplicity and modernity.
}
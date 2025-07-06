package com.example.upitracker.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.upitracker.data.AppDatabase // ✨ Import AppDatabase
import com.example.upitracker.data.ArchivedSmsMessage // ✨ Import new Entity
import com.example.upitracker.data.Transaction
import com.example.upitracker.data.UpiLiteSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
// import com.example.upitracker.util.RegexPreference // Already present from previous changes

class SmsReceiver(
    private val onTransactionParsed: (Transaction) -> Unit,
    private val onUpiLiteSummaryReceived: (UpiLiteSummary) -> Unit
) : BroadcastReceiver() {

    private val receiverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }
        Log.d("SmsReceiver", "SMS Received Intent processing started.")

        // ✨ Get DAO instance ✨
        val archivedSmsDao = AppDatabase.getDatabase(context.applicationContext).archivedSmsMessageDao()

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        messages?.forEachIndexed { index, sms ->
            val body = sms.messageBody.orEmpty()
            val sender = sms.originatingAddress.orEmpty()
            val smsTimestamp = sms.timestampMillis

            Log.d("SmsReceiver", "Processing SMS [$index] from $sender: \"$body\"")

            var isUpiRelated = false // Flag to check if SMS was UPI related for backup

            val bankName = when {
                sender.uppercase().contains("HDFC") -> "HDFC Bank"
                sender.uppercase().contains("ICICI") -> "ICICI Bank"
                sender.uppercase().contains("SBI") || sender.contains("SBIN") -> "State Bank of India"
                sender.uppercase().contains("AXIS") -> "Axis Bank"
                sender.uppercase().contains("KOTAK") -> "Kotak Mahindra Bank"
                sender.uppercase().contains("PNB") -> "Punjab National Bank"
                sender.uppercase().contains("PAYTM") -> "Paytm Payments Bank"
                sender.uppercase().contains("IDBI") -> "IDBI Bank"
                sender.uppercase().contains("YES") -> "Yes Bank"
                sender.uppercase().contains("CITI") -> "Citibank"
                sender.uppercase().contains("BOB") -> "Bank of Baroda"
                sender.uppercase().contains("UNION") -> "Union Bank of India"
                sender.uppercase().contains("INDUSIND") -> "IndusInd Bank"
                sender.uppercase().contains("AU") -> "AU Small Finance Bank"
                sender.uppercase().contains("FEDERAL") -> "Federal Bank"
                sender.uppercase().contains("RBL") -> "RBL Bank"
                sender.uppercase().contains("CREDIT") -> "Credit Bank"
                else -> null
            }

            // Attempt to parse as UPI Lite Summary first
            val liteSummary = parseUpiLiteSummarySms(body)
            if (liteSummary != null) {
                Log.i("SmsReceiver", "Successfully parsed UPI Lite Summary: $liteSummary")
                isUpiRelated = true
                onUpiLiteSummaryReceived(liteSummary)
                // return@forEachIndexed // Keep this if a lite summary should not be processed further
            } else {
                Log.d("SmsReceiver", "SMS from $sender not a UPI Lite Summary. Attempting regular UPI parse.")
            }

            if (!isUpiRelated) { // Only attempt regular parse if not already identified as Lite summary
                receiverScope.launch {
                    var customRegexPatterns: List<Regex> = emptyList()
                    com.example.upitracker.util.RegexPreference.getRegexPatterns(context).firstOrNull()?.let { patternsSet ->
                        customRegexPatterns = patternsSet.mapNotNull {
                            try { Regex(it, RegexOption.IGNORE_CASE) }
                            catch (e: Exception) { Log.w("SmsReceiver", "Invalid custom regex pattern: '$it'", e); null }
                        }
                    }

                    val transaction = parseUpiSms(
                        message = body,
                        sender = sender,
                        smsDate = smsTimestamp,
                        customRegexList = customRegexPatterns,
                        bankName = bankName
                    )
                    if (transaction != null) {
                        Log.i("SmsReceiver", "Successfully parsed UPI Transaction: $transaction")
                        // Set flag here too if transaction is found by this parser
                        // This part runs in a different coroutine, so direct flag setting is tricky.
                        // Simpler: back up after parseUpiSms if it's successful within its scope.
                        // For now, let's back up based on either parser succeeding.

                        // Launch another coroutine or ensure DB ops are main-safe if callbacks are on main.
                        // The onTransactionParsed callback handles its own scope.
                        onTransactionParsed(transaction)

                        // ✨ Backup logic for regular UPI SMS ✨
                        val archivedSms = ArchivedSmsMessage(
                            originalSender = sender,
                            originalBody = body,
                            originalTimestamp = smsTimestamp,
                            backupTimestamp = System.currentTimeMillis()
                        )
                        archivedSmsDao.insertArchivedSms(archivedSms)
                        Log.i("SmsReceiver", "Regular UPI SMS backed up.")
                    } else {
                        Log.d("SmsReceiver", "SMS from $sender NOT recognized as a UPI transaction by parsers.")
                    }
                }
            }

            // ✨ Backup logic: If it was a UPI Lite Summary, back it up here ✨
            // This ensures it's backed up even if we returned early after Lite Summary parsing.
            if (liteSummary != null) { // Check if liteSummary was parsed successfully earlier
                receiverScope.launch { // Use a coroutine for DB operation
                    val archivedSms = ArchivedSmsMessage(
                        originalSender = sender,
                        originalBody = body,
                        originalTimestamp = smsTimestamp,
                        backupTimestamp = System.currentTimeMillis()
                    )
                    archivedSmsDao.insertArchivedSms(archivedSms)
                    Log.i("SmsReceiver", "UPI Lite SMS backed up.")
                }
            }
        }
    }
}
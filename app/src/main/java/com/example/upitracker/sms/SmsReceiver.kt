package com.example.upitracker.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.example.upitracker.data.AppDatabase
import com.example.upitracker.data.ArchivedSmsMessage
import com.example.upitracker.util.BankIdentifier
import com.example.upitracker.util.NotificationHelper
import com.example.upitracker.util.RegexPreference
import com.example.upitracker.widget.UpiExpenseWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        val pendingResult = goAsync()
        val receiverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        receiverScope.launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val transactionDao = db.transactionDao()
                val archivedSmsDao = db.archivedSmsMessageDao()
                val upiLiteSummaryDao = db.upiLiteSummaryDao()

                val storedPatterns = RegexPreference.getRegexPatterns(context).first()
                val customRegexPatterns = storedPatterns.map { patternStr ->
                    Regex(patternStr, RegexOption.IGNORE_CASE)
                }

                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return@launch

                for (sms in messages) {
                    val body = sms.displayMessageBody ?: continue
                    val sender = sms.displayOriginatingAddress ?: "Unknown"
                    val smsTimestamp = sms.timestampMillis

                    // ✨ START: SENDER NORMALIZATION LAYER ✨
                    // Strips routing prefixes (e.g., "AX-ICICIB" -> "ICICIB", "AD-DISPUTE" -> "DISPUTE")
                    val normalizedSender = if (sender.contains("-")) {
                        sender.substringAfter("-").uppercase(Locale.getDefault()).trim()
                    } else {
                        sender.uppercase(Locale.getDefault()).trim()
                    }

                    // Attempt standard resolution first
                    var bankName = BankIdentifier.getBankName(sender)

                    // If resolution leaks a generic metadata flag, scan the body content contextually
                    if (bankName.isNullOrBlank() ||
                        normalizedSender == "DISPUTE" ||
                        normalizedSender == "ALERT" ||
                        normalizedSender == "CMPLNT" ||
                        bankName.uppercase(Locale.getDefault()) == "DISPUTE"
                    ) {
                        val bodyLower = body.lowercase(Locale.getDefault())
                        bankName = when {
                            bodyLower.contains("icici") -> "ICICI Bank"
                            bodyLower.contains("hdfc") -> "HDFC Bank"
                            bodyLower.contains("sbi") || bodyLower.contains("state bank") -> "SBI"
                            bodyLower.contains("axis") -> "Axis Bank"
                            bodyLower.contains("kotak") -> "Kotak Bank"
                            bodyLower.contains("pnb") || bodyLower.contains("punjab national") -> "PNB"
                            bodyLower.contains("bob") || bodyLower.contains("baroda") -> "Bank of Baroda"
                            else -> "Other Bank" // Graceful default instead of exposing raw internal channel headers
                        }
                    }
                    // ✨ END: SENDER NORMALIZATION LAYER ✨

                    // 1. Parse standard UPI transactions
                    val transaction = parseUpiSms(
                        message = body,
                        sender = sender,
                        smsDate = smsTimestamp,
                        customRegexList = customRegexPatterns,
                        bankName = bankName
                    )

                    if (transaction != null) {
                        val insertedId = transactionDao.insertReturningId(transaction)
                        val savedTransaction = transaction.copy(id = insertedId.toInt())

                        if (savedTransaction.type == "DEBIT" && savedTransaction.category == null) {
                            NotificationHelper.showNewTransactionNotification(context, savedTransaction)
                        }

                        val archivedSms = ArchivedSmsMessage(
                            originalSender = sender,
                            originalBody = body,
                            originalTimestamp = smsTimestamp,
                            backupTimestamp = System.currentTimeMillis()
                        )
                        archivedSmsDao.insertArchivedSms(archivedSms)

                        val widgetManager = GlanceAppWidgetManager(context)
                        val widget = UpiExpenseWidget()
                        val glanceIds = widgetManager.getGlanceIds(widget.javaClass)
                        glanceIds.forEach { glanceId ->
                            widget.update(context, glanceId)
                        }
                    }

                    // 2. Parse UPI Lite Wallet summaries
                    val liteSummary = parseUpiLiteSummarySms(body)
                    if (liteSummary != null) {
                        val existingSummary = upiLiteSummaryDao.getSummaryByDateAndBank(
                            date = liteSummary.date,
                            bank = liteSummary.bank
                        )

                        if (existingSummary == null) {
                            upiLiteSummaryDao.insert(liteSummary)
                        } else if (existingSummary.transactionCount != liteSummary.transactionCount ||
                            existingSummary.totalAmount != liteSummary.totalAmount) {
                            upiLiteSummaryDao.update(
                                existingSummary.copy(
                                    transactionCount = liteSummary.transactionCount,
                                    totalAmount = liteSummary.totalAmount
                                )
                            )
                        }

                        val archivedSms = ArchivedSmsMessage(
                            originalSender = sender,
                            originalBody = body,
                            originalTimestamp = smsTimestamp,
                            backupTimestamp = System.currentTimeMillis()
                        )
                        archivedSmsDao.insertArchivedSms(archivedSms)
                    }
                }
            } catch (e: Exception) {
                Log.e("SmsReceiver", "Error processing background SMS tracker payload", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
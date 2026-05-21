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

                // Safely fetch and compile your custom user-defined regex patterns from storage
                val storedPatterns = RegexPreference.getRegexPatterns(context).first()
                val customRegexPatterns = storedPatterns.map { patternStr ->
                    Regex(patternStr, RegexOption.IGNORE_CASE)
                }

                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return@launch

                for (sms in messages) {
                    val body = sms.displayMessageBody ?: continue
                    val sender = sms.displayOriginatingAddress ?: "Unknown"
                    val smsTimestamp = sms.timestampMillis
                    val bankName = BankIdentifier.getBankName(sender)

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

                    // 2. Parse UPI Lite Wallet summaries with your precise original conditions
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
package com.example.upitracker.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.example.upitracker.data.AppDatabase // ✨ Import AppDatabase
import com.example.upitracker.data.ArchivedSmsMessage // ✨ Import new Entity
import com.example.upitracker.data.Transaction
import com.example.upitracker.data.UpiLiteSummary
import com.example.upitracker.util.BankIdentifier
import com.example.upitracker.util.NotificationHelper
import com.example.upitracker.widget.UpiExpenseWidget
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

        // ✨ Get DAO instance ✨
        val archivedSmsDao = AppDatabase.getDatabase(context.applicationContext).archivedSmsMessageDao()

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        messages?.forEachIndexed { index, sms ->
            val body = sms.messageBody.orEmpty()
            val sender = sms.originatingAddress.orEmpty()
            val smsTimestamp = sms.timestampMillis


            var isUpiRelated = false // Flag to check if SMS was UPI related for backup

            val bankName = BankIdentifier.getBankName(sender)
            if (bankName == null) {
                Log.d("SmsReceiver", "SMS from $sender does not match any known bank.")
                return@forEachIndexed
            }

            val liteSummary = parseUpiLiteSummarySms(body)
            if (liteSummary != null) {
                isUpiRelated = true
                onUpiLiteSummaryReceived(liteSummary)
            } else {
                Log.d("SmsReceiver", "SMS from $sender not a UPI Lite Summary. Attempting regular UPI parse.")
            }

            if (!isUpiRelated) {
                receiverScope.launch {
                    var customRegexPatterns: List<Regex> = emptyList()
                    com.example.upitracker.util.RegexPreference.getRegexPatterns(context)
                        .firstOrNull()?.let { patternsSet ->
                        customRegexPatterns = patternsSet.mapNotNull {
                            try {
                                Regex(it, RegexOption.IGNORE_CASE)
                            } catch (e: Exception) {
                                Log.w("SmsReceiver", "Invalid custom regex pattern: '$it'", e); null
                            }
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
                        isUpiRelated = true

                        val db = AppDatabase.getDatabase(context)
                        val dao = db.transactionDao()

                        // ✨ 2. INSERT AND GET THE ID (This removes the warning!)
                        val newId = dao.insertReturningId(transaction)

                        // Create a copy with the correct ID to pass to UI/Notification
                        val savedTransaction = transaction.copy(id = newId.toInt())

                        onTransactionParsed(savedTransaction)

                        // ✨ 4. SHOW NOTIFICATION (Only if it's a debit & uncategorized)
                        if (savedTransaction.type == "DEBIT" && savedTransaction.category == null) {
                            NotificationHelper.showNewTransactionNotification(context, savedTransaction)
                        }

                        val context = context.applicationContext
                        val widget = UpiExpenseWidget()
                        val manager = GlanceAppWidgetManager(context)
                        manager.getGlanceIds(UpiExpenseWidget::class.java).forEach { glanceId ->
                            widget.update(context, glanceId)
                        }
                        val glanceIds = manager.getGlanceIds(widget.javaClass)
                        glanceIds.forEach { glanceId ->
                            widget.update(context, glanceId)
                        }

                    }
                    if (isUpiRelated) {
                        val archivedSms = ArchivedSmsMessage(
                            originalSender = sender,
                            originalBody = body,
                            originalTimestamp = smsTimestamp,
                            backupTimestamp = System.currentTimeMillis()
                        )
                        archivedSmsDao.insertArchivedSms(archivedSms)
                        Log.i("SmsReceiver", "UPI-related SMS from $sender backed up.")
                    }
                }
            }
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
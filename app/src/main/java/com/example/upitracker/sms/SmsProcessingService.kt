package com.example.upitracker.sms

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.example.upitracker.data.AppDatabase
import com.example.upitracker.data.ArchivedSmsMessage
import com.example.upitracker.data.Budget
import com.example.upitracker.data.BudgetPeriod
import com.example.upitracker.data.CategorySuggestionRule
import com.example.upitracker.data.RuleField
import com.example.upitracker.data.RuleLogic
import com.example.upitracker.data.RuleMatcher
import com.example.upitracker.data.Transaction
import com.example.upitracker.util.BankIdentifier
import com.example.upitracker.util.NotificationHelper
import com.example.upitracker.util.RegexPreference
import com.example.upitracker.util.TagUtils
import com.example.upitracker.util.ThemePreference
import com.example.upitracker.widget.UpiExpenseWidget
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale
import java.security.MessageDigest

data class SmsProcessingResult(
    val newTxnCount: Int = 0,
    val processedSummaries: Int = 0,
    val archivedCount: Int = 0
)

data class SmsProcessingConfig(
    val customRegexPatterns: List<Regex>,
    val categoryRules: List<CategorySuggestionRule>,
    val activeBudgets: List<Budget>,
    val refundKeyword: String,
    val upiLiteEnabled: Boolean
)

object SmsProcessingService {

    suspend fun fetchProcessingConfig(context: Context): SmsProcessingConfig = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val db = AppDatabase.getDatabase(appContext)

        val storedPatterns = RegexPreference.getRegexPatterns(appContext).firstOrNull().orEmpty()
        val customRegexPatterns = storedPatterns.mapNotNull { pattern ->
            try {
                Regex(pattern, RegexOption.IGNORE_CASE)
            } catch (e: Exception) {
                Log.w("SmsProcessingService", "Invalid custom regex skipped: $pattern", e)
                null
            }
        }

        SmsProcessingConfig(
            customRegexPatterns = customRegexPatterns,
            categoryRules = db.categorySuggestionRuleDao().getAllRules().first(),
            activeBudgets = db.budgetDao().getAllActiveBudgets().first(),
            refundKeyword = ThemePreference.getRefundKeywordFlow(appContext).first(),
            upiLiteEnabled = ThemePreference.isUpiLiteEnabledFlow(appContext).first()
        )
    }

    suspend fun processSmsBatch(
        context: Context,
        smsList: List<Triple<String, String, Long>>,
        updateWidget: Boolean = false,
        config: SmsProcessingConfig? = null,
        showNotifications: Boolean = false
    ): SmsProcessingResult = withContext(Dispatchers.IO) {
        var newTxnCount = 0
        var processedSummaries = 0
        var archivedCount = 0

        val currentConfig = config ?: fetchProcessingConfig(context)
        val db = AppDatabase.getDatabase(context.applicationContext)

        db.withTransaction {
            smsList.forEach { (sender, body, smsDate) ->
                val result = processSingleSms(
                    context = context,
                    sender = sender,
                    body = body,
                    smsDate = smsDate,
                    updateWidget = updateWidget,
                    config = currentConfig,
                    showNotification = showNotifications
                )

                newTxnCount += result.newTxnCount
                processedSummaries += result.processedSummaries
                archivedCount += result.archivedCount
            }
        }

        SmsProcessingResult(
            newTxnCount = newTxnCount,
            processedSummaries = processedSummaries,
            archivedCount = archivedCount
        )
    }

    suspend fun processSingleSms(
        context: Context,
        sender: String,
        body: String,
        smsDate: Long,
        updateWidget: Boolean = false,
        config: SmsProcessingConfig? = null,
        showNotification: Boolean = true
    ): SmsProcessingResult = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val db = AppDatabase.getDatabase(appContext)

        val transactionDao = db.transactionDao()
        val archivedSmsDao = db.archivedSmsMessageDao()
        val upiLiteSummaryDao = db.upiLiteSummaryDao()

        var isUpiRelated = false
        var newTxnCount = 0
        var processedSummaries = 0
        var archivedCount = 0

        val currentConfig = config ?: fetchProcessingConfig(appContext)

        val bankName = resolveBankName(sender = sender, body = body)

        val parsedTransaction = parseUpiSms(
            message = body,
            sender = sender,
            smsDate = smsDate,
            customRegexList = currentConfig.customRegexPatterns,
            bankName = bankName
        )

        if (parsedTransaction != null) {
            isUpiRelated = true

            val finalTransaction = prepareTransactionForInsert(
                transaction = parsedTransaction,
                rules = currentConfig.categoryRules
            )

            val insertedId = transactionDao.insertIfNotDuplicate(finalTransaction)

            if (insertedId != -1L) {
                newTxnCount++

                val savedTransaction = finalTransaction.copy(id = insertedId.toInt())

                if (savedTransaction.type == "DEBIT" && savedTransaction.category == null) {
                    if (showNotification) {
                        val alertsEnabled = ThemePreference.isTransactionAlertsEnabledFlow(appContext).first()
                        if (alertsEnabled) {
                            val actionsEnabled = ThemePreference.isNotificationActionsEnabledFlow(appContext).first()
                            val suggested = if (actionsEnabled) {
                                val topMerchantCategories = transactionDao.getTopCategoriesForMerchant(savedTransaction.senderOrReceiver)
                                val globalTopCategories = transactionDao.getGlobalTopCategories()
                                (topMerchantCategories + globalTopCategories + listOf("Food", "Transport", "Bills"))
                                    .filter { it.isNotBlank() }
                                    .distinct()
                                    .take(3)
                            } else {
                                emptyList()
                            }
                            val redactContent = ThemePreference.isNotificationContentRedactedFlow(appContext).first()
                            NotificationHelper.showNewTransactionNotification(appContext, savedTransaction, suggested, redactContent)
                        }
                    }
                }

                if (showNotification) {
                    checkBudgetForNewTransaction(
                        context = appContext,
                        transaction = savedTransaction,
                        refundCategory = currentConfig.refundKeyword,
                        budgets = currentConfig.activeBudgets,
                        transactionDao = transactionDao,
                        budgetDao = db.budgetDao()
                    )
                }

                if (updateWidget) {
                    updateWidgets(appContext)
                }
            } else {
                Log.d(
                    "DuplicateTxnFix",
                    "Skipped duplicate SMS transaction. sender=$sender, timestamp=$smsDate"
                )
            }
        }

        if (currentConfig.upiLiteEnabled) {
            val liteSummary = parseUpiLiteSummarySms(body)

            if (liteSummary != null) {
                isUpiRelated = true

                val existingSummary = upiLiteSummaryDao.getSummaryByDateAndBank(
                    date = liteSummary.date,
                    bank = liteSummary.bank
                )

                if (existingSummary == null) {
                    upiLiteSummaryDao.insert(liteSummary)
                    processedSummaries++
                } else if (
                    existingSummary.transactionCount != liteSummary.transactionCount ||
                    existingSummary.totalAmountPaise != liteSummary.totalAmountPaise
                ) {
                    upiLiteSummaryDao.update(
                        existingSummary.copy(
                            transactionCount = liteSummary.transactionCount,
                            totalAmountPaise = liteSummary.totalAmountPaise
                        )
                    )
                }
            }
        }

        val looksLikePaymentMessage = Regex("\\b(upi|debited|credited|txn|transaction|paid|received)\\b", RegexOption.IGNORE_CASE).containsMatchIn(body)
        if (isUpiRelated || looksLikePaymentMessage) {
            val archivedSms = ArchivedSmsMessage(
                originalSender = sender,
                originalBody = MessageDigest.getInstance("SHA-256")
                    .digest(body.toByteArray(Charsets.UTF_8))
                    .joinToString("") { "%02x".format(it) },
                originalTimestamp = smsDate,
                backupTimestamp = System.currentTimeMillis(),
                parseStatus = if (isUpiRelated) "PARSED" else "UNMATCHED"
            )

            archivedSmsDao.insertArchivedSms(archivedSms)
            archivedCount++
        }

        // Keep the last processed SMS timestamp up to date
        ThemePreference.setLastProcessedSmsTimestamp(appContext, smsDate)

        SmsProcessingResult(
            newTxnCount = newTxnCount,
            processedSummaries = processedSummaries,
            archivedCount = archivedCount
        )
    }

    fun prepareTransactionForInsert(
        transaction: Transaction,
        rules: List<CategorySuggestionRule>
    ): Transaction {
        val extractedTags = TagUtils.extractTags("${transaction.description} ${transaction.note}")
        val transactionWithTags = transaction.copy(tags = extractedTags)

        if (!transaction.category.isNullOrBlank()) {
            return transactionWithTags
        }

        var bestMatch: CategorySuggestionRule? = null

        for (rule in rules) {
            val textToMatch = when (rule.fieldToMatch) {
                RuleField.DESCRIPTION -> transaction.description
                RuleField.SENDER_OR_RECEIVER -> transaction.senderOrReceiver
            }.lowercase()

            val keywords = rule.keyword
                .split(',')
                .map { it.trim().lowercase() }
                .filter { it.isNotBlank() }

            if (keywords.isEmpty()) continue

            val checkMatch: (String) -> Boolean = { keyword ->
                when (rule.matcher) {
                    RuleMatcher.CONTAINS -> textToMatch.contains(keyword)
                    RuleMatcher.EQUALS -> textToMatch == keyword
                    RuleMatcher.STARTS_WITH -> textToMatch.startsWith(keyword)
                    RuleMatcher.ENDS_WITH -> textToMatch.endsWith(keyword)
                }
            }

            val isMatch = if (rule.logic == RuleLogic.ALL) {
                keywords.all { checkMatch(it) }
            } else {
                keywords.any { checkMatch(it) }
            }

            if (isMatch) {
                bestMatch = when {
                    bestMatch == null -> rule
                    rule.priority > bestMatch.priority -> rule
                    rule.priority == bestMatch.priority &&
                            rule.keyword.length > bestMatch.keyword.length -> rule
                    else -> bestMatch
                }
            }
        }

        return if (bestMatch != null) {
            transactionWithTags.copy(category = bestMatch.categoryName)
        } else {
            transactionWithTags
        }
    }

    fun resolveBankName(sender: String, body: String): String {
        val normalizedSender = BankIdentifier.normalizeDltSenderHeader(sender)

        var bankName = BankIdentifier.getBankName(sender)
            ?: BankIdentifier.getBankName(normalizedSender)

        if (
            bankName.isNullOrBlank() ||
            normalizedSender == "DISPUTE" ||
            normalizedSender == "ALERT" ||
            normalizedSender == "CMPLNT" ||
            normalizedSender == "NOTICE" ||
            normalizedSender == "UPDATE" ||
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
                else -> "Other Bank"
            }
        }

        return bankName
    }

    suspend fun updateWidgets(context: Context) {
        try {
            val widgetManager = GlanceAppWidgetManager(context)
            val widget = UpiExpenseWidget()
            val glanceIds = widgetManager.getGlanceIds(widget.javaClass)

            glanceIds.forEach { glanceId ->
                widget.update(context, glanceId)
            }
        } catch (e: Exception) {
            Log.e("SmsProcessingService", "Widget update failed", e)
        }
    }

    private suspend fun checkBudgetForNewTransaction(
        context: Context,
        transaction: Transaction,
        refundCategory: String,
        budgets: List<Budget>,
        transactionDao: com.example.upitracker.data.TransactionDao,
        budgetDao: com.example.upitracker.data.BudgetDao
    ) {
        if (
            transaction.type != "DEBIT" ||
            transaction.category.equals(refundCategory, ignoreCase = true)
        ) {
            return
        }

        val relevantBudget = budgets.find {
            it.categoryName.equals(transaction.category, ignoreCase = true)
        } ?: return

        val (periodStart, periodEnd) = when (relevantBudget.periodType) {
            BudgetPeriod.WEEKLY -> getCurrentWeekRange()
            BudgetPeriod.MONTHLY -> getCurrentMonthDateRange()
            BudgetPeriod.YEARLY -> getCurrentYearRange()
        }

        if (relevantBudget.lastNotificationTimestamp >= periodStart) {
            return
        }

        val spentInCurrentPeriod = transactionDao.getTransactionsForBudgetCheck(
            categoryName = relevantBudget.categoryName,
            startDate = periodStart,
            endDate = periodEnd,
            refundCategory = refundCategory
        ).sumOf { it.amount }

        if (spentInCurrentPeriod > relevantBudget.budgetAmount) {
            NotificationHelper.showBudgetExceededNotification(
                context = context,
                budget = relevantBudget,
                spentAmount = spentInCurrentPeriod
            )

            budgetDao.update(
                relevantBudget.copy(
                    lastNotificationTimestamp = System.currentTimeMillis()
                )
            )
        }
    }

    private fun getCurrentMonthDateRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val start = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)

        return start to calendar.timeInMillis
    }

    private fun getCurrentWeekRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val start = calendar.timeInMillis

        calendar.add(Calendar.WEEK_OF_YEAR, 1)
        calendar.add(Calendar.MILLISECOND, -1)

        return start to calendar.timeInMillis
    }

    private fun getCurrentYearRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_YEAR, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val start = calendar.timeInMillis

        calendar.add(Calendar.YEAR, 1)
        calendar.add(Calendar.MILLISECOND, -1)

        return start to calendar.timeInMillis
    }
}

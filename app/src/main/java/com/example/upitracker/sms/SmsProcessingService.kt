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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

data class SmsProcessingResult(
    val newTxnCount: Int = 0,
    val processedSummaries: Int = 0,
    val archivedCount: Int = 0
)

object SmsProcessingService {

    suspend fun processSmsBatch(
        context: Context,
        smsList: List<Triple<String, String, Long>>,
        updateWidget: Boolean = false
    ): SmsProcessingResult = withContext(Dispatchers.IO) {
        var newTxnCount = 0
        var processedSummaries = 0
        var archivedCount = 0

        smsList.forEach { (sender, body, smsDate) ->
            val result = processSingleSms(
                context = context,
                sender = sender,
                body = body,
                smsDate = smsDate,
                updateWidget = updateWidget
            )

            newTxnCount += result.newTxnCount
            processedSummaries += result.processedSummaries
            archivedCount += result.archivedCount
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
        updateWidget: Boolean = false
    ): SmsProcessingResult = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val db = AppDatabase.getDatabase(appContext)

        val transactionDao = db.transactionDao()
        val archivedSmsDao = db.archivedSmsMessageDao()
        val upiLiteSummaryDao = db.upiLiteSummaryDao()
        val categoryRuleDao = db.categorySuggestionRuleDao()
        val budgetDao = db.budgetDao()

        var isUpiRelated = false
        var newTxnCount = 0
        var processedSummaries = 0
        var archivedCount = 0

        val storedPatterns = RegexPreference.getRegexPatterns(appContext).firstOrNull().orEmpty()
        val customRegexPatterns = storedPatterns.mapNotNull { pattern ->
            try {
                Regex(pattern, RegexOption.IGNORE_CASE)
            } catch (e: Exception) {
                Log.w("SmsProcessingService", "Invalid custom regex skipped: $pattern", e)
                null
            }
        }

        val bankName = resolveBankName(sender = sender, body = body)

        val parsedTransaction = parseUpiSms(
            message = body,
            sender = sender,
            smsDate = smsDate,
            customRegexList = customRegexPatterns,
            bankName = bankName
        )

        if (parsedTransaction != null) {
            isUpiRelated = true

            val rules = categoryRuleDao.getAllRules().first()
            val finalTransaction = prepareTransactionForInsert(
                transaction = parsedTransaction,
                rules = rules
            )

            val insertedId = transactionDao.insertIfNotDuplicate(finalTransaction)

            if (insertedId != -1L) {
                newTxnCount++

                val savedTransaction = finalTransaction.copy(id = insertedId.toInt())

                if (savedTransaction.type == "DEBIT" && savedTransaction.category == null) {
                    NotificationHelper.showNewTransactionNotification(appContext, savedTransaction)
                }

                checkBudgetForNewTransaction(
                    context = appContext,
                    transaction = savedTransaction,
                    refundCategory = ThemePreference.getRefundKeywordFlow(appContext).first(),
                    budgets = budgetDao.getAllActiveBudgets().first(),
                    transactionDao = transactionDao,
                    budgetDao = budgetDao
                )

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

        val upiLiteEnabled = ThemePreference.isUpiLiteEnabledFlow(appContext).first()

        if (upiLiteEnabled) {
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
                    existingSummary.totalAmount != liteSummary.totalAmount
                ) {
                    upiLiteSummaryDao.update(
                        existingSummary.copy(
                            transactionCount = liteSummary.transactionCount,
                            totalAmount = liteSummary.totalAmount
                        )
                    )
                }
            }
        }

        if (isUpiRelated) {
            val archivedSms = ArchivedSmsMessage(
                originalSender = sender,
                originalBody = body,
                originalTimestamp = smsDate,
                backupTimestamp = System.currentTimeMillis()
            )

            archivedSmsDao.insertArchivedSms(archivedSms)
            archivedCount++
        }

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

    private fun resolveBankName(sender: String, body: String): String {
        val normalizedSender = if (sender.contains("-")) {
            sender.substringAfter("-").uppercase(Locale.getDefault()).trim()
        } else {
            sender.uppercase(Locale.getDefault()).trim()
        }

        var bankName = BankIdentifier.getBankName(sender)

        if (
            bankName.isNullOrBlank() ||
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
                else -> "Other Bank"
            }
        }

        return bankName
    }

    private suspend fun updateWidgets(context: Context) {
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
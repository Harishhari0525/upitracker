package com.example.upitracker.util

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.upitracker.data.AppDatabase
import com.example.upitracker.data.BudgetPeriod
import com.example.upitracker.data.OVERALL_MONTHLY_BUDGET_CATEGORY
import kotlinx.coroutines.flow.first
import java.util.Calendar
import androidx.core.content.edit
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class BudgetCheckerWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "BudgetCheckerWorker"

        fun enqueue(context: Context) {
            val constraints = androidx.work.Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<BudgetCheckerWorker>(12, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }

    override suspend fun doWork(): Result {
        Log.d(WORK_NAME, "Worker starting: Checking budget statuses...")
        try {
            val db = AppDatabase.getDatabase(applicationContext)
            val budgetDao = db.budgetDao()
            val transactionDao = db.transactionDao()

            val activeBudgets = budgetDao.getAllActiveBudgets().first()
            if (activeBudgets.isEmpty()) return Result.success()
            val includeUpiLite = ThemePreference.isUpiLiteEnabledFlow(applicationContext).first()
            val refundCategory = ThemePreference.getRefundKeywordFlow(applicationContext).first()
            val upiLiteSummaries = if (includeUpiLite) {
                db.upiLiteSummaryDao().getAllSummaries().first()
            } else {
                emptyList()
            }

            activeBudgets.forEach { budget ->
                val (periodStart, periodEnd) = getCurrentPeriodRange(budget.periodType)

                // Only check if we haven't already notified for the current period
                if (budget.lastNotificationTimestamp < periodStart) {
                    val spentInCurrentPeriod =
                        if (budget.categoryName == OVERALL_MONTHLY_BUDGET_CATEGORY) {
                            val transactionSpend = transactionDao.getSpentAmountInRangeSync(
                                startDate = periodStart,
                                endDate = periodEnd,
                                refundCategory = refundCategory
                            ) ?: 0.0
                            val upiLiteSpend = upiLiteSummaries
                                .filter { it.date in periodStart..periodEnd }
                                .sumOf { it.totalAmount }
                            transactionSpend + upiLiteSpend
                        } else {
                            transactionDao.getSpentAmountForCategoryInRangeSync(
                                startDate = periodStart,
                                endDate = periodEnd,
                                categoryName = budget.categoryName,
                                refundCategory = refundCategory
                            ) ?: 0.0
                        }

                    if (spentInCurrentPeriod >= budget.budgetAmount) {
                        Log.d(WORK_NAME, "A budget was exceeded; notifying user")
                        NotificationHelper.showBudgetExceededNotification(applicationContext, budget, spentInCurrentPeriod)

                        // Update the budget with a new timestamp to prevent re-notifying
                        val updatedBudget = budget.copy(lastNotificationTimestamp = System.currentTimeMillis())
                        budgetDao.update(updatedBudget)
                    } else if (spentInCurrentPeriod >= budget.budgetAmount * 0.85) {
                        val sharedPrefs = applicationContext.getSharedPreferences("budget_notifications_prefs", Context.MODE_PRIVATE)
                        val warningKey = "budget_warned_period_${budget.id}"
                        val hasWarned = sharedPrefs.getLong(warningKey, Long.MIN_VALUE) == periodStart
                        if (!hasWarned) {
                            Log.d(WORK_NAME, "A budget warning threshold was reached")
                            NotificationHelper.showBudgetWarningNotification(applicationContext, budget, spentInCurrentPeriod)
                            sharedPrefs.edit { putLong(warningKey, periodStart) }
                        }
                    }
                }
            }
            Log.i(WORK_NAME, "Budget check finished successfully.")
            return Result.success()
        } catch (e: Exception) {
            Log.e(WORK_NAME, "Error checking budgets.", e)
            return Result.retry()
        }
    }

    private fun getCurrentPeriodRange(period: BudgetPeriod): Pair<Long, Long> {
        val calendar = Calendar.getInstance()

        when (period) {
            BudgetPeriod.WEEKLY -> {
                calendar.firstDayOfWeek = Calendar.MONDAY
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            }
            BudgetPeriod.MONTHLY -> calendar.set(Calendar.DAY_OF_MONTH, 1)
            BudgetPeriod.YEARLY -> calendar.set(Calendar.DAY_OF_YEAR, 1)
        }

        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val periodStart = calendar.timeInMillis

        when (period) {
            BudgetPeriod.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            BudgetPeriod.MONTHLY -> calendar.add(Calendar.MONTH, 1)
            BudgetPeriod.YEARLY -> calendar.add(Calendar.YEAR, 1)
        }
        calendar.add(Calendar.MILLISECOND, -1)
        val periodEnd = calendar.timeInMillis

        return periodStart to periodEnd
    }
}

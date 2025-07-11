package com.example.upitracker.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.upitracker.R
import com.example.upitracker.data.Budget
import com.example.upitracker.data.RecurringRule
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

object NotificationHelper {

    // Define Channel IDs as constants to prevent typos
    private const val UPCOMING_PAYMENT_CHANNEL_ID = "upcoming_payments_channel"
    private const val BUDGET_ALERTS_CHANNEL_ID = "budget_alerts_channel"

    fun createNotificationChannels(context: Context) {
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Channel for Upcoming Payments
        val upcomingPaymentChannel = NotificationChannel(
            UPCOMING_PAYMENT_CHANNEL_ID,
            "Upcoming Payments",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications for recurring payments that are due soon."
        }
        notificationManager.createNotificationChannel(upcomingPaymentChannel)

        // Channel for Budget Alerts
        val budgetAlertsChannel = NotificationChannel(
            BUDGET_ALERTS_CHANNEL_ID,
            "Budget Alerts",
            NotificationManager.IMPORTANCE_HIGH // Use HIGH importance for budget alerts
        ).apply {
            description = "Notifications for when spending exceeds the allocated budget."
        }
        notificationManager.createNotificationChannel(budgetAlertsChannel)
    }

    fun showUpcomingPaymentNotification(context: Context, rule: RecurringRule) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("en").setRegion("IN").build())
        val daysUntil = TimeUnit.MILLISECONDS.toDays(rule.nextDueDate - System.currentTimeMillis()).coerceAtLeast(0)
        val dueText = if (daysUntil == 0L) "due today" else "due in $daysUntil day(s)"
        val notificationId = rule.id

        val builder = NotificationCompat.Builder(context, UPCOMING_PAYMENT_CHANNEL_ID) // Use correct channel
            .setSmallIcon(R.drawable.ic_stat_notifications)
            .setContentTitle("Upcoming Payment: ${rule.description}")
            .setContentText("${currencyFormatter.format(rule.amount)} is $dueText.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }

    fun showBudgetExceededNotification(context: Context, budget: Budget, spentAmount: Double) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("en").setRegion("IN").build())
        val overspendAmount = spentAmount - budget.budgetAmount
        val notificationId = budget.id + 1000 // Use a unique ID based on the budget ID

        val builder = NotificationCompat.Builder(context, BUDGET_ALERTS_CHANNEL_ID) // ✨ Ensure we use the correct Channel ID ✨
            .setSmallIcon(R.drawable.ic_stat_notifications)
            .setContentTitle("Budget Alert: ${budget.categoryName}")
            .setContentText("You've spent ${currencyFormatter.format(spentAmount)}, overspending by ${currencyFormatter.format(overspendAmount)}.")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("You've spent ${currencyFormatter.format(spentAmount)} of your ${currencyFormatter.format(budget.budgetAmount)} budget for '${budget.categoryName}', overspending by ${currencyFormatter.format(overspendAmount)}."))
            .setPriority(NotificationCompat.PRIORITY_HIGH) // High priority to make it pop up
            .setAutoCancel(true)

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }
}
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

    private const val UPCOMING_PAYMENT_CHANNEL_ID = "upcoming_payments_channel"
    private const val BUDGET_ALERTS_CHANNEL_ID = "budget_alerts_channel"

    fun createNotificationChannels(context: Context) {
        val upcomingPaymentChannel = NotificationChannel(
            UPCOMING_PAYMENT_CHANNEL_ID,
            "Upcoming Payments",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications for recurring payments that are due soon."
        }

        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(upcomingPaymentChannel)

        val budgetAlertsChannel = NotificationChannel(
            BUDGET_ALERTS_CHANNEL_ID,
            "Budget Alerts",
            NotificationManager.IMPORTANCE_HIGH // Use high importance for alerts
        ).apply {
            description = "Notifications for when spending exceeds the allocated budget."
        }

    }

    fun showUpcomingPaymentNotification(context: Context, rule: RecurringRule) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // If permission is not granted, do not proceed.
            // The app should request this permission from the UI layer.
            return
        }

        val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("en").setRegion("IN").build())
        val daysUntil = TimeUnit.MILLISECONDS.toDays(rule.nextDueDate - System.currentTimeMillis()).coerceAtLeast(0)
        val dueText = if (daysUntil == 0L) "due today" else "due in $daysUntil day(s)"

        val notificationId = rule.id // Use the rule's ID to ensure each notification is unique

        val builder = NotificationCompat.Builder(context, UPCOMING_PAYMENT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notifications) // You will need to create this small icon
            .setContentTitle("Upcoming Payment: ${rule.description}")
            .setContentText("${currencyFormatter.format(rule.amount)} is $dueText.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, builder.build())
        }
    }

    fun showBudgetExceededNotification(context: Context, budget: Budget, spentAmount: Double) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("en").setRegion("IN").build())
        val overspendAmount = spentAmount - budget.budgetAmount

        // Use a unique ID based on the budget ID + a constant to avoid clashes
        val notificationId = budget.id + 1000

        val builder = NotificationCompat.Builder(context, BUDGET_ALERTS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notifications)
            .setContentTitle("Budget Alert: ${budget.categoryName}")
            .setContentText("You've spent ${currencyFormatter.format(spentAmount)}, overspending by ${currencyFormatter.format(overspendAmount)}.")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("You've spent ${currencyFormatter.format(spentAmount)} of your ${currencyFormatter.format(budget.budgetAmount)} budget for '${budget.categoryName}', overspending by ${currencyFormatter.format(overspendAmount)}."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, builder.build())
        }
    }

}
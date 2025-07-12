package com.example.upitracker.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.upitracker.R
import com.example.upitracker.data.Budget
import com.example.upitracker.data.RecurringRule
import com.example.upitracker.network.GitHubRelease
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import android.net.Uri
import androidx.core.net.toUri

object NotificationHelper {

    // Define Channel IDs as constants to prevent typos
    private const val UPCOMING_PAYMENT_CHANNEL_ID = "upcoming_payments_channel"
    private const val BUDGET_ALERTS_CHANNEL_ID = "budget_alerts_channel"

    private const val APP_UPDATES_CHANNEL_ID = "app_updates_channel"

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

        val appUpdatesChannel = NotificationChannel(
            APP_UPDATES_CHANNEL_ID,
            "App Updates",
            NotificationManager.IMPORTANCE_LOW // Low importance is fine for update checks
        ).apply {
            description = "Notifications for new app versions available."
        }
        notificationManager.createNotificationChannel(appUpdatesChannel)
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
    fun showUpdateAvailableNotification(context: Context, release: GitHubRelease) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        // Create an explicit intent that opens the release URL in the app's WebViewActivity
        val intent = Intent(context, WebViewActivity::class.java).apply {
            putExtra("url", release.htmlUrl)
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notificationId = 99 // A fixed ID for the update notification

        val builder = NotificationCompat.Builder(context, APP_UPDATES_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notifications)
            .setContentTitle("Update Available: ${release.name}")
            .setContentText("Tap to download and install the latest version.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent) // This makes the notification clickable
            .setAutoCancel(true) // Dismiss the notification when tapped

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }
}
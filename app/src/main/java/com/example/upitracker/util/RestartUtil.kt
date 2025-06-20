package com.example.upitracker.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import kotlin.system.exitProcess

object RestartUtil {
    /**
     * Schedules the application to be restarted after a brief delay and then terminates the current process.
     * @param context The application context.
     */
    fun restartApp(context: Context) {
        val packageName = context.packageName
        val packageManager = context.packageManager

        // Create an intent identical to the one used to launch the app from the home screen.
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        } ?: return // Exit if the launch intent cannot be found

        // Create a PendingIntent to be fired by the AlarmManager.
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Get the AlarmManager system service.
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Set an alarm to trigger the PendingIntent in a short time (e.g., 500ms).
        alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 500, pendingIntent)

        // Terminate the current app process.
        exitProcess(0)
    }
}
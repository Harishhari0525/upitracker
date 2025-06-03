package com.example.upitracker.util // Or your preferred package

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.upitracker.data.AppDatabase
import java.util.Calendar
import java.util.Date

class CleanupArchivedSmsWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "CleanupArchivedSmsWorker"
        // Define retention period, e.g., 30 days. This could be made configurable later.
        const val RETENTION_PERIOD_DAYS = 30L
    }

    override suspend fun doWork(): Result {
        Log.d(WORK_NAME, "Starting cleanup of old archived SMS messages.")
        return try {
            val database = AppDatabase.getDatabase(applicationContext)
            val archivedSmsDao = database.archivedSmsMessageDao()

            // Calculate the cutoff timestamp
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -RETENTION_PERIOD_DAYS.toInt())
            // Set to the beginning of that day to ensure full days are counted
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val cutoffTimestamp = calendar.timeInMillis

            Log.d(WORK_NAME, "Calculated cutoff timestamp: $cutoffTimestamp (${Date(cutoffTimestamp)}) for $RETENTION_PERIOD_DAYS days retention.")

            // Delete records older than the cutoff timestamp
            // The DAO method needs to be implemented if it's just a placeholder
            // Assuming `deleteOldArchivedSms` takes a timestamp and deletes records *before* it.
            archivedSmsDao.deleteOldArchivedSms(cutoffTimestamp) // This was in ArchivedSmsMessageDao.kt

            Log.i(WORK_NAME, "Cleanup of old archived SMS messages completed successfully.")
            Result.success()
        } catch (e: Exception) {
            Log.e(WORK_NAME, "Error during cleanup of archived SMS messages.", e)
            Result.failure()
        }
    }
}
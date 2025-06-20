package com.example.upitracker.util

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.upitracker.data.AppDatabase
import java.util.concurrent.TimeUnit

class PermanentDeleteWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "PermanentDeleteWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(WORK_NAME, "Starting permanent deletion of old soft-deleted transactions.")
        return try {
            val database = AppDatabase.getDatabase(applicationContext)
            val transactionDao = database.transactionDao()

            // Set a cutoff time. For example, delete items that have been in the trash for more than 1 day.
            val oneDayInMillis = TimeUnit.DAYS.toMillis(1)
            val cutoffTimestamp = System.currentTimeMillis() - oneDayInMillis

            // This new DAO method does the actual deletion.
            transactionDao.permanentlyDeletePending(cutoffTimestamp)

            Log.i(WORK_NAME, "Permanent deletion worker completed successfully.")
            Result.success()
        } catch (e: Exception) {
            Log.e(WORK_NAME, "Error during permanent deletion of transactions.", e)
            Result.failure()
        }
    }
}
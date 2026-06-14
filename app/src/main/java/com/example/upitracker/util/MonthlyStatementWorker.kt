package com.example.upitracker.util

import android.content.Context
import android.util.Log
import androidx.core.content.FileProvider
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.upitracker.data.AppDatabase
import kotlinx.coroutines.flow.first
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MonthlyStatementWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "monthly_statement_worker"
    }

    override suspend fun doWork(): Result {
        Log.d(WORK_NAME, "Starting automated monthly statement generation...")

        val context = applicationContext
        val db = AppDatabase.getDatabase(context)
        val transactionDao = db.transactionDao()

        try {
            val calendar = Calendar.getInstance()
            
            // Only run this logic if it's the 1st day of the month
            if (calendar.get(Calendar.DAY_OF_MONTH) != 1) {
                Log.d(WORK_NAME, "Not the 1st of the month. Skipping statement generation.")
                return Result.success()
            }
            
            // Get the previous month's date range
            calendar.add(Calendar.MONTH, -1)
            
            // Format month name for display
            val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            val monthName = monthFormat.format(calendar.time)

            // Start of previous month
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startDate = calendar.timeInMillis

            // End of previous month
            calendar.add(Calendar.MONTH, 1)
            calendar.add(Calendar.MILLISECOND, -1)
            val endDate = calendar.timeInMillis

            // Fetch transactions for the previous month
            val monthTransactions = transactionDao.getTransactionsInRangeSync(startDate, endDate)

            if (monthTransactions.isEmpty()) {
                Log.d(WORK_NAME, "No transactions found for $monthName. Skipping statement.")
                return Result.success()
            }

            // Generate the PDF file in the cache directory
            val fileName = "UPI_Tracker_Statement_${monthName.replace(" ", "_")}.pdf"
            val file = File(context.cacheDir, fileName)
            
            // This is a simplified call; assuming PdfGenerator has a method that writes to a File
            // Since PdfGenerator in this codebase writes directly to an OutputStream via URI, 
            // we create a URI using FileProvider.
            
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)

            // Use the existing PdfGenerator
            PdfGenerator.generatePassbookPdf(
                context = context,
                transactions = monthTransactions,
                statementPeriod = monthName,
                targetUri = uri
            )

            // Show the notification with the intent to email the generated PDF
            NotificationHelper.showMonthlyStatementNotification(context, uri, monthName)

            Log.d(WORK_NAME, "Successfully generated and notified statement for $monthName.")
            return Result.success()

        } catch (e: Exception) {
            Log.e(WORK_NAME, "Error generating monthly statement", e)
            return Result.retry()
        }
    }
}

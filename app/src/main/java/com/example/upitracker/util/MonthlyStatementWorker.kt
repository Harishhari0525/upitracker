package com.example.upitracker.util

import android.content.Context
import android.util.Log
import androidx.core.content.FileProvider
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.upitracker.data.AppDatabase
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

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
            val staleCutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2)
            val statementDirectory = File(context.cacheDir, "statements").apply {
                if (!exists() && !mkdirs()) {
                    throw java.io.IOException("Unable to create statement cache directory")
                }
            }
            statementDirectory.listFiles { file ->
                file.isFile && file.name.startsWith("UPI_Tracker_Statement_") && file.extension == "pdf"
            }?.filter { it.lastModified() < staleCutoff }?.forEach { it.delete() }

            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MONTH, -1)
            val periodKey = SimpleDateFormat("yyyy-MM", Locale.ROOT).format(calendar.time)
            val lastGeneratedPeriod =
                ThemePreference.getLastMonthlyStatementPeriodFlow(context).first()
            if (lastGeneratedPeriod == periodKey) {
                return Result.success()
            }
            
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
                ThemePreference.setLastMonthlyStatementPeriod(context, periodKey)
                return Result.success()
            }

            // Generate the PDF file in the cache directory
            val fileName = "UPI_Tracker_Statement_${monthName.replace(" ", "_")}.pdf"
            val file = File(statementDirectory, fileName)
            if (file.exists() && !file.delete()) {
                throw java.io.IOException("Unable to replace previous statement")
            }
            
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
            ThemePreference.setLastMonthlyStatementPeriod(context, periodKey)

            Log.d(WORK_NAME, "Successfully generated and notified statement for $monthName.")
            return Result.success()

        } catch (e: Exception) {
            Log.e(WORK_NAME, "Error generating monthly statement", e)
            return Result.retry()
        }
    }
}

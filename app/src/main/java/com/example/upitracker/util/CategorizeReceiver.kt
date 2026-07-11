package com.example.upitracker.util

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.RemoteInput
import com.example.upitracker.data.AppDatabase
import com.example.upitracker.data.Category
import com.example.upitracker.util.NotificationHelper.EXTRA_CATEGORY
import com.example.upitracker.util.NotificationHelper.EXTRA_NOTIFICATION_ID
import com.example.upitracker.util.NotificationHelper.EXTRA_TRANSACTION_ID
import com.example.upitracker.util.NotificationHelper.REMOTE_INPUT_CATEGORY
import com.example.upitracker.util.inferCategoryDefaults
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CategorizeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val transactionId = intent.getIntExtra(EXTRA_TRANSACTION_ID, -1)
        val quickCategory = intent.getStringExtra(EXTRA_CATEGORY)
        val typedCategory = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(REMOTE_INPUT_CATEGORY)
            ?.toString()
        val category = (typedCategory ?: quickCategory)
            ?.trim()
            ?.replace(Regex("\\s+"), " ")
            ?.takeIf { it.isNotBlank() }
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        if (transactionId != -1 && category != null) {
            // Use a Coroutine to update the DB in the background
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val dao = db.transactionDao()
                    val categoryDao = db.categoryDao()

                    // 1. Fetch the transaction
                    val transaction = dao.getTransactionByIdSync(transactionId) // We need a sync method in DAO

                    if (transaction == null) {
                        Log.w("CategorizeReceiver", "Transaction not found for notification category update")
                        return@launch
                    }

                    // 2. Ensure manually typed categories become reusable in the app.
                    val existingCategory = categoryDao.getCategoryByNameCaseInsensitive(category)
                    val finalCategory = existingCategory?.name ?: category
                    if (existingCategory == null) {
                        val defaults = inferCategoryDefaults(finalCategory)
                        categoryDao.insert(
                            Category(
                                name = finalCategory,
                                iconName = defaults.iconName,
                                colorHex = defaults.colorHex
                            )
                        )
                    }

                    // 3. Update category
                    dao.update(transaction.copy(category = finalCategory))
                    Log.d("CategorizeReceiver", "Transaction category updated")

                    // 4. Dismiss the notification
                    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.cancel(notificationId)
                } catch (e: Exception) {
                    Log.e("CategorizeReceiver", "Error updating category", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}

package com.example.upitracker.util

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.upitracker.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CategorizeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val transactionId = intent.getIntExtra("TXN_ID", -1)
        val category = intent.getStringExtra("CATEGORY")
        val notificationId = intent.getIntExtra("NOTIF_ID", -1)

        if (transactionId != -1 && category != null) {
            // Use a Coroutine to update the DB in the background
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val dao = db.transactionDao()

                    // 1. Fetch the transaction
                    val transaction = dao.getTransactionByIdSync(transactionId) // We need a sync method in DAO

                    // 2. Update category
                    if (transaction != null) {
                        dao.update(transaction.copy(category = category))
                        Log.d("CategorizeReceiver", "Transaction $transactionId updated to $category")
                    }

                    // 3. Dismiss the notification
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
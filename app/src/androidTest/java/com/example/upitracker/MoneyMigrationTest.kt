package com.example.upitracker

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.upitracker.data.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyMigrationTest {
    @Test fun migration24To27PreservesAndRoundsMoney() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val name = "migration-money-test.db"
        context.deleteDatabase(name)
        helper(name, 24, object : SupportSQLiteOpenHelper.Callback(24) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE transactions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, amount REAL NOT NULL, type TEXT NOT NULL, date INTEGER NOT NULL, description TEXT NOT NULL, senderOrReceiver TEXT NOT NULL, note TEXT NOT NULL, category TEXT, isArchived INTEGER NOT NULL, pendingDeletionTimestamp INTEGER, linkedTransactionId INTEGER, bankName TEXT, balanceAfterTransaction REAL, receiptImagePath TEXT, tags TEXT NOT NULL)")
                db.execSQL("CREATE TABLE budgets (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, categoryName TEXT NOT NULL, budgetAmount REAL NOT NULL, periodType TEXT NOT NULL, startDate INTEGER NOT NULL, isActive INTEGER NOT NULL, allowRollover INTEGER NOT NULL, lastNotificationTimestamp INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE recurring_rules (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, amount REAL NOT NULL, description TEXT NOT NULL, categoryName TEXT NOT NULL, periodType TEXT NOT NULL, dayOfPeriod INTEGER NOT NULL, nextDueDate INTEGER NOT NULL, creationDate INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE upi_lite_summaries (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, transactionCount INTEGER NOT NULL, totalAmount REAL NOT NULL, date INTEGER NOT NULL, bank TEXT NOT NULL)")
                db.execSQL("CREATE TABLE archived_sms_messages (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, originalSender TEXT NOT NULL, originalBody TEXT NOT NULL, originalTimestamp INTEGER NOT NULL, backupTimestamp INTEGER NOT NULL)")
                db.execSQL("INSERT INTO transactions VALUES (1, 10.005, 'DEBIT', 1, 'x', 'y', '', NULL, 0, NULL, NULL, 'SBI', 100.125, NULL, '')")
                db.execSQL("INSERT INTO budgets VALUES (1, 'Food', 500.555, 'MONTHLY', 1, 1, 0, 0)")
                db.execSQL("INSERT INTO recurring_rules VALUES (1, 99.995, 'Plan', 'Bills', 'MONTHLY', 1, 1, 1)")
                db.execSQL("INSERT INTO upi_lite_summaries VALUES (1, 2, 12.345, 1, 'SBI')")
                db.execSQL("INSERT INTO archived_sms_messages VALUES (1, 'SBI', 'sensitive body', 1, 1)")
            }
            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }).use { it.writableDatabase }

        helper(name, 27, object : SupportSQLiteOpenHelper.Callback(27) {
            override fun onCreate(db: SupportSQLiteDatabase) = Unit
            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                AppDatabase.MIGRATION_24_25.migrate(db)
                AppDatabase.MIGRATION_25_26.migrate(db)
                AppDatabase.MIGRATION_26_27.migrate(db)
            }
        }).use { open ->
            val db = open.writableDatabase
            db.query("SELECT amount, balanceAfterTransaction FROM transactions").use {
                it.moveToFirst(); assertEquals(1001L, it.getLong(0)); assertEquals(10013L, it.getLong(1))
            }
            db.query("SELECT originalBody, parseStatus FROM archived_sms_messages").use {
                it.moveToFirst(); assertEquals("redacted:1", it.getString(0)); assertEquals("PARSED", it.getString(1))
            }
        }
        context.deleteDatabase(name)
    }

    private fun helper(name: String, version: Int, callback: SupportSQLiteOpenHelper.Callback) =
        FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(ApplicationProvider.getApplicationContext())
                .name(name).callback(callback).build()
        )
}

package com.example.upitracker.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Transaction::class,
        UpiLiteSummary::class,
        ArchivedSmsMessage::class, // ✨ Add new entity ✨
        Budget::class
    ],
    version = 8, // ✨ Version Incremented to 4 ✨
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun upiLiteSummaryDao(): UpiLiteSummaryDao
    abstract fun archivedSmsMessageDao(): ArchivedSmsMessageDao // ✨ Add new DAO abstract function ✨
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        // Migration from version 1 to version 2 (UpiLiteSummary.date String to Long)
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS upi_lite_summaries")
                // Room auto-creates based on new schema for version 2
            }
        }

        // Migration from version 2 to version 3 (Add 'category' column to 'transactions')
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN category TEXT DEFAULT NULL")
            }
        }

        // ✨ New: Migration from version 3 to version 4 (Add 'archived_sms_messages' table) ✨
        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `archived_sms_messages` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `originalSender` TEXT NOT NULL,
                        `originalBody` TEXT NOT NULL,
                        `originalTimestamp` INTEGER NOT NULL,
                        `backupTimestamp` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }
        val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add the new 'isArchived' column to the 'transactions' table.
                // It's an INTEGER because Boolean maps to INTEGER (0 or 1) in SQLite.
                // Default to 0 (false). NOT NULL constraint is good practice.
                db.execSQL("ALTER TABLE transactions ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_5_6: Migration = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `budgets` (
                        `categoryName` TEXT NOT NULL, 
                        `budgetAmount` REAL NOT NULL, 
                        PRIMARY KEY(`categoryName`)
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_6_7: Migration = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Drop the old budgets table
                db.execSQL("DROP TABLE IF EXISTS `budgets`")
                // Create the new budgets table based on the updated Budget entity
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `budgets` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `categoryName` TEXT NOT NULL, 
                        `budgetAmount` REAL NOT NULL,
                        `periodType` TEXT NOT NULL,
                        `startDate` INTEGER NOT NULL,
                        `isActive` INTEGER NOT NULL DEFAULT 1
                    )
                """.trimIndent())
            }
        }
        val MIGRATION_7_8: Migration = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add the new 'allowRollover' column to the 'budgets' table.
                // INTEGER is used for Boolean (0=false, 1=true). Default to 0 (false).
                db.execSQL("ALTER TABLE budgets ADD COLUMN allowRollover INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "upi_tracker_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8) // ✨ Add new migration ✨
                    // Consider .fallbackToDestructiveMigration() only if absolutely necessary during heavy dev
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
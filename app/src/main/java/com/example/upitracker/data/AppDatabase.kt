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
        ArchivedSmsMessage::class // ✨ Add new entity ✨
    ],
    version = 4, // ✨ Version Incremented to 4 ✨
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun upiLiteSummaryDao(): UpiLiteSummaryDao
    abstract fun archivedSmsMessageDao(): ArchivedSmsMessageDao // ✨ Add new DAO abstract function ✨

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

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "upi_tracker_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4) // ✨ Add new migration ✨
                    // Consider .fallbackToDestructiveMigration() only if absolutely necessary during heavy dev
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
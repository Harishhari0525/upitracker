package com.example.upitracker.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Transaction::class, UpiLiteSummary::class],
    version = 3, // ✨ Version Incremented to 3 ✨
    exportSchema = false // Keep this true for good practice
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun upiLiteSummaryDao(): UpiLiteSummaryDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        // Migration from version 1 to version 2 (UpiLiteSummary.date String to Long)
        // This should remain if users might upgrade from version 1.
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // For UpiLiteSummary table: date column changed from TEXT (String) to INTEGER (Long).
                // This migration drops the old upi_lite_summaries table.
                // Room will then automatically create the new upi_lite_summaries table
                // based on the updated UpiLiteSummary entity definition.
                // Existing UpiLiteSummary data will be lost. Transaction data is unaffected by this specific migration.
                db.execSQL("DROP TABLE IF EXISTS upi_lite_summaries")
                // Room will auto-create the new table structure based on the entity for version 2.
            }
        }

        // ✨ New: Migration from version 2 to version 3 (Add 'category' column to 'transactions') ✨
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add the new 'category' column to the 'transactions' table.
                // It's a TEXT type because String? maps to TEXT in SQLite.
                // It can be NULL (nullable String).
                db.execSQL("ALTER TABLE transactions ADD COLUMN category TEXT DEFAULT NULL")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "upi_tracker_db"
                )
                    // ✨ Add ALL your migration paths ✨
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    // If you want to handle cases where a migration path isn't defined for other future versions
                    // (especially during development), you can still add a fallback.
                    // However, for defined paths like 1 to 2 and 2 to 3, these migrations will be used.
                    // .fallbackToDestructiveMigration() // Consider removing if you are providing all necessary migrations.
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
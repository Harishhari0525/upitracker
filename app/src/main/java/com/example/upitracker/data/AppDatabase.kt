package com.example.upitracker.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Transaction::class, UpiLiteSummary::class],
    version = 2, // ✨ Version Incremented to 2 ✨
    exportSchema = false // ✨ Recommended: Set to true. Create a schemas folder in your app directory.
    // After building, Room will generate a schema/2.json file there. Commit this to version control.
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun upiLiteSummaryDao(): UpiLiteSummaryDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        // ✨ Define your Migration from version 1 to version 2 ✨
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // For UpiLiteSummary table: date column changed from TEXT (String) to INTEGER (Long).
                // Converting "DD-MMM-YY" text to a proper timestamp in SQL is complex and error-prone.
                // This migration will drop the old upi_lite_summaries table.
                // Room will then automatically create the new upi_lite_summaries table
                // based on the updated UpiLiteSummary entity definition.
                // This means existing UpiLiteSummary data will be lost. Transaction data is unaffected.
                db.execSQL("DROP TABLE IF EXISTS upi_lite_summaries")

                // Note: If the 'transactions' table schema had also changed,
                // you would add SQL ALTER TABLE statements for it here.
                // For example:
                // db.execSQL("ALTER TABLE transactions ADD COLUMN new_column_name INTEGER DEFAULT 0 NOT NULL")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "upi_tracker_db"
                )
                    // ✨ Add the migration path from version 1 to version 2 ✨
                    .addMigrations(MIGRATION_1_2)
                    // If you want to handle cases where a migration path isn't defined for other future versions
                    // (especially during development), you can still add a fallback.
                    // However, for defined paths like 1 to 2, MIGRATION_1_2 will be used.
                    // .fallbackToDestructiveMigration() // Remove this if you are providing all necessary migrations
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
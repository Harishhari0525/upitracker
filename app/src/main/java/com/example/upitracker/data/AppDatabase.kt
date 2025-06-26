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
        Budget::class,
        CategorySuggestionRule::class,
        RecurringRule::class
    ],
    version = 13,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun upiLiteSummaryDao(): UpiLiteSummaryDao
    abstract fun archivedSmsMessageDao(): ArchivedSmsMessageDao // ✨ Add new DAO abstract function ✨
    abstract fun budgetDao(): BudgetDao
    abstract fun categorySuggestionRuleDao(): CategorySuggestionRuleDao
    abstract fun recurringRuleDao(): RecurringRuleDao

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
        val MIGRATION_8_9: Migration = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `category_suggestion_rules` (
                        `keyword` TEXT NOT NULL, 
                        `categoryName` TEXT NOT NULL, 
                        PRIMARY KEY(`keyword`)
                    )
                """.trimIndent())
            }
        }
        val MIGRATION_9_10: Migration = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN pendingDeletionTimestamp INTEGER DEFAULT NULL")
            }
        }
        val MIGRATION_10_11: Migration = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `recurring_rules` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `amount` REAL NOT NULL,
                        `description` TEXT NOT NULL,
                        `categoryName` TEXT NOT NULL,
                        `periodType` TEXT NOT NULL,
                        `dayOfPeriod` INTEGER NOT NULL,
                        `nextDueDate` INTEGER NOT NULL,
                        `creationDate` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_11_12: Migration = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Drop the old simple table
                db.execSQL("DROP TABLE IF EXISTS `category_suggestion_rules`")
                // Create the new, more powerful table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `category_suggestion_rules` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `fieldToMatch` TEXT NOT NULL,
                        `matcher` TEXT NOT NULL,
                        `keyword` TEXT NOT NULL,
                        `categoryName` TEXT NOT NULL,
                        `priority` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }
        val MIGRATION_12_13: Migration = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Step 1: Create a new temporary table with the correct, final schema.
                db.execSQL("""
            CREATE TABLE `archived_sms_messages_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `originalSender` TEXT NOT NULL,
                `originalBody` TEXT NOT NULL,
                `originalTimestamp` INTEGER NOT NULL,
                `backupTimestamp` INTEGER NOT NULL
            )
        """.trimIndent())

                // Step 2: Copy only the unique rows from the old table into the new one.
                // The GROUP BY clause ensures that only one row for each combination of
                // sender, body, and timestamp is inserted.
                db.execSQL("""
            INSERT INTO `archived_sms_messages_new` (`originalSender`, `originalBody`, `originalTimestamp`, `backupTimestamp`)
            SELECT `originalSender`, `originalBody`, `originalTimestamp`, MIN(`backupTimestamp`)
            FROM `archived_sms_messages`
            GROUP BY `originalSender`, `originalBody`, `originalTimestamp`
        """.trimIndent())

                // Step 3: Drop the old table with the duplicate data.
                db.execSQL("DROP TABLE `archived_sms_messages`")

                // Step 4: Rename our new, clean table to the original table's name.
                db.execSQL("ALTER TABLE `archived_sms_messages_new` RENAME TO `archived_sms_messages`")

                // Step 5: Finally, add the unique index to the now-clean table. This ensures
                // the schema matches what Room expects for version 13.
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_archived_sms_messages_on_content` ON `archived_sms_messages` (`originalSender`, `originalBody`, `originalTimestamp`)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "upi_tracker_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6,
                        MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11
                        ,
                        MIGRATION_11_12, MIGRATION_12_13) // ✨ Add new migration ✨
                    .fallbackToDestructiveMigration(false)  // only if absolutely necessary during heavy dev
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
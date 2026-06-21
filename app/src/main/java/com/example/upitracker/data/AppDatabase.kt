package com.example.upitracker.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Transaction::class,
        UpiLiteSummary::class,
        ArchivedSmsMessage::class, // ✨ Add new entity ✨
        Budget::class,
        CategorySuggestionRule::class,
        RecurringRule::class,
        Category::class
    ],
    version = 27,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun upiLiteSummaryDao(): UpiLiteSummaryDao
    abstract fun archivedSmsMessageDao(): ArchivedSmsMessageDao // ✨ Add new DAO abstract function ✨
    abstract fun budgetDao(): BudgetDao
    abstract fun categorySuggestionRuleDao(): CategorySuggestionRuleDao
    abstract fun recurringRuleDao(): RecurringRuleDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        val MIGRATION_25_26: Migration = object : Migration(25, 26) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE archived_sms_messages SET originalBody = 'redacted:' || id")
            }
        }

        val MIGRATION_26_27: Migration = object : Migration(26, 27) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE archived_sms_messages ADD COLUMN parseStatus TEXT NOT NULL DEFAULT 'PARSED'")
            }
        }

        val MIGRATION_22_23: Migration = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN balanceAfterTransaction REAL DEFAULT NULL")
            }
        }

        val MIGRATION_23_24: Migration = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP INDEX IF EXISTS `index_transactions_amount_date_type`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_amount_date_type` ON `transactions` (`amount`, `date`, `type`)")
            }
        }

        val MIGRATION_24_25: Migration = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE `transactions_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `amount` INTEGER NOT NULL,
                        `type` TEXT NOT NULL,
                        `date` INTEGER NOT NULL,
                        `description` TEXT NOT NULL,
                        `senderOrReceiver` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `category` TEXT,
                        `isArchived` INTEGER NOT NULL,
                        `pendingDeletionTimestamp` INTEGER,
                        `linkedTransactionId` INTEGER,
                        `bankName` TEXT,
                        `balanceAfterTransaction` INTEGER,
                        `receiptImagePath` TEXT,
                        `tags` TEXT NOT NULL
                    )
                """)
                db.execSQL("""
                    INSERT INTO `transactions_new`
                    SELECT `id`, CAST(ROUND(`amount` * 100.0) AS INTEGER), `type`, `date`,
                           `description`, `senderOrReceiver`, `note`, `category`, `isArchived`,
                           `pendingDeletionTimestamp`, `linkedTransactionId`, `bankName`,
                           CASE WHEN `balanceAfterTransaction` IS NULL THEN NULL
                                ELSE CAST(ROUND(`balanceAfterTransaction` * 100.0) AS INTEGER) END,
                           `receiptImagePath`, `tags`
                    FROM `transactions`
                """)
                db.execSQL("DROP TABLE `transactions`")
                db.execSQL("ALTER TABLE `transactions_new` RENAME TO `transactions`")
                db.execSQL("CREATE INDEX `index_transactions_amount_date_type` ON `transactions` (`amount`, `date`, `type`)")
                db.execSQL("CREATE INDEX `index_transactions_isArchived_pendingDeletionTimestamp_date` ON `transactions` (`isArchived`, `pendingDeletionTimestamp`, `date`)")
                db.execSQL("CREATE INDEX `index_transactions_category` ON `transactions` (`category`)")
                db.execSQL("CREATE INDEX `index_transactions_bankName` ON `transactions` (`bankName`)")

                db.execSQL("""
                    CREATE TABLE `budgets_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `categoryName` TEXT NOT NULL,
                        `budgetAmount` INTEGER NOT NULL,
                        `periodType` TEXT NOT NULL,
                        `startDate` INTEGER NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        `allowRollover` INTEGER NOT NULL,
                        `lastNotificationTimestamp` INTEGER NOT NULL
                    )
                """)
                db.execSQL("""
                    INSERT INTO `budgets_new`
                    SELECT `id`, `categoryName`, CAST(ROUND(`budgetAmount` * 100.0) AS INTEGER),
                           `periodType`, `startDate`, `isActive`, `allowRollover`, `lastNotificationTimestamp`
                    FROM `budgets`
                """)
                db.execSQL("DROP TABLE `budgets`")
                db.execSQL("ALTER TABLE `budgets_new` RENAME TO `budgets`")

                db.execSQL("""
                    CREATE TABLE `recurring_rules_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `amount` INTEGER NOT NULL,
                        `description` TEXT NOT NULL,
                        `categoryName` TEXT NOT NULL,
                        `periodType` TEXT NOT NULL,
                        `dayOfPeriod` INTEGER NOT NULL,
                        `nextDueDate` INTEGER NOT NULL,
                        `creationDate` INTEGER NOT NULL
                    )
                """)
                db.execSQL("""
                    INSERT INTO `recurring_rules_new`
                    SELECT `id`, CAST(ROUND(`amount` * 100.0) AS INTEGER), `description`,
                           `categoryName`, `periodType`, `dayOfPeriod`, `nextDueDate`, `creationDate`
                    FROM `recurring_rules`
                """)
                db.execSQL("DROP TABLE `recurring_rules`")
                db.execSQL("ALTER TABLE `recurring_rules_new` RENAME TO `recurring_rules`")

                db.execSQL("""
                    CREATE TABLE `upi_lite_summaries_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `transactionCount` INTEGER NOT NULL,
                        `totalAmount` INTEGER NOT NULL,
                        `date` INTEGER NOT NULL,
                        `bank` TEXT NOT NULL
                    )
                """)
                db.execSQL("""
                    INSERT INTO `upi_lite_summaries_new`
                    SELECT `id`, `transactionCount`, CAST(ROUND(`totalAmount` * 100.0) AS INTEGER), `date`, `bank`
                    FROM `upi_lite_summaries`
                """)
                db.execSQL("DROP TABLE `upi_lite_summaries`")
                db.execSQL("ALTER TABLE `upi_lite_summaries_new` RENAME TO `upi_lite_summaries`")
            }
        }

        val MIGRATION_21_22: Migration = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_isArchived_pendingDeletionTimestamp_date` ON `transactions` (`isArchived`, `pendingDeletionTimestamp`, `date`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_category` ON `transactions` (`category`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_bankName` ON `transactions` (`bankName`)")
            }
        }

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE upi_lite_summaries RENAME TO upi_lite_summaries_legacy")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `upi_lite_summaries` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `transactionCount` INTEGER NOT NULL,
                        `totalAmount` REAL NOT NULL,
                        `date` INTEGER NOT NULL,
                        `bank` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO upi_lite_summaries (id, transactionCount, totalAmount, date, bank)
                    SELECT id, transactionCount, totalAmount,
                           CASE
                               WHEN typeof(date) = 'integer' THEN CAST(date AS INTEGER)
                               WHEN date GLOB '[0-9]*' THEN CAST(date AS INTEGER)
                               ELSE 0
                           END,
                           bank
                    FROM upi_lite_summaries_legacy
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE upi_lite_summaries_legacy")
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

        private class AppDatabaseCallback(
            private val context: Context, // Inject context directly to safely fetch the database thread instance
            private val scope: CoroutineScope
        ) : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)

                scope.launch {
                    try {
                        val database = getDatabase(context)
                        populateDefaultCategories(database.categoryDao())
                    } catch (e: Exception) {
                        android.util.Log.e("AppDatabase", "Error during primary category auto-seed pass", e)
                    }
                }
            }
        }

        suspend fun populateDefaultCategories(categoryDao: CategoryDao) {
            val defaultCategories = listOf(
                Category(name = "Food",          iconName = "Fast-food",          colorHex = "#FFC107"),
                Category(name = "Shopping",      iconName = "ShoppingBag",        colorHex = "#4CAF50"),
                Category(name = "Transport",     iconName = "DirectionsCar",      colorHex = "#2196F3"),
                Category(name = "Bills",         iconName = "ReceiptLong",        colorHex = "#9C27B0"),
                Category(name = "Entertainment", iconName = "Theaters",           colorHex = "#E91E63"),
                Category(name = "Groceries",     iconName = "LocalGroceryStore",  colorHex = "#FF5722"),
                Category(name = "Health",        iconName = "Favorite",           colorHex = "#F44336"),
                Category(name = "Rent",          iconName = "HomeWork",           colorHex = "#795548"),
                Category(name = "Other",         iconName = "MoreHoriz",          colorHex = "#607D8B"),
                Category(name = "Salary",        iconName = "Payments",           colorHex = "#009688")
            )
            categoryDao.insertAll(defaultCategories)
        }

        val MIGRATION_13_14: Migration = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                CREATE TABLE IF NOT EXISTS `categories_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `iconName` TEXT NOT NULL,
                    `colorHex` TEXT NOT NULL
                )
                """)

                val tableExists = db.query(
                    "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = 'categories'"
                ).use { it.moveToFirst() }
                if (tableExists) {
                    val columns = mutableSetOf<String>()
                    db.query("PRAGMA table_info(`categories`)").use { cursor ->
                        val nameIndex = cursor.getColumnIndex("name")
                        while (cursor.moveToNext()) columns += cursor.getString(nameIndex)
                    }
                    if ("name" in columns) {
                        val idExpression = if ("id" in columns) "`id`" else "NULL"
                        val iconExpression = if ("iconName" in columns) "COALESCE(`iconName`, 'Category')" else "'Category'"
                        val colorExpression = if ("colorHex" in columns) "COALESCE(`colorHex`, '#808080')" else "'#808080'"
                        db.execSQL(
                            "INSERT OR IGNORE INTO `categories_new` (`id`, `name`, `iconName`, `colorHex`) " +
                                "SELECT $idExpression, `name`, $iconExpression, $colorExpression FROM `categories`"
                        )
                    }
                    db.execSQL("DROP TABLE `categories`")
                }
                db.execSQL("ALTER TABLE `categories_new` RENAME TO `categories`")
            }
        }

        val MIGRATION_14_15: Migration = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN linkedTransactionId INTEGER DEFAULT NULL")
            }
        }
        val MIGRATION_15_16: Migration = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN bankName TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_16_17: Migration = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add the new 'logic' column with a default value of 'ANY' for all existing rules.
                db.execSQL("ALTER TABLE category_suggestion_rules ADD COLUMN logic TEXT NOT NULL DEFAULT 'ANY'")
            }
        }

        val MIGRATION_17_18: Migration = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE budgets ADD COLUMN lastNotificationTimestamp INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_18_19: Migration = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN receiptImagePath TEXT DEFAULT NULL")
            }
        }
        val MIGRATION_19_20: Migration = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add 'tags' column, default to empty string
                db.execSQL("ALTER TABLE transactions ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_20_21: Migration = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_transactions_amount_date_type` ON `transactions` (`amount`, `date`, `type`)")
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
                        MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11,
                        MIGRATION_11_12, MIGRATION_12_13,MIGRATION_13_14, MIGRATION_14_15,MIGRATION_15_16,MIGRATION_16_17,MIGRATION_17_18,
                        MIGRATION_18_19,MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23,
                        MIGRATION_23_24, MIGRATION_24_25, MIGRATION_25_26, MIGRATION_26_27)
                    .addCallback(AppDatabaseCallback(context.applicationContext, CoroutineScope(Dispatchers.IO)))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun closeDatabase() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }
    }
}

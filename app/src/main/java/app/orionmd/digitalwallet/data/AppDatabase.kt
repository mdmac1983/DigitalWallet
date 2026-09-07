package app.orionmd.digitalwallet.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration

@Database(
    entities = [
        CardEntity::class, IdDocumentEntity::class, PasswordEntity::class,
        ContactEntity::class, FinanceEntity::class, StatementEntity::class, HoldingEntity::class,
        SaleEntity::class
    ],
    version = 10,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun cardDao(): CardDao
    abstract fun idDocumentDao(): IdDocumentDao
    abstract fun passwordDao(): PasswordDao
    abstract fun contactDao(): ContactDao
    abstract fun financeDao(): FinanceDao
    abstract fun statementDao(): StatementDao
    abstract fun holdingDao(): HoldingDao
    abstract fun saleDao(): SaleDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        /** v1 -> v2: added the "Finances at a Glance" statement-import feature - a new
         * statements table, plus a nullable statementId column on finances linking an imported
         * entry back to the statement it came from. Written by hand (rather than
         * fallbackToDestructiveMigration) so upgrading doesn't wipe anything already saved. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `statements` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `type` TEXT NOT NULL,
                        `institutionEnc` TEXT NOT NULL,
                        `statementMonthEnc` TEXT NOT NULL,
                        `sourceFileName` TEXT,
                        `sourceDisplayNameEnc` TEXT NOT NULL,
                        `sourceMimeType` TEXT,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("ALTER TABLE finances ADD COLUMN statementId INTEGER")
            }
        }

        /** v2 -> v3: added manually-entered stock/crypto holdings ("Portfolio" section of
         * Finances at a Glance) - a new holdings table. Nothing existing changes shape, so this
         * is purely additive. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `holdings` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `holdingType` TEXT NOT NULL,
                        `symbolEnc` TEXT NOT NULL,
                        `nameEnc` TEXT NOT NULL,
                        `quantityEnc` TEXT NOT NULL,
                        `costBasisEnc` TEXT NOT NULL,
                        `currentPriceEnc` TEXT NOT NULL,
                        `notesEnc` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        /** v3 -> v4: added optional beginning/ending balance fields to a statement - what the
         * statement itself says its balance was, typed in by the user rather than computed by
         * the app. Purely additive; empty string (which round-trips through CryptoManager as
         * empty) means "not provided" for every row that existed before this migration. */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE statements ADD COLUMN beginningBalanceEnc TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE statements ADD COLUMN endingBalanceEnc TEXT NOT NULL DEFAULT ''")
            }
        }

        /** v4 -> v5: added the Sales tab (services sold to clients) - a new sales table. Purely
         * additive. */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sales` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `dateEnc` TEXT NOT NULL,
                        `clientIdEnc` TEXT NOT NULL,
                        `serviceEnc` TEXT NOT NULL,
                        `priceEnc` TEXT NOT NULL,
                        `commissionFeeEnc` TEXT NOT NULL,
                        `partnerFeeEnc` TEXT NOT NULL,
                        `paid` INTEGER NOT NULL,
                        `notesEnc` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        /** v5 -> v6: added an optional custom type-label to Finances entries (e.g. "Deposit" /
         * "Withdrawal" / "Debit" / "Credit" instead of the default Income/Expense wording).
         * Purely additive; blank (which round-trips through CryptoManager as empty) means "use
         * the default wording" for every row that existed before this migration. */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE finances ADD COLUMN typeLabelEnc TEXT NOT NULL DEFAULT ''")
            }
        }

        /** v6 -> v7: replaced Sales' plain paid/unpaid boolean with a status (Unpaid/Partial/Paid)
         * plus an amount-paid field, so a bill that's partially settled can say how much. SQLite
         * can't reliably drop/retype a column across every supported API level, so this rebuilds
         * the table: create the new shape, copy every row across (existing `paid=1` rows become
         * PAID with amountPaid left blank - see [amountPaidValue] for why a PAID row with a blank
         * amount is still treated as fully paid), drop the old table, rename the new one in. */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sales_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `dateEnc` TEXT NOT NULL,
                        `clientIdEnc` TEXT NOT NULL,
                        `serviceEnc` TEXT NOT NULL,
                        `priceEnc` TEXT NOT NULL,
                        `commissionFeeEnc` TEXT NOT NULL,
                        `partnerFeeEnc` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `amountPaidEnc` TEXT NOT NULL,
                        `notesEnc` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO sales_new
                        (id, dateEnc, clientIdEnc, serviceEnc, priceEnc, commissionFeeEnc, partnerFeeEnc, status, amountPaidEnc, notesEnc, createdAt, updatedAt)
                    SELECT
                        id, dateEnc, clientIdEnc, serviceEnc, priceEnc, commissionFeeEnc, partnerFeeEnc,
                        CASE WHEN paid = 1 THEN 'PAID' ELSE 'UNPAID' END, '', notesEnc, createdAt, updatedAt
                    FROM sales
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE sales")
                db.execSQL("ALTER TABLE sales_new RENAME TO sales")
            }
        }

        const val DB_NAME = "digital_wallet.db"

        /** v7 -> v8: added optional credit limit and billing address (street/city/state/zip)
         * fields to Cards. Purely additive. */
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cards ADD COLUMN creditLimitEnc TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE cards ADD COLUMN billingAddressEnc TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE cards ADD COLUMN billingCityEnc TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE cards ADD COLUMN billingStateEnc TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE cards ADD COLUMN billingZipEnc TEXT NOT NULL DEFAULT ''")
            }
        }

        /** v8 -> v9: added a persisted sortOrder column to cards and id_documents so both lists can
         * be manually reordered by dragging. Backfilled row-by-row (rather than a single SQL
         * statement) so every existing card/ID keeps the exact order it displayed in before this
         * upgrade (previously ORDER BY updatedAt DESC) instead of being silently reshuffled -
         * window functions like ROW_NUMBER() aren't reliably available across every SQLite version
         * Android ships. */
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cards ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE id_documents ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
                backfillSortOrder(db, "cards")
                backfillSortOrder(db, "id_documents")
            }

            private fun backfillSortOrder(db: SupportSQLiteDatabase, table: String) {
                val cursor = db.query("SELECT id FROM $table ORDER BY updatedAt DESC")
                cursor.use {
                    var order = 0
                    while (it.moveToNext()) {
                        val id = it.getLong(0)
                        db.execSQL("UPDATE $table SET sortOrder = ? WHERE id = ?", arrayOf(order, id))
                        order++
                    }
                }
            }
        }

        /** v9 -> v10: added a persisted sortOrder column to passwords and contacts (same reasoning
         * as [MIGRATION_8_9] for cards/id_documents - lets both lists support a Manual sort mode
         * that survives edits), plus a category column on contacts so Contacts can also be sorted
         * "by type" the way Passwords already could via its existing category column. Backfilled
         * the same row-by-row way as MIGRATION_8_9 so nothing visually reshuffles on upgrade. */
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE passwords ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE contacts ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE contacts ADD COLUMN category TEXT NOT NULL DEFAULT 'OTHER'")
                backfillSortOrder(db, "passwords")
                backfillSortOrder(db, "contacts")
            }

            private fun backfillSortOrder(db: SupportSQLiteDatabase, table: String) {
                val cursor = db.query("SELECT id FROM $table ORDER BY updatedAt DESC")
                cursor.use {
                    var order = 0
                    while (it.moveToNext()) {
                        val id = it.getLong(0)
                        db.execSQL("UPDATE $table SET sortOrder = ? WHERE id = ?", arrayOf(order, id))
                        order++
                    }
                }
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                ).addMigrations(
                    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
                    MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10
                ).build().also { instance = it }
            }
        }

        /** Closes the live connection and forgets the singleton, so the next [getInstance] call
         * opens a fresh one. Needed by [BackupManager] before it copies or replaces the database
         * file out from under Room - never call this while anything might still be mid-query. */
        fun closeInstance() {
            synchronized(this) {
                instance?.close()
                instance = null
            }
        }
    }
}

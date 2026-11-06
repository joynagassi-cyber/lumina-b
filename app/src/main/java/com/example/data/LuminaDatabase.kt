package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        BudgetEntity::class,
        CashRegisterEntity::class,
        ChurchProfileEntity::class,
        MemberEntity::class,
        UserEntity::class,
        ProjectEntity::class,
        SyncContributorEntity::class
    ],
    version = 12,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LuminaDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun cashRegisterDao(): CashRegisterDao
    abstract fun churchProfileDao(): ChurchProfileDao
    abstract fun memberDao(): MemberDao
    abstract fun userDao(): UserDao
    abstract fun projectDao(): ProjectDao
    abstract fun syncContributorDao(): SyncContributorDao

    companion object {
        private const val LEGACY_DATABASE_NAME = "lumina_financial.db"
        private val INSTANCES = mutableMapOf<String, LuminaDatabase>()
        private val DATABASE_SCOPE = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /** Compatibilité avec les tests et les installations historiques. */
        fun getDatabase(context: Context, scope: CoroutineScope? = null): LuminaDatabase =
            getDatabaseNamed(context, LEGACY_DATABASE_NAME, scope)

        /** Ouvre un fichier Room distinct pour chaque dossier d'église. */
        fun getDatabaseNamed(
            context: Context,
            databaseName: String,
            @Suppress("UNUSED_PARAMETER") scope: CoroutineScope? = null
        ): LuminaDatabase {
            require(databaseName.matches(Regex("[A-Za-z0-9_.-]+\\.db"))) { "Nom de base invalide" }
            return synchronized(INSTANCES) {
                INSTANCES[databaseName] ?: Room.databaseBuilder(
                    context.applicationContext,
                    LuminaDatabase::class.java,
                    databaseName
                )
                    .addCallback(LuminaDatabaseCallback(DATABASE_SCOPE, databaseName))
                    .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
                    // Les installations anciennes conservent leurs migrations explicites.
                    .fallbackToDestructiveMigrationFrom(1, 2, 3, 4)
                    .build()
                    .also { INSTANCES[databaseName] = it }
            }
        }

        fun closeDatabaseNamed(databaseName: String) {
            synchronized(INSTANCES) { INSTANCES.remove(databaseName)?.close() }
        }

        /**
         * Passage à la version 6 : identité stable des enregistrements (`uid`),
         * horodatage de dernière modification et tombes de suppression.
         *
         * Aucune donnée existante n'est perdue : les colonnes sont ajoutées avec
         * une valeur neutre, puis chaque ligne reçoit un identifiant unique.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // --- Écritures ---
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `uid` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `updatedBy` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE `transactions` SET `uid` = lower(hex(randomblob(16))) WHERE `uid` = ''")

                // --- Catégories ---
                db.execSQL("ALTER TABLE `categories` ADD COLUMN `uid` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `categories` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `categories` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE `categories` SET `uid` = lower(hex(randomblob(16))) WHERE `uid` = ''")

                // --- Caisses ---
                db.execSQL("ALTER TABLE `cash_registers` ADD COLUMN `uid` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `cash_registers` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `cash_registers` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE `cash_registers` SET `uid` = lower(hex(randomblob(16))) WHERE `uid` = ''")

                // --- Membres ---
                db.execSQL("ALTER TABLE `members` ADD COLUMN `uid` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `members` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `members` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE `members` SET `uid` = lower(hex(randomblob(16))) WHERE `uid` = ''")

                // --- Personnes autorisées sur l'appareil ---
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `app_users` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`uid` TEXT NOT NULL, " +
                        "`displayName` TEXT NOT NULL, " +
                        "`role` TEXT NOT NULL, " +
                        "`pinHash` TEXT NOT NULL, " +
                        "`pinSalt` TEXT NOT NULL, " +
                        "`isActive` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`lastLoginAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, " +
                        "`isDeleted` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_app_users_uid` ON `app_users` (`uid`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_app_users_displayName` ON `app_users` (`displayName`)")
            }
        }

        /**
         * Version 7 : annulation par contre-écriture et clôture des comptes.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `reversalOfUid` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `isCancelled` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `church_profile` ADD COLUMN `booksClosedUntilMillis` INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** Version 8 : projets / fonds affectés. */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `projectId` INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `projects` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`uid` TEXT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`description` TEXT NOT NULL, " +
                        "`targetCents` INTEGER NOT NULL, " +
                        "`isClosed` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, " +
                        "`isDeleted` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_projects_uid` ON `projects` (`uid`)")
            }
        }

        /** Version 9 : distingue projets, campagnes, événements et collectes sans perdre les dossiers existants. */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `projects` ADD COLUMN `kind` TEXT NOT NULL DEFAULT 'PROJECT'")
            }
        }

        /** Version 10 : identifiant stable utilisé pour empêcher les imports entre églises. */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `church_profile` ADD COLUMN `churchUid` TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE `church_profile` SET `churchUid` = lower(hex(randomblob(16))) WHERE `churchUid` = ''")
            }
        }

        /** Version 11 : carnet local des expéditeurs de fichiers d'échange, sans comptes/PIN. */
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `sync_contributors` (" +
                        "`uid` TEXT NOT NULL, " +
                        "`displayName` TEXT NOT NULL, " +
                        "`role` TEXT NOT NULL, " +
                        "`sourceDeviceId` TEXT NOT NULL, " +
                        "`firstSeenAt` INTEGER NOT NULL, " +
                        "`lastSeenAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`uid`))"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_contributors_displayName` ON `sync_contributors` (`displayName`)")
            }
        }

        /** Version 12 : périodes et liste d'actions pour les projets, données existantes conservées. */
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `projects` ADD COLUMN `startAtMillis` INTEGER")
                db.execSQL("ALTER TABLE `projects` ADD COLUMN `endAtMillis` INTEGER")
                db.execSQL("ALTER TABLE `projects` ADD COLUMN `actionItemsJson` TEXT NOT NULL DEFAULT '[]'")
            }
        }

        private class LuminaDatabaseCallback(
            private val scope: CoroutineScope?,
            private val databaseName: String
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                scope?.launch(Dispatchers.IO) {
                    val database = synchronized(INSTANCES) { INSTANCES[databaseName] }
                    database?.let { seedBaseData(it) }
                }
            }
        }

        /**
         * Données de départ : un plan de comptes adapté à une église et une
         * caisse principale vide.
         *
         * Aucune opération ni budget fictif n'est créé : l'utilisateur démarre
         * avec un dossier propre, alimenté uniquement par ses saisies.
         */
        private suspend fun seedBaseData(database: LuminaDatabase) {
            if (database.categoryDao().getAllCategoriesOnce().isNotEmpty()) return

            val expenses = DefaultChartOfAccounts.expenses
            val incomes = DefaultChartOfAccounts.incomes

            for ((name, color, icon) in expenses) {
                database.categoryDao().insertCategory(
                    CategoryEntity(
                        name = name,
                        type = TransactionType.EXPENSE,
                        colorHex = color,
                        isDefault = true,
                        iconName = icon
                    )
                )
            }
            for ((name, color, icon) in incomes) {
                database.categoryDao().insertCategory(
                    CategoryEntity(
                        name = name,
                        type = TransactionType.INCOME,
                        colorHex = color,
                        isDefault = true,
                        iconName = icon
                    )
                )
            }

            if (database.cashRegisterDao().getCount() == 0) {
                database.cashRegisterDao().insertCashRegister(
                    CashRegisterEntity(
                        name = "Caisse principale",
                        description = "Caisse physique de l'église",
                        initialBalanceCents = 0L,
                        iconType = "cash",
                        isDefault = true
                    )
                )
            }
        }
    }
}

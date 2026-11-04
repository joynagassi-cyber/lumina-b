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
import kotlinx.coroutines.launch

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        BudgetEntity::class,
        CashRegisterEntity::class,
        ChurchProfileEntity::class,
        MemberEntity::class,
        UserEntity::class
    ],
    version = 6,
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

    companion object {
        @Volatile
        private var INSTANCE: LuminaDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope? = null): LuminaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LuminaDatabase::class.java,
                    "lumina_financial.db"
                )
                .addCallback(LuminaDatabaseCallback(scope))
                .addMigrations(MIGRATION_5_6)
                // Filet de sécurité pour les installations très anciennes (v4 et avant)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
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

        private class LuminaDatabaseCallback(
            private val scope: CoroutineScope?
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                scope?.launch(Dispatchers.IO) {
                    INSTANCE?.let { database -> seedBaseData(database) }
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

            val expenses = listOf(
                Triple("Salaires & indemnités", "#3730A3", ""),
                Triple("Loyer & charges", "#4338CA", ""),
                Triple("Électricité & eau", "#4F46E5", ""),
                Triple("Entretien & réparations", "#6366F1", ""),
                Triple("Équipement & matériel", "#818CF8", ""),
                Triple("Transport & carburant", "#52525B", ""),
                Triple("Aide sociale & diaconie", "#71717A", ""),
                Triple("Missions & évangélisation", "#3F3F46", ""),
                Triple("Communication & internet", "#27272A", ""),
                Triple("Fournitures de bureau", "#57534E", ""),
                Triple("Restauration & accueil", "#78716C", ""),
                Triple("Autres sorties", "#A1A1AA", "")
            )
            val incomes = listOf(
                Triple("Dîmes", "#1E1B4B", ""),
                Triple("Offrandes", "#3730A3", ""),
                Triple("Dons spéciaux", "#4338CA", ""),
                Triple("Projets & constructions", "#4F46E5", ""),
                Triple("Vente d'objets & littérature", "#6366F1", ""),
                Triple("Subventions & partenariats", "#818CF8", ""),
                Triple("Autres entrées", "#A1A1AA", "")
            )

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

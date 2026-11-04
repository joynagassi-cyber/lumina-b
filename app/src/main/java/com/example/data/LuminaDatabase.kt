package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        BudgetEntity::class,
        CashRegisterEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LuminaDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun cashRegisterDao(): CashRegisterDao

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
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class LuminaDatabaseCallback(
            private val scope: CoroutineScope?
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                scope?.launch(Dispatchers.IO) {
                    INSTANCE?.let { database ->
                        populateInitialData(database)
                    }
                }
            }
        }

        private suspend fun populateInitialData(database: LuminaDatabase) {
            val now = System.currentTimeMillis()
            val dayMillis = 24 * 60 * 60 * 1000L
            val monthKey = SimpleDateFormat("yyyy-MM", Locale.FRANCE).format(Date(now))

            // 1. Initialiser les Caisses par défaut (Multi-caisses déterministe)
            val defaultRegisters = listOf(
                CashRegisterEntity(
                    name = "Caisse Espèces",
                    description = "Liquidités & dépenses courantes",
                    initialBalanceCents = 150000L, // 150 000 FCFA
                    iconType = "cash",
                    isDefault = true
                ),
                CashRegisterEntity(
                    name = "Mobile Money (Wave / Orange)",
                    description = "Transactions mobiles & transferts UEMOA",
                    initialBalanceCents = 350000L, // 350 000 FCFA
                    iconType = "mobile",
                    isDefault = false
                ),
                CashRegisterEntity(
                    name = "Compte Bancaire",
                    description = "Virements principaux & épargne",
                    initialBalanceCents = 1500000L, // 1 500 000 FCFA
                    iconType = "bank",
                    isDefault = false
                )
            )

            val caisseIds = mutableListOf<Long>()
            for (reg in defaultRegisters) {
                val id = database.cashRegisterDao().insertCashRegister(reg)
                caisseIds.add(id)
            }

            val espId = caisseIds.getOrNull(0) ?: 1L
            val mobId = caisseIds.getOrNull(1) ?: 2L
            val bqId = caisseIds.getOrNull(2) ?: 3L

            // 2. Initialiser les catégories par défaut
            val defaultCategories = listOf(
                // Catégories Dépenses
                CategoryEntity(name = "Alimentation", type = TransactionType.EXPENSE, colorHex = "#71717A", isDefault = true),
                CategoryEntity(name = "Logement", type = TransactionType.EXPENSE, colorHex = "#3730A3", isDefault = true),
                CategoryEntity(name = "Transport", type = TransactionType.EXPENSE, colorHex = "#4338CA", isDefault = true),
                CategoryEntity(name = "Loisirs", type = TransactionType.EXPENSE, colorHex = "#6366F1", isDefault = true),
                CategoryEntity(name = "Santé", type = TransactionType.EXPENSE, colorHex = "#52525B", isDefault = true),
                CategoryEntity(name = "Factures (CIE / SODECI)", type = TransactionType.EXPENSE, colorHex = "#27272A", isDefault = true),
                CategoryEntity(name = "Autre dépense", type = TransactionType.EXPENSE, colorHex = "#3F3F46", isDefault = true),

                // Catégories Entrées
                CategoryEntity(name = "Salaire", type = TransactionType.INCOME, colorHex = "#4338CA", isDefault = true),
                CategoryEntity(name = "Freelance & Prestations", type = TransactionType.INCOME, colorHex = "#3730A3", isDefault = true),
                CategoryEntity(name = "Vente & Commerce", type = TransactionType.INCOME, colorHex = "#6366F1", isDefault = true),
                CategoryEntity(name = "Transfert / Virement", type = TransactionType.INCOME, colorHex = "#818CF8", isDefault = true),
                CategoryEntity(name = "Autre revenu", type = TransactionType.INCOME, colorHex = "#52525B", isDefault = true)
            )

            for (cat in defaultCategories) {
                database.categoryDao().insertCategory(cat)
            }

            // 3. Budgets en FCFA pour le mois en cours
            val sampleBudgets = listOf(
                BudgetEntity(categoryName = "Alimentation", amountCents = 150000L, monthYearKey = monthKey), // 150 000 FCFA
                BudgetEntity(categoryName = "Logement", amountCents = 300000L, monthYearKey = monthKey),     // 300 000 FCFA
                BudgetEntity(categoryName = "Transport", amountCents = 50000L, monthYearKey = monthKey),    // 50 000 FCFA
                BudgetEntity(categoryName = "Factures (CIE / SODECI)", amountCents = 40000L, monthYearKey = monthKey) // 40 000 FCFA
            )

            for (b in sampleBudgets) {
                database.budgetDao().insertOrUpdateBudget(b)
            }

            // 4. Initialiser des transactions réalistes en FCFA avec Caisses associées
            val initialList = listOf(
                TransactionEntity(
                    title = "Virement Salaire Mensuel",
                    type = TransactionType.INCOME,
                    amountCents = 1250000L, // 1 250 000 FCFA
                    category = "Salaire",
                    timestamp = now - (dayMillis * 12),
                    note = "Salaire mensuel sur compte bancaire",
                    cashRegisterId = bqId,
                    cashRegisterName = "Compte Bancaire"
                ),
                TransactionEntity(
                    title = "Loyer appartement",
                    type = TransactionType.EXPENSE,
                    amountCents = 275000L, // 275 000 FCFA
                    category = "Logement",
                    timestamp = now - (dayMillis * 11),
                    note = "Paiement loyer mensuel",
                    cashRegisterId = bqId,
                    cashRegisterName = "Compte Bancaire"
                ),
                TransactionEntity(
                    title = "Prestation informatique client",
                    type = TransactionType.INCOME,
                    amountCents = 250000L, // 250 000 FCFA
                    category = "Freelance & Prestations",
                    timestamp = now - (dayMillis * 8),
                    note = "Dépôt Wave reçu",
                    cashRegisterId = mobId,
                    cashRegisterName = "Mobile Money (Wave / Orange)"
                ),
                TransactionEntity(
                    title = "Grandes courses marché & supermarché",
                    type = TransactionType.EXPENSE,
                    amountCents = 65000L, // 65 000 FCFA
                    category = "Alimentation",
                    timestamp = now - (dayMillis * 6),
                    note = "Achat vivres",
                    cashRegisterId = espId,
                    cashRegisterName = "Caisse Espèces"
                ),
                TransactionEntity(
                    title = "Carburant & Transport",
                    type = TransactionType.EXPENSE,
                    amountCents = 25000L, // 25 000 FCFA
                    category = "Transport",
                    timestamp = now - (dayMillis * 4),
                    note = "Paiement essence",
                    cashRegisterId = mobId,
                    cashRegisterName = "Mobile Money (Wave / Orange)"
                ),
                TransactionEntity(
                    title = "Retrait espèces pour menues dépenses",
                    type = TransactionType.INCOME,
                    amountCents = 50000L, // 50 000 FCFA
                    category = "Transfert / Virement",
                    timestamp = now - (dayMillis * 2),
                    note = "Approvisionnement caisse cash",
                    cashRegisterId = espId,
                    cashRegisterName = "Caisse Espèces"
                )
            )

            for (tx in initialList) {
                database.transactionDao().insertTransaction(tx)
            }
        }
    }
}

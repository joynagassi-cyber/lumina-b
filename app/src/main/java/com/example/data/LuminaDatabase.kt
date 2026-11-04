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

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        BudgetEntity::class,
        CashRegisterEntity::class,
        ChurchProfileEntity::class,
        MemberEntity::class
    ],
    version = 5,
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

package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.BudgetDao
import com.example.data.BudgetEntity
import com.example.data.CashRegisterDao
import com.example.data.CashRegisterEntity
import com.example.data.CategoryDao
import com.example.data.CategoryEntity
import com.example.data.LuminaDatabase
import com.example.data.TransactionDao
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class RoomDatabaseTest {

    private lateinit var database: LuminaDatabase
    private lateinit var transactionDao: TransactionDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var budgetDao: BudgetDao
    private lateinit var cashRegisterDao: CashRegisterDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LuminaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        transactionDao = database.transactionDao()
        categoryDao = database.categoryDao()
        budgetDao = database.budgetDao()
        cashRegisterDao = database.cashRegisterDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    @Test
    fun insert_and_read_transactions() = runBlocking {
        val tx = TransactionEntity(
            title = "Achat Supermarché",
            type = TransactionType.EXPENSE,
            amountCents = 35000L,
            category = "Alimentation",
            timestamp = 1000L,
            note = "Courses hebdomadaires",
            cashRegisterId = 1L,
            cashRegisterName = "Caisse Espèces"
        )

        val id = transactionDao.insertTransaction(tx)
        assertTrue(id > 0)

        val allTx = transactionDao.getAllTransactions().first()
        assertEquals(1, allTx.size)
        assertEquals("Achat Supermarché", allTx[0].title)
        assertEquals(35000L, allTx[0].amountCents)
        assertEquals(TransactionType.EXPENSE, allTx[0].type)
        assertEquals("Alimentation", allTx[0].category)

        val fetched = transactionDao.getById(id)
        assertNotNull(fetched)
        assertEquals("Achat Supermarché", fetched?.title)
    }

    @Test
    fun transaction_period_and_category_queries() = runBlocking {
        val tx1 = TransactionEntity(
            title = "Courses 1",
            type = TransactionType.EXPENSE,
            amountCents = 15000L,
            category = "Alimentation",
            timestamp = 2000L
        )
        val tx2 = TransactionEntity(
            title = "Carburant",
            type = TransactionType.EXPENSE,
            amountCents = 20000L,
            category = "Transport",
            timestamp = 5000L
        )
        val tx3 = TransactionEntity(
            title = "Salaire",
            type = TransactionType.INCOME,
            amountCents = 350000L,
            category = "Salaire",
            timestamp = 8000L
        )

        transactionDao.insertTransactions(listOf(tx1, tx2, tx3))

        // Filtrage temporel
        val inRange = transactionDao.getTransactionsBetween(1500L, 6000L).first()
        assertEquals(2, inRange.size)

        // Filtrage par catégorie
        val foodTx = transactionDao.getTransactionsByCategory("Alimentation").first()
        assertEquals(1, foodTx.size)
        assertEquals("Courses 1", foodTx[0].title)

        // Somme des dépenses sur période
        val totalExpense = transactionDao.getTotalExpenseBetween(1000L, 10000L).first()
        assertEquals(35000L, totalExpense)

        // Somme des revenus sur période
        val totalIncome = transactionDao.getTotalIncomeBetween(1000L, 10000L).first()
        assertEquals(350000L, totalIncome)
    }

    @Test
    fun category_crud_and_filtering() = runBlocking {
        val catExpense = CategoryEntity(
            name = "Loisirs",
            type = TransactionType.EXPENSE,
            colorHex = "#6366F1"
        )
        val catIncome = CategoryEntity(
            name = "Investissements",
            type = TransactionType.INCOME,
            colorHex = "#10B981"
        )

        val idExp = categoryDao.insertCategory(catExpense)
        val idInc = categoryDao.insertCategory(catIncome)

        assertTrue(idExp > 0)
        assertTrue(idInc > 0)

        val expenses = categoryDao.getCategoriesByType(TransactionType.EXPENSE).first()
        assertEquals(1, expenses.size)
        assertEquals("Loisirs", expenses[0].name)

        val incomes = categoryDao.getCategoriesByType(TransactionType.INCOME).first()
        assertEquals(1, incomes.size)
        assertEquals("Investissements", incomes[0].name)

        val found = categoryDao.getCategoryByName("Loisirs")
        assertNotNull(found)
        assertEquals("#6366F1", found?.colorHex)
    }

    @Test
    fun budget_constraint_creation_and_period_retrieval() = runBlocking {
        val constraint = BudgetEntity(
            categoryName = "Alimentation",
            amountCents = 150000L,
            monthYearKey = "2026-11",
            alertThresholdPercent = 85,
            isHardLimit = true,
            notes = "Plafond mensuel strict alimentation"
        )

        val id = budgetDao.insertOrUpdateBudget(constraint)
        assertTrue(id > 0)

        val periodBudgets = budgetDao.getBudgetsForPeriod("2026-11").first()
        assertEquals(1, periodBudgets.size)
        assertEquals(150000L, periodBudgets[0].amountCents)
        assertEquals(85, periodBudgets[0].alertThresholdPercent)
        assertTrue(periodBudgets[0].isHardLimit)

        val retrieved = budgetDao.getBudgetForCategoryAndPeriod("Alimentation", "2026-11")
        assertNotNull(retrieved)
        assertEquals(150000L, retrieved?.amountCents)

        // Upsert / Mise à jour de la contrainte
        val updated = retrieved!!.copy(amountCents = 180000L)
        budgetDao.insertOrUpdateBudget(updated)

        val afterUpdate = budgetDao.getBudgetForCategoryAndPeriod("Alimentation", "2026-11")
        assertEquals(180000L, afterUpdate?.amountCents)

        // Suppression par catégorie
        budgetDao.deleteByCategory("Alimentation")
        val afterDelete = budgetDao.getBudgetForCategoryAndPeriod("Alimentation", "2026-11")
        assertNull(afterDelete)
    }

    @Test
    fun cash_register_persistence() = runBlocking {
        val reg = CashRegisterEntity(
            name = "Caisse Coffre",
            description = "Réserve de sécurité",
            initialBalanceCents = 500000L,
            iconType = "vault",
            isDefault = false
        )

        val id = cashRegisterDao.insertCashRegister(reg)
        assertTrue(id > 0)

        val list = cashRegisterDao.getAllCashRegisters().first()
        assertEquals(1, list.size)
        assertEquals("Caisse Coffre", list[0].name)
        assertEquals(500000L, list[0].initialBalanceCents)

        val fetched = cashRegisterDao.getById(id)
        assertNotNull(fetched)
        assertEquals("vault", fetched?.iconType)
    }
}

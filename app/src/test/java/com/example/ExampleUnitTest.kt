package com.example

import com.example.data.BudgetEntity
import com.example.data.CashRegisterEntity
import com.example.data.CategoryEntity
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.ui.CategoryBudgetProgress
import com.example.util.AmountDisplayMode
import com.example.util.CurrencyFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun deterministic_financial_balance_calculation_in_fcfa() {
        // En FCFA d'Afrique de l'Ouest (XOF), arithmétique entière sans perte
        val income = TransactionEntity(
            title = "Salaire Mensuel",
            type = TransactionType.INCOME,
            amountCents = 450000L,
            category = "Revenus",
            cashRegisterId = 3L,
            cashRegisterName = "Compte Bancaire"
        )
        val expense = TransactionEntity(
            title = "Loyer Appartement",
            type = TransactionType.EXPENSE,
            amountCents = 150000L,
            category = "Logement",
            cashRegisterId = 3L,
            cashRegisterName = "Compte Bancaire"
        )

        val netBalance = income.amountCents - expense.amountCents
        assertEquals(300000L, netBalance)
    }

    @Test
    fun currency_formatter_fcfa_modes() {
        val amount = 150000L

        // Mode Standard
        val standard = CurrencyFormatter.format(amount, AmountDisplayMode.STANDARD)
            .replace("\u00A0", " ").replace("\u202F", " ")
        assertEquals("150 000 FCFA", standard)

        // Mode Compact
        val compact = CurrencyFormatter.format(amount, AmountDisplayMode.COMPACT)
            .replace("\u00A0", " ").replace("\u202F", " ")
        assertEquals("150 k FCFA", compact)

        val millionAmount = 1500000L
        val compactMillion = CurrencyFormatter.format(millionAmount, AmountDisplayMode.COMPACT)
            .replace("\u00A0", " ").replace("\u202F", " ")
        assertEquals("1,5 M FCFA", compactMillion)

        // Mode Comptable Précis
        val comptable = CurrencyFormatter.format(amount, AmountDisplayMode.COMPTABLE)
            .replace("\u00A0", " ").replace("\u202F", " ")
        assertEquals("150 000,00 FCFA", comptable)

        // Mode Discret
        val discret = CurrencyFormatter.format(amount, AmountDisplayMode.DISCRET)
        assertEquals("•••••• FCFA", discret)
    }

    @Test
    fun transaction_amount_formatted_fcfa() {
        val tx = TransactionEntity(
            title = "Achat Marché",
            type = TransactionType.EXPENSE,
            amountCents = 25000L,
            category = "Alimentation",
            cashRegisterId = 1L,
            cashRegisterName = "Caisse Espèces"
        )
        val formatted = tx.amountFormatted.replace("\u00A0", " ").replace("\u202F", " ")
        assertEquals("25 000 FCFA", formatted)
        assertEquals("Caisse Espèces", tx.cashRegisterName)
    }

    @Test
    fun caisse_balance_calculation() {
        val caisse = CashRegisterEntity(
            id = 1L,
            name = "Caisse Espèces",
            description = "Dépenses courantes",
            initialBalanceCents = 50000L,
            iconType = "cash",
            isDefault = true
        )

        val tx1 = TransactionEntity(
            title = "Dépôt initial",
            type = TransactionType.INCOME,
            amountCents = 30000L,
            category = "Dépôt",
            cashRegisterId = 1L,
            cashRegisterName = "Caisse Espèces"
        )

        val tx2 = TransactionEntity(
            title = "Carburant",
            type = TransactionType.EXPENSE,
            amountCents = 15000L,
            category = "Transport",
            cashRegisterId = 1L,
            cashRegisterName = "Caisse Espèces"
        )

        val currentBalance = caisse.initialBalanceCents + tx1.amountCents - tx2.amountCents
        assertEquals(65000L, currentBalance)
    }

    @Test
    fun caisse_transfer_invariants() {
        val sourceInitial = 100000L
        val targetInitial = 25000L
        val transferAmount = 40000L

        // Virement: débit source, crédit cible
        val sourceCurrent = sourceInitial - transferAmount
        val targetCurrent = targetInitial + transferAmount

        assertEquals(60000L, sourceCurrent)
        assertEquals(65000L, targetCurrent)

        // L'invariant de conservation totale de trésorerie est respecté
        assertEquals(sourceInitial + targetInitial, sourceCurrent + targetCurrent)
    }

    @Test
    fun budget_progress_calculation_in_fcfa() {
        val budget = BudgetEntity(
            categoryName = "Alimentation",
            amountCents = 100000L, // 100 000 FCFA
            monthYearKey = "2026-11"
        )
        val spentCents = 75000L // 75 000 FCFA

        val percentage = (spentCents.toDouble() / budget.amountCents.toDouble()).toFloat()
        val remaining = budget.amountCents - spentCents

        val progress = CategoryBudgetProgress(
            categoryName = budget.categoryName,
            categoryColorHex = "#4338CA",
            budgetId = budget.id,
            budgetAmountCents = budget.amountCents,
            spentAmountCents = spentCents,
            percentage = percentage,
            remainingCents = remaining,
            isExceeded = spentCents > budget.amountCents,
            isNearLimit = percentage >= 0.8f && spentCents <= budget.amountCents
        )

        assertEquals(0.75f, progress.percentage, 0.001f)
        assertEquals(25000L, progress.remainingCents)
        assertFalse(progress.isExceeded)
        assertFalse(progress.isNearLimit)
    }

    @Test
    fun budget_exceeded_calculation_in_fcfa() {
        val budgetAmount = 50000L // 50 000 FCFA
        val spent = 62000L // 62 000 FCFA
        val remaining = budgetAmount - spent

        assertTrue(remaining < 0)
        assertEquals(-12000L, remaining)
        assertTrue(spent > budgetAmount)
    }

    @Test
    fun custom_category_creation() {
        val cat = CategoryEntity(
            name = "Mobile Money Fees",
            type = TransactionType.EXPENSE,
            colorHex = "#3730A3",
            isDefault = false
        )
        assertEquals("Mobile Money Fees", cat.name)
        assertEquals(TransactionType.EXPENSE, cat.type)
        assertEquals("#3730A3", cat.colorHex)
        assertFalse(cat.isDefault)
    }
}

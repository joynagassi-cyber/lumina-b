package com.example

import com.example.data.BudgetEntity
import com.example.data.CashRegisterEntity
import com.example.data.ProjectEntity
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.sync.SyncBundle
import com.example.sync.SyncBundleCodec
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SyncBundleTest {
    @Test
    fun le_bundle_conserve_l_identifiant_stable_de_l_eglise() {
        val original = SyncBundle(
            churchName = "Église de Kandi",
            churchUid = "eglise-uid-123",
            deviceId = "device-a",
            author = "Trésorier"
        )

        val decoded = SyncBundleCodec.decode(SyncBundleCodec.encode(original))

        assertEquals(original.churchUid, decoded.churchUid)
        assertEquals(original.churchName, decoded.churchName)
    }

    @Test
    fun echange_transporte_budgets_projets_et_relations_stables() {
        val register = CashRegisterEntity(id = 5, name = "Caisse principale", uid = "register-uid")
        val project = ProjectEntity(id = 7, name = "Toiture", uid = "project-uid")
        val transaction = TransactionEntity(
            id = 11,
            title = "Don affecté",
            type = TransactionType.INCOME,
            amountCents = 250_000,
            category = "Dons",
            cashRegisterId = register.id,
            cashRegisterName = register.name,
            projectId = project.id,
            uid = "transaction-uid"
        )
        val original = SyncBundle(
            registers = listOf(register),
            projects = listOf(project),
            budgets = listOf(BudgetEntity(categoryName = "Dons", amountCents = 500_000, monthYearKey = "2026-11")),
            transactions = listOf(transaction)
        )

        val decoded = SyncBundleCodec.decode(SyncBundleCodec.encode(original))

        assertEquals(500_000L, decoded.budgets.single().amountCents)
        assertEquals("project-uid", decoded.projects.single().uid)
        assertEquals("project-uid", decoded.projectUidByTransactionUid["transaction-uid"])
        assertEquals("register-uid", decoded.registerUidByTransactionUid["transaction-uid"])
    }

    @Test
    fun l_indicateur_de_justificatif_est_conserve_pour_refuser_une_fusion_avec_perte() {
        val transaction = TransactionEntity(
            title = "Facture",
            type = TransactionType.EXPENSE,
            amountCents = 100,
            category = "Frais",
            uid = "tx-with-receipt",
            receiptPhotoUri = "content://local/private/receipt.jpg"
        )

        val decoded = SyncBundleCodec.decode(SyncBundleCodec.encode(SyncBundle(transactions = listOf(transaction))))

        assertEquals(setOf("tx-with-receipt"), decoded.transactionUidsWithReceipts)
    }

    @Test
    fun ancien_bundle_sans_identifiant_reste_lisible() {
        val legacy = JSONObject().apply {
            put("format", SyncBundleCodec.FORMAT)
            put("version", 1)
            put("churchName", "Église historique")
            put("deviceId", "legacy-device")
        }.toString().toByteArray(Charsets.UTF_8)

        val decoded = SyncBundleCodec.decode(legacy)

        assertEquals("", decoded.churchUid)
        assertEquals("Église historique", decoded.churchName)
    }
}

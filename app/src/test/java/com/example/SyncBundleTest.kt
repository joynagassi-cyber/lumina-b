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
            author = "Awa Bio",
            authorRole = "COMPTABLE"
        )

        val decoded = SyncBundleCodec.decode(SyncBundleCodec.encode(original))

        assertEquals(original.churchUid, decoded.churchUid)
        assertEquals(original.churchName, decoded.churchName)
        assertEquals(original.author, decoded.author)
        assertEquals(original.authorRole, decoded.authorRole)
    }

    @Test
    fun echange_transporte_budgets_projets_et_relations_stables() {
        val register = CashRegisterEntity(id = 5, name = "Caisse principale", uid = "register-uid")
        val project = ProjectEntity(
            id = 7,
            name = "Événement jeunesse",
            uid = "project-uid",
            kind = "EVENT",
            startAtMillis = 1_790_000_000_000L,
            endAtMillis = 1_790_172_800_000L,
            actionItemsJson = "[{\"id\":\"task-1\",\"title\":\"Réserver la salle\",\"isDone\":false}]"
        )
        val transaction = TransactionEntity(
            id = 11,
            title = "Don affecté",
            type = TransactionType.INCOME,
            amountCents = 250_000,
            category = "Dons",
            cashRegisterId = register.id,
            cashRegisterName = register.name,
            projectId = project.id,
            uid = "transaction-uid",
            receiptPhotoUri = "/private/receipt.jpg"
        )
        val original = SyncBundle(
            registers = listOf(register),
            projects = listOf(project),
            budgets = listOf(BudgetEntity(categoryName = "Dons", amountCents = 500_000, monthYearKey = "2026-11")),
            transactions = listOf(transaction),
            receiptFilesByTransactionUid = mapOf("transaction-uid" to byteArrayOf(1, 2, 3, 4))
        )

        val decoded = SyncBundleCodec.decode(SyncBundleCodec.encode(original))

        assertEquals(500_000L, decoded.budgets.single().amountCents)
        assertEquals("project-uid", decoded.projects.single().uid)
        assertEquals(1_790_000_000_000L, decoded.projects.single().startAtMillis)
        assertEquals(1_790_172_800_000L, decoded.projects.single().endAtMillis)
        assertEquals("[{\"id\":\"task-1\",\"title\":\"Réserver la salle\",\"isDone\":false}]", decoded.projects.single().actionItemsJson)
        assertEquals("project-uid", decoded.projectUidByTransactionUid["transaction-uid"])
        assertEquals("register-uid", decoded.registerUidByTransactionUid["transaction-uid"])
        assertEquals(listOf<Byte>(1, 2, 3, 4), decoded.receiptFilesByTransactionUid.getValue("transaction-uid").toList())
    }

    @Test(expected = IllegalArgumentException::class)
    fun export_refuse_un_justificatif_annonce_mais_absent() {
        val transaction = TransactionEntity(
            title = "Facture",
            type = TransactionType.EXPENSE,
            amountCents = 100,
            category = "Frais",
            uid = "tx-with-receipt",
            receiptPhotoUri = "/private/receipt.jpg"
        )
        SyncBundleCodec.encode(SyncBundle(transactions = listOf(transaction)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun import_refuse_un_ancien_fichier_sans_les_octets_du_justificatif() {
        val legacy = JSONObject().apply {
            put("format", SyncBundleCodec.FORMAT)
            put("version", 1)
            put("transactions", org.json.JSONArray().put(JSONObject().apply {
                put("uid", "tx-with-receipt")
                put("hasReceipt", true)
            }))
        }.toString().toByteArray(Charsets.UTF_8)

        SyncBundleCodec.decode(legacy)
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

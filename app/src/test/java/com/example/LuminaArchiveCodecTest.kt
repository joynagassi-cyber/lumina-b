package com.example

import com.example.data.BudgetEntity
import com.example.data.CashRegisterEntity
import com.example.data.CategoryEntity
import com.example.data.ChurchProfileEntity
import com.example.data.MemberEntity
import com.example.data.ProjectEntity
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.data.UserEntity
import com.example.export.BackupSnapshot
import com.example.export.LuminaArchiveCodec
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LuminaArchiveCodecTest {
    @Test
    fun archive_zip_conserve_le_dossier_et_les_justificatifs() {
        val snapshot = BackupSnapshot(
            createdAt = 1234L,
            churchName = "Église de Kandi",
            profile = ChurchProfileEntity(
                churchName = "Église de Kandi",
                churchUid = "church-kandi",
                logoBase64 = "base64-logo",
                accentColorHex = "#B9382E",
                onboardingCompleted = true
            ),
            users = listOf(UserEntity(id = 1, displayName = "Accès local", pinHash = "secret-hash")),
            members = listOf(MemberEntity(id = 4, uid = "member-4", firstName = "Awa", lastName = "Bio")),
            categories = listOf(CategoryEntity(id = 3, uid = "category-3", name = "Dîmes", type = TransactionType.INCOME)),
            budgets = listOf(BudgetEntity(id = 5, categoryName = "Carburant", amountCents = 3000, monthYearKey = "2026-11")),
            registers = listOf(CashRegisterEntity(id = 2, uid = "register-2", name = "Banque")),
            projects = listOf(ProjectEntity(id = 6, uid = "project-6", name = "École", isDeleted = true)),
            transactions = listOf(
                TransactionEntity(
                    id = 7,
                    uid = "transaction-7",
                    title = "Dépense",
                    type = TransactionType.EXPENSE,
                    amountCents = 5000,
                    category = "Carburant",
                    cashRegisterId = 2,
                    cashRegisterName = "Banque",
                    receiptPhotoUri = "/chemin/privé/source.jpg",
                    projectId = 6
                )
            )
        )
        val receipt = byteArrayOf(1, 2, 3, 4, 5)

        val archive = LuminaArchiveCodec.encode(snapshot, mapOf("transaction-7" to receipt))
        assertArrayEquals(byteArrayOf('P'.code.toByte(), 'K'.code.toByte()), archive.copyOfRange(0, 2))
        val decoded = LuminaArchiveCodec.decode(archive)

        assertEquals("Église de Kandi", decoded.snapshot.profile?.churchName)
        assertEquals("#B9382E", decoded.snapshot.profile?.accentColorHex)
        assertEquals("base64-logo", decoded.snapshot.profile?.logoBase64)
        assertEquals(1, decoded.snapshot.transactions.size)
        assertEquals(1, decoded.snapshot.budgets.size)
        assertEquals(1, decoded.snapshot.projects.size)
        assertTrue(decoded.snapshot.projects.single().isDeleted)
        assertEquals("archive:receipts/transaction-7.jpg", decoded.snapshot.transactions.single().receiptPhotoUri)
        assertArrayEquals(receipt, decoded.receiptFilesByTransactionUid["transaction-7"])
        assertEquals("/chemin/privé/source.jpg", snapshot.transactions.single().receiptPhotoUri)
    }

    @Test
    fun archive_refuse_fichier_etranger_et_piece_jointe_orpheline() {
        try {
            LuminaArchiveCodec.decode("pas un zip".toByteArray())
            throw AssertionError("Le contenu étranger doit être refusé")
        } catch (_: IllegalArgumentException) {
            // attendu
        }
        try {
            LuminaArchiveCodec.decode(LuminaArchiveCodec.encode(BackupSnapshot(), mapOf("orphan" to byteArrayOf(1))))
            throw AssertionError("Une pièce jointe orpheline doit être refusée")
        } catch (_: IllegalArgumentException) {
            // attendu
        }
    }
}

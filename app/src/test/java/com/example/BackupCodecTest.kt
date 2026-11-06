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
import com.example.export.BackupCodec
import com.example.export.BackupCrypto
import com.example.export.BackupSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Garde-fous de la sauvegarde : le dossier doit pouvoir partir sur un fichier
 * puis revenir à l'identique, et une sauvegarde protégée ne doit s'ouvrir
 * qu'avec le bon mot de passe.
 */
@RunWith(RobolectricTestRunner::class)
class BackupCodecTest {

    private fun dossier() = BackupSnapshot(
        createdAt = 1_700_000_000_000L,
        churchName = "Église de Kandi",
        profile = ChurchProfileEntity(
            churchName = "Église de Kandi",
            denomination = "Assemblée",
            city = "Kandi",
            booksClosedUntilMillis = 1_690_000_000_000L,
            accentColorHex = "#0F766E",
            onboardingCompleted = true
        ),
        users = listOf(
            UserEntity(id = 1, displayName = "Trésorier", role = "TRESORIER")
        ),
        members = listOf(
            MemberEntity(id = 3, firstName = "Awa", lastName = "Bio", phone = "+229")
        ),
        categories = listOf(
            CategoryEntity(id = 2, name = "Dîmes", type = TransactionType.INCOME)
        ),
        budgets = listOf(
            BudgetEntity(
                id = 5,
                categoryName = "Carburant",
                amountCents = 25_000,
                monthYearKey = "2026-11"
            )
        ),
        registers = listOf(
            CashRegisterEntity(id = 1, name = "Caisse espèces", isDefault = true),
            CashRegisterEntity(id = 2, name = "Compte bancaire", iconType = "bank")
        ),
        projects = listOf(
            ProjectEntity(
                id = 42,
                name = "Conférence annuelle",
                targetCents = 5_000_000,
                kind = "EVENT",
                startAtMillis = 1_790_000_000_000L,
                endAtMillis = 1_790_172_800_000L,
                actionItemsJson = "[{\"id\":\"task-1\",\"title\":\"Réserver la salle\",\"isDone\":true}]"
            )
        ),
        transactions = listOf(
            TransactionEntity(
                id = 11,
                title = "Offrande spéciale",
                type = TransactionType.INCOME,
                amountCents = 75_000,
                category = "Offrandes",
                timestamp = 1_690_000_000_000L,
                cashRegisterId = 1,
                cashRegisterName = "Caisse espèces",
                uid = "uid-11",
                projectId = 42
            ),
            TransactionEntity(
                id = 12,
                title = "Annulée",
                type = TransactionType.EXPENSE,
                amountCents = 10_000,
                category = "Carburant",
                timestamp = 1_690_100_000_000L,
                cashRegisterId = 2,
                cashRegisterName = "Compte bancaire",
                uid = "uid-12",
                isCancelled = true,
                reversalOfUid = "uid-11"
            )
        )
    )

    @Test
    fun sauvegarde_aller_retour_conserve_tout() {
        val original = dossier()
        val relu = BackupCodec.decode(BackupCodec.encode(original))

        assertEquals(original.churchName, relu.churchName)
        assertEquals(original.profile?.churchUid, relu.profile?.churchUid)
        assertEquals(original.createdAt, relu.createdAt)
        assertEquals(2, relu.transactionCount)
        assertEquals(1, relu.memberCount)
        assertEquals(2, relu.registers.size)
        assertEquals(1, relu.projects.size)
        assertEquals("EVENT", relu.projects.single().kind)
        assertEquals(1_790_000_000_000L, relu.projects.single().startAtMillis)
        assertEquals(1_790_172_800_000L, relu.projects.single().endAtMillis)
        assertEquals("[{\"id\":\"task-1\",\"title\":\"Réserver la salle\",\"isDone\":true}]", relu.projects.single().actionItemsJson)
        assertEquals(1, relu.budgets.size)
        assertEquals(1, relu.users.size)

        // L'affectation à un projet et l'annulation doivent survivre au trajet.
        val offrande = relu.transactions.first { it.uid == "uid-11" }
        assertEquals(42L, offrande.projectId)
        assertEquals(75_000L, offrande.amountCents)
        assertEquals(TransactionType.INCOME, offrande.type)

        val annulee = relu.transactions.first { it.uid == "uid-12" }
        assertTrue(annulee.isCancelled)
        assertEquals("uid-11", annulee.reversalOfUid)

        val profil = relu.profile
        assertNotNull(profil)
        assertEquals(1_690_000_000_000L, profil!!.booksClosedUntilMillis)
        assertEquals("#0F766E", profil.accentColorHex)
    }

    @Test
    fun sauvegarde_vide_reste_lisible() {
        val relu = BackupCodec.decode(BackupCodec.encode(BackupSnapshot()))
        assertEquals(0, relu.transactionCount)
        assertEquals(0, relu.members.size)
    }

    @Test
    fun texte_etranger_est_refuse() {
        try {
            BackupCodec.decode("{\"format\":\"autre\"}")
            fail("Un fichier étranger doit être refusé")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("sauvegarde", ignoreCase = true))
        }
    }

    @Test
    fun protection_par_mot_de_passe() {
        val clair = BackupCodec.encode(dossier())
        val chiffre = BackupCrypto.encrypt(clair, "mon-code-2026")

        assertTrue(BackupCrypto.isEncrypted(chiffre))
        assertFalse(chiffre.contains("Construction du temple"))
        assertEquals(clair, BackupCrypto.decrypt(chiffre, "mon-code-2026"))

        try {
            BackupCrypto.decrypt(chiffre, "mauvais-code")
            fail("Un mauvais mot de passe doit échouer")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("incorrect", ignoreCase = true))
        }
    }
}

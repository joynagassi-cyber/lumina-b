package com.example

import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.sync.SyncMergePolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncMergePolicyTest {
    @Test
    fun toute_divergence_de_contenu_est_signalee_meme_si_une_copie_est_plus_recente() {
        val local = TransactionEntity(
            uid = "transaction-stable",
            title = "Dépense",
            type = TransactionType.EXPENSE,
            amountCents = 100,
            category = "Frais",
            updatedAt = 100
        )
        val newerButDifferent = local.copy(title = "Dépense modifiée", updatedAt = 500)
        val normalized = newerButDifferent.copy(updatedAt = local.updatedAt)

        assertTrue(SyncMergePolicy.hasDiverged(local, normalized))
        assertFalse(SyncMergePolicy.hasDiverged(local, local.copy(updatedAt = 500).copy(updatedAt = local.updatedAt)))
    }
}

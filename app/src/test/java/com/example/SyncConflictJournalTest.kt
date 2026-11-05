package com.example

import com.example.sync.SyncConflictItem
import com.example.sync.SyncConflictJournal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class SyncConflictJournalTest {
    @Test
    fun conflit_et_decision_sont_conserves_dans_le_journal_du_dossier() {
        val context = RuntimeEnvironment.getApplication()
        val workspace = "test-${UUID.randomUUID()}"
        val conflict = SyncConflictItem(
            key = "transaction:tx-1",
            kind = "Écriture",
            uid = "tx-1",
            label = "Dépense",
            localValue = "Local · 100",
            incomingValue = "Reçu · 120",
            localUpdatedAt = 10L,
            incomingUpdatedAt = 10L
        )

        val saved = SyncConflictJournal.recordDetected(context, workspace, byteArrayOf(1, 2, 3), listOf(conflict))
        assertEquals(1, SyncConflictJournal.pending(context, workspace).size)
        assertTrue(SyncConflictJournal.pending(context, "${workspace}-autre").isEmpty())
        assertTrue(SyncConflictJournal.readPendingBundle(context, workspace, saved.single()).isNotEmpty())

        SyncConflictJournal.markResolved(context, workspace, saved.single(), "KEEP_LOCAL")

        assertTrue(SyncConflictJournal.pending(context, workspace).isEmpty())
        assertEquals("KEEP_LOCAL", SyncConflictJournal.history(context, workspace).single().resolution)
    }
}

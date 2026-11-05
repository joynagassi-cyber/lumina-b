package com.example

import com.example.ui.isTransactionLocked
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionClosureTest {
    @Test
    fun la_cloture_verrouille_la_borne_et_toutes_les_ecritures_anterieures() {
        val closedUntil = 1_800_000_000_000L

        assertTrue(isTransactionLocked(closedUntil - 1L, closedUntil))
        assertTrue(isTransactionLocked(closedUntil, closedUntil))
        assertFalse(isTransactionLocked(closedUntil + 1L, closedUntil))
        assertFalse(isTransactionLocked(closedUntil - 1L, 0L))
    }
}

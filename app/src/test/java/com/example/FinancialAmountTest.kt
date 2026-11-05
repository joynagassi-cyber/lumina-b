package com.example

import com.example.util.FinancialAmount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialAmountTest {
    @Test
    fun montant_parse_sans_conversion_binaire_et_arrondit_au_plus_proche() {
        assertEquals(1L, FinancialAmount.parseTransactionAmount("0,5"))
        assertEquals(2L, FinancialAmount.parseTransactionAmount("1.5"))
        assertEquals(2500L, FinancialAmount.parseTransactionAmount("2500"))
    }

    @Test
    fun montants_non_finis_nuls_negatifs_ou_hors_limite_sont_refuses() {
        assertNull(FinancialAmount.parseTransactionAmount("Infinity"))
        assertNull(FinancialAmount.parseTransactionAmount("NaN"))
        assertNull(FinancialAmount.parseTransactionAmount("0"))
        assertNull(FinancialAmount.parseTransactionAmount("-1"))
        assertNull(FinancialAmount.parseTransactionAmount("9223372036854775808"))
        assertFalse(FinancialAmount.isValidTransactionAmount(Long.MAX_VALUE))
        assertTrue(FinancialAmount.isValidTransactionAmount(1L))
    }
}

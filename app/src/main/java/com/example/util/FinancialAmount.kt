package com.example.util

import java.math.BigDecimal
import java.math.RoundingMode

/** Parsing and bounds for user-entered whole-unit financial amounts. */
object FinancialAmount {
    // Leaves headroom for aggregating up to one million maximum-sized entries in Long.
    const val MAX_TRANSACTION_AMOUNT: Long = Long.MAX_VALUE / 1_000_000L

    fun isValidTransactionAmount(amount: Long): Boolean =
        amount in 1L..MAX_TRANSACTION_AMOUNT

    /** Parses a positive amount, accepts either decimal comma or point, and rounds half up. */
    fun parseTransactionAmount(text: String): Long? {
        val normalized = text.trim().replace(',', '.')
        if (normalized.isEmpty()) return null
        val amount = runCatching { BigDecimal(normalized) }.getOrNull() ?: return null
        if (amount.signum() <= 0) return null
        val rounded = runCatching { amount.setScale(0, RoundingMode.HALF_UP).longValueExact() }.getOrNull()
            ?: return null
        return rounded.takeIf(::isValidTransactionAmount)
    }
}

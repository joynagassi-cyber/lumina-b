package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

enum class AmountDisplayMode(val label: String, val description: String) {
    STANDARD("Standard (FCFA)", "Ex: 150 000 FCFA"),
    COMPACT("Abrégé (k / M)", "Ex: 150 k FCFA"),
    COMPTABLE("Comptable Précis", "Ex: 150 000,00 FCFA"),
    DISCRET("Mode Discret", "Masque les montants (•••••• FCFA)")
}

object CurrencyFormatter {

    const val CURRENCY_CODE = "FCFA"

    private val symbols = DecimalFormatSymbols(Locale.FRANCE).apply {
        groupingSeparator = ' '
        decimalSeparator = ','
    }

    private fun threadLocalFormat(pattern: String) = object : ThreadLocal<DecimalFormat>() {
        override fun initialValue(): DecimalFormat = DecimalFormat(pattern, symbols)
    }

    // DecimalFormat is mutable and not thread-safe; UI state, exports and reports can
    // format amounts concurrently on different dispatchers.
    private val standardFormat = threadLocalFormat("#,##0")
    private val preciseFormat = threadLocalFormat("#,##0.00")
    private val compactDecimalFormat = threadLocalFormat("#,##0.#")

    fun format(
        amount: Long,
        mode: AmountDisplayMode = AmountDisplayMode.STANDARD,
        withSign: Boolean = false,
        isPositive: Boolean? = null
    ): String {
        val signPrefix = when {
            withSign && (isPositive == true || (isPositive == null && amount > 0)) -> "+"
            withSign && (isPositive == false || (isPositive == null && amount < 0)) -> "-"
            else -> ""
        }

        val absAmount = Math.abs(amount)

        val formattedNumber = when (mode) {
            AmountDisplayMode.DISCRET -> "••••••"
            AmountDisplayMode.STANDARD -> standardFormat.get().format(absAmount)
            AmountDisplayMode.COMPTABLE -> preciseFormat.get().format(absAmount.toDouble())
            AmountDisplayMode.COMPACT -> {
                when {
                    absAmount >= 1_000_000_000L -> {
                        compactDecimalFormat.get().format(absAmount / 1_000_000_000.0) + " Md"
                    }
                    absAmount >= 1_000_000L -> {
                        compactDecimalFormat.get().format(absAmount / 1_000_000.0) + " M"
                    }
                    absAmount >= 10_000L -> {
                        compactDecimalFormat.get().format(absAmount / 1_000.0) + " k"
                    }
                    else -> standardFormat.get().format(absAmount)
                }
            }
        }

        return "$signPrefix$formattedNumber $CURRENCY_CODE"
    }
}

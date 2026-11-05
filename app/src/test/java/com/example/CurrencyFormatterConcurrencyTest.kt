package com.example

import com.example.util.CurrencyFormatter
import com.example.util.AmountDisplayMode
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.Executors

class CurrencyFormatterConcurrencyTest {
    @Test
    fun formatage_concurrent_reste_deterministe() {
        val samples = listOf(1_234_567L, 98_765_432L, 12_345L, 6_789_012_345L)
        val expected = samples.map { CurrencyFormatter.format(it, AmountDisplayMode.STANDARD) }
        val pool = Executors.newFixedThreadPool(6)
        try {
            val results = (0 until 6).map { worker ->
                pool.submit {
                    repeat(1_000) { iteration ->
                        val index = (worker + iteration) % samples.size
                        assertEquals(expected[index], CurrencyFormatter.format(samples[index]))
                    }
                }
            }
            results.forEach { it.get() }
        } finally {
            pool.shutdownNow()
        }
    }
}

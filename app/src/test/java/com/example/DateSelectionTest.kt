package com.example

import com.example.util.localDateAtOffset
import com.example.util.localTimestampToPickerUtcMillis
import com.example.util.pickerUtcMillisToLocalMidday
import com.example.util.sameLocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class DateSelectionTest {
    @Test
    fun pickerRoundTripPreservesTheSelectedCalendarDay() {
        val originalZone = TimeZone.getDefault()
        val originalLocale = Locale.getDefault()
        try {
            listOf("Africa/Porto-Novo", "Pacific/Kiritimati", "America/Los_Angeles").forEach { zone ->
                TimeZone.setDefault(TimeZone.getTimeZone(zone))
                Locale.setDefault(Locale.FRANCE)
                val source = Calendar.getInstance().apply {
                    clear()
                    set(2024, Calendar.FEBRUARY, 29, 23, 45, 0)
                }.timeInMillis
                val restored = pickerUtcMillisToLocalMidday(localTimestampToPickerUtcMillis(source))
                assertTrue("The selected date must survive the $zone conversion", sameLocalDate(source, restored))
                val restoredLocal = Calendar.getInstance().apply { timeInMillis = restored }
                assertEquals(12, restoredLocal.get(Calendar.HOUR_OF_DAY))
            }
        } finally {
            TimeZone.setDefault(originalZone)
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun yesterdayUsesCalendarDaysRatherThanSubtractingTwentyFourHours() {
        val zone = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Africa/Porto-Novo"))
            val today = Calendar.getInstance().apply {
                clear()
                set(2026, Calendar.MARCH, 2, 12, 0, 0)
            }.timeInMillis
            val yesterday = Calendar.getInstance().apply { timeInMillis = localDateAtOffset(today, -1) }
            assertEquals(Calendar.MARCH, yesterday.get(Calendar.MONTH))
            assertEquals(1, yesterday.get(Calendar.DAY_OF_MONTH))
        } finally {
            TimeZone.setDefault(zone)
        }
    }
}

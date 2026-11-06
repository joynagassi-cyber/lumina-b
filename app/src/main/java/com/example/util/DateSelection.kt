package com.example.util

import java.util.Calendar
import java.util.TimeZone

/** Convertit entre les jours UTC fournis par Material DatePicker et la date locale du dossier. */
fun localTimestampToPickerUtcMillis(timestamp: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = timestamp }
    return utcDateMillis(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
}

/** Restitue le jour choisi à midi local, évitant les changements de date liés aux fuseaux. */
fun pickerUtcMillisToLocalMidday(pickerMillis: Long): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = pickerMillis }
    return Calendar.getInstance().apply {
        clear()
        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), 12, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

fun localDateAtOffset(timestamp: Long, dayOffset: Int): Long = Calendar.getInstance().apply {
    timeInMillis = timestamp
    add(Calendar.DAY_OF_MONTH, dayOffset)
    set(Calendar.HOUR_OF_DAY, 12)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

fun localStartOfDay(timestamp: Long): Long = Calendar.getInstance().apply {
    timeInMillis = timestamp
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

fun localEndOfDay(timestamp: Long): Long = Calendar.getInstance().apply {
    timeInMillis = timestamp
    set(Calendar.HOUR_OF_DAY, 23)
    set(Calendar.MINUTE, 59)
    set(Calendar.SECOND, 59)
    set(Calendar.MILLISECOND, 999)
}.timeInMillis

fun sameLocalDate(firstMillis: Long, secondMillis: Long): Boolean {
    val first = Calendar.getInstance().apply { timeInMillis = firstMillis }
    val second = Calendar.getInstance().apply { timeInMillis = secondMillis }
    return first.get(Calendar.ERA) == second.get(Calendar.ERA) &&
        first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
        first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR)
}

private fun utcDateMillis(year: Int, month: Int, day: Int): Long =
    Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(year, month, day, 0, 0, 0)
    }.timeInMillis

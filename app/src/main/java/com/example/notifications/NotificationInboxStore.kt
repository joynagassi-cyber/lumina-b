package com.example.notifications

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.UUID

/** Persistent local inbox for user-facing notifications; entries older than one calendar month are pruned. */
object NotificationInboxStore {
    data class Entry(
        val id: String,
        val title: String,
        val body: String,
        val createdAt: Long,
        val isRead: Boolean
    )

    private const val PREFS = "lumina_notification_inbox"
    private const val KEY_ENTRIES = "entries"

    @Synchronized
    fun list(context: Context): List<Entry> {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val entries = decode(prefs.getString(KEY_ENTRIES, "[]").orEmpty())
        val cutoff = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }.timeInMillis
        val retained = entries.filter { it.createdAt >= cutoff }.sortedByDescending { it.createdAt }
        if (retained.size != entries.size) save(prefs, retained)
        return retained
    }

    @Synchronized
    fun add(context: Context, title: String, body: String, now: Long = System.currentTimeMillis()): Entry {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val entry = Entry(UUID.randomUUID().toString(), title, body, now, false)
        save(prefs, (list(context) + entry).sortedByDescending { it.createdAt })
        return entry
    }

    @Synchronized
    fun markRead(context: Context, id: String) = update(context) { entries ->
        entries.map { if (it.id == id) it.copy(isRead = true) else it }
    }

    @Synchronized
    fun markAllRead(context: Context) = update(context) { entries -> entries.map { it.copy(isRead = true) } }

    private fun update(context: Context, transform: (List<Entry>) -> List<Entry>) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        save(prefs, transform(list(context)))
    }

    private fun save(prefs: android.content.SharedPreferences, entries: List<Entry>) {
        val json = JSONArray().apply {
            entries.forEach { entry ->
                put(JSONObject().apply {
                    put("id", entry.id)
                    put("title", entry.title)
                    put("body", entry.body)
                    put("createdAt", entry.createdAt)
                    put("isRead", entry.isRead)
                })
            }
        }
        prefs.edit().putString(KEY_ENTRIES, json.toString()).apply()
    }

    private fun decode(raw: String): List<Entry> = runCatching {
        val json = JSONArray(raw)
        (0 until json.length()).mapNotNull { index ->
            val item = json.optJSONObject(index) ?: return@mapNotNull null
            val title = item.optString("title").trim()
            if (title.isBlank()) null else Entry(
                id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                title = title,
                body = item.optString("body"),
                createdAt = item.optLong("createdAt"),
                isRead = item.optBoolean("isRead", false)
            )
        }
    }.getOrDefault(emptyList())
}

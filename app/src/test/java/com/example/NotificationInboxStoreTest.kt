package com.example

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.example.notifications.NotificationInboxStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
class NotificationInboxStoreTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun unread_entry_can_be_marked_read_individually_and_all_at_once() {
        context.getSharedPreferences("lumina_notification_inbox", Context.MODE_PRIVATE).edit().clear().commit()
        val first = NotificationInboxStore.add(context, "Confirmation", "Entrée enregistrée")
        val second = NotificationInboxStore.add(context, "Budget", "Plafond bientôt atteint")

        NotificationInboxStore.markRead(context, first.id)
        assertTrue(NotificationInboxStore.list(context).first { it.id == first.id }.isRead)
        assertFalse(NotificationInboxStore.list(context).first { it.id == second.id }.isRead)

        NotificationInboxStore.markAllRead(context)
        assertTrue(NotificationInboxStore.list(context).all { it.isRead })
    }

    @Test
    fun entries_older_than_one_calendar_month_are_removed() {
        context.getSharedPreferences("lumina_notification_inbox", Context.MODE_PRIVATE).edit().clear().commit()
        val oldDate = Calendar.getInstance().apply { add(Calendar.MONTH, -2) }.timeInMillis
        NotificationInboxStore.add(context, "Ancienne", "Ne doit plus apparaître", oldDate)
        NotificationInboxStore.add(context, "Récente", "Toujours visible")

        val entries = NotificationInboxStore.list(context)
        assertEquals(1, entries.size)
        assertEquals("Récente", entries.single().title)
    }
}

package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.cloud.CloudBackupLocalStore
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CloudBackupScheduleLimitTest {
    @Test
    fun at_most_three_distinct_sync_days_are_allowed_in_a_rolling_week() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("lumina_cloud_backup_links", Context.MODE_PRIVATE).edit().clear().commit()
        val firstDay = java.util.Calendar.getInstance().apply {
            clear()
            set(2026, java.util.Calendar.JANUARY, 10, 12, 0, 0)
        }
        fun atDay(offset: Int): Long = (firstDay.clone() as java.util.Calendar).apply {
            add(java.util.Calendar.DAY_OF_YEAR, offset)
        }.timeInMillis

        assertTrue(CloudBackupLocalStore.claimSyncDay(context, "sync-test", atDay(0)))
        assertTrue("same-day retry is grouped into the same sync day", CloudBackupLocalStore.claimSyncDay(context, "sync-test", atDay(0) + 1000L))
        assertTrue(CloudBackupLocalStore.claimSyncDay(context, "sync-test", atDay(1)))
        assertTrue(CloudBackupLocalStore.claimSyncDay(context, "sync-test", atDay(2)))
        assertFalse(CloudBackupLocalStore.claimSyncDay(context, "sync-test", atDay(3)))
        assertTrue(CloudBackupLocalStore.claimSyncDay(context, "sync-test", atDay(7)))
    }
}

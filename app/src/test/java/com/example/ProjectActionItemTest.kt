package com.example

import com.example.data.ProjectActionItem
import com.example.data.decodeProjectActionItems
import com.example.data.encodeProjectActionItems
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProjectActionItemTest {
    @Test
    fun checklist_round_trip_keeps_identity_and_completion() {
        val original = listOf(
            ProjectActionItem(id = "task-a", title = "Réserver la salle", isDone = true),
            ProjectActionItem(id = "task-b", title = "Préparer le programme")
        )

        val restored = decodeProjectActionItems(encodeProjectActionItems(original))

        assertEquals(original, restored)
        assertTrue(restored.first().isDone)
        assertFalse(restored.last().isDone)
    }

    @Test
    fun malformed_checklist_is_ignored_safely() {
        assertTrue(decodeProjectActionItems("not-json").isEmpty())
    }
}

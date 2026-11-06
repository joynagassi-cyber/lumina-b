package com.example

import com.example.data.ProjectActionItem
import com.example.data.ProjectEntity
import com.example.data.decodeProjectActionItems
import com.example.data.encodeProjectActionItems
import com.example.data.moveProjectActionItem
import com.example.export.BackupCodec
import com.example.export.BackupSnapshot
import com.example.sync.SyncBundle
import com.example.sync.SyncBundleCodec
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
    fun checklist_round_trip_keeps_nested_subtasks_for_backup_and_sync() {
        val original = listOf(
            ProjectActionItem(
                id = "task-parent",
                title = "Préparer le lieu",
                subTasks = listOf(
                    ProjectActionItem(id = "task-child", title = "Installer les chaises", isDone = true),
                    ProjectActionItem(id = "task-child-2", title = "Tester le son")
                )
            )
        )

        assertEquals(original, decodeProjectActionItems(encodeProjectActionItems(original)))
    }

    @Test
    fun nested_checklist_survives_full_backup_and_data_exchange_codecs() {
        val nestedTasks = encodeProjectActionItems(
            listOf(ProjectActionItem(id = "parent", title = "Préparer", subTasks = listOf(
                ProjectActionItem(id = "child", title = "Installer", isDone = true)
            )))
        )
        val project = ProjectEntity(id = 7L, uid = "event-uid", name = "Événement", kind = "EVENT", actionItemsJson = nestedTasks)

        val backupRoundTrip = BackupCodec.decode(BackupCodec.encode(BackupSnapshot(projects = listOf(project))))
        val exchangeRoundTrip = SyncBundleCodec.decode(SyncBundleCodec.encode(SyncBundle(projects = listOf(project))))

        assertEquals(nestedTasks, backupRoundTrip.projects.single().actionItemsJson)
        assertEquals(nestedTasks, exchangeRoundTrip.projects.single().actionItemsJson)
        assertEquals("Installer", decodeProjectActionItems(exchangeRoundTrip.projects.single().actionItemsJson).single().subTasks.single().title)
    }

    @Test
    fun reordering_tasks_and_subtasks_persists_their_nested_data() {
        val first = ProjectActionItem(id = "first", title = "Préparer", subTasks = listOf(ProjectActionItem(id = "child", title = "Installer")))
        val second = ProjectActionItem(id = "second", title = "Ranger")
        val moved = moveProjectActionItem(listOf(first, second), fromIndex = 0, direction = 1)
        val movedChildren = moveProjectActionItem(first.subTasks + ProjectActionItem(id = "child-2", title = "Nettoyer"), fromIndex = 0, direction = 1)

        assertEquals(listOf(second, first), moved)
        assertEquals(listOf("Nettoyer", "Installer"), movedChildren.map { it.title })
        assertEquals(first.subTasks, moved.last().subTasks)
        assertEquals(moved, decodeProjectActionItems(encodeProjectActionItems(moved)))
    }

    @Test
    fun legacy_checklist_without_subtasks_remains_readable() {
        val legacy = """[{"id":"legacy","title":"Tâche existante","isDone":true}]"""
        val restored = decodeProjectActionItems(legacy)

        assertEquals("Tâche existante", restored.single().title)
        assertTrue(restored.single().isDone)
        assertTrue(restored.single().subTasks.isEmpty())
    }

    @Test
    fun malformed_checklist_is_ignored_safely() {
        assertTrue(decodeProjectActionItems("not-json").isEmpty())
    }
}

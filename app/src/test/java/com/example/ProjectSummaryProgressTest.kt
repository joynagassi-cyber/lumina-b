package com.example

import com.example.ui.ProjectSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectSummaryProgressTest {
    @Test
    fun event_progress_uses_expenses_against_the_preserved_budget() {
        val event = ProjectSummary(
            id = 1L,
            uid = "event",
            name = "Fête annuelle",
            description = "",
            targetCents = 100_000L,
            collectedCents = 90_000L,
            spentCents = 35_000L,
            isClosed = false,
            kind = "EVENT"
        )

        assertEquals(0.35f, event.progress, 0.0001f)
        assertEquals(100_000L, event.targetCents)
    }

    @Test
    fun project_progress_remains_collection_based() {
        val project = ProjectSummary(
            id = 2L,
            uid = "project",
            name = "Construction",
            description = "",
            targetCents = 100_000L,
            collectedCents = 55_000L,
            spentCents = 12_000L,
            isClosed = false
        )

        assertEquals(0.55f, project.progress, 0.0001f)
    }
}

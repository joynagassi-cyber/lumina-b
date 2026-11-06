package com.example

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.ui.ProjectSummary
import com.example.ui.screens.ProjectsScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EventProjectScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun opening_created_event_displays_its_calendar_and_action_list() {
        val start = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.NOVEMBER, 12, 12, 0, 0)
        }.timeInMillis
        val end = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.NOVEMBER, 14, 12, 0, 0)
        }.timeInMillis
        val event = ProjectSummary(
            id = 42L,
            uid = "event-42",
            name = "Rencontre annuelle",
            description = "Événement de test",
            targetCents = 250_000L,
            collectedCents = 300_000L,
            spentCents = 80_000L,
            isClosed = false,
            kind = "EVENT",
            startAtMillis = start,
            endAtMillis = end
        )

        composeRule.setContent {
            MaterialTheme {
                ProjectsScreen(
                    projects = listOf(event),
                    onAddProject = { _, _, _, _, _, _, _ -> },
                    onUpdateProject = { _, _, _, _, _, _, _, _, _ -> },
                    onDeleteProject = { }
                )
            }
        }

        composeRule.onNodeWithTag("project_card_42").performScrollTo().performClick()
        composeRule.onNodeWithTag("project_detail_42").assertIsDisplayed()
        composeRule.onNodeWithTag("event_management_card").assertIsDisplayed()
        composeRule.onNodeWithTag("project_event_calendar").assertIsDisplayed()
        composeRule.onNodeWithTag("project_action_items").assertIsDisplayed()
    }
}

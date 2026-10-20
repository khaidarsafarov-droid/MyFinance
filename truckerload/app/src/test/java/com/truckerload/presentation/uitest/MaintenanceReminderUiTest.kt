package com.truckerload.presentation.uitest

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import com.truckerload.R
import com.truckerload.domain.model.MaintenanceProgress
import com.truckerload.domain.model.MaintenanceReminderType
import com.truckerload.domain.model.MaintenanceTask
import com.truckerload.presentation.components.SoftAppPageScaffold
import com.truckerload.presentation.components.verticalContentScroll
import com.truckerload.presentation.screens.maintenance.MaintenanceTasksSection
import com.truckerload.presentation.theme.TruckerLoadTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Maintenance (ТО) reminder screen. The production screen reads a Hilt view
 * model; this composes the same scaffold and task section with a demo oil-change
 * reminder.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp")
class MaintenanceReminderUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dueReminder_showsMilesLeft_andMarksDone() {
        var completedId: Long? = null
        var backPressed = false
        val oilChange = MaintenanceProgress(
            task = MaintenanceTask(
                id = 42L,
                title = "Oil change",
                startDate = "2026-09-01",
                reminderType = MaintenanceReminderType.MILES,
                intervalMiles = 2000.0,
                odometerAtStart = 100_000.0,
            ),
            milesDrivenSinceStart = 1200.0,
            estimatedOdometer = 101_200.0,
            targetOdometer = 102_000.0,
            milesRemaining = 800.0,
            daysRemaining = null,
            isDue = true,
            loadsCounted = 4,
            progressFraction = 0.6f,
        )

        composeRule.setContent {
            TruckerLoadTheme(darkTheme = true, dynamicColor = false) {
                SoftAppPageScaffold(
                    title = stringResource(R.string.maintenance_title),
                    useDashboardHero = true,
                    showBack = true,
                    onBack = { backPressed = true },
                    showPhoneMenu = false,
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .padding(padding)
                            .verticalContentScroll()
                            .padding(horizontal = 16.dp),
                    ) {
                        MaintenanceTasksSection(
                            activeProgress = listOf(oilChange),
                            completedTasks = emptyList(),
                            onAddTask = {},
                            onCompleteTask = { completedId = it },
                            onDeleteTask = {},
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithText("Maintenance").assertIsDisplayed()
        composeRule.onNodeWithText("Reminders").assertIsDisplayed()
        composeRule.onNodeWithText("Oil change").assertIsDisplayed()
        composeRule.onNodeWithText("Urgent").assertIsDisplayed()
        composeRule.onNodeWithText("Service required now!").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Driven since service: 1,200 mi (4 loads) · 800 mi left")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Done").performScrollTo().performClick()
        assertEquals(42L, completedId)
        composeRule.onNode(hasContentDescription("Back") and hasClickAction())
            .performSemanticsAction(SemanticsActions.OnClick)
        assertTrue(backPressed)
    }
}

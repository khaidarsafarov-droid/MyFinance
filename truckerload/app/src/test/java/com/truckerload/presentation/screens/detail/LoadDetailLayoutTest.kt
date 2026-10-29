package com.truckerload.presentation.screens.detail

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.truckerload.data.backup.BackupTestFixtures
import com.truckerload.data.local.entities.ScanEntity
import com.truckerload.domain.model.withRouteMetrics
import com.truckerload.domain.money.Money
import com.truckerload.presentation.theme.TruckerLoadTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "en-rUS-w390dp-h900dp")
class LoadDetailLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun summaryShowsRouteDateAndSixMetrics() {
        composeRule.setContent {
            TruckerLoadTheme(darkTheme = true, dynamicColor = false, reduceMotion = true) {
                LoadDetailLoadedContent(
                    load = filledLoad(),
                    photos = emptyList(),
                    scans = listOf(sampleScan()),
                    onEditMiles = {},
                    onEditFinish = {},
                    onSaveDeadhead = {},
                    onDisputeChanged = {},
                    onPhotoClick = {},
                    onCamera = {},
                    onScanner = {},
                    onScanClick = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        composeRule.onNodeWithText("Garner, NC → Dallas, TX").assertIsDisplayed()
        composeRule.onNodeWithText("Gross").assertIsDisplayed()
        composeRule.onNodeWithText("Miles").assertIsDisplayed()
        composeRule.onNodeWithText("RPM").assertIsDisplayed()
        composeRule.onNodeWithText("Duration").assertIsDisplayed()
        composeRule.onNodeWithText("Pace").assertIsDisplayed()
        composeRule.onAllNodesWithText("Stops").assertCountEquals(2)
        composeRule.onNodeWithText("Photos & scans").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Camera").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Scanner").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("rate-con.pdf").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("PU #1").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun chipsShowCurrentValuesAndOpenExistingEditors() {
        var milesClicks = 0
        var finishClicks = 0
        var cameraClicks = 0
        var scannerClicks = 0
        composeRule.setContent {
            TruckerLoadTheme(darkTheme = true, dynamicColor = false, reduceMotion = true) {
                LoadDetailLoadedContent(
                    load = filledLoad(),
                    photos = emptyList(),
                    scans = emptyList(),
                    onEditMiles = { milesClicks++ },
                    onEditFinish = { finishClicks++ },
                    onSaveDeadhead = {},
                    onDisputeChanged = {},
                    onPhotoClick = {},
                    onCamera = { cameraClicks++ },
                    onScanner = { scannerClicks++ },
                    onScanClick = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        composeRule.onNodeWithText("850 mi").performClick()
        assertEquals(1, milesClicks)
        composeRule.onNodeWithText("Delivered").performClick()
        assertEquals(1, finishClicks)
        composeRule.onNodeWithText("Deadhead 45 mi").assertIsDisplayed()
        composeRule.onNodeWithText("Dispute open").performClick()
        composeRule.onNodeWithText("In dispute").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Response date").assertIsDisplayed()
        composeRule.onNodeWithText("Camera").performScrollTo().performClick()
        composeRule.onNodeWithText("Scanner").performScrollTo().performClick()
        assertEquals(1, cameraClicks)
        assertEquals(1, scannerClicks)
    }

    @Test
    fun unsetChipsUseAddLabels() {
        composeRule.setContent {
            TruckerLoadTheme(darkTheme = false, dynamicColor = false, reduceMotion = true) {
                LoadDetailLoadedContent(
                    load = emptyLoad(),
                    photos = emptyList(),
                    scans = emptyList(),
                    onEditMiles = {},
                    onEditFinish = {},
                    onSaveDeadhead = {},
                    onDisputeChanged = {},
                    onPhotoClick = {},
                    onCamera = {},
                    onScanner = {},
                    onScanClick = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        composeRule.onNodeWithText("Add miles").assertIsDisplayed()
        composeRule.onNodeWithText("Add deadhead").assertIsDisplayed()
        composeRule.onNodeWithText("Complete load").assertIsDisplayed()
        composeRule.onNodeWithText("Add dispute").assertIsDisplayed()
        composeRule.onNodeWithText("No stops on this load").performScrollTo().assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("Change deadhead").fetchSemanticsNodes().isEmpty())
    }

    private fun filledLoad() = BackupTestFixtures.sampleLoad().copy(
        deadheadMiles = 45.0,
        isDispute = true,
        disputeResponseDate = "2026-07-25",
        disputeAmount = Money.fromDollars(120.0),
    ).withRouteMetrics()

    private fun emptyLoad() = BackupTestFixtures.sampleLoad().copy(
        totalMiles = 0.0,
        deadheadMiles = 0.0,
        actualFinishDate = null,
        isDispute = false,
        disputeCompleted = false,
        stops = emptyList(),
        puCount = 0,
        delCount = 0,
        stopCount = 0,
        durationDays = 0.0,
        pace = Money.ZERO,
        totalRate = Money.ZERO,
    ).withRouteMetrics()

    private fun sampleScan() = ScanEntity(
        id = "scan-1",
        fileName = "rate-con.pdf",
        filePath = "/tmp/rate-con.pdf",
        timestamp = 0L,
        fileSizeBytes = 1200L,
        pageCount = 2,
    )
}

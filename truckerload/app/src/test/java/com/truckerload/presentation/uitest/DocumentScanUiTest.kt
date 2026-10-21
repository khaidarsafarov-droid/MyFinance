package com.truckerload.presentation.uitest

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.truckerload.domain.model.ScanDocumentCategory
import com.truckerload.presentation.screens.scanner.PendingScan
import com.truckerload.presentation.screens.scanner.ScanResultScreen
import com.truckerload.presentation.screens.scanner.ScannerFlowScreen
import com.truckerload.presentation.screens.scanner.ScannerUiState
import com.truckerload.presentation.screens.scanner.ScannerViewModel
import com.truckerload.presentation.theme.TruckerLoadTheme
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Document scan flow. Play Services is not available under Robolectric, so the
 * entry screen shows the unavailable state. The result screen is driven with a
 * demo PDF and fake OCR text.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h900dp")
class DocumentScanUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun scannerEntry_showsUnavailableWithoutPlayServices() {
        val viewModel = mock<ScannerViewModel>()
        whenever(viewModel.uiState).thenReturn(MutableStateFlow(ScannerUiState()))
        whenever(viewModel.isAttachedToLoad).thenReturn(false)
        var finished = false

        composeRule.setContent {
            TruckerLoadTheme(darkTheme = true, dynamicColor = false) {
                ScannerFlowScreen(
                    onFinished = { finished = true },
                    onOpenGallery = {},
                    viewModel = viewModel,
                )
            }
        }

        composeRule.onNodeWithText("Scanner unavailable").assertIsDisplayed()
        composeRule.onNodeWithText("Back").performClick()
        assertTrue(finished)
    }

    @Test
    fun scanResult_showsDemoDocumentAndSaves() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File(context.cacheDir, "demo-rate-con.pdf")
        file.writeText("demo scan page")
        var saved = false
        var category = ScanDocumentCategory.OTHER
        val pending = PendingScan(
            file = file,
            pageCount = 2,
            ocrText = "DEMO BOL 1001",
            timestamp = 1_761_000_000_000L,
            category = ScanDocumentCategory.OTHER,
        )

        composeRule.setContent {
            TruckerLoadTheme(darkTheme = true, dynamicColor = false) {
                ScanResultScreen(
                    pending = pending,
                    sessionCount = 1,
                    isAttachedToLoad = false,
                    onSaveToApp = { saved = true },
                    onSaveToPhone = {},
                    onShare = {},
                    onAddAnother = {},
                    onOpenGallery = {},
                    onCategoryChange = { category = it },
                    onClose = {},
                )
            }
        }

        composeRule.onNodeWithText("Scan results").assertIsDisplayed()
        composeRule.onNodeWithText("demo-rate-con.pdf").assertIsDisplayed()
        composeRule.onNodeWithText("Pages: 2").assertIsDisplayed()
        composeRule.onNodeWithText("Show recognized text").performScrollTo().performClick()
        composeRule.onNodeWithText("DEMO BOL 1001").assertIsDisplayed()
        composeRule.onNodeWithText("Save to app").performScrollTo().performClick()
        assertTrue(saved)
        assertEquals(ScanDocumentCategory.OTHER, category)
    }
}

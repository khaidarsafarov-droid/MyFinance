package com.truckerload.presentation.uitest

import android.graphics.BitmapFactory
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.truckerload.presentation.screens.camera.CameraScreen
import com.truckerload.presentation.screens.camera.CameraUiState
import com.truckerload.presentation.screens.camera.CameraViewModel
import com.truckerload.presentation.screens.camera.CapturedPhoto
import com.truckerload.presentation.screens.camera.PhotoPreviewScreen
import com.truckerload.presentation.theme.TruckerLoadTheme
import com.truckerload.utils.LocationData
import com.truckerload.utils.PhotoManager
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Camera screen plus the location-stamp review. CameraX and GPS are faked:
 * a generated JPEG and [FakeLocationStamp] stand in for the hardware.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [34], qualifiers = "w360dp-h800dp")
class LocationPhotoUiTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @Inject lateinit var location: FakeLocationStamp

    @Inject lateinit var camera: FakeCameraCapture

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun cameraScreen_showsCaptureChromeWithoutHardware() {
        val viewModel = mock<CameraViewModel>()
        whenever(viewModel.uiState).thenReturn(MutableStateFlow(CameraUiState()))
        var backPressed = false

        composeRule.setContent {
            TruckerLoadTheme(darkTheme = true, dynamicColor = false) {
                CameraScreen(
                    sessionCount = 0,
                    onOpenBatch = {},
                    onBack = { backPressed = true },
                    viewModel = viewModel,
                )
            }
        }

        composeRule.onNodeWithText("Take photo").assertIsDisplayed()
        composeRule.onNodeWithText("Camera permission denied").assertIsDisplayed()
        composeRule.onNode(hasContentDescription("Back")).performClick()
        assertTrue(backPressed)
    }

    @Test
    fun stampedPreview_showsFakeCityAndCoordinates() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val captured = camera.writeDemoJpeg()
        val decoded = BitmapFactory.decodeFile(captured.absolutePath)
        checkNotNull(decoded) { "demo jpeg did not decode" }
        val fix = LocationData(
            latitude = location.latitude,
            longitude = location.longitude,
            city = location.city,
            state = location.state,
            zipCode = location.zipCode,
        )
        val stamped = PhotoManager(context).savePhoto(
            bitmap = decoded,
            locationData = fix,
            timestamp = 1_761_000_000_000L,
            watermarkTitle = "T-DEMO1",
        )
        decoded.recycle()
        assertTrue(stamped.length() > 0L)
        assertEquals("Garner, NC", fix.cityStateLine)

        val photo = CapturedPhoto(
            file = stamped,
            locationData = fix,
            timestamp = 1_761_000_000_000L,
        )
        var saved = false

        composeRule.setContent {
            TruckerLoadTheme(darkTheme = true, dynamicColor = false) {
                PhotoPreviewScreen(
                    photo = photo,
                    onCancel = {},
                    onRetake = {},
                    onSaved = {},
                    onSave = { saved = true },
                    saveSuccess = false,
                )
            }
        }

        composeRule.onNodeWithText("Garner, NC").assertIsDisplayed()
        composeRule.onNodeWithText("27529").assertIsDisplayed()
        composeRule.onNodeWithText("35.711° N, 78.614° W").assertIsDisplayed()
        composeRule.onNodeWithText("Save").performClick()
        assertTrue(saved)
    }
}

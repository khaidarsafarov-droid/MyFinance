package com.truckerload.presentation.screens.add

import androidx.lifecycle.SavedStateHandle
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.data.repository.AiRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.domain.model.Load
import com.truckerload.domain.parser.LoadField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * A load that is missing non-blocking fields must wait for the driver to confirm
 * before it reaches Room.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AddLoadConfirmIncompleteTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var loadRepository: LoadRepository
    private lateinit var viewModel: AddLoadViewModel

    @Before
    fun setUp() = runTest {
        Dispatchers.setMain(dispatcher)
        loadRepository = org.mockito.kotlin.mock()
        whenever(loadRepository.getByTripId(any())).thenReturn(null)
        viewModel = AddLoadViewModel(
            application = RuntimeEnvironment.getApplication(),
            loadRepository = loadRepository,
            aiRepository = AiRepository(),
            settingsDataStore = SettingsDataStore(RuntimeEnvironment.getApplication()),
            savedStateHandle = SavedStateHandle(),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun incompleteDraftIsHeldUntilConfirmed() = runTest {
        viewModel.setMode(AddLoadInputMode.MANUAL)
        viewModel.setManualRate("2100.50")
        viewModel.setManualPoint(0, "Garner, NC")

        save()

        val pending = viewModel.uiState.value.confirmIncomplete
        assertNotNull("save must pause on gaps", pending)
        assertTrue(pending!!.missingOptional.contains(LoadField.DELIVERY))
        assertTrue(pending.missingOptional.contains(LoadField.MILES))
        verify(loadRepository, never()).insertLoad(any(), any())

        viewModel.confirmIncompleteSave(
            saveErrorFormatter = { it },
            onOptimisticInsert = null,
        )

        verify(loadRepository, times(1)).insertLoad(any(), any())
        assertNull(viewModel.uiState.value.confirmIncomplete)
        assertNotNull(viewModel.uiState.value.savedLoad)
    }

    @Test
    fun dismissingConfirmationKeepsTheDraftUnsaved() = runTest {
        viewModel.setMode(AddLoadInputMode.MANUAL)
        viewModel.setManualRate("1500")
        viewModel.setManualPoint(0, "Reno, NV")
        save()

        viewModel.dismissIncompleteConfirm()

        verify(loadRepository, never()).insertLoad(any(), any())
        assertNull(viewModel.uiState.value.confirmIncomplete)
        assertEquals("1500", viewModel.uiState.value.manual.rate)
    }

    @Test
    fun completeDraftSavesWithoutAskingForConfirmation() = runTest {
        viewModel.setMode(AddLoadInputMode.MANUAL)
        viewModel.setManualTripId("T-77001")
        viewModel.setManualDate("2026-08-20")
        viewModel.setManualRate("2100.50")
        viewModel.setManualMiles("700")
        viewModel.setManualPoint(0, "Garner, NC")
        viewModel.setManualPoint(1, "Dallas, TX")

        save()

        assertNull("nothing missing, no prompt", viewModel.uiState.value.confirmIncomplete)
        verify(loadRepository, times(1)).insertLoad(any(), any())
        val saved: Load? = viewModel.uiState.value.savedLoad
        assertEquals(2100.50, saved!!.totalRate, 0.01)
        assertEquals(700.0, saved.totalMiles, 0.01)
    }

    @Test
    fun missingRateKeepsTheSaveButtonDisabled() = runTest {
        viewModel.setMode(AddLoadInputMode.MANUAL)
        viewModel.setManualPoint(0, "Garner, NC")

        val fields = viewModel.uiState.value.manual
        assertFalse("rate is required, not confirmable", fields.canSave())
        assertTrue(fields.completeness().missingRequired.contains(LoadField.RATE))
    }

    private fun save() = viewModel.save(
        parseFailedFallback = "parse failed",
        saveErrorFormatter = { it },
        onOptimisticInsert = null,
    )
}

package com.truckerload.presentation.screens.social

import com.truckerload.domain.social.EnhancedDriverProfile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileUiSharingTest {

    @Test
    fun lastProfileSurvivesUnsubscribeSoReturnDoesNotShowTheSpinner() = runTest {
        val profile = mock<EnhancedDriverProfile>()
        val loaded = ProfileUiState(profile = profile, givenName = "Ada", familyName = "Lovelace")
        val source = MutableStateFlow(loaded)
        val state = source.stateIn(backgroundScope, ProfileUiSharing, ProfileUiState())
        val dispatcher = UnconfinedTestDispatcher(testScheduler)

        val job = launch(dispatcher) { state.collect { } }
        runCurrent()
        assertSame(profile, state.value.profile)
        assertEquals("Ada", state.value.givenName)

        job.cancel()
        advanceTimeBy(5_000)
        runCurrent()

        assertSame(profile, state.value.profile)
        assertEquals("Ada", state.value.givenName)

        val seen = mutableListOf<ProfileUiState>()
        val again = launch(dispatcher) { state.collect { seen += it } }
        assertEquals(listOf(loaded), seen)
        again.cancel()
    }
}

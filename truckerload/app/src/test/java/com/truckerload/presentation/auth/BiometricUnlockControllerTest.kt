package com.truckerload.presentation.auth

import android.content.Context
import androidx.biometric.BiometricPrompt
import androidx.test.core.app.ApplicationProvider
import com.truckerload.data.preferences.BiometricUnlockStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private class FakeAuthenticator(var available: Boolean = true) : BiometricAuthenticator {
    var prompts = 0
    var callback: BiometricAuthenticator.Callback? = null

    override fun isAvailable() = available

    override fun authenticate(callback: BiometricAuthenticator.Callback) {
        prompts++
        this.callback = callback
    }
}

private object TestMessages : BiometricUnlockMessages {
    override fun failed() = "failed"

    override fun cancelled() = "cancelled"
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BiometricUnlockControllerTest {

    private var unlocks = 0
    private val errors = mutableListOf<String?>()

    private fun controller(authenticator: BiometricAuthenticator?) = BiometricUnlockController(
        authenticator = authenticator,
        messages = TestMessages,
        onUnlocked = { unlocks++ },
        onError = { errors += it },
    )

    @After
    fun resetSession() {
        BiometricSession.unlockedThisProcess = false
    }

    @Test
    fun successfulPromptUnlocksOnce() {
        val fake = FakeAuthenticator()
        controller(fake).launch()
        assertEquals(1, fake.prompts)
        assertEquals(0, unlocks)

        fake.callback!!.onSucceeded()

        assertEquals(1, unlocks)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun userCancelAndNegativeButtonStayLockedWithCancelledMessage() {
        listOf(BiometricPrompt.ERROR_USER_CANCELED, BiometricPrompt.ERROR_NEGATIVE_BUTTON).forEach { code ->
            errors.clear()
            val fake = FakeAuthenticator()
            controller(fake).launch()
            fake.callback!!.onError(code, "system text")
            assertEquals(listOf<String?>("cancelled"), errors)
        }
        assertEquals(0, unlocks)
    }

    @Test
    fun otherErrorsShowTheSystemMessageAndStayLocked() {
        val fake = FakeAuthenticator()
        controller(fake).launch()
        fake.callback!!.onError(BiometricPrompt.ERROR_LOCKOUT, "Too many attempts")
        assertEquals(listOf<String?>("Too many attempts"), errors)
        assertEquals(0, unlocks)
    }

    @Test
    fun unrecognizedFingerprintStaysLocked() {
        val fake = FakeAuthenticator()
        controller(fake).launch()
        fake.callback!!.onFailed()
        assertEquals(listOf<String?>("failed"), errors)
        assertEquals(0, unlocks)
    }

    @Test
    fun unavailableHardwareNeverPromptsAndNeverUnlocks() {
        val fake = FakeAuthenticator(available = false)
        controller(fake).launch()
        assertEquals(0, fake.prompts)
        assertEquals(listOf<String?>("failed"), errors)
        assertEquals(0, unlocks)
    }

    @Test
    fun missingActivityNeverUnlocks() {
        controller(null).launch()
        assertEquals(listOf<String?>("failed"), errors)
        assertEquals(0, unlocks)
    }

    @Test
    fun retryAfterAFailureCanStillUnlock() {
        val fake = FakeAuthenticator()
        val gate = controller(fake)
        gate.launch()
        fake.callback!!.onError(BiometricPrompt.ERROR_USER_CANCELED, "x")
        gate.launch()
        assertEquals(2, fake.prompts)
        fake.callback!!.onSucceeded()
        assertEquals(1, unlocks)
    }

    @Test
    fun sessionFlagStartsLockedAndUnlockStoreRoundTrips() {
        assertFalse(BiometricSession.unlockedThisProcess)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = BiometricUnlockStore(context)
        store.clear()
        assertFalse(store.isEnabled())
        store.setEnabled(true)
        assertTrue(BiometricUnlockStore(context).isEnabled())
        store.clear()
        assertFalse(store.isEnabled())
        assertNull(errors.firstOrNull())
    }
}

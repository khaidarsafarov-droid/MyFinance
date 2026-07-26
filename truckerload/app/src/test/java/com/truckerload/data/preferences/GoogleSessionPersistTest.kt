package com.truckerload.data.preferences

import com.truckerload.data.auth.SilentAuthRestorer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GoogleSessionPersistTest {

    @Test
    fun googleLoginPersistsEvenWhenRememberMeFalse() {
        val context = RuntimeEnvironment.getApplication()
        val auth = AuthStore(context)

        auth.login(
            userId = "google_sub_persist_1",
            email = "driver@example.com",
            rememberMe = false,
            accessToken = "access-token",
            refreshToken = "refresh-token",
            googleSub = "google-sub-1",
            provider = AuthProvider.GOOGLE,
        )

        assertTrue(auth.isLoggedIn.value)
        assertEquals(AuthProvider.GOOGLE, auth.authProvider())
        assertNotNull(auth.sessionOrNull())

        // Google OAuth must survive process death even if the checkbox was unchecked.
        assertEquals("google_sub_persist_1", auth.currentUserIdOrNull())
        assertEquals("refresh-token", auth.sessionOrNull()?.refreshToken)
    }

    @Test
    fun localOnlySilentRestoreDoesNotTouchCredentialManagerPath() {
        val context = RuntimeEnvironment.getApplication()
        val auth = AuthStore(context)
        val profile = UserProfileStore(context)

        auth.login(
            userId = "google_sub_persist_2",
            email = "driver2@example.com",
            rememberMe = true,
            googleSub = "google-sub-2",
            provider = AuthProvider.GOOGLE,
        )

        // LOCAL_ONLY_MODE is true in this workspace build — restore must stay VERIFIED
        // without launching Credential Manager (regression for "Google every launch").
        val health = kotlinx.coroutines.runBlocking {
            SilentAuthRestorer.restore(context, auth, profile)
        }
        assertEquals(AuthSessionHealth.VERIFIED, health)
        assertTrue(auth.isLoggedIn.value)
        assertFalse(auth.userId.value.isNullOrBlank())
    }
}

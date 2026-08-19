package com.truckerload.data.preferences

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuthRegistrationPersistTest {

    @Before
    fun setUp() {
        AuthStore.resetForTests()
        SecurePreferences.resetFallbackForTests()
    }

    @After
    fun tearDown() {
        AuthStore.resetForTests()
        SecurePreferences.resetFallbackForTests()
    }

    @Test
    fun emailSignupCredentialsSurviveSecureStorageFallback() {
        SecurePreferences.markFallbackForTests()
        val context = RuntimeEnvironment.getApplication()
        val credentials = AuthCredentialsStore(context)

        credentials.saveCredentials("Driver@Example.com", "Password1")

        assertTrue(credentials.hasCredentialsFor("driver@example.com"))
        assertTrue(credentials.validateCredentials("driver@example.com", "Password1"))
        assertFalse(credentials.validateCredentials("driver@example.com", "WrongPass1"))
    }

    @Test
    fun emailSessionSurvivesColdStartWithoutProviderKey() {
        val context = RuntimeEnvironment.getApplication()
        val auth = AuthStore(context)
        val profileStore = UserProfileStore(context)

        AuthLogin.completeLogin(
            authStore = auth,
            userProfileStore = profileStore,
            userId = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
            profile = UserProfile(
                email = "cloud@example.com",
                givenName = "Cloud",
                familyName = "Driver",
                photoUrl = null,
            ),
        )

        // Simulate older prefs that only kept identity flags (same store AuthStore uses).
        SecurePreferences.resetFallbackForTests()
        SecurePreferences.open(context, "truckerload_auth_enc").edit()
            .remove("auth_provider")
            .commit()

        AuthStore.resetForTests()
        SecurePreferences.resetFallbackForTests()
        val restored = AuthStore(context)

        assertTrue(restored.isLoggedIn.value)
        assertEquals("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", restored.currentUserIdOrNull())
        assertEquals(AuthProvider.EMAIL, restored.authProvider())
        assertFalse(restored.authProvider() == AuthProvider.LOCAL)
    }

    @Test
    fun uuidLoginNeverResolvesToGuestLocalProvider() {
        val context = RuntimeEnvironment.getApplication()
        AuthStore.resetForTests()
        val auth = AuthStore(context)

        auth.login(
            userId = "11111111-2222-3333-4444-555555555555",
            email = "u@example.com",
            rememberMe = true,
            provider = AuthProvider.LOCAL,
        )

        assertEquals(AuthProvider.EMAIL, auth.authProvider())
        assertTrue(auth.isLoggedIn.value)
    }

    @Test
    fun localEmailRegistrationCanLoginAfterRestart() {
        val context = RuntimeEnvironment.getApplication()
        val credentials = AuthCredentialsStore(context)
        val auth = AuthStore(context)
        val profiles = UserProfileStore(context)
        val email = "tablet@example.com"
        val password = "SecurePass1"

        credentials.saveCredentials(email, password)
        AuthLogin.completeLogin(
            authStore = auth,
            userProfileStore = profiles,
            userId = AccountIds.fromEmail(email),
            profile = UserProfile(
                email = email,
                givenName = "Tablet",
                familyName = "User",
                photoUrl = null,
            ),
        )

        AuthStore.resetForTests()
        val restoredAuth = AuthStore(context)
        val restoredCreds = AuthCredentialsStore(context)

        assertTrue(restoredAuth.isLoggedIn.value)
        assertEquals(AccountIds.fromEmail(email), restoredAuth.currentUserIdOrNull())
        assertTrue(restoredCreds.validateCredentials(email, password))
    }

    @Test
    fun googleCloudLogin_storesIdTokenBesideSupabaseJwt() {
        val context = RuntimeEnvironment.getApplication()
        val auth = AuthStore(context)
        val profiles = UserProfileStore(context)

        AuthLogin.completeLogin(
            authStore = auth,
            userProfileStore = profiles,
            userId = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
            profile = UserProfile(
                email = "cloud-google@example.com",
                givenName = "Cloud",
                familyName = "Google",
                photoUrl = null,
                googleId = "google-sub-cloud",
            ),
            accessToken = "supabase.access.jwt",
            refreshToken = "supabase.refresh",
            googleIdToken = "google.id.token",
        )

        assertEquals("supabase.access.jwt", auth.accessTokenOrNull())
        assertEquals("google.id.token", auth.googleIdTokenOrNull())
        assertEquals(AuthProvider.GOOGLE, auth.authProvider())
    }
}

package com.truckerload.data.preferences

import com.truckerload.data.auth.SilentAuthRestorer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GoogleSessionPersistTest {

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
        assertEquals(AccountIds.fromGoogleSub("google-sub-1"), auth.currentUserIdOrNull())
        assertEquals("refresh-token", auth.sessionOrNull()?.refreshToken)
    }

    @Test
    fun emailLoginPersistsEvenWhenRememberMeFalse() {
        val context = RuntimeEnvironment.getApplication()
        val auth = AuthStore(context)

        auth.login(
            userId = "local_email_persist_1",
            email = "driver@example.com",
            rememberMe = false,
            provider = AuthProvider.EMAIL,
        )

        assertTrue(auth.isLoggedIn.value)
        assertEquals(AuthProvider.EMAIL, auth.authProvider())
        assertEquals("local_email_persist_1", auth.currentUserIdOrNull())
        assertTrue(auth.sessionOrNull() != null)
    }

    @Test
    fun googleSilentRestoreKeepsLocalSessionWithoutCredentialManager() {
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

        // Must not launch Credential Manager; offline → OFFLINE_LOCAL, otherwise VERIFIED.
        val health = kotlinx.coroutines.runBlocking {
            SilentAuthRestorer.restore(context, auth, profile)
        }
        assertTrue(
            health == AuthSessionHealth.VERIFIED || health == AuthSessionHealth.OFFLINE_LOCAL,
        )
        assertTrue(auth.isLoggedIn.value)
        assertFalse(auth.userId.value.isNullOrBlank())
    }

    @Test
    fun googleIdTokenSurvivesColdStartAlongsideSupabaseJwt() {
        val context = RuntimeEnvironment.getApplication()
        val auth = AuthStore(context)

        auth.login(
            userId = "google_voice_user",
            email = "voice@example.com",
            rememberMe = true,
            accessToken = "supabase.jwt",
            refreshToken = "supabase.refresh",
            googleSub = "google-sub-voice",
            provider = AuthProvider.GOOGLE,
            googleIdToken = "google.id.token",
        )

        assertEquals("supabase.jwt", auth.accessTokenOrNull())
        assertEquals("google.id.token", auth.googleIdTokenOrNull())

        val secretsOnDisk = !SecurePreferences.plaintextFallbackUsed
        AuthStore.resetForTests()
        val restored = AuthStore(context)
        assertTrue(restored.isLoggedIn.value)
        assertEquals(AccountIds.fromGoogleSub("google-sub-voice"), restored.currentUserIdOrNull())
        if (secretsOnDisk) {
            assertEquals("supabase.jwt", restored.accessTokenOrNull())
            assertEquals("google.id.token", restored.googleIdTokenOrNull())
        }
    }

    @Test
    fun updateTokensDoesNotWipeGoogleIdToken() {
        val context = RuntimeEnvironment.getApplication()
        val auth = AuthStore(context)
        auth.login(
            userId = "google_refresh_user",
            email = "refresh@example.com",
            rememberMe = true,
            accessToken = "old.jwt",
            refreshToken = "old.refresh",
            googleSub = "sub-refresh",
            provider = AuthProvider.GOOGLE,
            googleIdToken = "google.id.token",
        )

        auth.updateTokens("new.jwt", "new.refresh")

        assertEquals("new.jwt", auth.accessTokenOrNull())
        assertEquals("google.id.token", auth.googleIdTokenOrNull())
    }

    @Test
    fun logoutAndNextUserDoNotInheritGoogleIdToken() {
        val context = RuntimeEnvironment.getApplication()
        val auth = AuthStore(context)
        auth.login(
            userId = "google_user_a",
            email = "a@example.com",
            rememberMe = true,
            accessToken = "a.jwt",
            googleSub = "sub-a",
            provider = AuthProvider.GOOGLE,
            googleIdToken = "google.id.a",
        )
        auth.logout()
        assertNull(auth.googleIdTokenOrNull())
        assertNull(auth.accessTokenOrNull())

        auth.login(
            userId = "email_user_b",
            email = "b@example.com",
            rememberMe = true,
            provider = AuthProvider.EMAIL,
        )
        assertNull(auth.googleIdTokenOrNull())
        assertEquals("email_user_b", auth.currentUserIdOrNull())
    }

    @Test
    fun sameUserReloginWithoutGoogleTokenPreservesIt() {
        val context = RuntimeEnvironment.getApplication()
        val auth = AuthStore(context)
        auth.login(
            userId = "google_keep",
            email = "keep@example.com",
            rememberMe = true,
            accessToken = "jwt",
            googleSub = "sub-keep",
            provider = AuthProvider.GOOGLE,
            googleIdToken = "google.id.keep",
        )
        auth.setLoggedIn(true)
        assertEquals("google.id.keep", auth.googleIdTokenOrNull())
        assertEquals("jwt", auth.accessTokenOrNull())
    }

    @Test
    fun googleUuidSessionRewritesToCanonicalGoogleIdOnColdStart() {
        val context = RuntimeEnvironment.getApplication()
        val auth = AuthStore(context)
        val sub = "unify-cold-sub"
        auth.login(
            userId = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
            email = "voice@example.com",
            rememberMe = true,
            googleSub = sub,
            provider = AuthProvider.GOOGLE,
        )
        AuthStore.resetForTests()
        val restored = AuthStore(context)
        assertEquals(AccountIds.fromGoogleSub(sub), restored.currentUserIdOrNull())
        assertTrue(restored.isLoggedIn.value)
    }

    @Test
    fun googleLoginWithSupabaseUuidCopiesJournalToCanonicalId() {
        val context = RuntimeEnvironment.getApplication()
        val uuid = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
        val sub = "unify-login-sub"
        val source = context.getDatabasePath(
            com.truckerload.data.local.AppDatabase.databaseNameFor(uuid),
        )
        source.parentFile?.mkdirs()
        android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(source, null).use { db ->
            db.execSQL("CREATE TABLE t(id INTEGER PRIMARY KEY)")
            db.execSQL("INSERT INTO t VALUES (7)")
        }

        val auth = AuthStore(context)
        auth.login(
            userId = uuid,
            email = "same@example.com",
            rememberMe = true,
            googleSub = sub,
            provider = AuthProvider.GOOGLE,
        )

        val canonical = AccountIds.fromGoogleSub(sub)
        assertEquals(canonical, auth.currentUserIdOrNull())
        assertTrue(
            com.truckerload.data.local.DatabaseFileCopy.isHealthyDatabase(
                context.getDatabasePath(
                    com.truckerload.data.local.AppDatabase.databaseNameFor(canonical),
                ),
            ),
        )
    }

    @Test
    fun tryCompleteLogin_oneGoogleAccountIgnoresSupabaseUuid() {
        val context = RuntimeEnvironment.getApplication()
        val auth = AuthStore(context)
        val profiles = UserProfileStore(context)
        val sub = "one-google-one-login"
        val ok = AuthLogin.tryCompleteLogin(
            authStore = auth,
            userProfileStore = profiles,
            supabaseUserId = "bbbbbbbb-cccc-dddd-eeee-ffffffffffff",
            profile = UserProfile(
                email = "driver@example.com",
                givenName = "D",
                familyName = "R",
                photoUrl = null,
                googleId = sub,
            ),
        )
        assertTrue(ok)
        assertEquals(AccountIds.fromGoogleSub(sub), auth.currentUserIdOrNull())
        assertEquals(AccountIds.fromGoogleSub(sub), profiles.boundUserIdOrNull)
    }
}

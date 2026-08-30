package com.truckerload.data.remote

import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.truckerload.BuildConfig
import com.truckerload.data.backup.GoogleDriveBackupPrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GoogleSignInClientsTest {

    @Test
    fun isDeveloperError_status10() {
        val err = ApiException(Status(CommonStatusCodes.DEVELOPER_ERROR))
        assertTrue(GoogleSignInClients.isDeveloperError(err))
        assertTrue(GoogleSignInClients.isDeveloperError(RuntimeException(err)))
    }

    @Test
    fun isDeveloperError_otherCodesFalse() {
        assertFalse(
            GoogleSignInClients.isDeveloperError(ApiException(Status(CommonStatusCodes.NETWORK_ERROR))),
        )
        assertFalse(GoogleSignInClients.isDeveloperError(IllegalStateException("no")))
    }

    @Test
    fun shouldRetryWithoutIdToken_onlyWhenWebClientConfigured() {
        val err = ApiException(Status(CommonStatusCodes.DEVELOPER_ERROR))
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) {
            assertFalse(
                GoogleSignInClients.shouldRetryWithoutIdToken(err, alreadyOmittingIdToken = false),
            )
        } else {
            assertTrue(
                GoogleSignInClients.shouldRetryWithoutIdToken(err, alreadyOmittingIdToken = false),
            )
            assertFalse(
                GoogleSignInClients.shouldRetryWithoutIdToken(err, alreadyOmittingIdToken = true),
            )
        }
    }

    @Test
    fun loginOptions_doesNotRequestIdTokenWhenDisabled() {
        val gso = GoogleSignInClients.loginOptions(requestIdToken = false)
        assertFalse(gso.isIdTokenRequested)
    }

    @Test
    fun loginOptions_idTokenFollowsWebClientConfig() {
        val with = GoogleSignInClients.loginOptions(requestIdToken = true)
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) {
            assertFalse(with.isIdTokenRequested)
        } else {
            assertTrue(with.isIdTokenRequested)
        }
    }

    @Test
    fun driveOptions_requestsAppDataScope() {
        val gso = GoogleSignInClients.driveOptions()
        assertTrue(
            gso.scopes.any { it.scopeUri == GoogleDriveBackupPrefs.DRIVE_APPDATA_SCOPE },
        )
    }

    @Test
    fun identityOptions_doesNotRequestDriveScope() {
        val gso = GoogleSignInClients.identityOptions()
        assertFalse(
            gso.scopes.any { it.scopeUri == GoogleDriveBackupPrefs.DRIVE_APPDATA_SCOPE },
        )
    }

    @Test
    fun driveOptions_setsAccountNameForConsent() {
        val gso = GoogleSignInClients.driveOptions("driver@example.com")
        assertEquals("driver@example.com", gso.account?.name)
    }

    @Test
    fun loginOptions_requestsAppDataScopeForBackupAtSignIn() {
        val gso = GoogleSignInClients.loginOptions(requestIdToken = false)
        assertTrue(
            gso.scopes.any { it.scopeUri == GoogleDriveBackupPrefs.DRIVE_APPDATA_SCOPE },
        )
        assertFalse(
            gso.scopes.any { it.scopeUri.contains("drive.file") && !it.scopeUri.contains("appdata") },
        )
    }
}

package com.truckerload.presentation.auth

import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.truckerload.utils.InstalledSigningSha1
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GoogleSignInSupportTest {

    @Test
    fun formatError_developerErrorIncludesInstalledSha1() {
        val context = RuntimeEnvironment.getApplication()
        val sha = InstalledSigningSha1.fingerprint(context)
        val message = GoogleSignInSupport.formatError(
            context,
            ApiException(Status(CommonStatusCodes.DEVELOPER_ERROR)),
        )
        assertTrue(message.contains("10") || message.contains("SHA"))
        if (sha != null) {
            assertTrue(message.contains(sha))
        }
    }

    @Test
    fun formatError_cancelIsNotWrappedAsGenericError() {
        val context = RuntimeEnvironment.getApplication()
        val message = GoogleSignInSupport.formatError(
            context,
            ApiException(Status(CommonStatusCodes.CANCELED)),
        )
        assertFalse(message.contains("OAuth"))
    }
}

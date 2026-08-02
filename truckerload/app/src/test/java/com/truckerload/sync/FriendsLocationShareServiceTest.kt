package com.truckerload.sync

import android.Manifest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FriendsLocationShareServiceTest {

    @Test
    fun hasLocationPermission_falseByDefault() {
        val context = RuntimeEnvironment.getApplication()
        assertFalse(FriendsLocationShareService.hasLocationPermission(context))
    }

    @Test
    fun hasLocationPermission_trueWithFineLocation() {
        val context = RuntimeEnvironment.getApplication()
        Shadows.shadowOf(context).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        assertTrue(FriendsLocationShareService.hasLocationPermission(context))
    }

    @Test
    fun hasLocationPermission_trueWithCoarseOnly() {
        val context = RuntimeEnvironment.getApplication()
        Shadows.shadowOf(context).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
        assertTrue(FriendsLocationShareService.hasLocationPermission(context))
    }

    @Test
    fun start_isNoOpWithoutPermission() {
        val context = RuntimeEnvironment.getApplication()
        // Must not throw when permission is missing (was a crash source via FGS).
        FriendsLocationShareService.start(context)
        assertFalse(FriendsLocationShareService.hasLocationPermission(context))
    }
}

package com.truckerload.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimePermissionSnapshotTest {

    @Test
    fun locationGranted_whenFineOrCoarse() {
        val none = RuntimePermissionSnapshot.fromFlags(
            camera = false,
            fineLocation = false,
            coarseLocation = false,
            sdkInt = 34,
            postNotifications = false,
        )
        assertFalse(none.locationGranted)

        val fine = RuntimePermissionSnapshot.fromFlags(
            camera = false,
            fineLocation = true,
            coarseLocation = false,
            sdkInt = 34,
            postNotifications = false,
        )
        assertTrue(fine.locationGranted)

        val coarse = RuntimePermissionSnapshot.fromFlags(
            camera = false,
            fineLocation = false,
            coarseLocation = true,
            sdkInt = 34,
            postNotifications = false,
        )
        assertTrue(coarse.locationGranted)
    }

    @Test
    fun notificationsAssumedGrantedBelowAndroid13() {
        val belowTiramisu = RuntimePermissionSnapshot.fromFlags(
            camera = false,
            fineLocation = false,
            coarseLocation = false,
            sdkInt = 32,
            postNotifications = false,
        )
        assertTrue(belowTiramisu.notificationsGranted)

        val tiramisuDenied = RuntimePermissionSnapshot.fromFlags(
            camera = false,
            fineLocation = false,
            coarseLocation = false,
            sdkInt = 33,
            postNotifications = false,
        )
        assertFalse(tiramisuDenied.notificationsGranted)

        val tiramisuGranted = RuntimePermissionSnapshot.fromFlags(
            camera = true,
            fineLocation = true,
            coarseLocation = true,
            sdkInt = 33,
            postNotifications = true,
        )
        assertTrue(tiramisuGranted.cameraGranted)
        assertTrue(tiramisuGranted.notificationsGranted)
    }

    @Test
    fun settingsSection_refreshesSnapshotWhenAppResumes() {
        val file = java.io.File(
            "src/main/java/com/truckerload/presentation/screens/settings/PrivacySettingsSection.kt",
        ).takeIf { it.isFile }
            ?: java.io.File(
                "app/src/main/java/com/truckerload/presentation/screens/settings/PrivacySettingsSection.kt",
            )
        val source = file.readText()
        assertTrue(source.contains("Lifecycle.Event.ON_RESUME"))
        assertTrue(source.contains("refreshPermissions"))
        assertTrue(source.contains("systemSettingsLauncher"))
    }
}

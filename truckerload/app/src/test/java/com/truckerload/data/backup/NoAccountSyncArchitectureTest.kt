package com.truckerload.data.backup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Google Drive sync and Google Sign-In were removed on purpose. Keep them out. */
class NoAccountSyncArchitectureTest {

    @Test
    fun mainSourcesHaveNoGoogleSignInOrDriveClient() {
        val offenders = mainJavaDir().walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { file ->
                val text = file.readText()
                FORBIDDEN.any { text.contains(it) }
            }
            .map { it.name }
            .toList()
        assertTrue("Account sync code is back: $offenders", offenders.isEmpty())
    }

    @Test
    fun gradleHasNoPlayServicesAuth() {
        val gradle = listOf(File("build.gradle.kts"), File("app/build.gradle.kts"))
            .first(File::isFile)
            .readText()
        assertFalse(gradle.contains("libs.play.services.auth"))
        assertFalse(gradle.contains("GOOGLE_DRIVE_API_KEY"))
    }

    private fun mainJavaDir(): File = listOf(File("src/main/java"), File("app/src/main/java")).first(File::isDirectory)

    private companion object {
        val FORBIDDEN = listOf(
            "com.google.android.gms.auth",
            "GoogleSignIn",
            "googleapis.com/drive",
            "drive.appdata",
            "requestEmail()",
        )
    }
}

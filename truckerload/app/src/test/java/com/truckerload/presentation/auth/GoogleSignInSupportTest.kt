package com.truckerload.presentation.auth

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleSignInSupportTest {

    @Test
    fun buildSignInOptions_delegatesToSharedLoginOptions() {
        val source = readMainSource("com/truckerload/presentation/auth/GoogleSignInSupport.kt")
        assertTrue(source.contains("fun buildSignInOptions"))
        assertTrue(source.contains("GoogleSignInClients.loginOptions"))
    }

    @Test
    fun formatError_usesInstalledSha1ForDeveloperError() {
        val source = readMainSource("com/truckerload/presentation/auth/GoogleSignInSupport.kt")
        assertTrue(source.contains("InstalledSigningSha1.fingerprint"))
        assertTrue(source.contains("login_google_developer_error"))
        assertTrue(source.contains("SIGN_OUT_TIMEOUT_MS"))
    }

    @Test
    fun developerErrorString_isUserFacing() {
        val values = File("src/main/res/values/strings.xml").takeIf { it.isFile }
            ?: File("app/src/main/res/values/strings.xml")
        val xml = values.readText()
        val line = xml.lineSequence().first { it.contains("name=\"login_google_developer_error\"") }
        assertTrue(line.contains("Please try again"))
        assertTrue(!line.contains("Cloud Console"))
        assertTrue(!line.contains("SHA-1"))
    }

    private fun readMainSource(relativePath: String): String {
        val candidates = listOf(
            File("src/main/java/$relativePath"),
            File("app/src/main/java/$relativePath"),
            File("../app/src/main/java/$relativePath"),
        )
        return candidates.firstOrNull(File::isFile)?.readText()
            ?: error("Main source not found: $relativePath")
    }
}

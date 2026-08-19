package com.truckerload.data.remote

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleAuthArchitectureTest {

    @Test
    fun driveSignInIntent_usesActivityNotApplicationContext() {
        val service = readMainSource("com/truckerload/data/backup/GoogleDriveBackupService.kt")
        assertTrue(service.contains("fun signInIntent(activity: Activity)"))
        assertFalse(service.contains("applicationContext, gso"))
        assertFalse(service.contains("getClient(context.applicationContext"))

        val clients = readMainSource("com/truckerload/data/remote/GoogleSignInClients.kt")
        assertTrue(clients.contains("fun driveSignInIntent(activity: Activity)"))
        assertTrue(clients.contains("GoogleSignIn.getClient(activity"))
    }

    @Test
    fun logout_signsOutGoogleAccount() {
        val source = readMainSource("com/truckerload/sync/SessionTeardown.kt")
        assertTrue(source.contains("GoogleSignInClients.signOutDevice"))
    }

    @Test
    fun credentialManager_requiresActivity() {
        val source = readMainSource("com/truckerload/data/remote/CredentialManagerGoogleSignIn.kt")
        assertTrue(source.contains("resolveActivity(context)"))
        assertTrue(source.contains("getCredentialAsync(activity"))
    }

    @Test
    fun settingsDriveSection_unwrapsActivity() {
        val source = readMainSource(
            "com/truckerload/presentation/screens/settings/GoogleDriveSyncSection.kt",
        )
        assertTrue(source.contains("findActivity()"))
        assertFalse(source.contains("context as? Activity"))
        assertTrue(source.contains("linkedAccountEmail"))
        assertTrue(source.contains("startDriveSync"))
        assertTrue(source.contains("drive_sync_now"))
    }

    @Test
    fun driveSignInIntent_reconsentsLastAccountForAppDataScope() {
        val clients = readMainSource("com/truckerload/data/remote/GoogleSignInClients.kt")
        assertTrue(clients.contains("setAccountName"))
        assertTrue(clients.contains("getLastSignedInAccount"))
    }

    @Test
    fun legacyLogin_retriesDeveloperErrorWithoutIdToken() {
        val source = readMainSource(
            "com/truckerload/presentation/screens/login/LegacyGoogleSignInBridge.kt",
        )
        assertTrue(source.contains("launchLegacyGoogleSignIn"))
        assertTrue(source.contains("shouldRetryWithoutIdToken"))
    }

    @Test
    fun login_skipsCredentialManagerHang() {
        val repo = readMainSource("com/truckerload/data/repository/auth/AuthRepositoryImpl.kt")
        val start = repo.indexOf("fun requestGoogleIdToken")
        val end = repo.indexOf("override suspend fun signInWithGoogle")
        assertTrue(start >= 0 && end > start)
        val method = repo.substring(start, end)
        assertFalse(method.contains("getGoogleIdToken"))
        assertTrue(method.contains("FallBackToLegacy"))

        val launcher = readMainSource("com/truckerload/presentation/auth/GoogleSignInLauncher.kt")
        assertTrue(launcher.contains("launchLegacy()"))
        assertFalse(launcher.contains("CredentialManagerGoogleSignIn.getGoogleIdToken"))
    }

    @Test
    fun googleCloudLogin_persistsIdTokenForVoiceBearer() {
        val launcher = readMainSource("com/truckerload/presentation/auth/GoogleSignInLauncher.kt")
        assertTrue(launcher.contains("googleIdToken = idToken"))
        assertTrue(launcher.contains("googleIdToken = googleIdToken"))

        val repo = readMainSource("com/truckerload/data/repository/auth/AuthRepositoryImpl.kt")
        assertTrue(repo.contains("googleIdToken = idToken"))
        assertTrue(repo.contains("googleIdToken = credential.idToken"))

        val interceptor = readMainSource("com/truckerload/data/remote/ktor/KtorAuthInterceptor.kt")
        assertTrue(interceptor.contains("v1/voice/token"))
        assertTrue(interceptor.contains("googleIdTokenOrNull"))
        assertTrue(interceptor.contains("KtorBearerToken.select"))
        assertTrue(interceptor.contains("request.headers.remove(HttpHeaders.Authorization)"))
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

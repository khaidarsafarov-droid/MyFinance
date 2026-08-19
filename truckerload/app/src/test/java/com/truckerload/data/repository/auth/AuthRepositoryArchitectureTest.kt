package com.truckerload.data.repository.auth

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositoryArchitectureTest {

    @Test
    fun module_bindsAuthRepository() {
        val source = readMainSource("com/truckerload/di/AuthRepositoryModule.kt")
        assertTrue(source.contains("@InstallIn(SingletonComponent::class)"))
        assertTrue(source.contains("AuthRepositoryImpl"))
        assertTrue(source.contains("AuthRepository"))
    }

    @Test
    fun loginScreen_delegatesToAuthViewModel() {
        val source = readMainSource("com/truckerload/presentation/screens/login/LoginScreen.kt")
        assertTrue(source.contains("AuthViewModel"))
        assertTrue(source.contains("hiltViewModel"))
        assertTrue(!source.contains("SupabaseAuthService"))
        assertTrue(!source.contains("saveProfileAndLogin"))
    }

    @Test
    fun cloudLogin_registersDeviceSlotBeforeKeepingSession() {
        val auth = readMainSource("com/truckerload/data/repository/auth/AuthRepositoryImpl.kt")
        assertTrue(auth.contains("DeviceSlotLogin.afterSessionPersisted"))
        val engine = readMainSource("com/truckerload/data/sync/CloudSyncEngine.kt")
        assertTrue(engine.contains("DEVICE_SLOT_DENIED"))
        assertTrue(engine.contains("registerCurrentDevice"))
    }

    @Test
    fun googleSignIn_passesIdTokenThroughCompleteLogin() {
        val auth = readMainSource("com/truckerload/data/repository/auth/AuthRepositoryImpl.kt")
        assertTrue(auth.contains("googleIdToken = idToken"))
        assertTrue(auth.contains("googleIdToken = googleIdToken"))
        val login = readMainSource("com/truckerload/data/preferences/AuthLogin.kt")
        assertTrue(login.contains("googleIdToken: String?"))
        assertTrue(login.contains("googleIdToken = googleIdToken"))
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

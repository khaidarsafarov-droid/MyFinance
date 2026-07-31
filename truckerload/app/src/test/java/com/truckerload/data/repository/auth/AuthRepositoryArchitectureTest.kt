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

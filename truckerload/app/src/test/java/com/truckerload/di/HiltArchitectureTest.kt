package com.truckerload.di

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HiltArchitectureTest {

    @Test
    fun singletonModule_bindsOnlyProcessSafeStores() {
        val source = readMainSource("com/truckerload/di/ApplicationStoreModule.kt")

        assertTrue(source.contains("@InstallIn(SingletonComponent::class)"))
        listOf(
            "AuthStore",
            "AuthCredentialsStore",
            "UserProfileStore",
            "SettingsDataStore",
            "PushTokenStore",
        ).forEach { type ->
            assertTrue("$type must remain an application-scoped binding", source.contains("): $type ="))
        }
        assertFalse("Room databases must remain account-scoped", source.contains("AppDatabase"))
        assertFalse(
            "Repositories must remain account-scoped",
            source.contains("com.truckerload.data.repository"),
        )
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

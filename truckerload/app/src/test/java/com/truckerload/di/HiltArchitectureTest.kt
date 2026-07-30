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

    @Test
    fun userAccountModule_bridgesSessionWithoutSingletonScope() {
        val source = readMainSource("com/truckerload/di/UserAccountModule.kt")

        assertTrue(source.contains("@InstallIn(SingletonComponent::class)"))
        assertTrue(source.contains("UserComponentManager"))
        assertTrue(source.contains("manager.require()"))
        assertFalse(
            "Account repos must not be cached as @Singleton",
            source.contains("@Singleton"),
        )
        assertTrue(source.contains("fun provideLoadRepository"))
        assertTrue(source.contains("fun provideSocialRepository"))
        assertTrue(source.contains("fun provideAppDatabase"))
    }

    @Test
    fun userComponentManager_ownsSessionLifecycle() {
        val source = readMainSource("com/truckerload/di/UserComponentManager.kt")

        assertTrue(source.contains("fun startSession"))
        assertTrue(source.contains("fun endSession"))
        assertTrue(source.contains("AppDatabase.closeCurrent()"))
        assertTrue(source.contains("userProfileStore.unbind()"))
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

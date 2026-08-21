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
            "CallPrivacyStore",
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
        // Strip KDoc/comments so mentions of @Singleton in docs do not fail the guard.
        val codeOnly = source
            .replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("//.*"), "")
        assertFalse(
            "Account repos must not be cached as @Singleton",
            codeOnly.contains("@Singleton"),
        )
        assertTrue(source.contains("fun provideLoadRepository"))
        assertTrue(source.contains("fun provideProfileRepository"))
        assertTrue(source.contains("fun provideCrowdRpmRepository"))
        assertTrue(source.contains("fun provideAppDatabase"))
        assertFalse(source.contains("fun provideChatRepository"))
        assertFalse(source.contains("fun provideSocialSyncCoordinator"))
        assertFalse(source.contains("fun provideVoiceRepository"))
        assertFalse(source.contains("fun provideSocial" + "Repository"))
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

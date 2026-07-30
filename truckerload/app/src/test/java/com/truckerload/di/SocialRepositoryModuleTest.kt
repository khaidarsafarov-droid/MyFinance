package com.truckerload.di

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialRepositoryModuleTest {

    @Test
    fun module_wiresProfileRepository() {
        val source = readMainSource("com/truckerload/di/SocialRepositoryModule.kt")
        assertTrue(source.contains("ProfileRepositoryImpl"))
        assertTrue(source.contains("val profile: ProfileRepository"))
        assertTrue(source.contains("facade = SocialRepository"))
    }

    @Test
    fun facade_isDeprecatedAndDelegates() {
        val source = readMainSource("com/truckerload/data/repository/SocialRepository.kt")
        assertTrue(source.contains("@Deprecated"))
        assertTrue(source.contains("profile.uploadAvatar"))
        assertTrue(source.contains("chat.watchChats"))
        assertTrue(source.contains("syncCoordinator.ensureInitialized"))
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

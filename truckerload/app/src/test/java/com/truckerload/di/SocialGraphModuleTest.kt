package com.truckerload.di

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialGraphModuleTest {

    @Test
    fun module_wiresDomainRepositoriesWithoutFacade() {
        val source = readMainSource("com/truckerload/di/SocialGraphModule.kt")
        assertTrue(source.contains("ProfileRepositoryImpl"))
        assertTrue(source.contains("val profile: ProfileRepository"))
        assertTrue(source.contains("val crowdRpm: CrowdRpmRepository"))
        assertFalse(source.contains("val chat: ChatRepository"))
        assertFalse(source.contains("val syncCoordinator: SocialSyncCoordinator"))
        assertFalse(source.contains("facade"))
        assertFalse(source.contains("data.repository.Social"))
    }

    @Test
    fun deprecatedFacadeFile_isRemoved() {
        val relative = "com/truckerload/data/repository/Social" + "Repository.kt"
        val candidates = listOf(
            File("src/main/java/$relative"),
            File("app/src/main/java/$relative"),
            File("../app/src/main/java/$relative"),
        )
        assertFalse(
            "Deprecated social facade must be deleted",
            candidates.any(File::isFile),
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

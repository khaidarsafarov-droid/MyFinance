package com.truckerload.presentation.theme

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** Launcher + in-app mark must use kit purple, not the old navy. */
class AppIconBrandColorTest {

    @Test
    fun launcherBackground_isForestPrimary() {
        val xml = readRes("drawable/ic_launcher_background.xml")
        assertTrue(xml.contains("#FF5B54E6"))
        assertTrue(!xml.contains("#FF143882"))
        assertTrue(!xml.contains("#FF2F4F3E"))
    }

    @Test
    fun launcherForegroundFallback_isForestPrimary() {
        val xml = readRes("drawable/ic_launcher_foreground.xml")
        assertTrue(xml.contains("#FF5B54E6"))
        assertTrue(!xml.contains("#FF143882"))
        assertTrue(!xml.contains("#FF2F4F3E"))
    }

    @Test
    fun inAppLogoPlate_usesForestPrimary() {
        val src = readSource("presentation/components/AppBrandLogo.kt")
        assertTrue(src.contains("SoftUiColors.ForestPrimary"))
        assertTrue(!src.contains("0xFF143882"))
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            File("src/main/java/com/truckerload/$relativePath"),
            File("app/src/main/java/com/truckerload/$relativePath"),
            File("../app/src/main/java/com/truckerload/$relativePath"),
        )
        return candidates.firstOrNull(File::isFile)?.readText()
            ?: error("Source not found: $relativePath")
    }

    private fun readRes(relativePath: String): String {
        val candidates = listOf(
            File("src/main/res/$relativePath"),
            File("app/src/main/res/$relativePath"),
            File("../app/src/main/res/$relativePath"),
        )
        return candidates.firstOrNull(File::isFile)?.readText()
            ?: error("Resource not found: $relativePath")
    }
}

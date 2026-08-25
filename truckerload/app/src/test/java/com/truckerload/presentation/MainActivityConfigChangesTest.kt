package com.truckerload.presentation

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * QUALITY_100 #82 — MainActivity declares configChanges so filter/UI state survives rotation.
 */
class MainActivityConfigChangesTest {

    @Test
    fun mainActivity_declaresOrientationScreenSizeConfigChanges() {
        val candidates = listOf(
            File("src/main/AndroidManifest.xml"),
            File("app/src/main/AndroidManifest.xml"),
            File("../app/src/main/AndroidManifest.xml"),
            File("/workspace/truckerload/app/src/main/AndroidManifest.xml"),
        )
        val text = candidates.firstOrNull { it.exists() }?.readText()
            ?: error("AndroidManifest.xml not found")
        assertTrue(text.contains("android:name=\".presentation.MainActivity\""))
        val mainBlock = text.substringAfter("android:name=\".presentation.MainActivity\"")
            .substringBefore("<activity")
        assertTrue(mainBlock.contains("orientation"))
        assertTrue(mainBlock.contains("screenSize"))
        assertTrue(mainBlock.contains("screenLayout"))
        assertTrue(mainBlock.contains("smallestScreenSize"))
        assertTrue(mainBlock.contains("android:resizeableActivity=\"true\""))
    }
}

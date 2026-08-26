package com.truckerload.shared

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class IosClientWiringTest {
    private val iosRoot: File = findIosRoot()

    @Test
    fun `Xcode project embeds the umbrella KMP framework`() {
        val pbx = File(iosRoot, "TruckoRig.xcodeproj/project.pbxproj").readText()
        assertTrue("TruckerLoadShared" in pbx, "pbxproj must link TruckerLoadShared")
        assertTrue(
            "embed-shared-framework.sh" in pbx,
            "pbxproj must run the Gradle embed script before compiling Swift",
        )
        assertTrue(
            "ENABLE_USER_SCRIPT_SANDBOXING = NO" in pbx,
            "Xcode must allow the Gradle script to write the framework",
        )
    }

    @Test
    fun `Swift UI imports SharedBusinessLogic from TruckerLoadShared`() {
        val view = File(iosRoot, "TruckoRig/WeeklyGoalView.swift").readText()
        assertTrue("import TruckerLoadShared" in view)
        assertTrue("SharedBusinessLogic" in view)
    }

    @Test
    fun `embed script targets the umbrella shared module`() {
        val script = File(iosRoot, "embed-shared-framework.sh").readText()
        assertTrue(":shared:embedAndSignAppleFrameworkForXcode" in script)
        assertTrue("truckerload.enableIos=true" in script)
    }

    private fun findIosRoot(): File {
        val start = File(".").canonicalFile
        generateSequence(start) { it.parentFile }.forEach { dir ->
            val nested = File(dir, "ios")
            if (File(nested, "TruckoRig.xcodeproj/project.pbxproj").isFile) return nested
            val underGradle = File(dir, "truckerload/ios")
            if (File(underGradle, "TruckoRig.xcodeproj/project.pbxproj").isFile) return underGradle
        }
        error("ios/ client not found walking up from $start")
    }
}

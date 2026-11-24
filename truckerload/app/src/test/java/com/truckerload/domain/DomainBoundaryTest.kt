package com.truckerload.domain

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Domain use cases talk to [com.truckerload.domain.store] ports.
 * They do not import the data layer, Room, or [com.truckerload.data.local.AppDatabase].
 */
class DomainBoundaryTest {

    @Test
    fun domainSourcesDoNotImportDataRoomOrAppDatabase() {
        val root = domainRoot()
        val violations = root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file -> violationsIn(file) }
            .toList()
        assertTrue(violations.joinToString("\n"), violations.isEmpty())
    }

    private fun violationsIn(file: File): List<String> {
        val code = file.readText()
            .replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
            .lineSequence()
            .map { it.substringBefore("//") }
            .joinToString("\n")
        val forbidden = listOf(
            "com.truckerload.data",
            "androidx.room",
            "AppDatabase",
        )
        return forbidden.mapNotNull { token ->
            if (code.contains(token)) "${file.path} mentions $token" else null
        }
    }

    private fun domainRoot(): File {
        val candidates = listOf(
            File("src/main/java/com/truckerload/domain"),
            File("app/src/main/java/com/truckerload/domain"),
            File("../app/src/main/java/com/truckerload/domain"),
        )
        return candidates.firstOrNull { it.isDirectory }
            ?: error("domain sources not found from ${File(".").absolutePath}")
    }
}

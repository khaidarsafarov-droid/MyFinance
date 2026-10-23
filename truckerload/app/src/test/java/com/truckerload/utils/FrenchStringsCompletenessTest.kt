package com.truckerload.utils

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every default string and plural must exist in French with the same format placeholders.
 */
class FrenchStringsCompletenessTest {

    @Test
    fun everyDefaultResource_hasFrenchTranslationWithMatchingPlaceholders() {
        val base = read("values/strings.xml")
        val french = read("values-fr/strings.xml")
        val baseNames = resourceNames(base)
        val frenchNames = resourceNames(french)
        assertEquals(emptySet<String>(), baseNames - frenchNames)
        assertEquals(emptySet<String>(), frenchNames - baseNames)

        val baseBodies = resourceBodies(base)
        val frenchBodies = resourceBodies(french)
        val mismatches = baseNames.mapNotNull { name ->
            val expected = placeholders(baseBodies.getValue(name))
            val actual = placeholders(frenchBodies.getValue(name))
            if (expected == actual) null else "$name expected $expected actual $actual"
        }
        assertEquals(emptyList<String>(), mismatches)

        assertTrue(french.contains("quantity=\"one\""))
        assertTrue(french.contains("quantity=\"other\""))
        assertTrue(!french.contains("quantity=\"few\""))
        assertTrue(!french.contains("quantity=\"many\""))
    }

    private fun read(relative: String): String = listOf(
        File("src/main/res/$relative"),
        File("app/src/main/res/$relative"),
        File("/workspace/truckerload/app/src/main/res/$relative"),
    ).first { it.isFile }.readText()

    private fun resourceNames(xml: String): Set<String> =
        NAME.findAll(xml).map { it.groupValues[1] }.toSet()

    private fun resourceBodies(xml: String): Map<String, String> {
        val bodies = linkedMapOf<String, String>()
        STRING.findAll(xml).forEach { bodies[it.groupValues[1]] = it.groupValues[2] }
        PLURAL.findAll(xml).forEach { bodies[it.groupValues[1]] = it.groupValues[2] }
        return bodies
    }

    private fun placeholders(body: String): List<String> =
        PLACEHOLDER.findAll(body.replace("%%", "")).map { it.value }.toList()

    private companion object {
        val NAME = Regex("""<(?:string|plurals|string-array) name="([^"]+)"""")
        val STRING = Regex("""<string name="([^"]+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
        val PLURAL = Regex("""<plurals name="([^"]+)"[^>]*>(.*?)</plurals>""", RegexOption.DOT_MATCHES_ALL)
        val PLACEHOLDER = Regex(
            """%(?:\d+\$)?[-#+ 0,(]*\d*(?:\.\d+)?(?:[tT][A-Za-z]|[bBhHsScCdoxXeEfgGaA%])""",
        )
    }
}

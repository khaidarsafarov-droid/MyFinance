package com.truckerload.domain.parser

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Runs every sample in `resources/ratecon-corpus` through the same extraction the add-load form
 * and the Telegram bot use, and compares rate, miles, trip/load id and pickup/delivery cities.
 * The pass rate is printed and written to `build/ratecon-corpus-report.txt`.
 */
class RateConCorpusTest {

    private val realFloor = 0.6
    private val syntheticFloor = 0.45

    private data class Case(val id: String, val real: Boolean, val failures: List<String>)

    @Test
    fun corpus() {
        val real = CorpusCheck.realIds()
        val cases = CorpusCheck.ids().map { id ->
            val draft = MessageParseService().extractLoadFields(CorpusCheck.sample(id), CorpusCheck.REFERENCE_MILLIS)
            Case(id, id in real, CorpusCheck.failures(id, draft))
        }
        val report = buildString {
            cases.filter { it.failures.isNotEmpty() }.forEach { appendLine("${it.id} real=${it.real}: ${it.failures}") }
        }
        val realCases = cases.filter { it.real }
        val synth = cases.filterNot { it.real }
        println(
            "CORPUS real ${realCases.count { it.failures.isEmpty() }}/${realCases.size} " +
                "synthetic ${synth.count { it.failures.isEmpty() }}/${synth.size}\n$report",
        )
        File("build/ratecon-corpus-report.txt").apply { parentFile.mkdirs() }.writeText(report)
        // Regression floor: the pass rate must not drop; lower-quality OCR/synthetic quirks are tracked, not gated.
        val realRate = realCases.count { it.failures.isEmpty() }.toDouble() / realCases.size
        val synthRate = synth.count { it.failures.isEmpty() }.toDouble() / synth.size
        assertTrue("real pass rate $realRate:\n$report", realRate >= realFloor)
        assertTrue("synthetic pass rate $synthRate", synthRate >= syntheticFloor)
    }
}

package com.truckerload.domain.parser

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards "add on top, never break": `parser-golden/old-outputs.json` holds what the parser on main
 * (before the new schemes) returned for every corpus sample. Wherever that old answer matched the
 * expected values, the new chain must return exactly the same fields.
 */
class ParserRegressionTest {

    private val golden = JSONObject(
        javaClass.classLoader!!.getResource("parser-golden/old-outputs.json")!!.readText(),
    )

    /** Old values that were junk (a "DATE" id, a rate of 3, a generated id) are allowed to disappear. */
    private fun old(id: String): LoadDraftFields = rawOld(id).dropImplausible()

    private fun rawOld(id: String): LoadDraftFields {
        val o = golden.getJSONObject(id)
        return LoadDraftFields(
            tripId = o.getString("tripId"),
            date = o.getString("date"),
            rate = o.getString("rate"),
            miles = o.getString("miles"),
            pointA = o.getString("pointA"),
            pointB = o.getString("pointB"),
        )
    }

    /** The parsers default a missing date to "today" (the reference day); that is not an extracted value. */
    private val defaultDate = java.time.Instant.ofEpochMilli(CorpusCheck.REFERENCE_MILLIS)
        .atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString()

    private fun changedFields(old: LoadDraftFields, now: LoadDraftFields): List<String> = listOfNotNull(
        "tripId ${old.tripId}->${now.tripId}".takeIf { old.tripId.isNotBlank() && old.tripId != now.tripId },
        "date ${old.date}->${now.date}".takeIf { old.date.isNotBlank() && old.date != defaultDate && old.date != now.date },
        "rate ${old.rate}->${now.rate}".takeIf { old.rate.isNotBlank() && old.rate != now.rate },
        "miles ${old.miles}->${now.miles}".takeIf { old.miles.isNotBlank() && old.miles != now.miles },
        "pointA ${old.pointA}->${now.pointA}".takeIf { old.pointA.isNotBlank() && old.pointA != now.pointA },
        "pointB ${old.pointB}->${now.pointB}".takeIf { old.pointB.isNotBlank() && old.pointB != now.pointB },
    )

    /** Every value the old parser produced correctly stays byte-for-byte; the new schemes may only add blanks. */
    @Test
    fun correctOldResultsAreUnchanged() {
        val correct = CorpusCheck.ids().filter { CorpusCheck.failures(it, old(it)).isEmpty() && !old(it).isEmpty() }
        assertTrue("expected a meaningful set of previously-correct samples, got ${correct.size}", correct.size >= 15)
        val changed = correct.associateWith {
            val now = MessageParseService().extractLoadFields(CorpusCheck.sample(it), CorpusCheck.REFERENCE_MILLIS)
            changedFields(old(it), now)
        }.filterValues { it.isNotEmpty() }
        assertEquals("previously correct samples whose result changed: $changed", emptyMap<String, List<String>>(), changed)
    }

    @Test
    fun noPreviouslyCorrectSampleRegressesToWrong() {
        val broken = CorpusCheck.ids().filter { id ->
            CorpusCheck.failures(id, old(id)).isEmpty() && !old(id).isEmpty() &&
                CorpusCheck.failures(
                    id,
                    MessageParseService().extractLoadFields(CorpusCheck.sample(id), CorpusCheck.REFERENCE_MILLIS),
                ).isNotEmpty()
        }
        assertEquals(emptyList<String>(), broken)
    }
}

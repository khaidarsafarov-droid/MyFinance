package com.truckerload.domain.parser

import org.json.JSONObject
import java.io.File
import java.util.Locale
import kotlin.math.abs

/** Shared helpers for the corpus tests: loading samples/expected values and comparing a draft to them. */
internal object CorpusCheck {
    const val REFERENCE_MILLIS = 1_795_000_000_000L

    val root: File =
        File(CorpusCheck::class.java.classLoader!!.getResource("ratecon-corpus/index.csv")!!.toURI()).parentFile

    fun ids(): List<String> = File(root, "expected").listFiles()!!.map { it.nameWithoutExtension }.sorted()

    fun realIds(): Set<String> = File(root, "index.csv").readLines().drop(1)
        .filter { it.contains(",real,") }
        .map { it.substringBefore(',') }
        .toSet()

    fun sample(id: String): String =
        File(root, "samples").listFiles()!!.first { it.name.startsWith("${id}_") }.readText()

    fun expected(id: String) = JSONObject(File(root, "expected/$id.json").readText())

    private fun norm(s: String?): String = s.orEmpty().lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "")

    fun failures(id: String, draft: LoadDraftFields): List<String> {
        val exp = expected(id)
        val fails = mutableListOf<String>()
        val total = exp.optJSONObject("rate")?.takeIf { !it.isNull("total") }?.getDouble("total")
        if (total != null) {
            val got = draft.parsedRate()
            if (got == null || abs(got - total) > 0.01) fails += "rate exp=$total got=$got"
        }
        if (!exp.isNull("miles")) {
            val m = exp.getDouble("miles")
            val got = draft.parsedMiles()
            if (m > 0 && abs(got - m) > m * 0.01) fails += "miles exp=$m got=$got"
        }
        val ids = listOfNotNull(
            exp.optString("tripId").takeIf { it.isNotBlank() && it != "null" },
            exp.optString("loadId").takeIf { it.isNotBlank() && it != "null" },
        )
        if (ids.isNotEmpty() && ids.none { norm(draft.tripId).contains(norm(it)) }) {
            fails += "id exp=$ids got=${draft.tripId}"
        }
        cityOf(exp, "pickup")?.let { c ->
            if (!norm(draft.pointA).contains(norm(c))) fails += "pickup exp=$c got=${draft.pointA}"
        }
        cityOf(exp, "delivery")?.let { c ->
            if (!norm(draft.pointB).contains(norm(c))) fails += "delivery exp=$c got=${draft.pointB}"
        }
        return fails
    }

    private fun cityOf(exp: JSONObject, key: String): String? {
        val obj = exp.optJSONObject(key)
        return obj?.takeIf { !it.isNull("city") }?.getString("city")
    }
}

package com.truckerload.domain.ingest

/**
 * Scores OCR / file text against real TruckoRig journals: loads, paycheck, diesel, DEF.
 * Longer / more specific markers beat generic words like «total».
 */
object ReceiptKindClassifier {

    private val loadMarkers = listOf(
        // [\s\-_]* also covers JSON / CSV keys such as trip_id and pu_address.
        6 to Regex("""Trip[\s\-_]*ID|T-[A-Z0-9]{6,}""", RegexOption.IGNORE_CASE),
        5 to Regex(
            """Total[\s\-_]*Rate|Line[\s\-_]*Haul|Pu[\s\-_]*address|Del[\s\-_]*address|""" +
                """Estimated[\s\-_]*Rate""",
            RegexOption.IGNORE_CASE,
        ),
        4 to Regex(
            """Total[\s\-_]*Loaded[\s\-_]*Miles|Amazon[\s\-_]*Relay|rate[\s\-_]*confirmation|""" +
                """load[\s\-_]*confirmation|load[\s\-_]*information|IEL[\s\-_]*PO""",
            RegexOption.IGNORE_CASE,
        ),
        3 to Regex("""\bPU#|\bP/U\s*#|\bDEL#|pickup|delivery|pick[\s\-_]*ups""", RegexOption.IGNORE_CASE),
    )

    private val paycheckMarkers = listOf(
        7 to Regex("""Driver\s*Settlement|Settlement\s*Summary|Payee\s*ID""", RegexOption.IGNORE_CASE),
        6 to Regex("""Net\s*Pay|Gross\s*Pay|Driver\s*Pay|зарплат""", RegexOption.IGNORE_CASE),
        5 to Regex("""paycheck|pay\s*stub|settlement|оклад""", RegexOption.IGNORE_CASE),
        4 to Regex("""Grand\s*Total|Settlement\s*Total|Cutoff\s*Date|Week\s*Start""", RegexOption.IGNORE_CASE),
        2 to Regex("""YTD|year[\s-]*to[\s-]*date""", RegexOption.IGNORE_CASE),
    )

    private val dieselMarkers = listOf(
        6 to Regex("""\bdiesel\b|\bдизел|\bДТ\b|diesel\s*fuel""", RegexOption.IGNORE_CASE),
        5 to Regex("""fuel\s*receipt|fuel\s*sale|reefer\s*fuel|топлив""", RegexOption.IGNORE_CASE),
        4 to Regex("""gallons?|\bgal\b|price\s*per\s*gal|PPG|л\.?\s*ДТ""", RegexOption.IGNORE_CASE),
        2 to Regex("""Pilot|Love'?s|Flying\s*J|TA\s*Petro|Fuel""", RegexOption.IGNORE_CASE),
    )

    private val defMarkers = listOf(
        8 to Regex("""diesel\s*exhaust\s*fluid|мочевин""", RegexOption.IGNORE_CASE),
        7 to Regex("""\bad[\s\-]?blue\b|\bbluedef\b""", RegexOption.IGNORE_CASE),
        6 to Regex("""\bDEF\b|\bAdBlue\b""", RegexOption.IGNORE_CASE),
        4 to Regex("""DEF\s*fill|DEF\s*gal""", RegexOption.IGNORE_CASE),
    )

    fun classify(text: String, fileName: String? = null): ReceiptKind {
        val scores = scores(text, fileName)
        val best = scores.maxByOrNull { it.value } ?: return ReceiptKind.UNKNOWN
        if (best.value <= 0) return ReceiptKind.UNKNOWN
        val load = scores[ReceiptKind.LOAD] ?: 0
        val def = scores[ReceiptKind.DEF] ?: 0
        val diesel = scores[ReceiptKind.DIESEL] ?: 0
        val pay = scores[ReceiptKind.PAYCHECK] ?: 0
        if (load >= 6 && load >= pay && load >= diesel && load >= def) return ReceiptKind.LOAD
        if (def >= 6 && def >= diesel) return ReceiptKind.DEF
        if (pay >= diesel && pay >= def && pay >= 4) return ReceiptKind.PAYCHECK
        if (diesel >= 4) return ReceiptKind.DIESEL
        return best.key
    }

    fun scores(text: String, fileName: String? = null): Map<ReceiptKind, Int> {
        val src = combined(text, fileName).trim()
        if (src.isBlank()) {
            return ReceiptKind.entries.associateWith { 0 }
        }
        return mapOf(
            ReceiptKind.LOAD to score(src, loadMarkers),
            ReceiptKind.PAYCHECK to score(src, paycheckMarkers),
            ReceiptKind.DIESEL to score(src, dieselMarkers),
            ReceiptKind.DEF to score(src, defMarkers),
            ReceiptKind.UNKNOWN to 0,
        )
    }

    private fun score(text: String, markers: List<Pair<Int, Regex>>): Int =
        markers.sumOf { (weight, regex) -> if (regex.containsMatchIn(text)) weight else 0 }

    private fun combined(text: String, fileName: String?): String =
        listOfNotNull(fileName?.replace('-', ' '), text).joinToString("\n")
}

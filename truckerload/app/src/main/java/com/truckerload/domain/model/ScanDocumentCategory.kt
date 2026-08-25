package com.truckerload.domain.model

/** Driver-facing folders for scanned papers. Stored as the enum name on the scans table. */
enum class ScanDocumentCategory {
    LOAD,
    PAYCHECK,
    DIESEL,
    TRUCK,
    OTHER,
    ;

    companion object {
        fun fromStored(value: String?): ScanDocumentCategory =
            entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: OTHER
    }
}

object ScanDocumentFinder {

    fun infer(loadId: String?, fileName: String, ocrText: String): ScanDocumentCategory {
        if (!loadId.isNullOrBlank()) return ScanDocumentCategory.LOAD
        val hay = "$fileName\n$ocrText".lowercase()
        val scores = ScanDocumentCategory.entries
            .filter { it != ScanDocumentCategory.OTHER }
            .associateWith { category -> keywords[category].orEmpty().count { hay.contains(it) } }
        val best = scores.maxByOrNull { it.value } ?: return ScanDocumentCategory.OTHER
        if (best.value <= 0) return ScanDocumentCategory.OTHER
        val tied = scores.filterValues { it == best.value }.keys
        return preferOrder.first { it in tied }
    }

    fun matches(
        storedCategory: String,
        fileName: String,
        ocrText: String,
        tripId: String,
        routeLabel: String,
        dateLabel: String,
        filter: ScanDocumentCategory?,
        query: String,
    ): Boolean {
        val category = ScanDocumentCategory.fromStored(storedCategory)
        if (filter != null && category != filter) return false
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) return true
        val hay = buildString {
            append(fileName)
            append('\n')
            append(ocrText)
            append('\n')
            append(tripId)
            append('\n')
            append(routeLabel)
            append('\n')
            append(dateLabel)
            append('\n')
            append(category.name)
            append('\n')
            searchAliases[category]?.forEach { alias ->
                append(alias)
                append('\n')
            }
        }.lowercase()
        return hay.contains(needle)
    }

    private val preferOrder = listOf(
        ScanDocumentCategory.LOAD,
        ScanDocumentCategory.PAYCHECK,
        ScanDocumentCategory.DIESEL,
        ScanDocumentCategory.TRUCK,
    )

    private val keywords: Map<ScanDocumentCategory, List<String>> = mapOf(
        ScanDocumentCategory.LOAD to listOf(
            "bill of lading",
            "rate confirmation",
            "rate con",
            "ratecon",
            "trip id",
            "load confirmation",
            "bol",
            "накладн",
            "груз",
            "relay",
            "consignee",
            "shipper",
            "pickup",
        ),
        ScanDocumentCategory.PAYCHECK to listOf(
            "paycheck",
            "pay check",
            "settlement",
            "payroll",
            "statement of earnings",
            "зарплат",
            "выплат",
            "расчётн",
            "расчетн",
        ),
        ScanDocumentCategory.DIESEL to listOf(
            "diesel",
            "fuel receipt",
            "fuel stop",
            "gallons",
            "gallon",
            "дизел",
            "топлив",
            "галлон",
        ),
        ScanDocumentCategory.TRUCK to listOf(
            "insurance",
            "registration",
            "inspection",
            "ifta",
            "irp",
            "cdl",
            "title",
            "страхов",
            "регистрац",
            "техосмотр",
            "допуск",
            "медкниж",
        ),
    )

    private val searchAliases: Map<ScanDocumentCategory, List<String>> = mapOf(
        ScanDocumentCategory.LOAD to listOf("груз", "load", "bol"),
        ScanDocumentCategory.PAYCHECK to listOf("зарплата", "paycheck", "settlement"),
        ScanDocumentCategory.DIESEL to listOf("дизель", "diesel", "fuel"),
        ScanDocumentCategory.TRUCK to listOf("машина", "truck", "insurance", "cdl"),
        ScanDocumentCategory.OTHER to listOf("другое", "other"),
    )
}

package com.truckerload.domain.ingest

data class ReceiptPreview(
    val kind: ReceiptKind,
    val amount: Double?,
    val gallons: Double?,
    val pricePerGallon: Double?,
    val date: String?,
    val location: String?,
    val vendor: String?,
    val driverName: String?,
    val tripId: String?,
    val miles: Double? = null,
    val pointA: String? = null,
    val pointB: String? = null,
    val extractedText: String,
    val highlightToken: String?,
    val sourceFileName: String? = null,
    val sourceFilePath: String? = null,
    val messageDateSeconds: Long? = null,
)

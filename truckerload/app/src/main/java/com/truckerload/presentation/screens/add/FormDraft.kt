package com.truckerload.presentation.screens.add

import com.truckerload.domain.model.Load

/**
 * Compares a form against the snapshot taken when it opened.
 * Autofill (GPS stop, last equipment) can move that snapshot while the draft is still clean.
 */
object FormDraft {
    private const val SEP = "\u001f"

    fun signature(vararg parts: String): String = parts.joinToString(SEP)

    fun isDirty(baseline: String?, current: String): Boolean =
        baseline != null && baseline != current

    fun baselineAfterAutoFill(baseline: String?, before: String, after: String): String? =
        if (baseline != null && baseline == before) after else baseline

    fun diesel(
        gallons: String,
        price: String,
        discount: String,
        location: String,
        recordedAtMillis: Long,
        weekNumber: Int,
        year: Int,
    ): String = signature(
        gallons,
        price,
        discount,
        location,
        recordedAtMillis.toString(),
        weekNumber.toString(),
        year.toString(),
    )

    fun paycheck(
        amount: String,
        recordedAtMillis: Long,
        weekNumber: Int,
        year: Int,
        sourceFileName: String,
        rawText: String,
    ): String = signature(
        amount,
        recordedAtMillis.toString(),
        weekNumber.toString(),
        year.toString(),
        sourceFileName,
        rawText,
    )

    fun addLoad(
        mode: String,
        rawText: String,
        tripId: String,
        date: String,
        rate: String,
        miles: String,
        points: List<String>,
        equipment: String,
        documentName: String,
    ): String = signature(
        mode,
        rawText,
        tripId,
        date,
        rate,
        miles,
        points.joinToString("|"),
        equipment,
        documentName,
    )

    fun editLoad(
        tripId: String,
        loadDate: String,
        finishDate: String,
        totalRate: String,
        totalMiles: String,
        pointA: String,
        pointB: String,
        equipment: String,
        dispute: Load?,
    ): String = signature(
        tripId,
        loadDate,
        finishDate,
        totalRate,
        totalMiles,
        pointA,
        pointB,
        equipment,
        dispute?.let {
            listOf(
                it.isDispute,
                it.disputeCompleted,
                it.disputeAmount?.formatPlain().orEmpty(),
                it.disputeApplyToLoad,
                it.disputeAmountApplied,
                it.disputeResponseDate,
            ).joinToString("|")
        }.orEmpty(),
    )
}

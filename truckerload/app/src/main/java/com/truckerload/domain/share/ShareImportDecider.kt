package com.truckerload.domain.share

import com.truckerload.domain.model.Load
import com.truckerload.domain.parser.LoadField
import com.truckerload.domain.parser.LoadParsingEngine
import com.truckerload.domain.parser.ManualLoadFactory
import com.truckerload.domain.parser.MessageParseService
import com.truckerload.domain.parser.ParseInput
import com.truckerload.domain.parser.ParseUtils
import java.util.Locale

sealed class ShareImportDecision {
    data class Add(
        val load: Load,
        val missing: List<LoadField>,
        val loadNumber: String,
        val broker: String,
        val fileHash: String,
    ) : ShareImportDecision()

    data class Duplicate(val existingLoadId: String) : ShareImportDecision()

    data object Reject : ShareImportDecision()
}

/**
 * Turns shared text into a save / duplicate / reject decision.
 * The load itself comes from [LoadParsingEngine]. Field extraction reuses [MessageParseService]. The score gate is
 * [ShareLoadClassifier], so a receipt that happens to contain a dollar amount
 * is not inserted.
 */
class ShareImportDecider(
    private val parser: MessageParseService = MessageParseService(),
    private val nowMillis: Long = System.currentTimeMillis(),
) {
    fun decide(
        text: String,
        fileName: String?,
        fileHash: String,
        index: ShareImportIndex,
        existingTripId: (String) -> String? = { null },
    ): ShareImportDecision {
        val reading = ShareLoadClassifier.read(text, fileName)
        if (!reading.accept) return ShareImportDecision.Reject

        val hash = fileHash.trim()
        if (hash.isNotBlank()) {
            index.findByHash(hash)?.let { return ShareImportDecision.Duplicate(it) }
        }

        val parsed = LoadParsingEngine.parse(
            if (fileName.isNullOrBlank()) {
                ParseInput.PlainText(text, nowMillis)
            } else {
                ParseInput.ExtractedDocument(text, fileName, nowMillis)
            },
        ).getOrNull()?.firstOrNull()
        val fields = parser.extractLoadFields(text, nowMillis, fileName)
        val loadNumber = (
            reading.loadNumber.ifBlank {
                listOf(fields.tripId, parsed?.tripId)
                    .firstOrNull { !it.isNullOrBlank() && !it.startsWith(GENERATED_PREFIX) }
                    .orEmpty()
            }
        ).uppercase(Locale.US)
        val broker = reading.broker
        if (loadNumber.isNotBlank()) {
            index.findByIdentity(loadNumber, broker)?.let { return ShareImportDecision.Duplicate(it) }
        }

        val rate = parsed?.totalRate?.cents?.takeIf { it > 0L }?.div(100.0)
            ?: fields.parsedRate()
            ?: reading.rate
            ?: 0.0
        val miles = parsed?.totalMiles?.takeIf { it > 0.0 }
            ?: fields.parsedMiles().takeIf { it > 0.0 }
            ?: reading.miles
            ?: 0.0
        val pickup = parsed?.pointA?.takeIf { it.isNotBlank() }
            ?: fields.pointA.ifBlank { reading.pickup }
        val delivery = parsed?.pointB?.takeIf { it.isNotBlank() }
            ?: fields.pointB.ifBlank { reading.delivery }
        val labeledDate = ParseUtils.normalizeDate(reading.dateRaw, referenceMillis = nowMillis)
            .takeIf { it.length >= 10 }
            ?: ParseUtils.normalizeDate(fields.date, referenceMillis = nowMillis)
                .takeIf { it.length >= 10 && dateWasInText(text, it) }
        val hadDate = labeledDate != null
        val built = if (
            parsed != null &&
            (loadNumber.isBlank() || parsed.tripId.equals(loadNumber, ignoreCase = true))
        ) {
            parsed
        } else {
            ManualLoadFactory.build(
                tripId = loadNumber,
                date = labeledDate.orEmpty(),
                rate = rate,
                miles = miles,
                pointA = pickup,
                pointB = delivery,
                rawMessage = text,
                nowMillis = nowMillis,
            )
        }
        val load = if (labeledDate != null && !built.date.startsWith(labeledDate)) {
            built.copy(date = labeledDate)
        } else {
            built
        }
        if (load.pointA.isBlank() && load.pointB.isBlank() && load.totalRate.cents <= 0L &&
            loadNumber.isBlank()
        ) {
            return ShareImportDecision.Reject
        }

        val tripKey = load.tripId.trim()
        if (tripKey.isNotBlank() && !tripKey.startsWith(GENERATED_PREFIX)) {
            existingTripId(tripKey)?.let { return ShareImportDecision.Duplicate(it) }
        }

        val hadTrip = loadNumber.isNotBlank()
        return ShareImportDecision.Add(
            load = load,
            missing = missingFields(load, hadDate, hadTrip),
            loadNumber = loadNumber,
            broker = broker,
            fileHash = hash,
        )
    }

    private fun dateWasInText(text: String, isoDate: String): Boolean {
        if (isoDate.length < 10) return false
        if (text.contains(isoDate)) return true
        val month = isoDate.substring(5, 7).trimStart('0')
        val day = isoDate.substring(8, 10).trimStart('0')
        return text.contains("$month/") && text.contains("/$day")
    }

    private fun missingFields(load: Load, hadDate: Boolean, hadTrip: Boolean): List<LoadField> =
        buildList {
            if (load.totalRate.cents <= 0L) add(LoadField.RATE)
            if (load.pointA.isBlank()) add(LoadField.PICKUP)
            if (load.pointB.isBlank()) add(LoadField.DELIVERY)
            if (load.totalMiles <= 0.0) add(LoadField.MILES)
            if (!hadDate) add(LoadField.DATE)
            if (!hadTrip) add(LoadField.TRIP_ID)
        }

    private companion object {
        const val GENERATED_PREFIX = "M-"
    }
}

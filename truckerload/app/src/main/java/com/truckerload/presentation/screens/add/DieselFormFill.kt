package com.truckerload.presentation.screens.add

import com.truckerload.domain.parser.DieselReceiptExtractor
import com.truckerload.domain.parser.DieselReceiptFields

data class DieselScanLabels(
    val gallons: String,
    val price: String,
    val discount: String,
    val location: String,
    val noneMessage: String,
    val foundTemplate: String,
)

object DieselFormFill {

    fun applyScan(
        state: AddDieselUiState,
        fields: DieselReceiptFields,
        rawText: String,
        labels: DieselScanLabels,
    ): AddDieselUiState {
        val found = buildList {
            if (fields.gallons != null) add(labels.gallons)
            if (fields.pricePerGallon != null) add(labels.price)
            if (fields.discountPricePerGallon != null) add(labels.discount)
            if (!fields.location.isNullOrBlank()) add(labels.location)
        }
        val message = if (found.isEmpty()) {
            labels.noneMessage
        } else {
            labels.foundTemplate.format(found.joinToString(", "))
        }
        return state.copy(
            gallonsText = fields.gallons?.let(DieselReceiptExtractor::formatField) ?: state.gallonsText,
            pricePerGallonText = fields.pricePerGallon?.let(DieselReceiptExtractor::formatField)
                ?: state.pricePerGallonText,
            discountPriceText = fields.discountPricePerGallon?.let(DieselReceiptExtractor::formatField)
                ?: state.discountPriceText,
            locationText = fields.location?.takeIf { it.isNotBlank() } ?: state.locationText,
            rawExtractedText = rawText,
            isScanning = false,
            scanMessage = message,
            error = null,
        )
    }
}

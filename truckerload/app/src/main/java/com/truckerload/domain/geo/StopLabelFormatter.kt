package com.truckerload.domain.geo

import com.truckerload.domain.parser.ParseUtils

/** Builds a truck-stop label from reverse-geocode parts (POI name + street + city/state). */
object StopLabelFormatter {

    fun format(
        featureName: String?,
        subThoroughfare: String?,
        thoroughfare: String?,
        locality: String?,
        subAdminArea: String?,
        adminArea: String?,
    ): String {
        val street = listOfNotNull(
            subThoroughfare?.trim()?.takeIf { it.isNotBlank() },
            thoroughfare?.trim()?.takeIf { it.isNotBlank() },
        ).joinToString(" ")
        val city = locality?.trim().orEmpty().ifBlank { subAdminArea?.trim().orEmpty() }
        val stateRaw = adminArea?.trim().orEmpty()
        val state = when {
            stateRaw.isBlank() -> ""
            stateRaw.length == 2 -> stateRaw.uppercase()
            else -> ParseUtils.normalizeUsState(stateRaw)
        }
        val cityState = listOf(city, state).filter { it.isNotBlank() }.joinToString(", ")
        val feature = featureName?.trim().orEmpty()
        val featureIsStreetNumber = feature.isNotEmpty() && feature.all { it.isDigit() || it == '-' || it == ' ' }
        val featureLooksLikeStreet = feature.equals(street, ignoreCase = true) ||
            (street.isNotBlank() && (street.contains(feature, ignoreCase = true) || feature.contains(street, ignoreCase = true))) ||
            featureIsStreetNumber
        val name = feature.takeIf { it.isNotBlank() && !featureLooksLikeStreet }
        return listOfNotNull(
            name,
            street.takeIf { it.isNotBlank() },
            cityState.takeIf { it.isNotBlank() },
        ).distinct().joinToString(", ")
    }
}

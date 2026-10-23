package com.truckerload.domain.units

/**
 * What the driver sees. Journal rows stay in miles and US gallons.
 * An explicit saved choice always wins. With no choice, French starts on kilometers
 * and every other language starts on miles. Language changes never overwrite a saved choice.
 */
enum class DistanceUnit {
    MILES,
    KILOMETERS,
    ;

    val isMetric: Boolean get() = this == KILOMETERS

    val storageTag: String get() = if (isMetric) TAG_KILOMETERS else TAG_MILES

    companion object {
        const val TAG_MILES = "mi"
        const val TAG_KILOMETERS = "km"

        fun fromStored(raw: String?): DistanceUnit? = when (raw?.trim()?.lowercase()) {
            TAG_MILES -> MILES
            TAG_KILOMETERS -> KILOMETERS
            else -> null
        }
    }
}

object DistanceUnitPolicy {
    fun resolve(explicit: DistanceUnit?, languageCode: String?): DistanceUnit {
        explicit?.let { return it }
        val language = languageCode.orEmpty()
            .lowercase()
            .substringBefore('-')
            .substringBefore('_')
            .trim()
        return if (language == "fr") DistanceUnit.KILOMETERS else DistanceUnit.MILES
    }
}

/** Process-wide display system. Hydrated at launch and when Settings changes. */
object DisplayUnits {
    @Volatile
    var current: DistanceUnit = DistanceUnit.MILES
        private set

    val isMetric: Boolean get() = current.isMetric

    fun install(unit: DistanceUnit) {
        current = unit
    }
}

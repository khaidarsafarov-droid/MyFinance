package com.example.myfinance.gemini

/**
 * Result of parsing a transport/relay document (e.g. Amazon Relay).
 * Strict schema: trip_details, itinerary (stops), penalty_policy.
 */
data class TripDetails(
    val id: String,
    val rate: Double,
    val miles: Double
)

data class RelayStop(
    val type: String,           // "PU" / "Pickup" or "DEL" / "Delivery"
    val locationCode: String?,  // PU#, location code
    val address: String,
    val street: String? = null,
    val city: String? = null,
    val state: String? = null,
    val zip: String? = null,
    val time: String?,          // ISO or readable
    val note: String? = null
)

data class PenaltyRule(
    val description: String,
    val penalty: Double
)

data class RelayParseResult(
    val tripDetails: TripDetails,
    val itinerary: List<RelayStop>,
    val penaltyPolicy: List<PenaltyRule> = emptyList()
) {
    /** Convert to app's ParsedTrip for adding to Loads (pointA = first stop, pointB = last stop). */
    fun toParsedTrip(): ParsedTrip {
        val first = itinerary.firstOrNull()
        val last = itinerary.lastOrNull()
        val pointA = first?.address?.takeIf { it.isNotBlank() } ?: first?.locationCode ?: "—"
        val pointB = last?.address?.takeIf { it.isNotBlank() } ?: last?.locationCode ?: "—"
        val startTime = first?.time ?: "—"
        val endTime = last?.time ?: "—"
        val date = first?.time?.take(10) ?: last?.time?.take(10) ?: java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        return ParsedTrip(
            pointA = pointA,
            pointB = pointB,
            miles = tripDetails.miles,
            cost = tripDetails.rate,
            startTime = startTime,
            endTime = endTime,
            orderNumber = tripDetails.id,
            date = date
        )
    }
}

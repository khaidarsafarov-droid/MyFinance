package com.truckerload.domain.friends

/**
 * Decodes Google's encoded polyline format used by the Directions API.
 * @see <a href="https://developers.google.com/maps/documentation/utilities/polylinealgorithm">Polyline encoding</a>
 */
object PolylineDecoder {

    fun decode(encoded: String): List<LatLngPoint> {
        if (encoded.isEmpty()) return emptyList()
        val out = ArrayList<LatLngPoint>()
        var index = 0
        var lat = 0
        var lng = 0
        while (index < encoded.length) {
            var shift = 0
            var result = 0
            var b: Int
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dLat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dLat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dLng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dLng

            out += LatLngPoint(lat / 1e5, lng / 1e5)
        }
        return out
    }
}

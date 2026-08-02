package com.truckerload.domain.friends

/**
 * Google encoded polyline codec (same algorithm as Maps PolyUtil).
 * Pure Kotlin so JVM unit tests do not need the Android maps-utils jar.
 */
object EncodedPolylineCodec {

    fun decode(encoded: String): List<LatLngPoint> {
        if (encoded.isBlank()) return emptyList()
        val path = ArrayList<LatLngPoint>()
        var index = 0
        var lat = 0
        var lng = 0
        while (index < encoded.length) {
            var result = 0
            var shift = 0
            var b: Int
            do {
                if (index >= encoded.length) return path
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            result = 0
            shift = 0
            do {
                if (index >= encoded.length) return path
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            path.add(LatLngPoint(lat / 1e5, lng / 1e5))
        }
        return path
    }
}

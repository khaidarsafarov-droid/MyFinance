package com.truckerload.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleDirectionsClientTest {

    private val client = GoogleDirectionsClient(apiKey = "test-key")

    @Test
    fun parseOkResponseDecodesOverviewPolyline() {
        // Same encoded sample as EncodedPolylineCodecTest
        val body = """
            {
              "status": "OK",
              "routes": [{
                "overview_polyline": {
                  "points": "_p~iF~ps|U_ulLnnqC_mqNvxq`@"
                }
              }]
            }
        """.trimIndent()
        val points = client.parseDirectionsBody(body)
        assertEquals(3, points.size)
        assertEquals(38.5, points.first().lat, 0.01)
        assertEquals(-126.453, points.last().lng, 0.01)
    }

    @Test
    fun parseNonOkThrows() {
        val body = """{"status":"REQUEST_DENIED","error_message":"API not enabled"}"""
        val error = runCatching { client.parseDirectionsBody(body) }.exceptionOrNull()
        assertTrue(error is IllegalStateException || error is Exception)
        assertTrue(error!!.message!!.contains("REQUEST_DENIED"))
    }
}

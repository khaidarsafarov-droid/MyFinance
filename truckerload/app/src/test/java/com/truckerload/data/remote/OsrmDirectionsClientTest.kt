package com.truckerload.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OsrmDirectionsClientTest {

    private val client = OsrmDirectionsClient()

    @Test
    fun parseOkResponseDecodesGeometryPolyline() {
        val body = """
            {
              "code": "Ok",
              "routes": [{
                "geometry": "_p~iF~ps|U_ulLnnqC_mqNvxq`@"
              }]
            }
        """.trimIndent()
        val points = client.parseOsrmBody(body)
        assertEquals(3, points.size)
        assertEquals(38.5, points.first().lat, 0.01)
        assertEquals(-126.453, points.last().lng, 0.01)
    }

    @Test
    fun parseNonOkThrows() {
        val body = """{"code":"NoRoute","message":"No route found"}"""
        val error = runCatching { client.parseOsrmBody(body) }.exceptionOrNull()
        assertTrue(error is IllegalStateException || error is Exception)
        assertTrue(error!!.message!!.contains("NoRoute"))
    }
}

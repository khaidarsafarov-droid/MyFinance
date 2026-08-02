package com.truckerload.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GoogleDirectionsServiceTest {

    private val service = GoogleDirectionsService()

    @Test
    fun parsesOkResponse() {
        val json = """
            {
              "status": "OK",
              "routes": [{
                "overview_polyline": {
                  "points": "_p~iF~ps|U_ulLnnqC_mqNvxq`@"
                }
              }]
            }
        """.trimIndent()
        val points = service.parseDirectionsResponse(json)
        assertNotNull(points)
        assertEquals(3, points!!.size)
    }

    @Test
    fun returnsNullForZeroResults() {
        val json = """{"status":"ZERO_RESULTS","routes":[]}"""
        assertNull(service.parseDirectionsResponse(json))
    }

    @Test
    fun returnsNullForEmptyBody() {
        assertNull(service.parseDirectionsResponse(""))
    }
}

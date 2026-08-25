package com.truckerload.domain.geo

import org.junit.Assert.assertEquals
import org.junit.Test

class StopLabelFormatterTest {

    @Test
    fun lovesStop_usesPoiNamePlusStreetAndCity() {
        val label = StopLabelFormatter.format(
            featureName = "Love's Travel Stop",
            subThoroughfare = "4120",
            thoroughfare = "I-40",
            locality = "Oklahoma City",
            subAdminArea = "Oklahoma County",
            adminArea = "Oklahoma",
        )
        assertEquals("Love's Travel Stop, 4120 I-40, Oklahoma City, OK", label)
    }

    @Test
    fun streetNumberFeature_isNotUsedAsStopName() {
        val label = StopLabelFormatter.format(
            featureName = "4120",
            subThoroughfare = "4120",
            thoroughfare = "West Main Street",
            locality = "Garner",
            subAdminArea = null,
            adminArea = "NC",
        )
        assertEquals("4120 West Main Street, Garner, NC", label)
    }

    @Test
    fun cityOnly_stillFormats() {
        val label = StopLabelFormatter.format(
            featureName = null,
            subThoroughfare = null,
            thoroughfare = null,
            locality = "Knoxville",
            subAdminArea = null,
            adminArea = "TN",
        )
        assertEquals("Knoxville, TN", label)
    }
}

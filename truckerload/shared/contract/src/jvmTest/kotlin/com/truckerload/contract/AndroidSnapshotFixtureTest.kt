package com.truckerload.contract

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AndroidSnapshotFixtureTest {
    @Test
    fun `loads current Android Gson snapshot fixture`() {
        val fixture = requireNotNull(javaClass.getResource("/fixtures/android-snapshot-v1.json")).readText()
        val snapshot = ContractJson.decodeFromString<AccountCloudSnapshot>(fixture)

        assertEquals(1, snapshot.version)
        assertEquals("5ca1ab1e-4aa9-4b48-9b1b-47666ed03461", snapshot.accountId)
        assertEquals(1721606400123, snapshot.updatedAt)
        assertNull(snapshot.entityCount)
        assertEquals(3, snapshot.resolvedEntityCount())
        assertEquals("""{"displayName":"Ivan","homeState":"NC"}""", snapshot.driverProfileJson)
    }
}

package com.truckerload.contract

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ContractsTest {
    @Test
    fun `contracts round trip without losing fields`() {
        val metadata = MediaMetadata(
            mediaId = "f3074574-4bb1-49c1-b1b9-24e2ab530469",
            fileName = "bol.pdf",
            contentType = "application/pdf",
            sizeBytes = 42,
            status = "ready",
            createdAt = 100,
            kind = MediaKind.SCAN,
            clientId = "scan-client-1",
            metadata = buildJsonObject { put("pageCount", 2) },
        )

        val encoded = ContractJson.encodeToString(metadata)
        assertEquals(metadata, ContractJson.decodeFromString<MediaMetadata>(encoded))

        val snapshot = AccountCloudSnapshot(
            accountId = "5ca1ab1e-4aa9-4b48-9b1b-47666ed03461",
            updatedAt = 123,
            backup = buildJsonObject { put("version", 1) },
            driverProfileJson = """{"displayName":"Ivan"}""",
            entityCount = 4,
        )
        assertEquals(
            snapshot,
            ContractJson.decodeFromString<AccountCloudSnapshot>(ContractJson.encodeToString(snapshot)),
        )
    }

    @Test
    fun `legacy media metadata remains decodable with safe defaults`() {
        val legacy =
            """{"mediaId":"f3074574-4bb1-49c1-b1b9-24e2ab530469","fileName":"bol.pdf","contentType":"application/pdf","sizeBytes":42,"status":"ready","createdAt":100}"""
        val metadata = ContractJson.decodeFromString<MediaMetadata>(legacy)

        assertEquals(MediaKind.SCAN, metadata.kind)
        assertEquals(metadata.mediaId, metadata.clientId)
        assertEquals(100, metadata.updatedAt)
    }

    @Test
    fun `loads current Android Gson snapshot fixture`() {
        val fixture =
            requireNotNull(javaClass.getResource("/fixtures/android-snapshot-v1.json")).readText()
        val snapshot = ContractJson.decodeFromString<AccountCloudSnapshot>(fixture)

        assertEquals(1, snapshot.version)
        assertEquals("5ca1ab1e-4aa9-4b48-9b1b-47666ed03461", snapshot.accountId)
        assertEquals(1721606400123, snapshot.updatedAt)
        assertNull(snapshot.entityCount)
        assertEquals(3, snapshot.resolvedEntityCount())
        assertEquals("""{"displayName":"Ivan","homeState":"NC"}""", snapshot.driverProfileJson)
    }

    @Test
    fun `push token registration contract round trips`() {
        val request = DevicePushTokenRequest("device-1", "opaque-token")

        assertEquals(
            request,
            ContractJson.decodeFromString<DevicePushTokenRequest>(
                ContractJson.encodeToString(
                    request
                )
            ),
        )
    }

    @Test
    fun `device register contract round trips and classifies tablets by smallest width`() {
        val request = DeviceRegisterRequest("device-1", DeviceSlotPolicy.TABLET)
        assertEquals(
            request,
            ContractJson.decodeFromString<DeviceRegisterRequest>(ContractJson.encodeToString(request)),
        )
        assertEquals(DeviceSlotPolicy.PHONE, DeviceSlotPolicy.fromSmallestWidthDp(599))
        assertEquals(DeviceSlotPolicy.TABLET, DeviceSlotPolicy.fromSmallestWidthDp(600))
        assertEquals(DeviceSlotPolicy.PHONE, DeviceSlotPolicy.normalize("PHONE"))
        assertEquals(null, DeviceSlotPolicy.normalize("laptop"))
    }
}

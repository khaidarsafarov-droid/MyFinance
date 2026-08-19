package com.truckerload.data.sync

import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class RemoteAccountCloudClientTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `snapshot request sends bearer and device headers and enforces user scope`() = runBlocking {
        val snapshot = snapshot("account-1")
        server.enqueue(MockResponse().setResponseCode(200).setBody(AccountCloudSnapshotCodec.toJson(snapshot)))
        val client = client()

        assertEquals(snapshot.accountId, client.read("account-1")?.accountId)
        val request = server.takeRequest()
        assertEquals("/v1/sync/snapshot", request.path)
        assertEquals("Bearer access-token", request.getHeader("Authorization"))
        assertEquals("device-1", request.getHeader("X-Device-Id"))
    }

    @Test
    fun `no-content read returns null`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(204))

        assertNull(client().read("account-1"))
    }

    @Test
    fun `malformed and cross-account responses fail closed`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{not-json"))
        assertThrows(IOException::class.java) { runBlocking { client().read("account-1") } }

        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody(AccountCloudSnapshotCodec.toJson(snapshot("account-2"))),
        )
        assertThrows(IOException::class.java) { runBlocking { client().read("account-1") } }
    }

    @Test
    fun `no-content write is a remote acknowledgement`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(204))

        val result = client().write(snapshot("account-1"))

        assertTrue(result.remoteAcknowledged)
        assertFalse(result.localWritten)
    }

    @Test
    fun `matching snapshot response acknowledges upload`() = runBlocking {
        val outgoing = snapshot("account-1")
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody(AccountCloudSnapshotCodec.toJson(outgoing)),
        )

        val result = client().write(outgoing)

        assertTrue(result.remoteAcknowledged)
        assertEquals("PUT", server.takeRequest().method)
    }

    @Test
    fun `device register posts form factor and maps 409 to slot taken`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody(
                    """{"deviceId":"device-1","formFactor":"phone","registeredAt":1,"lastSeenAt":1}""",
                ),
        )
        client().registerDevice("device-1", "phone")
        val registered = server.takeRequest()
        assertEquals("/v1/devices/register", registered.path)
        assertEquals("POST", registered.method)
        assertTrue(registered.body.readUtf8().contains("\"formFactor\":\"phone\""))

        server.enqueue(MockResponse().setResponseCode(409).setBody("""{"code":"device_slot_taken"}"""))
        val denied = assertThrows(DeviceSlotTakenException::class.java) {
            runBlocking { client().registerDevice("device-1", "phone") }
        }
        assertEquals("phone", denied.formFactor)
    }

    @Test
    fun `device unregister deletes this device id`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(204))
        client().unregisterDevice()
        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/v1/devices/register?deviceId=device-1", request.path)
    }

    @Test
    fun `stale LWW response is not an acknowledgement of outgoing data`() {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody(AccountCloudSnapshotCodec.toJson(snapshot("account-1").copy(updatedAt = 5))),
        )

        assertThrows(IOException::class.java) {
            runBlocking { client().write(snapshot("account-1").copy(updatedAt = 10)) }
        }
    }

    @Test
    fun `cleartext backend is rejected outside loopback and debug`() {
        assertThrows(IllegalArgumentException::class.java) {
            RemoteAccountCloudClient.validatedBaseUrl(
                "http://api.example.com",
                allowInsecureHttp = false,
            )
        }
    }

    @Test
    fun `hybrid caches remote reads and reports remote write failure`() = runBlocking {
        val cached = FakeBackend(snapshot = snapshot("account-1"))
        val remoteSnapshot = snapshot("account-1").copy(updatedAt = 20)
        val remoteRead = FakeBackend(snapshot = remoteSnapshot)
        val hybrid = HybridAccountCloudBackend(cached, remoteRead)

        assertEquals(20L, hybrid.read("account-1")?.updatedAt)
        assertEquals(20L, cached.snapshot?.updatedAt)

        remoteRead.failWrites = true
        val outgoing = snapshot("account-1").copy(updatedAt = 30)
        val result = hybrid.write(outgoing)
        assertEquals(30L, cached.snapshot?.updatedAt)
        assertFalse(result.successful)
        assertTrue(result.localWritten)
    }

    @Test
    fun `hybrid falls back to local snapshot when remote read fails`() = runBlocking {
        val localSnapshot = snapshot("account-1")
        val local = FakeBackend(snapshot = localSnapshot)
        val remote = FakeBackend(snapshot = null, failReads = true)

        assertEquals(localSnapshot, HybridAccountCloudBackend(local, remote).read("account-1"))
    }

    private fun client() = RemoteAccountCloudClient(
        backendUrl = server.url("/").toString(),
        accessToken = { "access-token" },
        deviceId = "device-1",
        allowInsecureHttp = false,
    )

    private fun snapshot(accountId: String) = AccountCloudSnapshot(
        accountId = accountId,
        updatedAt = 10,
    )
}

private class FakeBackend(
    var snapshot: AccountCloudSnapshot?,
    var failReads: Boolean = false,
    var failWrites: Boolean = false,
) : AccountCloudBackend {
    override val remoteConfigured: Boolean = true

    override suspend fun read(accountId: String): AccountCloudSnapshot? {
        if (failReads) throw IOException("offline")
        return snapshot
    }

    override suspend fun write(snapshot: AccountCloudSnapshot): AccountCloudWriteResult {
        if (failWrites) throw IOException("offline")
        this.snapshot = snapshot
        return AccountCloudWriteResult(
            localWritten = false,
            remoteConfigured = true,
            remoteAcknowledged = true,
        )
    }
}

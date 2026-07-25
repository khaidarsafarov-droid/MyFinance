package com.truckerload.data.sync

import com.truckerload.contract.ContractJson
import com.truckerload.contract.MediaKind
import com.truckerload.contract.MediaListResponse
import com.truckerload.contract.MediaMetadata
import com.truckerload.contract.MediaUploadRequest
import com.truckerload.contract.MediaUploadResponse
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class MediaCloudClientTest {
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
    fun `upload sends auth only to backend and puts exact bytes to signed target`() = runBlocking {
        val bytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 1, 2)
        val file = File.createTempFile("media-client", ".jpg").apply { writeBytes(bytes) }
        try {
            val checksum = MediaFilePolicy.sha256(file)
            val upload = MediaUploadResponse(
                mediaId = "11111111-1111-4111-8111-111111111111",
                uploadUrl = server.url("/signed-upload?token=opaque").toString(),
                headers = mapOf("Content-Type" to "image/jpeg"),
                expiresAt = System.currentTimeMillis() + 60_000,
            )
            val complete = metadata(checksum, bytes.size.toLong())
            server.enqueue(jsonResponse(upload, 201))
            server.enqueue(MockResponse().setResponseCode(200))
            server.enqueue(jsonResponse(complete))
            val client = client()
            val request = MediaUploadRequest(
                fileName = "proof.jpg",
                contentType = "image/jpeg",
                sizeBytes = bytes.size.toLong(),
                checksum = checksum,
                kind = MediaKind.PHOTO,
                clientId = "photo-1",
                metadata = buildJsonObject { put("city", "Raleigh") },
            )

            val response = client.requestUpload(request)
            client.putExactBytes(response, file, "image/jpeg", bytes.size.toLong())
            assertEquals(complete.mediaId, client.complete(response.mediaId, checksum).mediaId)

            val createRequest = server.takeRequest()
            assertEquals("/v1/media/upload-url", createRequest.path)
            assertEquals("Bearer access-token", createRequest.getHeader("Authorization"))
            assertEquals("device-1", createRequest.getHeader("X-Device-Id"))

            val putRequest = server.takeRequest()
            assertEquals("PUT", putRequest.method)
            assertNull(putRequest.getHeader("Authorization"))
            assertArrayEquals(bytes, putRequest.body.readByteArray())

            val completeRequest = server.takeRequest()
            assertEquals("/v1/media/complete", completeRequest.path)
            assertEquals("Bearer access-token", completeRequest.getHeader("Authorization"))
        } finally {
            file.delete()
        }
    }

    @Test
    fun `list downloads verified content and delete treats 404 as idempotent`() = runBlocking {
        val bytes = "%PDF-1.7 test".encodeToByteArray()
        val checksumFile = File.createTempFile("media-checksum", ".pdf").apply { writeBytes(bytes) }
        val destination = File.createTempFile("media-download", ".tmp").apply { delete() }
        try {
            val item = metadata(MediaFilePolicy.sha256(checksumFile), bytes.size.toLong()).copy(
                kind = MediaKind.SCAN,
                clientId = "scan-1",
                fileName = "scan.pdf",
                contentType = "application/pdf",
                downloadUrl = server.url("/signed-download?token=opaque").toString(),
            )
            server.enqueue(jsonResponse(MediaListResponse(listOf(item), nextSince = 7)))
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/pdf")
                    .setBody(Buffer().write(bytes)),
            )
            server.enqueue(MockResponse().setResponseCode(404))
            val client = client()

            val listed = client.list(0)
            assertEquals(7, listed.nextSince)
            client.download(listed.items.single(), destination)
            assertArrayEquals(bytes, destination.readBytes())
            client.delete(item.mediaId)

            assertEquals("/v1/media?since=0", server.takeRequest().path)
            assertNull(server.takeRequest().getHeader("Authorization"))
            assertEquals("DELETE", server.takeRequest().method)
        } finally {
            checksumFile.delete()
            destination.delete()
        }
    }

    private fun client() = MediaCloudClient(
        backendUrl = server.url("/").toString(),
        accessToken = { "access-token" },
        deviceId = "device-1",
        allowInsecureHttp = false,
    )

    private fun metadata(checksum: String, size: Long) = MediaMetadata(
        mediaId = "11111111-1111-4111-8111-111111111111",
        fileName = "proof.jpg",
        contentType = "image/jpeg",
        sizeBytes = size,
        checksum = checksum,
        status = "ready",
        createdAt = 1,
        completedAt = 2,
        kind = MediaKind.PHOTO,
        clientId = "photo-1",
        updatedAt = 2,
    )

    private inline fun <reified T> jsonResponse(value: T, code: Int = 200): MockResponse =
        MockResponse()
            .setResponseCode(code)
            .setHeader("Content-Type", "application/json")
            .setBody(ContractJson.encodeToString(value))
}

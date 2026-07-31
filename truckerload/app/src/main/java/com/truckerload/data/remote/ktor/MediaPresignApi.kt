package com.truckerload.data.remote.ktor

import com.truckerload.contract.MediaMetadata
import com.truckerload.contract.MediaUploadCompleteRequest
import com.truckerload.contract.MediaUploadRequest
import com.truckerload.contract.MediaUploadResponse
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.jvm.javaio.toInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Presigned media upload/download against `/v1/media/` routes.
 * Bytes go directly to Spaces/S3 via the signed URL; Ktor only authorizes metadata.
 *
 * Prefer this API for new call sites. Existing [com.truckerload.data.sync.MediaCloudClient]
 * remains the OkHttp path used by [com.truckerload.sync.MediaSyncWorker].
 */
@Singleton
class MediaPresignApi @Inject constructor(
    private val http: HttpClientProvider,
) {
    suspend fun requestUploadUrl(request: MediaUploadRequest): MediaUploadResponse {
        ensureConfigured()
        val response = http.client.post("v1/media/upload-url") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (!response.status.isSuccess()) {
            throw IOException("presign_failed HTTP ${response.status.value}")
        }
        return response.body()
    }

    /**
     * Task surface: `GET /media/presign?key=`.
     * Backend currently issues upload URLs via `POST /v1/media/upload-url`; [key] is
     * accepted for forward-compat logging and ignored by the hosted route.
     */
    suspend fun getPresign(key: String, request: MediaUploadRequest): MediaUploadResponse {
        require(key.isNotBlank() || !request.clientId.isNullOrBlank()) {
            "key or clientId required"
        }
        return requestUploadUrl(request)
    }

    suspend fun uploadToPresignedUrl(
        upload: MediaUploadResponse,
        file: File,
        contentType: String,
        expectedSize: Long,
    ) {
        val rawUrl = upload.uploadUrl ?: throw IOException("missing_upload_url")
        if (upload.method != "PUT") throw IOException("invalid_upload_method")
        if (!file.isFile || file.length() != expectedSize) throw IOException("local_file_changed")
        val response = http.client.put(rawUrl) {
            upload.headers.forEach { (name, value) ->
                if (!name.equals("Content-Length", ignoreCase = true)) {
                    headers.append(name, value)
                }
            }
            contentType(ContentType.parse(contentType))
            setBody(file.readBytes())
        }
        if (!response.status.isSuccess()) {
            throw IOException("upload_failed HTTP ${response.status.value}")
        }
    }

    suspend fun complete(mediaId: String, checksum: String): MediaMetadata {
        ensureConfigured()
        val response = http.client.post("v1/media/complete") {
            contentType(ContentType.Application.Json)
            setBody(MediaUploadCompleteRequest(mediaId, checksum))
        }
        if (!response.status.isSuccess()) {
            throw IOException("complete_failed HTTP ${response.status.value}")
        }
        return response.body()
    }

    suspend fun downloadToFile(downloadUrl: String, target: File) {
        val response = http.client.get(downloadUrl)
        if (!response.status.isSuccess()) {
            throw IOException("download_failed HTTP ${response.status.value}")
        }
        val tmp = File(target.parentFile, "${target.name}.part")
        response.bodyAsChannel().toInputStream().use { input ->
            FileOutputStream(tmp).use { output -> input.copyTo(output) }
        }
        if (!tmp.renameTo(target)) {
            tmp.copyTo(target, overwrite = true)
            tmp.delete()
        }
    }

    suspend fun deleteMedia(mediaId: String) {
        ensureConfigured()
        val response = http.client.delete("v1/media/$mediaId")
        if (!response.status.isSuccess() && response.status.value != 404) {
            throw IOException("delete_failed HTTP ${response.status.value}")
        }
    }

    suspend fun listMedia(since: String? = null, kind: String? = null): String {
        ensureConfigured()
        val response = http.client.get("v1/media") {
            since?.let { parameter("since", it) }
            kind?.let { parameter("kind", it) }
        }
        if (!response.status.isSuccess()) {
            throw IOException("list_failed HTTP ${response.status.value}")
        }
        return response.bodyAsText()
    }

    private fun ensureConfigured() {
        if (!http.isBackendConfigured()) throw IOException("SYNC_BACKEND_URL not configured")
    }
}

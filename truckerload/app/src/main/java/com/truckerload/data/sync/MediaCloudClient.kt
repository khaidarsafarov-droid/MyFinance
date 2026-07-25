package com.truckerload.data.sync

import com.truckerload.BuildConfig
import com.truckerload.contract.ContractJson
import com.truckerload.contract.MediaKind
import com.truckerload.contract.MediaListResponse
import com.truckerload.contract.MediaMetadata
import com.truckerload.contract.MediaUploadCompleteRequest
import com.truckerload.contract.MediaUploadRequest
import com.truckerload.contract.MediaUploadResponse
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class MediaCloudException(
    val errorCode: String,
    val httpStatus: Int? = null,
    val retryable: Boolean,
) : IOException(errorCode)

class MediaCloudClient(
    backendUrl: String,
    private val accessToken: () -> String?,
    private val deviceId: String,
    private val client: OkHttpClient = OkHttpClient(),
    private val allowInsecureHttp: Boolean = BuildConfig.DEBUG,
) {
    private val baseUrl = RemoteAccountCloudClient.validatedBaseUrl(backendUrl, allowInsecureHttp)

    suspend fun requestUpload(request: MediaUploadRequest): MediaUploadResponse =
        withContext(Dispatchers.IO) {
            val body = ContractJson.encodeToString(request).toRequestBody(JSON)
            executeJson(
                authorized(endpoint("v1", "media", "upload-url")).post(body).build(),
                MediaUploadResponse.serializer(),
            )
        }

    suspend fun putExactBytes(
        upload: MediaUploadResponse,
        file: File,
        contentType: String,
        expectedSize: Long,
    ) = withContext(Dispatchers.IO) {
        val rawUrl = upload.uploadUrl
            ?: throw MediaCloudException("missing_upload_url", retryable = false)
        if (upload.method != "PUT") throw MediaCloudException("invalid_upload_method", retryable = false)
        if (!file.isFile || file.length() != expectedSize) {
            throw MediaCloudException("local_file_changed", retryable = false)
        }
        val url = validateSignedUrl(rawUrl)
        val request = Request.Builder()
            .url(url)
            .put(file.asRequestBody(contentType.toMediaType()))
        upload.headers.forEach { (name, value) ->
            if (name.lowercase() !in FORBIDDEN_SIGNED_HEADERS && !name.equals("Content-Length", true)) {
                request.header(name, value)
            }
        }
        request.header("Content-Type", contentType)
        client.newCall(request.build()).execute().use { response ->
            if (!response.isSuccessful) throw httpError("upload_failed", response.code)
        }
    }

    suspend fun complete(mediaId: String, checksum: String): MediaMetadata =
        withContext(Dispatchers.IO) {
            val request = MediaUploadCompleteRequest(mediaId, checksum)
            executeJson(
                authorized(endpoint("v1", "media", "complete"))
                    .post(ContractJson.encodeToString(request).toRequestBody(JSON))
                    .build(),
                MediaMetadata.serializer(),
            )
        }

    suspend fun list(since: Long, kind: MediaKind? = null): MediaListResponse =
        withContext(Dispatchers.IO) {
            val url = endpoint("v1", "media").newBuilder()
                .addQueryParameter("since", since.coerceAtLeast(0).toString())
                .apply { if (kind != null) addQueryParameter("kind", kind.name) }
                .build()
            executeJson(authorized(url).get().build(), MediaListResponse.serializer())
        }

    suspend fun get(mediaId: String): MediaMetadata = withContext(Dispatchers.IO) {
        executeJson(
            authorized(endpoint("v1", "media", mediaId)).get().build(),
            MediaMetadata.serializer(),
        )
    }

    suspend fun delete(mediaId: String) = withContext(Dispatchers.IO) {
        client.newCall(authorized(endpoint("v1", "media", mediaId)).delete().build()).execute().use { response ->
            if (response.code != 204 && response.code != 404) {
                throw httpError("delete_failed", response.code)
            }
        }
    }

    suspend fun download(metadata: MediaMetadata, temporary: File) = withContext(Dispatchers.IO) {
        val rawUrl = metadata.downloadUrl
            ?: throw MediaCloudException("missing_download_url", retryable = true)
        MediaFilePolicy.validateRemote(
            metadata.kind,
            metadata.clientId,
            metadata.fileName,
            metadata.contentType,
            metadata.sizeBytes,
            metadata.checksum,
        )
        val url = validateSignedUrl(rawUrl)
        temporary.parentFile?.mkdirs()
        client.newCall(Request.Builder().url(url).get().build()).execute().use { response ->
            if (!response.isSuccessful) throw httpError("download_failed", response.code)
            val body = response.body
                ?: throw MediaCloudException("empty_download", retryable = true)
            val responseType = body.contentType()?.toString()?.substringBefore(';')?.lowercase()
            val expectedType = metadata.contentType.substringBefore(';').lowercase()
            if (responseType != null && responseType != expectedType) {
                throw MediaCloudException("content_type_mismatch", retryable = false)
            }
            if (body.contentLength() > MediaFilePolicy.MAX_BYTES) {
                throw MediaCloudException("download_too_large", retryable = false)
            }
            var copied = 0L
            try {
                body.byteStream().use { input ->
                    FileOutputStream(temporary, false).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            copied += count
                            if (copied > MediaFilePolicy.MAX_BYTES || copied > metadata.sizeBytes) {
                                throw MediaCloudException("download_too_large", retryable = false)
                            }
                            output.write(buffer, 0, count)
                        }
                        output.fd.sync()
                    }
                }
                if (copied != metadata.sizeBytes) {
                    throw MediaCloudException("size_mismatch", retryable = true)
                }
                MediaFilePolicy.contentMatches(temporary, expectedType)
                MediaFilePolicy.verifyChecksum(temporary, metadata.checksum)
            } catch (error: Throwable) {
                temporary.delete()
                throw error
            }
        }
    }

    private fun <T> executeJson(
        request: Request,
        serializer: kotlinx.serialization.KSerializer<T>,
    ): T {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw httpError("backend_request_failed", response.code)
            val body = response.body ?: throw MediaCloudException("empty_response", retryable = true)
            if (body.contentLength() > MAX_JSON_RESPONSE_BYTES) {
                throw MediaCloudException("response_too_large", retryable = false)
            }
            val bytes = body.byteStream().use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > MAX_JSON_RESPONSE_BYTES) {
                        throw MediaCloudException("response_too_large", retryable = false)
                    }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            return runCatching { ContractJson.decodeFromString(serializer, bytes.decodeToString()) }
                .getOrElse { throw MediaCloudException("malformed_response", retryable = false) }
        }
    }

    private fun authorized(url: HttpUrl): Request.Builder {
        val token = accessToken()?.takeIf(String::isNotBlank)
            ?: throw MediaCloudException("missing_session", retryable = true)
        return Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("X-Device-Id", deviceId)
    }

    private fun endpoint(vararg segments: String): HttpUrl =
        baseUrl.newBuilder().apply { segments.forEach(::addPathSegment) }.build()

    private fun validateSignedUrl(value: String): HttpUrl {
        val url = runCatching { value.toHttpUrl() }
            .getOrElse { throw MediaCloudException("invalid_signed_url", retryable = false) }
        if (url.scheme == "https") return url
        val loopback = url.host.equals("localhost", true) || url.host in setOf("127.0.0.1", "::1")
        if (url.scheme != "http" || (!loopback && !allowInsecureHttp)) {
            throw MediaCloudException("invalid_signed_url", retryable = false)
        }
        return url
    }

    private fun httpError(code: String, status: Int): MediaCloudException =
        MediaCloudException(
            errorCode = "http_$status",
            httpStatus = status,
            retryable = status == 401 ||
                status == 408 ||
                status == 409 ||
                status == 425 ||
                status == 429 ||
                status >= 500,
        )

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
        private const val MAX_JSON_RESPONSE_BYTES = 512 * 1024
        private val FORBIDDEN_SIGNED_HEADERS = setOf(
            "authorization",
            "proxy-authorization",
            "cookie",
            "host",
            "connection",
        )
    }
}

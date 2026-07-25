package com.truckerload.backend

import java.io.Closeable
import java.net.URI
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.time.Duration
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.HeadBucketRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest

data class PresignedUpload(
    val url: String,
    val headers: Map<String, String>,
    val expiresAt: Long,
)

data class PresignedDownload(
    val url: String,
    val expiresAt: Long,
)

data class StoredObject(val sizeBytes: Long, val checksum: String?)

interface ObjectStorage : Closeable {
    suspend fun presignUpload(
        mediaId: UUID,
        objectKey: String,
        contentType: String,
        sizeBytes: Long,
        expiresAt: Long,
    ): PresignedUpload

    suspend fun presignDownload(
        mediaId: UUID,
        objectKey: String,
        expiresAt: Long,
    ): PresignedDownload

    suspend fun stat(objectKey: String): StoredObject?
    suspend fun delete(objectKey: String)
    suspend fun isReady(): Boolean

    override fun close() = Unit
}

interface LocalUploadReceiver {
    suspend fun receive(
        mediaId: UUID,
        objectKey: String,
        expiresAt: Long,
        token: String,
        bytes: ByteArray,
    ): Boolean
}

interface LocalDownloadReceiver {
    suspend fun openDownload(
        mediaId: UUID,
        objectKey: String,
        expiresAt: Long,
        token: String,
    ): LocalDownload?
}

class LocalDownload internal constructor(
    val sizeBytes: Long,
    private val path: Path,
) {
    suspend fun copyTo(output: OutputStream, maxBytes: Long) = withContext(Dispatchers.IO) {
        require(sizeBytes in 0..maxBytes) { "Stored object is outside the download limit" }
        Files.newInputStream(path, StandardOpenOption.READ).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var copied = 0L
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                copied += count
                require(copied <= maxBytes) { "Stored object exceeded the download limit" }
                output.write(buffer, 0, count)
            }
            require(copied == sizeBytes) { "Stored object changed while streaming" }
        }
    }
}

class LocalObjectStorage(
    rootPath: String,
    private val publicBaseUrl: String,
    private val signingSecret: String,
) : ObjectStorage, LocalUploadReceiver, LocalDownloadReceiver {
    private val root: Path = Path.of(rootPath).toAbsolutePath().normalize()

    init {
        Files.createDirectories(root)
    }

    override suspend fun presignUpload(
        mediaId: UUID,
        objectKey: String,
        contentType: String,
        sizeBytes: Long,
        expiresAt: Long,
    ): PresignedUpload {
        val signature = signature(mediaId, objectKey, expiresAt)
        return PresignedUpload(
            url = "${publicBaseUrl.trimEnd('/')}/v1/media/local-upload/$mediaId" +
                "?expiresAt=$expiresAt&token=$signature",
            headers = mapOf("Content-Type" to contentType, "Content-Length" to sizeBytes.toString()),
            expiresAt = expiresAt,
        )
    }

    override suspend fun receive(
        mediaId: UUID,
        objectKey: String,
        expiresAt: Long,
        token: String,
        bytes: ByteArray,
    ): Boolean = withContext(Dispatchers.IO) {
        if (System.currentTimeMillis() > expiresAt) return@withContext false
        if (!constantTimeEquals(signature(mediaId, objectKey, expiresAt), token)) return@withContext false
        val destination = safePath(objectKey)
        Files.createDirectories(destination.parent)
        val temporary = Files.createTempFile(destination.parent, ".upload-", ".tmp")
        try {
            Files.write(temporary, bytes, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)
            try {
                Files.move(
                    temporary,
                    destination,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(temporary)
        }
        true
    }

    override suspend fun presignDownload(
        mediaId: UUID,
        objectKey: String,
        expiresAt: Long,
    ): PresignedDownload = PresignedDownload(
        url = "${publicBaseUrl.trimEnd('/')}/v1/media/local-download/$mediaId" +
            "?expiresAt=$expiresAt&token=${downloadSignature(mediaId, objectKey, expiresAt)}",
        expiresAt = expiresAt,
    )

    override suspend fun openDownload(
        mediaId: UUID,
        objectKey: String,
        expiresAt: Long,
        token: String,
    ): LocalDownload? = withContext(Dispatchers.IO) {
        if (System.currentTimeMillis() > expiresAt) return@withContext null
        val expected = downloadSignature(mediaId, objectKey, expiresAt)
        if (!constantTimeEquals(expected, token)) return@withContext null
        val candidate = safePath(objectKey)
        if (!Files.isRegularFile(candidate)) return@withContext null
        val realRoot = root.toRealPath()
        val realPath = candidate.toRealPath()
        if (!realPath.startsWith(realRoot) || !Files.isRegularFile(realPath)) return@withContext null
        LocalDownload(Files.size(realPath), realPath)
    }

    override suspend fun stat(objectKey: String): StoredObject? = withContext(Dispatchers.IO) {
        val path = safePath(objectKey)
        if (!Files.isRegularFile(path)) null else StoredObject(Files.size(path), null)
    }

    override suspend fun delete(objectKey: String) {
        withContext(Dispatchers.IO) { Files.deleteIfExists(safePath(objectKey)) }
    }

    override suspend fun isReady(): Boolean = withContext(Dispatchers.IO) {
        Files.isDirectory(root) && Files.isWritable(root)
    }

    private fun signature(mediaId: UUID, objectKey: String, expiresAt: Long): String =
        hmacSha256Base64Url(signingSecret, "$mediaId\n$objectKey\n$expiresAt")

    private fun downloadSignature(mediaId: UUID, objectKey: String, expiresAt: Long): String =
        hmacSha256Base64Url(signingSecret, "download\n$mediaId\n$objectKey\n$expiresAt")

    private fun safePath(objectKey: String): Path {
        val candidate = root.resolve(objectKey).normalize()
        require(candidate.startsWith(root)) { "Invalid object key" }
        return candidate
    }
}

class S3ObjectStorage(
    private val bucket: String,
    regionName: String,
    endpoint: String?,
    publicEndpoint: String?,
    pathStyle: Boolean,
) : ObjectStorage {
    private val serviceConfiguration = S3Configuration.builder()
        .pathStyleAccessEnabled(pathStyle)
        .build()
    private val client: S3Client
    private val presigner: S3Presigner

    init {
        val region = Region.of(regionName)
        val clientBuilder = S3Client.builder()
            .region(region)
            .serviceConfiguration(serviceConfiguration)
        val presignerBuilder = S3Presigner.builder()
            .region(region)
            .serviceConfiguration(serviceConfiguration)
        endpoint?.takeIf(String::isNotBlank)?.let {
            clientBuilder.endpointOverride(URI.create(it))
        }
        (publicEndpoint ?: endpoint)?.takeIf(String::isNotBlank)?.let {
            presignerBuilder.endpointOverride(URI.create(it))
        }
        client = clientBuilder.build()
        presigner = presignerBuilder.build()
    }

    override suspend fun presignUpload(
        mediaId: UUID,
        objectKey: String,
        contentType: String,
        sizeBytes: Long,
        expiresAt: Long,
    ): PresignedUpload = withContext(Dispatchers.IO) {
        val put = PutObjectRequest.builder()
            .bucket(bucket)
            .key(objectKey)
            .contentType(contentType)
            .contentLength(sizeBytes)
            .build()
        val duration = Duration.ofMillis((expiresAt - System.currentTimeMillis()).coerceAtLeast(1))
        val request = PutObjectPresignRequest.builder()
            .signatureDuration(duration)
            .putObjectRequest(put)
            .build()
        PresignedUpload(
            url = presigner.presignPutObject(request).url().toString(),
            headers = mapOf("Content-Type" to contentType, "Content-Length" to sizeBytes.toString()),
            expiresAt = expiresAt,
        )
    }

    override suspend fun presignDownload(
        mediaId: UUID,
        objectKey: String,
        expiresAt: Long,
    ): PresignedDownload = withContext(Dispatchers.IO) {
        val get = GetObjectRequest.builder()
            .bucket(bucket)
            .key(objectKey)
            .build()
        val duration = Duration.ofMillis((expiresAt - System.currentTimeMillis()).coerceAtLeast(1))
        val request = GetObjectPresignRequest.builder()
            .signatureDuration(duration)
            .getObjectRequest(get)
            .build()
        PresignedDownload(
            url = presigner.presignGetObject(request).url().toString(),
            expiresAt = expiresAt,
        )
    }

    override suspend fun stat(objectKey: String): StoredObject? = withContext(Dispatchers.IO) {
        runCatching {
            val result = client.headObject(
                HeadObjectRequest.builder().bucket(bucket).key(objectKey).build(),
            )
            StoredObject(result.contentLength(), result.checksumSHA256() ?: result.eTag())
        }.getOrNull()
    }

    override suspend fun delete(objectKey: String) {
        withContext(Dispatchers.IO) {
            client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(objectKey).build())
        }
    }

    override suspend fun isReady(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            client.headBucket(HeadBucketRequest.builder().bucket(bucket).build())
            true
        }.getOrDefault(false)
    }

    override fun close() {
        presigner.close()
        client.close()
    }
}

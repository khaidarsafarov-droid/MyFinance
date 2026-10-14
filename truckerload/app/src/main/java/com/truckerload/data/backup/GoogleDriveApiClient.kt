package com.truckerload.data.backup

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import com.truckerload.BuildConfig
import com.truckerload.data.remote.installSecretRedaction
import com.truckerload.utils.LogRedactor
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Google Drive API v3 (appDataFolder) через OkHttp.
 * Файл скрыт в app data пользователя — не засоряет «Мой диск».
 */
class GoogleDriveApiClient(
    private val appContext: Context,
    private val prefs: GoogleDriveBackupPrefs = GoogleDriveBackupPrefs(appContext),
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .installSecretRedaction(BuildConfig.DEBUG)
        .build()

    sealed class DriveError : Exception() {
        data class NeedsUserConsent(val intent: android.content.Intent) : DriveError()
        data class NotSignedIn(override val message: String = "Not signed in to Google Drive") : DriveError()
        data class Api(override val message: String) : DriveError()
    }

    fun lastSignedInAccount(): GoogleSignInAccount? =
        GoogleSignIn.getLastSignedInAccount(appContext)

    suspend fun uploadBackupJson(json: String): Result<Unit> = withContext(Dispatchers.IO) {
        withRetry {
            val token = accessToken()
            val existing = findBackupFileMeta(token)
            val existingId = prefs.driveFileId?.takeIf { it.isNotBlank() } ?: existing?.id
            val fileId = if (existingId != null) {
                updateFileContent(token, existingId, json)
                existingId
            } else {
                createFile(token, json)
            }
            prefs.driveFileId = fileId
            prefs.lastSyncAt = System.currentTimeMillis()
            // Refresh remote modified time after write
            findBackupFileMeta(token)?.modifiedAt?.let { prefs.remoteModifiedAt = it }
                ?: run { prefs.remoteModifiedAt = prefs.lastSyncAt }
        }
    }

    suspend fun downloadBackupJson(): Result<String> = withContext(Dispatchers.IO) {
        withRetry {
            val token = accessToken()
            val meta = findBackupFileMeta(token)
            val fileId = prefs.driveFileId?.takeIf { it.isNotBlank() }
                ?: meta?.id
                ?: throw DriveError.Api("No backup on Google Drive yet")
            prefs.driveFileId = fileId
            meta?.modifiedAt?.let { prefs.remoteModifiedAt = it }
            downloadFile(token, fileId)
        }
    }

    /**
     * Refreshes Drive file metadata. Failure is not the same as "no file":
     * a network error must not be treated as permission to overwrite the remote copy.
     */
    suspend fun probeRemoteBackup(): Result<Boolean> = refreshRemoteBackup()

    suspend fun refreshRemoteBackup(): Result<Boolean> = withContext(Dispatchers.IO) {
        com.truckerload.utils.runCatchingCancellable {
            val token = accessToken()
            val meta = findBackupFileMeta(token)
            if (meta != null) {
                prefs.driveFileId = meta.id
                if (meta.modifiedAt > 0L) prefs.remoteModifiedAt = meta.modifiedAt
            }
            meta != null
        }
    }

    suspend fun hasRemoteBackup(): Boolean = refreshRemoteBackup().getOrDefault(false)

    /** Remote newer than last successful local sync — useful for restore warnings. */
    fun isRemoteNewerThanLastSync(): Boolean =
        DriveSyncPolicy.remoteIsNewer(prefs.remoteModifiedAt, prefs.lastSyncAt)

    private suspend fun <T> withRetry(block: suspend () -> T): Result<T> {
        var last: Throwable? = null
        repeat(DriveSyncPolicy.DEFAULT_MAX_ATTEMPTS) { attempt ->
            try {
                return Result.success(block())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                if (e is DriveError.NeedsUserConsent || e is DriveError.NotSignedIn) {
                    return Result.failure(e)
                }
                last = e
                val retryable = DriveSyncPolicy.isRetryableFailure(e.message)
                if (!retryable || attempt == DriveSyncPolicy.DEFAULT_MAX_ATTEMPTS - 1) {
                    return Result.failure(e)
                }
                delay(DriveSyncPolicy.backoffDelayMs(attempt))
            }
        }
        return Result.failure(last ?: DriveError.Api("Unknown Drive error"))
    }

    private fun accessToken(): String {
        val account = lastSignedInAccount()?.account
            ?: throw DriveError.NotSignedIn()
        return try {
            GoogleAuthUtil.getToken(
                appContext,
                account,
                "oauth2:${GoogleDriveBackupPrefs.DRIVE_APPDATA_SCOPE}",
            )
        } catch (e: UserRecoverableAuthException) {
            val intent = e.intent
                ?: throw DriveError.Api("Drive consent required but no recovery intent")
            throw DriveError.NeedsUserConsent(intent)
        }
    }

    private data class RemoteFileMeta(val id: String, val modifiedAt: Long)

    private fun findBackupFileMeta(token: String): RemoteFileMeta? {
        val q = java.net.URLEncoder.encode(
            "name='${prefs.backupFileName()}' and trashed=false",
            Charsets.UTF_8.name(),
        )
        val url = "$DRIVE_FILES?spaces=appDataFolder&q=$q&fields=files(id,name,modifiedTime)&pageSize=1"
        val request = authorized(Request.Builder().url(url).get(), token).build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw DriveError.Api(LogRedactor.redact("List failed (${response.code}): $body"))
            }
            val files = JSONObject(body).optJSONArray("files") ?: return null
            if (files.length() == 0) return null
            val obj = files.getJSONObject(0)
            return RemoteFileMeta(
                id = obj.getString("id"),
                modifiedAt = parseRfc3339Millis(obj.optString("modifiedTime")),
            )
        }
    }


    private fun createFile(token: String, json: String): String {
        val metadata = JSONObject()
            .put("name", prefs.backupFileName())
            .put("parents", org.json.JSONArray().put("appDataFolder"))
            .toString()
        val boundary = "truckerload_${System.currentTimeMillis()}"
        val related = buildMultipartRelated(boundary, metadata, json)
        val request = authorized(
            Request.Builder()
                .url("$DRIVE_UPLOAD?uploadType=multipart")
                .post(related.toRequestBody("multipart/related; boundary=$boundary".toMediaType())),
            token,
        ).build()
        client.newCall(request).execute().use { response ->
            val respBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                Log.e(TAG, "createFile failed: ${LogRedactor.redact(respBody)}")
                throw DriveError.Api(LogRedactor.redact("Upload failed (${response.code}): $respBody"))
            }
            return JSONObject(respBody).getString("id")
        }
    }

    private fun updateFileContent(token: String, fileId: String, json: String) {
        val request = authorized(
            Request.Builder()
                .url("$DRIVE_UPLOAD/$fileId?uploadType=media")
                .patch(json.toRequestBody("application/json; charset=UTF-8".toMediaType())),
            token,
        ).build()
        client.newCall(request).execute().use { response ->
            val respBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw DriveError.Api(LogRedactor.redact("Update failed (${response.code}): $respBody"))
            }
        }
    }

    private fun downloadFile(token: String, fileId: String): String {
        val request = authorized(
            Request.Builder().url("$DRIVE_FILES/$fileId?alt=media").get(),
            token,
        ).build()
        client.newCall(request).execute().use { response ->
            val respBody = BackupDataCodec.stripBom(response.body?.string().orEmpty())
            if (!response.isSuccessful) {
                throw DriveError.Api("Download failed (${response.code})")
            }
            if (!respBody.trimStart().startsWith("{")) {
                throw DriveError.Api("Remote backup is not valid JSON")
            }
            return respBody
        }
    }

    private fun authorized(builder: Request.Builder, token: String): Request.Builder {
        builder.addHeader("Authorization", "Bearer $token")
        val key = BuildConfig.GOOGLE_DRIVE_API_KEY
        if (key.isNotBlank()) builder.addHeader("X-Goog-Api-Key", key)
        return builder
    }

    companion object {
        private const val TAG = "GoogleDriveApi"
        private const val DRIVE_FILES = "https://www.googleapis.com/drive/v3/files"
        private const val DRIVE_UPLOAD = "https://www.googleapis.com/upload/drive/v3/files"

        fun buildMultipartRelated(boundary: String, metadataJson: String, fileJson: String): String {
            return buildString {
                append("--").append(boundary).append("\r\n")
                append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                append(metadataJson).append("\r\n")
                append("--").append(boundary).append("\r\n")
                append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                append(fileJson).append("\r\n")
                append("--").append(boundary).append("--")
            }
        }

        fun parseRfc3339Millis(value: String?): Long {
            if (value.isNullOrBlank()) return 0L
            return runCatching {
                val formats = arrayOf(
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                    "yyyy-MM-dd'T'HH:mm:ss'Z'",
                )
                for (pattern in formats) {
                    val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                    val parsed = runCatching { sdf.parse(value)?.time }.getOrNull()
                    if (parsed != null) return parsed
                }
                0L
            }.getOrDefault(0L)
        }
    }
}

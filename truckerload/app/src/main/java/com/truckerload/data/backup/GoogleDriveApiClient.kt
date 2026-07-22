package com.truckerload.data.backup

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
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
        .build()

    sealed class DriveError : Exception() {
        data class NeedsUserConsent(val intent: android.content.Intent) : DriveError()
        data class NotSignedIn(override val message: String = "Not signed in to Google Drive") : DriveError()
        data class Api(override val message: String) : DriveError()
    }

    fun lastSignedInAccount(): GoogleSignInAccount? =
        GoogleSignIn.getLastSignedInAccount(appContext)

    suspend fun uploadBackupJson(json: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val token = accessToken()
            val existingId = prefs.driveFileId?.takeIf { it.isNotBlank() } ?: findBackupFileId(token)
            val fileId = if (existingId != null) {
                updateFileContent(token, existingId, json)
                existingId
            } else {
                createFile(token, json)
            }
            prefs.driveFileId = fileId
            prefs.lastSyncAt = System.currentTimeMillis()
        }
    }

    suspend fun downloadBackupJson(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val token = accessToken()
            val fileId = prefs.driveFileId?.takeIf { it.isNotBlank() }
                ?: findBackupFileId(token)
                ?: throw DriveError.Api("No backup on Google Drive yet")
            prefs.driveFileId = fileId
            downloadFile(token, fileId)
        }
    }

    suspend fun hasRemoteBackup(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val token = accessToken()
            val id = prefs.driveFileId?.takeIf { it.isNotBlank() } ?: findBackupFileId(token)
            if (id != null) prefs.driveFileId = id
            id != null
        }.getOrDefault(false)
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

    private fun findBackupFileId(token: String): String? {
        val q = java.net.URLEncoder.encode(
            "name='${GoogleDriveBackupPrefs.BACKUP_FILE_NAME}' and trashed=false",
            Charsets.UTF_8.name(),
        )
        val url = "$DRIVE_FILES?spaces=appDataFolder&q=$q&fields=files(id,name,modifiedTime)&pageSize=1"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw DriveError.Api("List failed (${response.code}): $body")
            }
            val files = JSONObject(body).optJSONArray("files") ?: return null
            if (files.length() == 0) return null
            return files.getJSONObject(0).getString("id")
        }
    }

    private fun createFile(token: String, json: String): String {
        val metadata = JSONObject()
            .put("name", GoogleDriveBackupPrefs.BACKUP_FILE_NAME)
            .put("parents", org.json.JSONArray().put("appDataFolder"))
            .toString()
        val boundary = "truckerload_${System.currentTimeMillis()}"
        val related = buildMultipartRelated(boundary, metadata, json)
        val request = Request.Builder()
            .url("$DRIVE_UPLOAD?uploadType=multipart")
            .addHeader("Authorization", "Bearer $token")
            .post(related.toRequestBody("multipart/related; boundary=$boundary".toMediaType()))
            .build()
        client.newCall(request).execute().use { response ->
            val respBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                Log.e(TAG, "createFile failed: $respBody")
                throw DriveError.Api("Upload failed (${response.code}): $respBody")
            }
            return JSONObject(respBody).getString("id")
        }
    }

    private fun updateFileContent(token: String, fileId: String, json: String) {
        val request = Request.Builder()
            .url("$DRIVE_UPLOAD/$fileId?uploadType=media")
            .addHeader("Authorization", "Bearer $token")
            .patch(json.toRequestBody("application/json; charset=UTF-8".toMediaType()))
            .build()
        client.newCall(request).execute().use { response ->
            val respBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw DriveError.Api("Update failed (${response.code}): $respBody")
            }
        }
    }

    private fun downloadFile(token: String, fileId: String): String {
        val request = Request.Builder()
            .url("$DRIVE_FILES/$fileId?alt=media")
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            val respBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw DriveError.Api("Download failed (${response.code}): $respBody")
            }
            if (!respBody.trimStart().startsWith("{")) {
                throw DriveError.Api("Remote backup is not valid JSON")
            }
            return respBody
        }
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
    }
}

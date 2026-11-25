package com.truckerload.utils

import android.content.Context
import android.net.Uri
import com.truckerload.data.backup.BackupArchive
import com.truckerload.data.backup.BackupData
import com.truckerload.data.backup.BackupDataCodec
import com.truckerload.data.backup.BackupMediaPlacer
import com.truckerload.data.backup.BackupRestoreException
import com.truckerload.data.backup.BackupRestoreParser
import com.truckerload.data.local.AppDatabase
import java.io.File
import java.io.IOException
import java.io.InputStream
import kotlin.coroutines.cancellation.CancellationException

/**
 * Restores a [BackupArchive] zip picked by the user:
 * 1. stream media into a staging folder inside `filesDir` (same disk, so moves are renames);
 * 2. decode `backup.json`;
 * 3. place media into photos/scans and point the records at them;
 * 4. write the journal through [BackupService.restoreDecoded] (account check, snapshot, rollback).
 * When step 4 fails, files created in step 3 are removed. Staging is always cleared.
 */
internal object BackupArchiveRestore {
    private const val STAGING = "restore-staging"

    fun isArchive(context: Context, uri: Uri): Boolean {
        val head = ByteArray(BackupArchive.ZIP_MAGIC.size)
        val read = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input -> readFully(input, head) } ?: 0
        }.getOrDefault(0)
        return read == head.size && BackupArchive.isZip(head)
    }

    suspend fun restore(
        context: Context,
        uri: Uri,
        allowUnboundAccount: Boolean,
        maxJsonBytes: Int,
    ): Result<BackupData> {
        val app = context.applicationContext
        val staging = File(app.filesDir, STAGING)
        staging.deleteRecursively()
        return try {
            val contents = app.contentResolver.openInputStream(uri)?.use { input ->
                BackupArchive.read(input, staging, maxJsonBytes)
            } ?: throw BackupRestoreException.ReadFailed()
            val json = BackupRestoreParser.parseToJson(contents.json.toByteArray(Charsets.UTF_8)).getOrThrow()
            val backup = BackupDataCodec.decode(json)
            val db = AppDatabase.getInstanceForActiveUser(app) ?: error("No active user session")
            val targets = BackupMediaPlacer.Targets(
                photosDir = MediaStorage.photosDir(app),
                scansDir = MediaStorage.scansDir(app),
                presentPhotoIds = db.photoDao().getAllPhotosOnce().idsWithFile { it.id to it.filePath },
                presentScanIds = db.scanDao().getAllScansOnce().idsWithFile { it.id to it.filePath },
            )
            val placement = BackupMediaPlacer.place(contents, backup, targets)
            BackupService.restoreDecoded(app, placement.backup, allowUnboundAccount)
                .onFailure { BackupMediaPlacer.discard(placement.created) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: BackupRestoreException) {
            Result.failure(e)
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: IllegalStateException) {
            Result.failure(e)
        } finally {
            staging.deleteRecursively()
        }
    }

    private fun <T> List<T>.idsWithFile(idAndPath: (T) -> Pair<String, String>): Set<String> =
        map(idAndPath).filter { (_, path) -> File(path).isFile }.map { it.first }.toSet()

    private fun readFully(input: InputStream, buffer: ByteArray): Int {
        var total = 0
        var read = 0
        while (total < buffer.size && read >= 0) {
            read = input.read(buffer, total, buffer.size - total)
            if (read > 0) total += read
        }
        return total
    }
}

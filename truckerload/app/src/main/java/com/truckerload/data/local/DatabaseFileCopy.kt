package com.truckerload.data.local

import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.io.File

/**
 * Safe copy of a SQLite database file plus WAL/SHM/journal sidecars.
 * Marks success only after a non-empty target passes [PRAGMA integrity_check].
 */
object DatabaseFileCopy {
    private const val TAG = "DatabaseFileCopy"
    private val SIDECARS = listOf("-wal", "-shm", "-journal")

    /**
     * Copies [source] → [target] with up to [maxAttempts] tries.
     * On failure deletes a partial [target] (and sidecars) so the next open can retry.
     */
    fun copyWithSidecars(
        source: File,
        target: File,
        maxAttempts: Int = 2,
        retryDelayMs: Long = 500L,
    ): Result<Unit> {
        if (maxAttempts < 1) {
            return Result.failure(IllegalArgumentException("maxAttempts >= 1"))
        }
        if (!source.exists()) {
            deleteDbTree(target)
            return Result.failure(IllegalArgumentException("source DB missing: ${source.path}"))
        }

        var lastError: Throwable? = null
        repeat(maxAttempts) { attempt ->
            deleteDbTree(target)
            val outcome = runCatching {
                source.copyTo(target, overwrite = false)
                SIDECARS.forEach { suffix -> copySidecar(source, target, suffix) }
                verifyCopiedDatabase(target)
            }
            if (outcome.isSuccess) return Result.success(Unit)
            lastError = outcome.exceptionOrNull()
            Log.e(TAG, "DB copy attempt ${attempt + 1}/$maxAttempts failed", lastError)
            deleteDbTree(target)
            if (attempt < maxAttempts - 1 && retryDelayMs > 0L) {
                try {
                    Thread.sleep(retryDelayMs)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }
        }
        return Result.failure(lastError ?: IllegalStateException("DB copy failed"))
    }

    fun deleteDbTree(dbFile: File) {
        if (dbFile.exists()) {
            if (!dbFile.delete()) {
                Log.w(TAG, "Could not delete ${dbFile.path}")
            }
        }
        SIDECARS.forEach { suffix ->
            val side = File(dbFile.path + suffix)
            if (side.exists() && !side.delete()) {
                Log.w(TAG, "Could not delete ${side.path}")
            }
        }
    }

    fun isHealthyDatabase(dbFile: File): Boolean =
        runCatching { verifyCopiedDatabase(dbFile) }.isSuccess

    private fun copySidecar(source: File, target: File, suffix: String) {
        val src = File(source.path + suffix)
        if (!src.exists()) return
        src.copyTo(File(target.path + suffix), overwrite = false)
    }

    private fun verifyCopiedDatabase(target: File) {
        require(target.exists()) { "target DB missing after copy" }
        require(target.length() > 0L) { "empty target DB" }
        SQLiteDatabase.openDatabase(
            target.path,
            null,
            SQLiteDatabase.OPEN_READONLY,
        ).use { db ->
            db.rawQuery("PRAGMA integrity_check", null).use { cursor ->
                require(cursor.moveToFirst()) { "integrity_check returned no rows" }
                val result = cursor.getString(0)
                require(result.equals("ok", ignoreCase = true)) {
                    "integrity_check=$result"
                }
            }
        }
    }
}

package com.truckerload.data.local

import android.content.Context
import android.util.Log
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.truckerload.utils.MediaStorage
import com.truckerload.utils.VerifiedFileCopy
import java.io.File

/**
 * One-time move of photos, scans and local backup copies from app-specific external
 * storage to internal `filesDir`. Safe to run on every start and after a crash:
 *
 * 1. Copy each file to the same relative path under `filesDir` (temp file, fsync,
 *    length + CRC32 check, rename). A file already there with the same bytes counts as done.
 * 2. Point the open journal's photo/scan rows at the new path, only when the new file holds
 *    the same bytes (or the old one is already gone).
 *    Other accounts' journals are rewritten when they open ([rewritePaths] in the Room callback).
 * 3. Delete an old file only after its copy verified and the rows were rewritten.
 *
 * A crash at any step leaves the old file in place, so nothing is lost; the next run continues.
 */
object MediaStorageMigration {
    private const val TAG = "MediaMigration"
    private val TABLES = listOf("photos", "scans")
    private val lock = Any()

    data class Report(val copied: Int, val alreadyThere: Int, val deleted: Int, val kept: Int)

    fun run(context: Context, db: SupportSQLiteDatabase?): Report = synchronized(lock) {
        val legacy = MediaStorage.legacyRoot(context) ?: return Report(0, 0, 0, 0)
        val internal = context.applicationContext.filesDir
        run(legacy, internal, db)
    }

    internal fun run(legacy: File, internal: File, db: SupportSQLiteDatabase?): Report {
        var copied = 0
        var same = 0
        var kept = 0
        val verified = mutableListOf<File>()
        legacyFiles(legacy).forEach { source ->
            val target = File(internal, source.relativeTo(legacy).path)
            val outcome = runCatching {
                when {
                    !target.exists() -> {
                        VerifiedFileCopy.copy(source, target)
                        copied++
                        true
                    }
                    VerifiedFileCopy.sameContent(source, target) -> {
                        same++
                        true
                    }
                    // A different file already holds that name: keep both, never overwrite.
                    else -> false
                }
            }.getOrElse { e ->
                Log.w(TAG, "copy kept the original: ${e.javaClass.simpleName}")
                false
            }
            if (outcome) verified += source else kept++
        }
        val rowsRewritten = db == null || runCatching { rewritePaths(db, legacy, internal) }
            .onFailure { e -> Log.w(TAG, "row rewrite failed: ${e.javaClass.simpleName}") }
            .isSuccess
        var deleted = 0
        if (rowsRewritten) {
            verified.forEach { source ->
                val target = File(internal, source.relativeTo(legacy).path)
                if (VerifiedFileCopy.sameContent(source, target) && source.delete()) deleted++
            }
            pruneEmptyDirs(legacy)
        }
        return Report(copied, same, deleted, kept + (verified.size - deleted))
    }

    /**
     * Points photo/scan rows that still name an old external path at the internal copy.
     * Rows whose internal copy does not exist yet stay as they are. Never throws on open.
     */
    fun rewritePaths(context: Context, db: SupportSQLiteDatabase) {
        val legacy = MediaStorage.legacyRoot(context) ?: return
        runCatching { rewritePaths(db, legacy, context.applicationContext.filesDir) }
            .onFailure { e -> Log.w(TAG, "row rewrite on open failed: ${e.javaClass.simpleName}") }
    }

    internal fun rewritePaths(db: SupportSQLiteDatabase, legacy: File, internal: File): Int {
        val prefix = legacy.absolutePath.trimEnd('/') + "/"
        var updated = 0
        db.beginTransaction()
        try {
            TABLES.forEach { table ->
                verifiedMoves(db, table, prefix, internal).forEach { (id, path) ->
                    db.execSQL("UPDATE $table SET filePath = ? WHERE id = ?", arrayOf<Any>(path, id))
                    updated++
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return updated
    }

    /** Rows of [table] under [prefix] whose internal copy holds the same bytes (or the old file is gone). */
    private fun verifiedMoves(
        db: SupportSQLiteDatabase,
        table: String,
        prefix: String,
        internal: File,
    ): List<Pair<String, String>> {
        val rows = mutableListOf<Pair<String, String>>()
        db.query(
            "SELECT id, filePath FROM $table WHERE substr(filePath, 1, ?) = ?",
            arrayOf<Any>(prefix.length, prefix),
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getString(0)
                val path = cursor.getString(1)
                if (id != null && path != null) rows += id to path
            }
        }
        return rows.mapNotNull { (id, path) ->
            val moved = File(internal, path.removePrefix(prefix))
            val old = File(path)
            val verified = moved.isFile && (!old.exists() || VerifiedFileCopy.sameContent(old, moved))
            if (verified) id to moved.absolutePath else null
        }
    }

    /** Files under the old photos/, scans/ and per-account backups/ folders. Temp leftovers are skipped. */
    private fun legacyFiles(legacy: File): List<File> {
        val roots = listOf(File(legacy, MediaStorage.PHOTOS), File(legacy, MediaStorage.SCANS)) +
            File(legacy, MediaStorage.BACKUPS).listFiles { file -> file.isDirectory }.orEmpty()
        return roots.filter { it.isDirectory }.flatMap { root ->
            root.walkTopDown().filter { it.isFile && !isTemp(it) }.toList()
        }
    }

    private fun isTemp(file: File): Boolean =
        file.name.endsWith(VerifiedFileCopy.TEMP_SUFFIX) || file.name.endsWith(".tmp")

    private fun pruneEmptyDirs(legacy: File) {
        val roots = listOf(MediaStorage.PHOTOS, MediaStorage.SCANS, MediaStorage.BACKUPS).map { File(legacy, it) }
        roots.filter { it.isDirectory }.forEach { root ->
            root.walkBottomUp().filter { it.isDirectory }.forEach { dir ->
                if (dir.listFiles().isNullOrEmpty()) dir.delete()
            }
        }
    }

    /** Old files still waiting to move (tests and diagnostics). */
    internal fun pendingFiles(legacy: File): Int = legacyFiles(legacy).size
}

/** Rewrites old external media paths each time a journal opens (cheap, idempotent). */
internal class MediaPathCallback(private val appContext: Context) : RoomDatabase.Callback() {
    override fun onOpen(db: SupportSQLiteDatabase) {
        MediaStorageMigration.rewritePaths(appContext, db)
    }
}

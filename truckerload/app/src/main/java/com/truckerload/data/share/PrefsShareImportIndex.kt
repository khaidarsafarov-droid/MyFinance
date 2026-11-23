package com.truckerload.data.share

import android.content.Context
import androidx.core.content.edit
import com.truckerload.data.preferences.AccountIds
import com.truckerload.domain.share.ShareImportIndex
import com.truckerload.domain.share.ShareImportKeys

/**
 * Per-profile record of shares that already became loads.
 * One row is load id, load number, broker, and file hash, separated by a unit separator.
 */
class PrefsShareImportIndex(
    context: Context,
    userId: String,
) : ShareImportIndex {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val key = "rows_${AccountIds.sanitizeFilePart(userId)}"

    override fun findByHash(fileHash: String): String? {
        val hash = fileHash.trim()
        if (hash.isBlank()) return null
        return rows().firstOrNull { it.fileHash == hash }?.loadId
    }

    override fun findByIdentity(loadNumber: String, broker: String): String? =
        rows().firstOrNull {
            ShareImportKeys.sameIdentity(it.loadNumber, it.broker, loadNumber, broker)
        }?.loadId

    override fun remember(loadId: String, loadNumber: String, broker: String, fileHash: String) {
        val id = loadId.trim()
        if (id.isBlank()) return
        val hash = fileHash.trim()
        val next = rows().filterNot { hash.isNotBlank() && it.fileHash == hash }.toMutableList()
        next += Row(
            loadId = id,
            loadNumber = ShareImportKeys.number(loadNumber),
            broker = ShareImportKeys.broker(broker),
            fileHash = hash,
        )
        write(next.takeLast(MAX_ROWS))
    }

    override fun forget(loadId: String) {
        val id = loadId.trim()
        write(rows().filterNot { it.loadId == id })
    }

    private fun rows(): List<Row> =
        prefs.getStringSet(key, emptySet()).orEmpty().mapNotNull { Row.parse(it) }

    private fun write(rows: List<Row>) {
        prefs.edit(commit = true) {
            putStringSet(key, rows.map { it.encode() }.toHashSet())
        }
    }

    private data class Row(
        val loadId: String,
        val loadNumber: String,
        val broker: String,
        val fileHash: String,
    ) {
        fun encode(): String = listOf(loadId, loadNumber, broker, fileHash).joinToString(SEP)

        companion object {
            fun parse(raw: String): Row? {
                val parts = raw.split(SEP)
                if (parts.size != 4 || parts[0].isBlank()) return null
                return Row(parts[0], parts[1], parts[2], parts[3])
            }
        }
    }

    private companion object {
        const val PREFS = "truckerload_share_import"
        const val SEP = "\u001f"
        const val MAX_ROWS = 200
    }
}

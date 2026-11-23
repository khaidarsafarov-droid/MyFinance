package com.truckerload.domain.share

import java.util.Locale

/**
 * Remembers shares that already became loads.
 * Identity is load number + broker. A blank broker matches only another blank broker.
 */
interface ShareImportIndex {
    fun findByHash(fileHash: String): String?
    fun findByIdentity(loadNumber: String, broker: String): String?
    fun remember(loadId: String, loadNumber: String, broker: String, fileHash: String)
    fun forget(loadId: String)
}

object ShareImportKeys {
    fun number(raw: String): String = raw.trim().uppercase(Locale.US)

    fun broker(raw: String): String =
        raw.trim().replace(Regex("""\s+"""), " ").lowercase(Locale.US)

    fun sameIdentity(
        leftNumber: String,
        leftBroker: String,
        rightNumber: String,
        rightBroker: String,
    ): Boolean {
        val left = number(leftNumber)
        val right = number(rightNumber)
        if (left.isBlank() || right.isBlank() || left != right) return false
        return broker(leftBroker) == broker(rightBroker)
    }
}

class MemoryShareImportIndex : ShareImportIndex {
    private data class Row(
        val loadId: String,
        val loadNumber: String,
        val broker: String,
        val fileHash: String,
    )

    private val rows = mutableListOf<Row>()

    override fun findByHash(fileHash: String): String? {
        val hash = fileHash.trim()
        if (hash.isBlank()) return null
        return rows.firstOrNull { it.fileHash == hash }?.loadId
    }

    override fun findByIdentity(loadNumber: String, broker: String): String? =
        rows.firstOrNull {
            ShareImportKeys.sameIdentity(it.loadNumber, it.broker, loadNumber, broker)
        }?.loadId

    override fun remember(loadId: String, loadNumber: String, broker: String, fileHash: String) {
        val id = loadId.trim()
        if (id.isBlank()) return
        val hash = fileHash.trim()
        if (hash.isNotBlank()) rows.removeAll { it.fileHash == hash }
        rows += Row(
            loadId = id,
            loadNumber = ShareImportKeys.number(loadNumber),
            broker = ShareImportKeys.broker(broker),
            fileHash = hash,
        )
    }

    override fun forget(loadId: String) {
        rows.removeAll { it.loadId == loadId.trim() }
    }
}

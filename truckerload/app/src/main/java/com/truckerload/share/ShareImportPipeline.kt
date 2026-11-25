package com.truckerload.share

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.core.content.FileProvider
import com.truckerload.data.local.DeletedLoadLedger
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.ScanRepository
import com.truckerload.domain.model.ScanDocumentCategory
import com.truckerload.domain.parser.LoadField
import com.truckerload.domain.share.ShareImportDecision
import com.truckerload.domain.share.ShareImportDecider
import com.truckerload.domain.share.ShareImportIndex
import com.truckerload.domain.share.SharePayloadPolicy
import com.truckerload.domain.share.ShareTextBundle
import com.truckerload.utils.AttachmentNaming
import com.truckerload.utils.LoadDocumentTextExtractor
import com.truckerload.utils.MediaStorage
import java.io.File
import java.io.IOException

internal data class ShareAddedLoad(
    val loadId: String,
    val tripId: String,
    val missing: List<LoadField>,
)

internal sealed class ShareOutcome {
    data class Added(val loads: List<ShareAddedLoad>) : ShareOutcome()
    data class Duplicate(val loadId: String) : ShareOutcome()
    data object Rejected : ShareOutcome()
    data object TooLarge : ShareOutcome()
    data object Unreadable : ShareOutcome()
}

/**
 * Copies the share, scores it, and either inserts a load or refuses it.
 * Text extraction reuses [LoadDocumentTextExtractor] (PDF text layer, then on-device OCR).
 */
internal class ShareImportPipeline(
    private val context: Context,
    private val loadRepository: LoadRepository,
    private val scanRepository: ScanRepository,
    private val index: ShareImportIndex,
    private val decider: ShareImportDecider = ShareImportDecider(),
    private val extractor: LoadDocumentTextExtractor = LoadDocumentTextExtractor(context),
) {
    private val inbox = ShareInbox(context)

    suspend fun run(inbound: ShareInbound): ShareOutcome {
        inbox.prepare()
        val pieces = collect(inbound)
        if (pieces.items.isEmpty()) {
            return if (pieces.sawTooLarge) ShareOutcome.TooLarge else ShareOutcome.Unreadable
        }
        val decided = pieces.items.map { piece ->
            piece to resolve(
                decider.decide(
                    text = piece.text,
                    fileName = piece.label,
                    fileHash = piece.hash,
                    index = index,
                ),
            )
        }
        val accepted = decided.mapNotNull { (piece, decision) ->
            (decision as? ShareImportDecision.Add)?.let { piece to it }
        }
        if (accepted.isNotEmpty()) {
            return ShareOutcome.Added(saveGroups(accepted))
        }
        if (pieces.items.size > 1) {
            val combined = combinedDecision(pieces.items)
            if (combined is ShareImportDecision.Add) {
                val saved = save(combined, pieces.items.mapNotNull { it.file }, pieces.items.map { it.hash })
                return ShareOutcome.Added(listOf(saved))
            }
            if (combined is ShareImportDecision.Duplicate) {
                return ShareOutcome.Duplicate(combined.existingLoadId)
            }
        }
        val duplicate = decided.firstNotNullOfOrNull { it.second as? ShareImportDecision.Duplicate }
        if (duplicate != null) return ShareOutcome.Duplicate(duplicate.existingLoadId)
        return ShareOutcome.Rejected
    }

    private suspend fun collect(inbound: ShareInbound): Collected {
        if (inbound.streams.isEmpty()) {
            val text = inbound.text.trim()
            if (text.isBlank()) return Collected(emptyList(), false)
            val hash = SharePayloadPolicy.sha256(text.toByteArray(Charsets.UTF_8))
            return Collected(
                listOf(Piece(text = text, file = null, hash = hash, mime = "text/plain", label = "shared.txt")),
                sawTooLarge = false,
            )
        }
        var sawTooLarge = false
        val items = mutableListOf<Piece>()
        inbound.streams.forEachIndexed { index, uri ->
            when (val copy = inbox.copy(uri, index, inbound.mimeType)) {
                is ShareCopy.Ok -> {
                    val extracted = readCopied(copy)
                    items += Piece(
                        text = merge(inbound.text, extracted),
                        file = copy.file,
                        hash = copy.hash,
                        mime = copy.mime,
                        label = copy.label,
                    )
                }
                ShareCopy.TooLarge -> sawTooLarge = true
                ShareCopy.Rejected -> Unit
            }
        }
        return Collected(items, sawTooLarge)
    }

    private suspend fun readCopied(copy: ShareCopy.Ok): String {
        if (copy.mime == "text/plain") {
            return runCatching { copy.file.readText().take(ShareTextBundle.BODY_CAP) }.getOrDefault("")
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            copy.file,
        )
        return extractor.extract(uri, copy.mime).getOrDefault("").take(ShareTextBundle.BODY_CAP)
    }

    private suspend fun combinedDecision(items: List<Piece>): ShareImportDecision {
        val text = items.joinToString("\n\n") { it.text }
        val hash = SharePayloadPolicy.sha256(
            items.joinToString("|") { it.hash }.toByteArray(Charsets.UTF_8),
        )
        return resolve(
            decider.decide(
                text = text,
                fileName = items.firstOrNull()?.label,
                fileHash = hash,
                index = index,
            ),
        )
    }

    private suspend fun resolve(decision: ShareImportDecision): ShareImportDecision {
        if (decision !is ShareImportDecision.Add) return decision
        val trip = decision.load.tripId.trim()
        if (trip.isBlank() || trip.startsWith("M-")) return decision
        val existing = loadRepository.getByTripId(trip) ?: return decision
        return ShareImportDecision.Duplicate(existing.id)
    }

    private suspend fun saveGroups(accepted: List<Pair<Piece, ShareImportDecision.Add>>): List<ShareAddedLoad> {
        val numbers = accepted.map { it.second.loadNumber }.filter { it.isNotBlank() }.distinct()
        if (numbers.size <= 1) {
            val best = accepted.minBy { it.second.missing.size }
            return listOf(
                save(
                    decision = best.second,
                    files = accepted.mapNotNull { it.first.file },
                    hashes = accepted.map { it.first.hash },
                ),
            )
        }
        return accepted.groupBy { it.second.loadNumber }.map { (_, group) ->
            val best = group.minBy { it.second.missing.size }
            save(best.second, group.mapNotNull { it.first.file }, group.map { it.first.hash })
        }
    }

    private suspend fun save(
        decision: ShareImportDecision.Add,
        files: List<File>,
        hashes: List<String>,
    ): ShareAddedLoad {
        val draft = decision.load
        DeletedLoadLedger.allowAgain(context, draft.id, draft.tripId)
        loadRepository.insertLoad(draft)
        val saved = loadRepository.getLoadById(draft.id)
            ?: loadRepository.getByTripId(draft.tripId)
            ?: error("share_insert_missing")
        val hashesToRemember = (hashes + decision.fileHash).filter { it.isNotBlank() }.distinct()
        hashesToRemember.forEach { hash ->
            index.remember(saved.id, decision.loadNumber, decision.broker, hash)
        }
        val attachments = files.ifEmpty { listOf(writeText(draft.rawMessage)) }
        attachments.forEachIndexed { index, file ->
            val stored = copyToScans(saved.tripId, saved.date, file, index, extensionOf(file))
            scanRepository.saveScan(
                fileName = stored.name,
                filePath = stored.absolutePath,
                timestamp = System.currentTimeMillis(),
                fileSizeBytes = stored.length(),
                pageCount = pageCount(stored),
                ocrText = draft.rawMessage.take(OCR_CAP),
                loadId = saved.id,
                category = ScanDocumentCategory.LOAD.name,
            )
            if (file.absolutePath.startsWith(context.cacheDir.absolutePath)) file.delete()
        }
        return ShareAddedLoad(saved.id, saved.tripId, decision.missing)
    }

    private fun writeText(text: String): File {
        val file = File(context.cacheDir, "share-inbox/note.txt")
        file.parentFile?.mkdirs()
        file.writeText(text)
        return file
    }

    private fun copyToScans(tripId: String, date: String, source: File, index: Int, extension: String): File {
        val scans = MediaStorage.scansDir(context)
        val name = uniqueName(scans, tripId, date, index, extension)
        val dest = File(scans, name)
        source.copyTo(dest, overwrite = true)
        return dest
    }

    private fun uniqueName(dir: File, tripId: String, date: String, index: Int, extension: String): String {
        val stamped = AttachmentNaming.buildFileName(tripId, date, System.currentTimeMillis(), extension)
        if (!File(dir, stamped).exists()) return stamped
        val dot = stamped.lastIndexOf('.')
        val base = if (dot > 0) stamped.substring(0, dot) else stamped
        val ext = if (dot > 0) stamped.substring(dot) else ""
        return "${base}_$index$ext"
    }

    private fun extensionOf(file: File): String {
        val ext = file.extension.lowercase()
        if (ext.isNotBlank() && ext != "bin") return ext
        return "txt"
    }

    private fun pageCount(file: File): Int {
        if (!file.name.endsWith(".pdf", ignoreCase = true)) return 1
        val descriptor = runCatching {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        }.getOrNull() ?: return 1
        return try {
            PdfRenderer(descriptor).use { renderer -> renderer.pageCount.coerceAtLeast(1) }
        } catch (error: IOException) {
            runCatching { descriptor.close() }
            1
        }
    }

    private fun merge(shared: String, extracted: String): String {
        if (shared.isBlank()) return extracted
        if (extracted.isBlank()) return shared
        if (extracted.contains(shared.take(80))) return extracted
        return shared + "\n" + extracted
    }

    private data class Piece(
        val text: String,
        val file: File?,
        val hash: String,
        val mime: String,
        val label: String,
    )

    private data class Collected(val items: List<Piece>, val sawTooLarge: Boolean)

    private companion object {
        const val OCR_CAP = 100_000
    }
}

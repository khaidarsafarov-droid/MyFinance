package com.truckerload.data.backup

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonIOException
import com.google.gson.JsonSyntaxException
import java.nio.charset.StandardCharsets

/** Pure JSON codec for [BackupData] — unit-testable without Room/Android. */
object BackupDataCodec {
    val gson: Gson = GsonBuilder().disableHtmlEscaping().create()

    private val utf8Bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())

    fun toJson(data: BackupData): String {
        val normalized = data.copy(
            schemaVersion = BackupSchema.CURRENT,
            version = BackupSchema.CURRENT,
        )
        return gson.toJson(normalized)
    }

    fun toUtf8Bytes(data: BackupData): ByteArray =
        toJson(data).toByteArray(StandardCharsets.UTF_8)

    fun fromJson(json: String): BackupData? =
        runCatching { decode(json) }.getOrNull()

    fun decode(json: String): BackupData {
        val stripped = stripBom(json).trim()
        if (stripped.isEmpty()) throw BackupRestoreException.InvalidFormat()
        val parsed = try {
            gson.fromJson(stripped, BackupData::class.java)
        } catch (_: JsonSyntaxException) {
            throw BackupRestoreException.Corrupted()
        } catch (_: JsonIOException) {
            throw BackupRestoreException.Corrupted()
        } catch (_: RuntimeException) {
            throw BackupRestoreException.Corrupted()
        } ?: throw BackupRestoreException.Corrupted()

        val schema = resolveSchemaVersion(parsed)
        if (schema > BackupSchema.CURRENT) {
            throw BackupRestoreException.SchemaTooNew(schema)
        }
        return parsed.copy(schemaVersion = schema, version = schema)
    }

    fun resolveSchemaVersion(data: BackupData): Int = when {
        data.schemaVersion > 0 -> data.schemaVersion
        data.version > 0 -> data.version
        else -> BackupSchema.V1
    }

    fun stripBom(text: String): String = text.removePrefix("\uFEFF")

    fun stripUtf8Bom(bytes: ByteArray): ByteArray {
        if (bytes.size >= 3 &&
            bytes[0] == utf8Bom[0] &&
            bytes[1] == utf8Bom[1] &&
            bytes[2] == utf8Bom[2]
        ) {
            return bytes.copyOfRange(3, bytes.size)
        }
        return bytes
    }
}

package com.truckerload.data.sync

import android.content.Context
import com.truckerload.data.preferences.AccountIds
import java.io.File

/**
 * Local file mirror of the account cloud database.
 *
 * Until Supabase/Firestore tables ship, this file is the durable account blob that
 * survives app reinstall when paired with Drive backup of the same JSON.
 * Cross-device restore on a new phone still needs Drive/server; the mirror
 * enables same-device wipe/relogin hydration and unit-testable sync flows.
 */
class AccountCloudMirror(context: Context) {
    private val root: File =
        File(context.applicationContext.filesDir, "cloud_account_mirror").apply { mkdirs() }

    fun read(accountId: String): AccountCloudSnapshot? {
        val file = fileFor(accountId)
        if (!file.exists()) return null
        return AccountCloudSnapshotCodec.fromJson(file.readText(Charsets.UTF_8))
    }

    fun write(snapshot: AccountCloudSnapshot) {
        val file = fileFor(snapshot.accountId)
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, "${file.name}.tmp")
        tmp.writeText(AccountCloudSnapshotCodec.toJson(snapshot), Charsets.UTF_8)
        if (!tmp.renameTo(file)) {
            file.writeText(AccountCloudSnapshotCodec.toJson(snapshot), Charsets.UTF_8)
            tmp.delete()
        }
    }

    fun exists(accountId: String): Boolean = fileFor(accountId).exists()

    fun delete(accountId: String) {
        fileFor(accountId).delete()
    }

    private fun fileFor(accountId: String): File {
        val safe = AccountIds.sanitizeFilePart(accountId.trim())
        return File(root, "$safe.json")
    }
}

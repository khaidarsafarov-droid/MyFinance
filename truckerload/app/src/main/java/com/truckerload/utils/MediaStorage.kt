package com.truckerload.utils

import android.content.Context
import java.io.File

/**
 * Photos, scans and the local backup copies live in app-private internal storage
 * (`filesDir`). Other apps cannot read it, and Android removes it with the app.
 * Auto Backup is off for every domain (see `data_extraction_rules.xml`).
 *
 * Older builds kept the same trees in app-specific external storage
 * ([legacyRoot]). [com.truckerload.data.local.MediaStorageMigration] moves them.
 */
object MediaStorage {
    const val PHOTOS = "photos"
    const val SCANS = "scans"
    const val BACKUPS = "backups"

    fun photosDir(context: Context): File = ensureDir(context, PHOTOS)

    fun scansDir(context: Context): File = ensureDir(context, SCANS)

    fun backupsRoot(context: Context): File = File(context.applicationContext.filesDir, BACKUPS)

    /** Where older builds kept the same folders. Null when external storage is unavailable. */
    fun legacyRoot(context: Context): File? {
        val app = context.applicationContext
        return runCatching { app.getExternalFilesDir(null) }.getOrNull()
    }

    fun legacyBackupsRoot(context: Context): File? = legacyRoot(context)?.let { File(it, BACKUPS) }

    private fun ensureDir(context: Context, name: String): File {
        val dir = File(context.applicationContext.filesDir, name)
        dir.mkdirs()
        return dir
    }
}

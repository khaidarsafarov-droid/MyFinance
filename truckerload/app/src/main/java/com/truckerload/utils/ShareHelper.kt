package com.truckerload.utils

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

class ShareHelper(private val context: Context) {

    fun sharePhoto(file: File) {
        shareFile(file, "image/jpeg", com.truckerload.R.string.share_photo)
    }

    fun sharePdf(file: File) {
        shareFile(file, "application/pdf", com.truckerload.R.string.send_to)
    }

    fun sharePhotos(files: List<File>) {
        shareFiles(files, "image/jpeg", com.truckerload.R.string.share_photos_batch)
    }

    fun sharePdfs(files: List<File>) {
        shareFiles(files, "application/pdf", com.truckerload.R.string.send_to)
    }

    private fun shareFile(file: File, mimeType: String, chooserTitleRes: Int) {
        shareFiles(listOf(file), mimeType, chooserTitleRes)
    }

    private fun shareFiles(files: List<File>, mimeType: String, chooserTitleRes: Int) {
        val existing = files.filter { it.exists() }
        if (existing.isEmpty()) return
        val uris = existing.map { file ->
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
        }
        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uris.first())
                clipData = ClipData.newUri(context.contentResolver, "share", uris.first())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = mimeType
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                clipData = clipDataForUris(uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        context.startActivity(
            Intent.createChooser(intent, context.getString(chooserTitleRes)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }

    private fun clipDataForUris(uris: List<Uri>): ClipData {
        val clip = ClipData.newUri(context.contentResolver, "share", uris.first())
        for (i in 1 until uris.size) {
            clip.addItem(ClipData.Item(uris[i]))
        }
        return clip
    }
}

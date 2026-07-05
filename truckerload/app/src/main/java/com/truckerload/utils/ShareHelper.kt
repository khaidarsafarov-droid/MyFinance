package com.truckerload.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

class ShareHelper(private val context: Context) {

    fun sharePhoto(file: File) {
        shareFile(file, "image/jpeg", com.truckerload.R.string.share_photo)
    }

    fun sharePdf(file: File) {
        shareFile(file, "application/pdf", com.truckerload.R.string.send_to)
    }

    private fun shareFile(file: File, mimeType: String, chooserTitleRes: Int) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, context.getString(chooserTitleRes)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }
}

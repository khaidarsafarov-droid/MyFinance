package com.truckerload.presentation.screens.gallery

import android.content.Context
import com.truckerload.utils.ShareHelper
import java.io.File

/** Shares one file or a multi-selection. [ShareHelper] grants read access through FileProvider. */
fun shareGalleryFiles(context: Context, kind: GalleryKind, paths: List<String>) {
    val files = paths.map { File(it) }.filter { it.isFile }
    if (files.isEmpty()) return
    val helper = ShareHelper(context)
    when (kind) {
        GalleryKind.PHOTOS -> if (files.size == 1) helper.sharePhoto(files.first()) else helper.sharePhotos(files)
        GalleryKind.SCANS -> if (files.size == 1) helper.sharePdf(files.first()) else helper.sharePdfs(files)
    }
}

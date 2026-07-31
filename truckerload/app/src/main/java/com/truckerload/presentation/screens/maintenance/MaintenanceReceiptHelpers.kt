package com.truckerload.presentation.screens.maintenance

import java.io.File

internal fun createTempReceiptFile(cacheDir: File): File {
    val dir = File(cacheDir, "maintenance").apply { mkdirs() }
    return File(dir, "capture_${System.currentTimeMillis()}.jpg")
}

package com.truckerload.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning

/**
 * ML Kit Document Scanner wrapper.
 *
 * Limitations (2026):
 * - Dependency 16.0.0-beta1 requires Google Play Services on a real device.
 * - Emulator without GMS will not work; use a physical device for QA.
 * - API 23+; up to 20 pages per session (configured in options).
 * - OCR language is handled separately in [OCRService] (ML Kit Latin + Tesseract rus+eng).
 */
class DocumentScannerService(private val context: Context) {

    fun createScanner(): GmsDocumentScanner {
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(20)
            .setResultFormats(
                GmsDocumentScannerOptions.RESULT_FORMAT_PDF,
                GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
            )
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
        return GmsDocumentScanning.getClient(options)
    }

    companion object {
        fun isAvailable(context: Context): Boolean {
            return GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
        }

        fun openPlayServicesUpdate(context: Context) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = "market://details?id=com.google.android.gms".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(intent) }
        }
    }
}

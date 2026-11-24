package com.truckerload.utils

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

/**
 * PdfBox-Android reads its glyph lists, CMaps and standard fonts from app assets.
 * The Application calls [init] once; plain JVM tests fall back to the classpath.
 */
object PdfBox {
    fun init(context: Context) {
        if (!PDFBoxResourceLoader.isReady()) PDFBoxResourceLoader.init(context.applicationContext ?: context)
    }
}

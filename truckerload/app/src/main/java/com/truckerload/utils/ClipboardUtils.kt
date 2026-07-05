package com.truckerload.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import com.truckerload.R

object ClipboardUtils {

    fun copyTextWithToast(context: Context, text: String, label: String = "ocr_text") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, context.getString(R.string.text_copied), Toast.LENGTH_SHORT).show()
    }
}

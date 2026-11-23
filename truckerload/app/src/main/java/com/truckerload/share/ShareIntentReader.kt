package com.truckerload.share

import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat
import com.truckerload.domain.share.SharePayloadPolicy
import com.truckerload.domain.share.ShareTextBundle

internal data class ShareInbound(
    val text: String,
    val streams: List<Uri>,
    val mimeType: String?,
)

/**
 * Reads ACTION_SEND and ACTION_SEND_MULTIPLE without trusting a single extra.
 * Senders put the URI in EXTRA_STREAM, in ClipData, or in both.
 */
internal object ShareIntentReader {

    fun read(intent: Intent?): ShareInbound? {
        if (intent == null) return null
        val action = intent.action
        if (action != Intent.ACTION_SEND && action != Intent.ACTION_SEND_MULTIPLE) return null
        val clipText = clipText(intent)
        val text = ShareTextBundle.combine(
            subject = intent.getStringExtra(Intent.EXTRA_SUBJECT),
            extraText = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString(),
            clipText = clipText,
        )
        return ShareInbound(
            text = text,
            streams = uris(intent, action),
            mimeType = intent.type,
        )
    }

    private fun uris(intent: Intent, action: String): List<Uri> {
        val fromExtra = if (action == Intent.ACTION_SEND) {
            listOfNotNull(IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java))
        } else {
            IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                .orEmpty()
        }
        val clip = intent.clipData
        val fromClip = if (clip == null) {
            emptyList()
        } else {
            buildList {
                for (index in 0 until clip.itemCount) {
                    clip.getItemAt(index)?.uri?.let { add(it) }
                }
            }
        }
        return (fromExtra + fromClip)
            .filter { it.scheme.equals("content", ignoreCase = true) }
            .distinctBy { it.toString() }
            .take(SharePayloadPolicy.MAX_FILES)
    }

    private fun clipText(intent: Intent): String? {
        val clip = intent.clipData ?: return null
        for (index in 0 until clip.itemCount) {
            val text = clip.getItemAt(index)?.text?.toString()?.trim().orEmpty()
            if (text.isNotBlank()) return text
        }
        return null
    }
}

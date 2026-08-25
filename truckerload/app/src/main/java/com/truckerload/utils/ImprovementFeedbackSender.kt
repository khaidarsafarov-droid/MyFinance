package com.truckerload.utils

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.truckerload.R
import com.truckerload.domain.feedback.ImprovementFeedbackMail

enum class ImprovementFeedbackSendResult {
    OPENED_EMAIL,
    COPIED_FALLBACK,
}

object ImprovementFeedbackSender {

    fun send(context: Context, draft: ImprovementFeedbackMail.Draft): ImprovementFeedbackSendResult {
        val mailto = Intent(Intent.ACTION_SENDTO, Uri.parse(ImprovementFeedbackMail.mailtoUriString(draft))).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(draft.to))
            putExtra(Intent.EXTRA_SUBJECT, draft.subject)
            putExtra(Intent.EXTRA_TEXT, draft.body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (mailto.resolveActivity(context.packageManager) != null) {
            context.startActivity(mailto)
            return ImprovementFeedbackSendResult.OPENED_EMAIL
        }
        val chooser = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(draft.to))
            putExtra(Intent.EXTRA_SUBJECT, draft.subject)
            putExtra(Intent.EXTRA_TEXT, draft.body)
        }
        return try {
            context.startActivity(
                Intent.createChooser(chooser, context.getString(R.string.improve_send)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
            ImprovementFeedbackSendResult.OPENED_EMAIL
        } catch (_: ActivityNotFoundException) {
            copyDraft(context, draft)
            ImprovementFeedbackSendResult.COPIED_FALLBACK
        }
    }

    private fun copyDraft(context: Context, draft: ImprovementFeedbackMail.Draft) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = "${draft.to}\n${draft.subject}\n\n${draft.body}"
        clipboard.setPrimaryClip(ClipData.newPlainText(BrandConstants.DISPLAY_NAME, text))
    }
}

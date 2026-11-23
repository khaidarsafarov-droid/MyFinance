package com.truckerload.domain.share

/**
 * Joins the text pieces a share sheet may split across subject, body, and clip data.
 * The subject is kept because mail apps often put "Rate confirmation" there and the
 * PDF only in [android.content.Intent.EXTRA_STREAM].
 */
object ShareTextBundle {

    fun combine(subject: String?, extraText: String?, clipText: String?): String {
        val body = extraText?.trim().orEmpty().ifBlank { clipText?.trim().orEmpty() }
        val subjectLine = subject?.trim().orEmpty()
        return buildString {
            if (subjectLine.isNotBlank()) {
                append("Subject: ")
                append(subjectLine.take(SUBJECT_CAP))
                append('\n')
            }
            append(body.take(BODY_CAP))
        }.trim()
    }

    const val BODY_CAP = 500_000
    private const val SUBJECT_CAP = 300
}

package com.truckerload.domain.feedback

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** Builds the support email the driver sends from in-app improvement feedback. */
object ImprovementFeedbackMail {
    const val SUPPORT_EMAIL = "Truckerlogsupport@gmail.com"

    enum class Topic {
        ADD,
        IMPROVE,
        WORKS,
        BROKEN,
    }

    data class Draft(
        val to: String,
        val subject: String,
        val body: String,
    )

    fun compose(
        topic: Topic,
        message: String,
        topicLabel: String,
        appVersion: String,
        androidRelease: String,
    ): Draft? {
        val text = message.trim()
        if (text.isEmpty()) return null
        val subject = "[Truck Log] $topicLabel"
        val body = buildString {
            appendLine("Тема: $topicLabel")
            appendLine()
            appendLine(text)
            appendLine()
            appendLine("—")
            appendLine("Truck Log $appVersion")
            appendLine("Android $androidRelease")
        }.trimEnd()
        return Draft(to = SUPPORT_EMAIL, subject = subject, body = body)
    }

    fun mailtoUriString(draft: Draft): String {
        val subject = encode(draft.subject)
        val body = encode(draft.body)
        return "mailto:${draft.to}?subject=$subject&body=$body"
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")
}

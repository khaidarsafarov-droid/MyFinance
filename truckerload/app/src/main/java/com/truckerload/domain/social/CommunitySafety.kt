package com.truckerload.domain.social

enum class CommunityReportReason(val rpcValue: String) {
    SPAM("spam"),
    HARASSMENT("harassment"),
    HATE("hate"),
    SEXUAL("sexual"),
    SCAM("scam"),
    OTHER("other"),
}

object ModerationCodes {
    const val EMPTY = "empty"
    const val TOO_LONG = "too_long"
    const val SPAM = "spam"
    const val LINK = "link"
    const val PII_EMAIL = "pii_email"
    const val PII_PHONE = "pii_phone"
    const val OFF_APP = "off_app"
}

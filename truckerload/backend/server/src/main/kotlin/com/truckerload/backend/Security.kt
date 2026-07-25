package com.truckerload.backend

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private val secureRandom = SecureRandom()

fun randomUrlToken(byteCount: Int = 32): String =
    ByteArray(byteCount).also(secureRandom::nextBytes).let {
        Base64.getUrlEncoder().withoutPadding().encodeToString(it)
    }

fun sha256(value: String): ByteArray =
    MessageDigest.getInstance("SHA-256").digest(value.toByteArray(StandardCharsets.UTF_8))

fun sha256Hex(value: String): String =
    sha256(value).joinToString("") { "%02x".format(it) }

fun hmacSha256Base64Url(secret: String, value: String): String {
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
    return Base64.getUrlEncoder().withoutPadding()
        .encodeToString(mac.doFinal(value.toByteArray(StandardCharsets.UTF_8)))
}

fun constantTimeEquals(expected: String, actual: String?): Boolean {
    if (actual == null) return false
    return MessageDigest.isEqual(
        expected.toByteArray(StandardCharsets.UTF_8),
        actual.toByteArray(StandardCharsets.UTF_8),
    )
}

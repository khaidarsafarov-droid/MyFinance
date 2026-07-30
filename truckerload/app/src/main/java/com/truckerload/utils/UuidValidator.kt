package com.truckerload.utils

/**
 * Validates identifiers before interpolating them into PostgREST filter URLs.
 */
object UuidValidator {
    private val UUID_REGEX = Regex(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
    )
    /** Local/dev account ids and UUIDs — rejects PostgREST filter metacharacters. */
    private val SAFE_FILTER_ID = Regex("^[A-Za-z0-9_.:-]{1,128}$")

    fun isUuid(value: String): Boolean = UUID_REGEX.matches(value.trim())

    /** Returns trimmed UUID or null when malformed. */
    fun sanitizeOrNull(value: String?): String? {
        val trimmed = value?.trim().orEmpty()
        return trimmed.takeIf { isUuid(it) }
    }

    fun requireUuid(value: String, label: String = "id"): String =
        sanitizeOrNull(value) ?: throw IllegalArgumentException("invalid $label")

    /**
     * Sanitizes a filter id for PostgREST `col=eq.value` usage.
     * Accepts UUIDs and safe local account ids; rejects `&`, `=`, commas, spaces.
     */
    fun sanitizeFilterIdOrNull(value: String?): String? {
        val trimmed = value?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        if (isUuid(trimmed)) return trimmed
        return trimmed.takeIf { SAFE_FILTER_ID.matches(it) }
    }

    fun requireFilterId(value: String, label: String = "id"): String =
        sanitizeFilterIdOrNull(value) ?: throw IllegalArgumentException("invalid $label")
}

package com.truckerload.domain.model

/** Canonical Trip ID for duplicate detection (import + CDC). */
fun normalizeTripId(raw: String): String = raw.trim().uppercase()

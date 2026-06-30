package com.truckerload.data.preferences

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        fun fromOrdinal(ordinal: Int): AppThemeMode =
            entries.getOrElse(ordinal) { SYSTEM }
    }
}

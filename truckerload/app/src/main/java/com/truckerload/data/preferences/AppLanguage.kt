package com.truckerload.data.preferences

enum class AppLanguage(val tag: String) {
    RU("ru"),
    EN("en");

    companion object {
        fun fromOrdinal(ordinal: Int): AppLanguage =
            entries.getOrElse(ordinal) { RU }
    }
}

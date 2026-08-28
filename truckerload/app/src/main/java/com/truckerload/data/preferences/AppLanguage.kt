package com.truckerload.data.preferences

enum class AppLanguage(val tag: String) {
    RU("ru"),
    EN("en"),
    ES("es");

    companion object {
        fun fromOrdinal(ordinal: Int): AppLanguage =
            entries.getOrElse(ordinal) { EN }

        fun fromTag(tag: String): AppLanguage {
            val language = tag.lowercase().substringBefore('-').substringBefore('_')
            return entries.firstOrNull { it.tag == language } ?: EN
        }
    }
}

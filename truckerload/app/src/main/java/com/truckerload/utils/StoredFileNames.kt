package com.truckerload.utils

/**
 * File names written under Downloads/TruckoRig or handed to another app.
 * Slashes and ".." are removed so a name cannot leave the intended folder.
 */
object StoredFileNames {

    fun sanitize(raw: String?): String {
        val base = raw
            ?.substringAfterLast('/')
            ?.substringAfterLast('\\')
            ?.trim()
            .orEmpty()
        val cleaned = base
            .replace(Regex("""[^A-Za-z0-9._ -]"""), "")
            .trim('.', ' ', '-')
            .take(80)
            .trim()
        if (cleaned.isBlank() || cleaned == "." || cleaned == "..") return "file"
        return cleaned
    }

    /**
     * Relative directory under the public Download folder.
     * The result always stays inside [BrandConstants.DOWNLOADS_FOLDER].
     */
    fun downloadsRelativeDir(raw: String?): String {
        val root = BrandConstants.DOWNLOADS_FOLDER
        val kept = raw.orEmpty()
            .split('/', '\\')
            .map { segment ->
                segment.trim().replace(Regex("""[^A-Za-z0-9._ -]"""), "").trim('.', ' ', '-')
            }
            .filter { it.isNotBlank() && it != "." && it != ".." }
        val joined = kept.joinToString("/")
        if (joined.isBlank() || joined == root) return root
        if (joined.startsWith("$root/")) return joined
        return "$root/$joined"
    }
}

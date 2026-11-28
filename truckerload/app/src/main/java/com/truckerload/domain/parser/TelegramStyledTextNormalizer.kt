package com.truckerload.domain.parser

import java.text.Normalizer

/**
 * Converts Telegram "fancy" Unicode letters (mathematical bold/sans-serif) to ASCII.
 * Relay bot posts often use 𝗧𝗿𝗶𝗽 𝗜𝗗 instead of Trip ID.
 */
object TelegramStyledTextNormalizer {

    /**
     * De-styles letters, applies NFKC, drops emoji / variation selectors / zero-width marks,
     * unifies dash variants to `-`, and trims every line. Newlines are kept.
     */
    fun normalize(input: String): String {
        val destyled = buildString(input.length) {
            input.codePoints().forEach { codePoint ->
                appendCodePoint(deStyleCodePoint(codePoint))
            }
        }
        val nfkc = Normalizer.normalize(destyled, Normalizer.Form.NFKC)
        val cleaned = buildString(nfkc.length) {
            nfkc.codePoints().forEach { cp ->
                when {
                    cp in DASH_RANGE || cp == MINUS_SIGN -> append('-')
                    cp == NBSP || cp in SPACE_RANGE -> append(' ')
                    isDecoration(cp) -> append(' ')
                    else -> appendCodePoint(cp)
                }
            }
        }
        return cleaned.lineSequence().joinToString("\n") { it.trim() }
    }

    private fun isDecoration(cp: Int): Boolean {
        val type = Character.getType(cp).toByte()
        return cp in VARIATION_SELECTORS || cp in INVISIBLE_MARKS || type in DECORATION_TYPES
    }

    private const val VS_FIRST = 0xFE00
    private const val VS_LAST = 0xFE0F
    private const val ZWSP = 0x200B
    private const val ZWNJ = 0x200C
    private const val ZWJ = 0x200D
    private const val WORD_JOINER = 0x2060
    private const val BOM = 0xFEFF
    private const val KEYCAP = 0x20E3
    private val VARIATION_SELECTORS = VS_FIRST..VS_LAST
    private val INVISIBLE_MARKS = setOf(ZWSP, ZWNJ, ZWJ, WORD_JOINER, BOM, KEYCAP)
    private val DECORATION_TYPES = setOf(
        Character.OTHER_SYMBOL,
        Character.SURROGATE,
        Character.PRIVATE_USE,
    )
    private const val DASH_FIRST = 0x2010
    private const val DASH_LAST = 0x2015
    private const val SPACE_FIRST = 0x2000
    private const val SPACE_LAST = 0x200A
    private val DASH_RANGE = DASH_FIRST..DASH_LAST
    private val SPACE_RANGE = SPACE_FIRST..SPACE_LAST
    private const val MINUS_SIGN = 0x2212
    private const val NBSP = 0xA0

    private fun deStyleCodePoint(code: Int): Int = when (code) {
        in 0x1D400..0x1D419 -> 'A'.code + (code - 0x1D400)
        in 0x1D41A..0x1D433 -> 'a'.code + (code - 0x1D41A)
        in 0x1D434..0x1D44D -> 'A'.code + (code - 0x1D434)
        in 0x1D44E..0x1D467 -> 'a'.code + (code - 0x1D44E)
        in 0x1D468..0x1D481 -> 'A'.code + (code - 0x1D468)
        in 0x1D482..0x1D49B -> 'a'.code + (code - 0x1D482)
        in 0x1D5D4..0x1D5ED -> 'A'.code + (code - 0x1D5D4)
        in 0x1D5EE..0x1D607 -> 'a'.code + (code - 0x1D5EE)
        in 0x1D608..0x1D621 -> 'A'.code + (code - 0x1D608)
        in 0x1D622..0x1D63B -> 'a'.code + (code - 0x1D622)
        in 0x1D63C..0x1D655 -> 'A'.code + (code - 0x1D63C)
        in 0x1D656..0x1D66F -> 'a'.code + (code - 0x1D656)
        in 0x1D7CE..0x1D7D7 -> '0'.code + (code - 0x1D7CE)
        in 0x1D7E2..0x1D7EB -> '0'.code + (code - 0x1D7E2)
        else -> code
    }
}

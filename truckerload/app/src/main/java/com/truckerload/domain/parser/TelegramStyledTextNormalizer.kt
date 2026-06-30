package com.truckerload.domain.parser

/**
 * Converts Telegram "fancy" Unicode letters (mathematical bold/sans-serif) to ASCII.
 * Relay bot posts often use 𝗧𝗿𝗶𝗽 𝗜𝗗 instead of Trip ID.
 */
object TelegramStyledTextNormalizer {

    fun normalize(input: String): String = buildString(input.length) {
        input.codePoints().forEach { codePoint ->
            appendCodePoint(deStyleCodePoint(codePoint))
        }
    }

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

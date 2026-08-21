package com.truckerload.domain.import.parser

class ParserFactory(
    private val telegramHtmlParser: LoadParser = TelegramHtmlExportParser(),
    private val telegramJsonParser: LoadParser = TelegramJsonExportParser(),
    private val htmlParser: LoadParser = HtmlLoadParser(),
    private val relayParser: LoadParser = RelayMessageParser(),
    private val textParser: LoadParser = TextLoadParser(),
    private val csvParser: LoadParser = CsvLoadParser(),
    private val exportParser: LoadParser = ExportTextLoadParser(),
) {
    fun getParser(type: ImportMessageType): LoadParser = when (type) {
        ImportMessageType.TELEGRAM_HTML -> telegramHtmlParser
        ImportMessageType.TELEGRAM_JSON -> telegramJsonParser
        ImportMessageType.HTML -> htmlParser
        ImportMessageType.RELAY_TEXT -> relayParser
        ImportMessageType.CSV -> csvParser
        ImportMessageType.EXPORT_TEXT -> exportParser
        ImportMessageType.PLAIN_TEXT, ImportMessageType.UNKNOWN -> textParser
    }
}

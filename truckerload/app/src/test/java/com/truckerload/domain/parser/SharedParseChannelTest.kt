package com.truckerload.domain.parser

import com.truckerload.data.repository.AiRepository
import com.truckerload.domain.import.model.ParsedLoad
import com.truckerload.domain.share.MemoryShareImportIndex
import com.truckerload.domain.share.ShareImportDecision
import com.truckerload.domain.share.ShareImportDecider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The same rate-confirmation text must become the same [ParsedLoad] whether it
 * arrives as chat paste, a Telegram message, or a shared PDF/image extract.
 */
class SharedParseChannelTest {

    @Test
    fun rateConfirmation_agreesAcrossChatTelegramAndShare() = runBlocking {
        val text = FakeRateConfirmation.TEXT
        val fileName = FakeRateConfirmation.FILE_NAME

        val chat = LoadParsingEngine.parse(
            ParseInput.PlainText(text, REF_MILLIS),
        ).getOrThrow().single()
        val telegram = LoadParsingEngine.parse(
            ParseInput.TelegramMessage(
                text = text,
                messageDateSeconds = REF_MILLIS / 1000,
                referenceMillis = REF_MILLIS,
            ),
        ).getOrThrow().single()
        val sharedFile = LoadParsingEngine.parse(
            ParseInput.ExtractedDocument(
                text = text,
                fileName = fileName,
                referenceMillis = REF_MILLIS,
            ),
        ).getOrThrow().single()

        val chatParsed = ParsedLoad.from(chat)
        assertEquals(chatParsed, ParsedLoad.from(telegram))
        assertEquals(chatParsed, ParsedLoad.from(sharedFile))
        assertTrue(chatParsed.tripId.contains(FakeRateConfirmation.TRIP))
        assertEquals(FakeRateConfirmation.RATE, chatParsed.totalRate, 0.01)
        assertEquals(FakeRateConfirmation.MILES, chatParsed.totalMiles, 0.01)
        assertTrue(chatParsed.pointA.contains(FakeRateConfirmation.PICKUP_CITY, ignoreCase = true))
        assertTrue(chatParsed.pointB.contains(FakeRateConfirmation.DELIVERY_CITY, ignoreCase = true))

        val pasted = MessageParseService().parseLoadsFromMessage(text, REF_MILLIS).getOrThrow().single()
        val filed = MessageParseService()
            .parseLoadsFromInboundText(text, REF_MILLIS, fileName)
            .getOrThrow()
            .single()
        val addLoad = AiRepository().parseLoadFromUserInput(text).getOrThrow()
        assertEquals(chatParsed, ParsedLoad.from(pasted))
        assertEquals(chatParsed, ParsedLoad.from(filed))
        assertEquals(chatParsed, ParsedLoad.from(addLoad))

        val decision = ShareImportDecider(nowMillis = REF_MILLIS).decide(
            text = text,
            fileName = fileName,
            fileHash = "rate-con-hash",
            index = MemoryShareImportIndex(),
        )
        val added = decision as ShareImportDecision.Add
        val shared = ParsedLoad.from(added.load)
        assertEquals(chatParsed.tripId, shared.tripId)
        assertEquals(chatParsed.totalRate, shared.totalRate, 0.01)
        assertEquals(chatParsed.totalMiles, shared.totalMiles, 0.01)
        assertEquals(chatParsed.pointA, shared.pointA)
        assertEquals(chatParsed.pointB, shared.pointB)
        assertEquals(chatParsed.stopCount, shared.stopCount)
    }

    private companion object {
        const val REF_MILLIS = 1_753_401_600_000L
    }
}

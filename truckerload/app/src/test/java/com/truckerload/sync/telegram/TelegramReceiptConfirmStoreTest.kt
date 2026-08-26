package com.truckerload.sync.telegram

import com.truckerload.domain.ingest.ReceiptKind
import com.truckerload.domain.ingest.ReceiptPreview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TelegramReceiptConfirmStoreTest {

    @Test
    fun roundTrip_keepsTelegramSourceFilePath() {
        val context = RuntimeEnvironment.getApplication()
        val prefs = context.getSharedPreferences("tg_confirm_test", 0)
        val store = TelegramReceiptConfirmStore(prefs, context)
        val preview = ReceiptPreview(
            kind = ReceiptKind.PAYCHECK,
            amount = 10907.79,
            gallons = null,
            pricePerGallon = null,
            date = "2026-08-16",
            location = null,
            vendor = null,
            driverName = "Khaidar Safarov",
            tripId = null,
            extractedText = "Net Pay: 10907.79",
            highlightToken = "10907.79",
            sourceFileName = "Settlement.pdf",
            sourceFilePath = "paychecks/uuid_Settlement.pdf",
            messageDateSeconds = 1_724_000_000L,
        )
        store.save("chat-1", preview)
        val loaded = store.load("chat-1")
        assertEquals("Settlement.pdf", loaded?.sourceFileName)
        assertEquals("paychecks/uuid_Settlement.pdf", loaded?.sourceFilePath)
        assertEquals(10907.79, loaded?.amount!!, 0.001)
    }

    @Test
    fun clear_discardFile_removesCopiedOriginal() {
        val context = RuntimeEnvironment.getApplication()
        val path = com.truckerload.utils.PaycheckSourceFiles.copyFromBytes(
            context,
            byteArrayOf(1, 2, 3, 4),
            "from_telegram.pdf",
        )
        val prefs = context.getSharedPreferences("tg_confirm_discard", 0)
        val store = TelegramReceiptConfirmStore(prefs, context)
        store.save(
            "chat-2",
            ReceiptPreview(
                kind = ReceiptKind.PAYCHECK,
                amount = 10.0,
                gallons = null,
                pricePerGallon = null,
                date = null,
                location = null,
                vendor = null,
                driverName = null,
                tripId = null,
                extractedText = "x",
                highlightToken = null,
                sourceFileName = "from_telegram.pdf",
                sourceFilePath = path,
            ),
        )
        store.clear("chat-2", discardFile = true)
        assertNull(store.load("chat-2"))
        org.junit.Assert.assertFalse(
            com.truckerload.utils.PaycheckSourceFiles.exists(context, path),
        )
    }
}

package com.truckerload.sync.telegram

import android.content.Context
import android.content.SharedPreferences
import com.truckerload.R
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.data.remote.TelegramUpdate
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.sync.TelegramChatRestore
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Routing order of [TelegramUpdateDispatcher.handleUpdate]: who may talk, then which stage answers. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TelegramUpdateDispatcherTest {

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var apiClient: TelegramApiClient
    private lateinit var dispatcher: TelegramUpdateDispatcher
    private val chatRestore: TelegramChatRestore = mock()
    private val loads: LoadRepository = mock()
    private val paychecks: PaycheckRepository = mock()
    private val diesel: DieselRepository = mock()

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        prefs = context.getSharedPreferences("dispatcher-test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        runBlocking { SettingsDataStore(context).clearTelegramChatId() }
        apiClient = mock()
        dispatcher = TelegramUpdateDispatcher(context, apiClient, TelegramMessageParser(context))
    }

    private fun bind(chatId: Long) = runBlocking { SettingsDataStore(context).saveTelegramChatId(chatId) }

    private fun send(update: TelegramUpdate) = runBlocking {
        dispatcher.handleUpdate(update, loads, paychecks, diesel, chatRestore, prefs, TelegramStateMachine(prefs))
    }

    private fun text(chatId: String, body: String) = TelegramUpdate(updateId = 1L, chatId = chatId, chatType = "private", text = body)

    @Test
    fun helpFromThePairedChatSendsTheHelpText() {
        bind(4242L)
        send(text("4242", "/help"))
        runBlocking { verify(apiClient).sendWithMenu("4242", context.getString(R.string.sync_help)) }
    }

    @Test
    fun cancelWithNothingOpenSaysSo() {
        bind(4242L)
        send(text("4242", "/cancel"))
        runBlocking {
            verify(apiClient).sendWithMenu("4242", context.getString(R.string.sync_restore_not_active))
        }
    }

    @Test
    fun emptyTextWithoutAttachmentReportsNoNewData() {
        bind(4242L)
        send(text("4242", "   "))
        runBlocking {
            verify(apiClient).sendMessage("4242", context.getString(R.string.sync_no_new_data))
        }
    }

    @Test
    fun anotherChatIsRejectedAndNothingIsStored() {
        bind(4242L)
        send(text("999", "/help"))
        runBlocking {
            verify(apiClient).sendMessage("999", context.getString(R.string.sync_chat_not_authorized))
            verify(apiClient, never()).sendWithMenu(any(), any())
            verify(chatRestore, never()).persistIncoming(any())
        }
    }

    @Test
    fun unpairedChatGetsThePairingHintOnce() {
        send(text("777", "/help"))
        send(text("777", "/help"))
        runBlocking {
            val hint = context.getString(R.string.sync_chat_pair_required)
            verify(apiClient).sendMessage("777", hint)
            verify(apiClient, never()).sendWithMenu(eq("777"), anyOrNull())
        }
    }

    @Test
    fun callbackWithoutChatIdIsRejectedWhenNothingIsPending() {
        bind(4242L)
        send(
            TelegramUpdate(
                updateId = 2L,
                chatId = "",
                chatType = "private",
                text = "cmd:help",
                callbackQueryId = "cb-1",
                isCallbackQuery = true,
            ),
        )
        runBlocking {
            verify(apiClient).answerCallbackQuery("cb-1", context.getString(R.string.sync_chat_not_authorized))
            verify(apiClient, never()).sendWithMenu(any(), any())
        }
    }

    @Test
    fun menuCallbackRunsTheCommandForThePairedChat() {
        bind(4242L)
        send(
            TelegramUpdate(
                updateId = 3L,
                chatId = "4242",
                chatType = "private",
                text = "cmd:help",
                callbackQueryId = "cb-2",
                isCallbackQuery = true,
            ),
        )
        runBlocking { verify(apiClient).answerCallbackQuery("cb-2", "OK") }
    }
}

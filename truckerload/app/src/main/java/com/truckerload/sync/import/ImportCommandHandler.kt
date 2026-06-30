package com.truckerload.sync.import

import android.content.Context
import com.truckerload.R
import com.truckerload.data.remote.TelegramApi

class ImportCommandHandler(
    private val context: Context,
    private val sessionManager: ImportSessionManager,
) {

    fun startImport(chatId: String, clearRestoreMode: (String) -> Unit) {
        clearRestoreMode(chatId)
        sessionManager.startSession(chatId)
    }

    suspend fun sendPrompt(chatId: String, telegramApi: TelegramApi) {
        telegramApi.sendMessage(chatId, context.getString(R.string.sync_import_prompt))
    }
}

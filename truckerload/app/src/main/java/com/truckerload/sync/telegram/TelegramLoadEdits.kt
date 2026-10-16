package com.truckerload.sync.telegram

import android.content.Context
import com.truckerload.R
import com.truckerload.data.repository.LoadRepository
import com.truckerload.domain.money.Money
import com.truckerload.domain.parser.ParseUtils
import com.truckerload.sync.TelegramLastLoad
import com.truckerload.sync.TelegramLoadReply
import com.truckerload.widget.WidgetRefresh

/** /undo removes the last load this chat saved. /rate 2500 corrects its rate. */
class TelegramLoadEdits(
    private val context: Context,
    private val apiClient: TelegramApiClient,
) {
    suspend fun handle(text: String, chatId: String, loadRepository: LoadRepository): Boolean {
        val trimmed = text.trim()
        if (TelegramLoadEditCommand.isUndo(trimmed)) {
            undo(chatId, loadRepository)
            return true
        }
        if (!TelegramLoadEditCommand.isRate(trimmed)) return false
        val amount = TelegramLoadEditCommand.amount(trimmed)
        if (amount == null) {
            apiClient.sendWithMenu(chatId, context.getString(R.string.sync_load_rate_bad))
            return true
        }
        correctRate(chatId, loadRepository, amount)
        return true
    }

    private suspend fun undo(chatId: String, loadRepository: LoadRepository) {
        val load = find(loadRepository)
        if (load == null) {
            TelegramLastLoad.clear(context)
            apiClient.sendWithMenu(chatId, context.getString(R.string.sync_load_undo_none))
            return
        }
        loadRepository.deleteLoad(load.id)
        TelegramLastLoad.clear(context)
        WidgetRefresh.refreshAndUpdate(context)
        apiClient.sendWithMenu(chatId, context.getString(R.string.sync_load_undo_done, load.tripId))
    }

    private suspend fun correctRate(chatId: String, loadRepository: LoadRepository, amount: Double) {
        val load = find(loadRepository)
        if (load == null) {
            TelegramLastLoad.clear(context)
            apiClient.sendWithMenu(chatId, context.getString(R.string.sync_load_undo_none))
            return
        }
        val updated = load.copy(
            totalRate = Money.fromDollars(amount),
            updatedAt = System.currentTimeMillis(),
        )
        loadRepository.updateLoad(updated)
        TelegramLastLoad.remember(context, updated)
        WidgetRefresh.refreshAndUpdate(context)
        val detail = TelegramLoadReply.detail(updated)
        apiClient.sendWithMenu(
            chatId,
            context.getString(R.string.sync_load_rate_updated, updated.tripId, detail),
        )
    }

    private suspend fun find(loadRepository: LoadRepository) =
        TelegramLastLoad.id(context)?.let { loadRepository.getLoadById(it) }
            ?: TelegramLastLoad.tripId(context)?.let { loadRepository.getByTripId(it) }
}

internal object TelegramLoadEditCommand {
    fun isUndo(text: String): Boolean = isCommand(text, "/undo")

    fun isRate(text: String): Boolean = isCommand(text, "/rate")

    fun amount(text: String): Double? {
        if (!isRate(text)) return null
        val rest = text.trim().substringAfter(' ', missingDelimiterValue = "")
        if (rest.isBlank()) return null
        return ParseUtils.parseMoney(rest).takeIf { it > 0.0 }
    }

    private fun isCommand(text: String, command: String): Boolean {
        val head = text.trim().substringBefore(' ')
        return head.equals(command, ignoreCase = true) ||
            head.startsWith("$command@", ignoreCase = true)
    }
}

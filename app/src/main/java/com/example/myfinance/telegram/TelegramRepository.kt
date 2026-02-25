package com.example.myfinance.telegram

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.telegramDataStore by preferencesDataStore(name = "telegram")

class TelegramRepository(private val context: Context) {

    private val chatIdKey = stringPreferencesKey("telegram_chat_id")
    private val lastUpdateIdKey = stringPreferencesKey("telegram_last_update_id")

    val chatId: Flow<String?> = context.telegramDataStore.data.map { prefs ->
        val id = prefs[chatIdKey]
        if (id.isNullOrBlank()) null else id
    }

    suspend fun setChatId(chatId: String?) {
        context.telegramDataStore.edit { it[chatIdKey] = chatId ?: "" }
    }

    suspend fun getChatId(): String? = chatId.first()

    suspend fun getLastProcessedUpdateId(): Long {
        val s = context.telegramDataStore.data.map { it[lastUpdateIdKey] }.first() ?: return 0L
        return s.toLongOrNull() ?: 0L
    }

    suspend fun setLastProcessedUpdateId(updateId: Long) {
        context.telegramDataStore.edit { it[lastUpdateIdKey] = updateId.toString() }
    }
}

package com.truckerload.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Все тексты, которые бот получил из Telegram (для восстановления по «восстанови»). */
@Entity(
    tableName = "telegram_inbox",
    indices = [Index(value = ["chatId", "messageDateSeconds"])]
)
data class TelegramInboxEntity(
    @PrimaryKey val updateId: Long,
    val chatId: String,
    val text: String,
    val messageDateSeconds: Long?,
    val receivedAt: Long
)

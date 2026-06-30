package com.truckerload.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.truckerload.data.local.entities.TelegramInboxEntity

@Dao
interface TelegramInboxDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: TelegramInboxEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entities: List<TelegramInboxEntity>)

    @Query("SELECT * FROM telegram_inbox WHERE chatId = :chatId ORDER BY messageDateSeconds ASC, updateId ASC")
    suspend fun getAllForChat(chatId: String): List<TelegramInboxEntity>

    @Query("SELECT COUNT(*) FROM telegram_inbox WHERE chatId = :chatId")
    suspend fun countForChat(chatId: String): Int
}

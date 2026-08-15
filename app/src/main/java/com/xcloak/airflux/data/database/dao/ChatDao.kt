package com.xcloak.airflux.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.xcloak.airflux.data.database.entity.ChatChannel
import com.xcloak.airflux.data.database.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Insert
    suspend fun insert(message: ChatMessageEntity): Long

    @Query("SELECT * FROM chat_messages WHERE channel = :channel ORDER BY timestamp ASC")
    fun getByChannel(channel: ChatChannel): Flow<List<ChatMessageEntity>>

    @Query("SELECT COUNT(*) FROM chat_messages WHERE channel = :channel")
    suspend fun countForChannel(channel: ChatChannel): Int

    @Query("DELETE FROM chat_messages WHERE channel = :channel AND id IN (SELECT id FROM chat_messages WHERE channel = :channel ORDER BY timestamp ASC LIMIT :count)")
    suspend fun deleteOldestForChannel(channel: ChatChannel, count: Int)

    @Query("DELETE FROM chat_messages WHERE channel = :channel")
    suspend fun clearChannel(channel: ChatChannel)
}
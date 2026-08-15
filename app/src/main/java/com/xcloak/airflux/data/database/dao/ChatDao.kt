package com.xcloak.airflux.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.xcloak.airflux.data.database.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Insert
    suspend fun insert(message: ChatMessageEntity): Long

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAll(): Flow<List<ChatMessageEntity>>

    @Query("SELECT COUNT(*) FROM chat_messages")
    suspend fun count(): Int

    @Query("DELETE FROM chat_messages WHERE id IN (SELECT id FROM chat_messages ORDER BY timestamp ASC LIMIT :count)")
    suspend fun deleteOldest(count: Int)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAll()
}
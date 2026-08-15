package com.xcloak.airflux.data.repository

import android.content.Context
import com.xcloak.airflux.data.database.AppDatabase
import com.xcloak.airflux.data.database.entity.ChatChannel
import com.xcloak.airflux.data.database.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

class ChatRepository(context: Context, private val channel: ChatChannel) {
    private val dao = AppDatabase.getInstance(context).chatDao()

    fun getAll(): Flow<List<ChatMessageEntity>> = dao.getByChannel(channel)

    suspend fun record(text: String, isMine: Boolean, freeLimit: Int?) {
        dao.insert(ChatMessageEntity(text = text, timestamp = System.currentTimeMillis(), isMine = isMine, channel = channel))
        if (freeLimit != null) {
            val total = dao.countForChannel(channel)
            if (total > freeLimit) dao.deleteOldestForChannel(channel, total - freeLimit)
        }
    }

    suspend fun clearAll() {
        dao.clearChannel(channel)
    }
}
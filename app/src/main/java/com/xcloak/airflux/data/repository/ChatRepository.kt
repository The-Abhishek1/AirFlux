package com.xcloak.airflux.data.repository

import android.content.Context
import com.xcloak.airflux.data.database.AppDatabase
import com.xcloak.airflux.data.database.entity.ChatChannel
import com.xcloak.airflux.data.database.entity.ChatMessageEntity
import com.xcloak.airflux.data.database.entity.ChatMsgType
import kotlinx.coroutines.flow.Flow

class ChatRepository(context: Context, private val channel: ChatChannel) {
    private val dao = AppDatabase.getInstance(context).chatDao()

    fun getAll(): Flow<List<ChatMessageEntity>> = dao.getByChannel(channel)

    suspend fun record(
        text: String,
        isMine: Boolean,
        freeLimit: Int?,
        type: ChatMsgType = ChatMsgType.TEXT,
        imageData: String? = null
    ) {
        dao.insert(ChatMessageEntity(text = text, timestamp = System.currentTimeMillis(), isMine = isMine, channel = channel, type = type, imageData = imageData))
        if (freeLimit != null) {
            val total = dao.countForChannel(channel)
            if (total > freeLimit) dao.deleteOldestForChannel(channel, total - freeLimit)
        }
    }

    suspend fun clearAll() {
        dao.clearChannel(channel)
    }
}
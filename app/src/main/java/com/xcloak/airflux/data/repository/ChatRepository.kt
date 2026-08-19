package com.xcloak.airflux.data.repository

import android.content.Context
import com.xcloak.airflux.core.common.ChatMediaUtils
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
        mediaPath: String? = null
    ) {
        dao.insert(ChatMessageEntity(text = text, timestamp = System.currentTimeMillis(), isMine = isMine, channel = channel, type = type, mediaPath = mediaPath))
        if (freeLimit != null) {
            val total = dao.countForChannel(channel)
            if (total > freeLimit) {
                val overflow = total - freeLimit
                val pathsToDelete = dao.getOldestMediaPaths(channel, overflow)
                pathsToDelete.forEach { ChatMediaUtils.deleteMediaFile(it) }
                dao.deleteOldestForChannel(channel, overflow)
            }
        }
    }

    suspend fun clearAll() {
        val paths = dao.getAllMediaPaths(channel)
        paths.forEach { ChatMediaUtils.deleteMediaFile(it) }
        dao.clearChannel(channel)
    }
}
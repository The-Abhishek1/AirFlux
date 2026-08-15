package com.xcloak.airflux.data.repository

import android.content.Context
import com.xcloak.airflux.data.database.AppDatabase
import com.xcloak.airflux.data.database.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

class ChatRepository(context: Context) {
    private val dao = AppDatabase.getInstance(context).chatDao()

    fun getAll(): Flow<List<ChatMessageEntity>> = dao.getAll()

    suspend fun record(text: String, isMine: Boolean, freeLimit: Int?) {
        dao.insert(ChatMessageEntity(text = text, timestamp = System.currentTimeMillis(), isMine = isMine))
        if (freeLimit != null) {
            val total = dao.count()
            if (total > freeLimit) dao.deleteOldest(total - freeLimit)
        }
    }

    suspend fun clearAll() {
        dao.clearAll()
    }
}
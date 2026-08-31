package com.xcloak.airflux.data.repository

import android.content.Context
import com.xcloak.airflux.data.database.AppDatabase
import com.xcloak.airflux.data.database.entity.HistoryEntity
import com.xcloak.airflux.data.database.entity.HistoryType
import kotlinx.coroutines.flow.Flow

class HistoryRepository(context: Context) {
    private val dao = AppDatabase.getInstance(context).historyDao()

    fun getAll(): Flow<List<HistoryEntity>> = dao.getAll()

    suspend fun record(fileName: String, sizeBytes: Long, mimeType: String, type: HistoryType, success: Boolean, mediaPath: String? = null) {
        dao.insert(
            HistoryEntity(
                fileName = fileName,
                sizeBytes = sizeBytes,
                mimeType = mimeType,
                type = type,
                timestamp = System.currentTimeMillis(),
                success = success,
                mediaPath = mediaPath
            )
        )
    }

    suspend fun clearAll() {
        dao.clearAll()
    }
}
package com.xcloak.airflux.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.xcloak.airflux.data.database.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert
    suspend fun insert(entry: HistoryEntity)

    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun getAll(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<HistoryEntity>>

    @Query("DELETE FROM history")
    suspend fun clearAll(): Int
}
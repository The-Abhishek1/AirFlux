package com.xcloak.airflux.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xcloak.airflux.data.database.entity.ScheduledDownloadEntity

@Dao
interface ScheduledDownloadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ScheduledDownloadEntity)

    @Query("SELECT * FROM scheduled_downloads WHERE fired = 0 ORDER BY triggerAtMillis ASC")
    suspend fun getPending(): List<ScheduledDownloadEntity>

    @Query("UPDATE scheduled_downloads SET fired = 1 WHERE id = :id")
    suspend fun markFired(id: String)

    @Query("DELETE FROM scheduled_downloads WHERE id = :id")
    suspend fun delete(id: String)
}

package com.xcloak.airflux.data.repository

import android.content.Context
import com.xcloak.airflux.data.database.AppDatabase
import com.xcloak.airflux.data.database.entity.ScheduledDownloadEntity
import com.xcloak.airflux.domain.model.ScheduledDownload

class ScheduledDownloadRepository(context: Context) {
    private val dao = AppDatabase.getInstance(context).scheduledDownloadDao()

    suspend fun getPending(): List<ScheduledDownload> =
        dao.getPending().map { ScheduledDownload(it.id, it.url, it.triggerAtMillis, it.wifiOnly, it.fired) }

    suspend fun insert(item: ScheduledDownload) {
        dao.insert(
            ScheduledDownloadEntity(
                id = item.id,
                url = item.url,
                triggerAtMillis = item.triggerAtMillis,
                wifiOnly = item.wifiOnly,
                fired = item.fired
            )
        )
    }

    suspend fun markFired(id: String) = dao.markFired(id)

    suspend fun delete(id: String) = dao.delete(id)
}

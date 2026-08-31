package com.xcloak.airflux.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class HistoryType { SENT, RECEIVED, DOWNLOADED }

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val sizeBytes: Long,
    val mimeType: String,
    val type: HistoryType,
    val timestamp: Long,
    val success: Boolean,
    val mediaPath: String? = null
)
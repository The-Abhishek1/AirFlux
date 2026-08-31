package com.xcloak.airflux.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted counterpart of the in-memory ScheduledDownload the UI shows.
 *
 * Previously the "Scheduled" list in DownloaderViewModel lived only in a ViewModel
 * MutableStateFlow — the WorkManager job that actually performs the download survives
 * navigation/process death, but the UI's knowledge of it didn't, so a scheduled download
 * could fire successfully with the user never seeing it listed again after leaving the
 * screen. This table lets the ViewModel rehydrate the list on init.
 */
@Entity(tableName = "scheduled_downloads")
data class ScheduledDownloadEntity(
    @PrimaryKey val id: String,
    val url: String,
    val triggerAtMillis: Long,
    val wifiOnly: Boolean,
    val fired: Boolean = false
)

package com.xcloak.airflux.feature.history.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xcloak.airflux.data.database.entity.HistoryEntity
import com.xcloak.airflux.data.repository.HistoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GalleryViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = HistoryRepository(application)

    val allHistory: StateFlow<List<HistoryEntity>> = repo.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val images: StateFlow<List<HistoryEntity>> = allHistory.map { list ->
        list.filter { it.mimeType.startsWith("image/") && it.success }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val videos: StateFlow<List<HistoryEntity>> = allHistory.map { list ->
        list.filter { it.mimeType.startsWith("video/") && it.success }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val files: StateFlow<List<HistoryEntity>> = allHistory.map { list ->
        list.filter { !it.mimeType.startsWith("image/") && !it.mimeType.startsWith("video/") && it.success }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearHistory() {
        viewModelScope.launch { repo.clearAll() }
    }
}

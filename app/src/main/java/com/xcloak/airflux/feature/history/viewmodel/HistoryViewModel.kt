package com.xcloak.airflux.feature.history.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xcloak.airflux.data.database.entity.HistoryEntity
import com.xcloak.airflux.data.repository.HistoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = HistoryRepository(application)

    val history: StateFlow<List<HistoryEntity>> = repo.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearHistory() {
        viewModelScope.launch { repo.clearAll() }
    }
}
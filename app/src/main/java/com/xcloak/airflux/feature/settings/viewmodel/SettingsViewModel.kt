package com.xcloak.airflux.feature.settings.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xcloak.airflux.core.billing.PlanManager
import com.xcloak.airflux.data.repository.HistoryRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val historyRepo = HistoryRepository(application)

    val isPro: StateFlow<Boolean> = PlanManager.isProFlow

    fun setProForTesting(value: Boolean) {
        PlanManager.setPro(value)
    }

    fun clearHistory() {
        viewModelScope.launch { historyRepo.clearAll() }
    }

    fun appVersion(): String {
        return try {
            val pm = getApplication<Application>().packageManager
            val packageName = getApplication<Application>().packageName
            pm.getPackageInfo(packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }
}
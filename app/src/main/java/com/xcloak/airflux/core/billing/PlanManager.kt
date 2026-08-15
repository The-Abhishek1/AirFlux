package com.xcloak.airflux.core.billing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object PlanManager {
    private val _isPro = MutableStateFlow(false)
    val isProFlow: StateFlow<Boolean> = _isPro.asStateFlow()

    // TODO Phase 9: replace with real Play Billing entitlement check
    val isPro: Boolean get() = _isPro.value

    fun setPro(value: Boolean) {
        _isPro.value = value
    }

    fun maxConcurrentTransfers(): Int = if (isPro) 3 else 1
}
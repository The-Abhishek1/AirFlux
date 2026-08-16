package com.xcloak.airflux.core.ads

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SpeedBoostManager {
    const val FREE_TIER_CAP_BYTES_PER_SEC = 300L * 1024 // 300 KB/s
    const val BOOST_DURATION_MILLIS = 15 * 60 * 1000L // 15 minutes

    private val _boostExpiresAt = MutableStateFlow(0L)
    val boostExpiresAt: StateFlow<Long> = _boostExpiresAt.asStateFlow()

    fun isBoostActive(): Boolean = System.currentTimeMillis() < _boostExpiresAt.value

    fun activateBoost() {
        _boostExpiresAt.value = System.currentTimeMillis() + BOOST_DURATION_MILLIS
    }

    fun remainingSeconds(): Long {
        val remaining = (_boostExpiresAt.value - System.currentTimeMillis()) / 1000
        return if (remaining > 0) remaining else 0
    }
}
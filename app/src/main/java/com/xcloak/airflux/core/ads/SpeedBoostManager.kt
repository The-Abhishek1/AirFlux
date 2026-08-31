package com.xcloak.airflux.core.ads

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SpeedBoostManager {
    // Applies to both local Share & Receive transfers and Download-from-Link on the free
    // tier (see ReceiveViewModel, DownloaderViewModel, and ScheduledDownloadWorker).
    const val FREE_TIER_CAP_BYTES_PER_SEC = 1L * 1024 * 1024 // 1 MB/s
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
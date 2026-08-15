package com.xcloak.airflux.core.security

import java.util.concurrent.ConcurrentHashMap

class RateLimiter(
    private val maxRequests: Int = 30,
    private val windowMillis: Long = 10_000
) {
    private val requestLog = ConcurrentHashMap<String, MutableList<Long>>()

    @Synchronized
    fun allowRequest(clientId: String): Boolean {
        val now = System.currentTimeMillis()
        val timestamps = requestLog.getOrPut(clientId) { mutableListOf() }
        timestamps.removeAll { now - it > windowMillis }
        if (timestamps.size >= maxRequests) return false
        timestamps.add(now)
        return true
    }
}
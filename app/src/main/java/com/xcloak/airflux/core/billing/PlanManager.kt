package com.xcloak.airflux.core.billing

object PlanManager {
    // TODO Phase 9: replace with real Play Billing entitlement check
    var isPro: Boolean = false

    fun maxConcurrentTransfers(): Int = if (isPro) 3 else 1
}
package com.xcloak.airflux.domain.model

data class ScheduledDownload(
    val id: String,
    val url: String,
    val triggerAtMillis: Long,
    val wifiOnly: Boolean,
    val fired: Boolean = false
)
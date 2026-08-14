package com.xcloak.airflux.domain.model

data class UrlDownloadItem(
    val id: String,
    val url: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long
)
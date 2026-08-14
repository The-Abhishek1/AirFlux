package com.xcloak.airflux.domain.model

data class RemoteFile(
    val index: Int,
    val name: String,
    val sizeBytes: Long,
    val mimeType: String
)
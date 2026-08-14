package com.xcloak.airflux.domain.model

import android.net.Uri

data class SelectedFile(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val mimeType: String
)
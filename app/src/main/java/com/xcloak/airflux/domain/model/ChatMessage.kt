package com.xcloak.airflux.domain.model

data class ChatMessage(
    val id: Long = 0,
    val text: String,
    val timestamp: Long,
    val isMine: Boolean
)
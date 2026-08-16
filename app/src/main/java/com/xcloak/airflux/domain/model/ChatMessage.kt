package com.xcloak.airflux.domain.model

import com.xcloak.airflux.data.database.entity.ChatMsgType

data class ChatMessage(
    val id: Long = 0,
    val text: String,
    val timestamp: Long,
    val isMine: Boolean,
    val type: ChatMsgType = ChatMsgType.TEXT,
    val imageData: String? = null
)
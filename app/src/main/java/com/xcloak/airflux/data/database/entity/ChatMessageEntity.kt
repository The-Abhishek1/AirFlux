package com.xcloak.airflux.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ChatChannel { WIFI, BLUETOOTH }
enum class ChatMsgType { TEXT, IMAGE, AUDIO, VIDEO }

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val timestamp: Long,
    val isMine: Boolean,
    val channel: ChatChannel = ChatChannel.WIFI,
    val type: ChatMsgType = ChatMsgType.TEXT,
    val imageData: String? = null // generic media payload — holds photo, voice, or video base64
)
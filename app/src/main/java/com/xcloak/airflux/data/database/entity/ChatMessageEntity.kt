package com.xcloak.airflux.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ChatChannel { WIFI, BLUETOOTH }

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val timestamp: Long,
    val isMine: Boolean,
    val channel: ChatChannel = ChatChannel.WIFI
)
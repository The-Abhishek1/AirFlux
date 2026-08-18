package com.xcloak.airflux.feature.chat.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xcloak.airflux.core.billing.PlanManager
import com.xcloak.airflux.core.common.ImageCompressUtils
import com.xcloak.airflux.core.network.ChatSession
import com.xcloak.airflux.core.network.NetworkUtils
import com.xcloak.airflux.core.security.SecurityUtils
import com.xcloak.airflux.data.database.entity.ChatChannel
import com.xcloak.airflux.data.database.entity.ChatMsgType
import com.xcloak.airflux.data.repository.ChatRepository
import com.xcloak.airflux.domain.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.xcloak.airflux.core.common.VideoUtils
sealed class ChatConnectionState {
    object Idle : ChatConnectionState()
    object Waiting : ChatConnectionState()
    object Connecting : ChatConnectionState()
    object Connected : ChatConnectionState()
    data class Error(val message: String) : ChatConnectionState()
    object Disconnected : ChatConnectionState()
}

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val CHAT_PORT = 8082
        const val FREE_HISTORY_LIMIT = 50
    }

    private val _sendError = kotlinx.coroutines.flow.MutableSharedFlow<String>(extraBufferCapacity = 1)
    val sendError: kotlinx.coroutines.flow.SharedFlow<String> = _sendError
    private val repo = ChatRepository(application, ChatChannel.WIFI)
    private val session = ChatSession(viewModelScope)

    private val _connectionState = MutableStateFlow<ChatConnectionState>(ChatConnectionState.Idle)
    val connectionState: StateFlow<ChatConnectionState> = _connectionState.asStateFlow()

    private val _hostInfo = MutableStateFlow<String?>(null)
    val hostInfo: StateFlow<String?> = _hostInfo.asStateFlow()

    val messages: StateFlow<List<ChatMessage>> = repo.getAll()
        .map { list -> list.map { ChatMessage(it.id, it.text, it.timestamp, it.isMine, it.type, it.imageData) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            session.connected.collect { isConnected ->
                _connectionState.value = when {
                    isConnected -> ChatConnectionState.Connected
                    _connectionState.value == ChatConnectionState.Connected -> ChatConnectionState.Disconnected
                    else -> _connectionState.value
                }
            }
        }
        viewModelScope.launch {
            session.incoming.collect { wire ->
                val limit = if (PlanManager.isPro) null else FREE_HISTORY_LIMIT
                when {
                    wire.type == "image" && wire.imageData != null ->
                        repo.record(wire.text, isMine = false, freeLimit = limit, type = ChatMsgType.IMAGE, imageData = wire.imageData)
                    wire.type == "audio" && wire.imageData != null ->
                        repo.record(wire.text, isMine = false, freeLimit = limit, type = ChatMsgType.AUDIO, imageData = wire.imageData)
                    wire.type == "video" && wire.imageData != null ->
                        repo.record(wire.text, isMine = false, freeLimit = limit, type = ChatMsgType.VIDEO, imageData = wire.imageData)
                    else ->
                        repo.record(wire.text, isMine = false, freeLimit = limit)
                }
            }
        }
    }

    fun startHosting() {
        val ip = NetworkUtils.getLocalIpAddress()
        if (ip == null) {
            _connectionState.value = ChatConnectionState.Error("No Wi-Fi connection detected")
            return
        }
        val token = SecurityUtils.generateSessionToken()
        _hostInfo.value = "$ip:$CHAT_PORT?token=$token"
        _connectionState.value = ChatConnectionState.Waiting
        session.startHost(CHAT_PORT, token)
    }

    fun joinChat(address: String) {
        val clean = address.removePrefix("http://").removePrefix("https://")
        val parts = clean.split("?token=")
        val hostPart = parts.getOrNull(0) ?: return
        val token = parts.getOrNull(1) ?: ""
        val ipPort = hostPart.split(":")
        val ip = ipPort.getOrNull(0) ?: return
        val port = ipPort.getOrNull(1)?.toIntOrNull() ?: CHAT_PORT

        _connectionState.value = ChatConnectionState.Connecting
        session.startJoin(ip, port, token)
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        session.send(text)
        viewModelScope.launch {
            val limit = if (PlanManager.isPro) null else FREE_HISTORY_LIMIT
            repo.record(text, isMine = true, freeLimit = limit)
        }
    }

    fun sendImage(uri: Uri) {
        if (!PlanManager.isPro) return
        viewModelScope.launch {
            val base64 = withContext(Dispatchers.IO) { ImageCompressUtils.uriToBase64Jpeg(getApplication(), uri) }
            if (base64 == null) return@launch
            session.sendImage(base64)
            repo.record("[Photo]", isMine = true, freeLimit = null, type = ChatMsgType.IMAGE, imageData = base64)
        }
    }

    fun sendAudio(base64: String) {
        if (!PlanManager.isPro) return
        session.sendAudio(base64)
        viewModelScope.launch {
            repo.record("[Voice message]", isMine = true, freeLimit = null, type = ChatMsgType.AUDIO, imageData = base64)
        }
    }
    fun sendVideo(uri: Uri) {
        if (!PlanManager.isPro) return
        val size = VideoUtils.getSizeBytes(getApplication(), uri)
        if (size <= 0 || size > VideoUtils.MAX_VIDEO_BYTES) {
            _sendError.tryEmit("Video must be under 5 MB")
            return
        }
        viewModelScope.launch {
            try {
                val base64 = withContext(Dispatchers.IO) { VideoUtils.uriToBase64(getApplication(), uri) }
                if (base64 == null) {
                    _sendError.tryEmit("Could not send video — file too large or unreadable")
                    return@launch
                }
                withContext(Dispatchers.IO) { session.sendVideo(base64) }
                repo.record("[Video]", isMine = true, freeLimit = null, type = ChatMsgType.VIDEO, imageData = base64)
            } catch (e: Throwable) {
                _sendError.tryEmit("Could not send video — try a smaller file")
            }
        }
    }
    fun clearChat() {
        viewModelScope.launch { repo.clearAll() }
    }

    fun disconnect() {
        session.close()
        _connectionState.value = ChatConnectionState.Idle
        _hostInfo.value = null
    }

    override fun onCleared() {
        super.onCleared()
        session.close()
    }
}
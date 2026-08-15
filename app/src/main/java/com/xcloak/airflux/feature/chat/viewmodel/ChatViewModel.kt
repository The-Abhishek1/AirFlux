package com.xcloak.airflux.feature.chat.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xcloak.airflux.core.billing.PlanManager
import com.xcloak.airflux.core.network.ChatSession
import com.xcloak.airflux.core.network.NetworkUtils
import com.xcloak.airflux.core.security.SecurityUtils
import com.xcloak.airflux.data.repository.ChatRepository
import com.xcloak.airflux.domain.model.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
    private val repo = ChatRepository(application, com.xcloak.airflux.data.database.entity.ChatChannel.WIFI)
    private val session = ChatSession(viewModelScope)

    private val _connectionState = MutableStateFlow<ChatConnectionState>(ChatConnectionState.Idle)
    val connectionState: StateFlow<ChatConnectionState> = _connectionState.asStateFlow()

    private val _hostInfo = MutableStateFlow<String?>(null)
    val hostInfo: StateFlow<String?> = _hostInfo.asStateFlow()

    val messages: StateFlow<List<ChatMessage>> = repo.getAll()
        .map { list -> list.map { ChatMessage(it.id, it.text, it.timestamp, it.isMine) } }
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
                repo.record(wire.text, isMine = false, freeLimit = limit)
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
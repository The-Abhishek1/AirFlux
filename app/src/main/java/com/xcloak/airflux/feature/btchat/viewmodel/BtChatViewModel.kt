package com.xcloak.airflux.feature.btchat.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xcloak.airflux.core.billing.PlanManager
import com.xcloak.airflux.core.common.ChatMediaUtils
import com.xcloak.airflux.core.common.ImageCompressUtils
import com.xcloak.airflux.core.common.MediaKind
import com.xcloak.airflux.core.common.VideoUtils
import com.xcloak.airflux.core.network.BluetoothChatSession
import com.xcloak.airflux.data.database.entity.ChatChannel
import com.xcloak.airflux.data.database.entity.ChatMsgType
import com.xcloak.airflux.data.repository.ChatRepository
import com.xcloak.airflux.domain.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class BtConnectionState {
    object Idle : BtConnectionState()
    object Waiting : BtConnectionState()
    object Connecting : BtConnectionState()
    object Connected : BtConnectionState()
    object Disconnected : BtConnectionState()
    data class Error(val message: String) : BtConnectionState()
}

data class BtDeviceInfo(val name: String, val address: String, val device: BluetoothDevice)

class BtChatViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val FREE_HISTORY_LIMIT = 50
    }

    private val repo = ChatRepository(application, ChatChannel.BLUETOOTH)
    private val session = BluetoothChatSession(viewModelScope)

    private val bluetoothManager = application.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager.adapter

    private val _connectionState = MutableStateFlow<BtConnectionState>(BtConnectionState.Idle)
    val connectionState: StateFlow<BtConnectionState> = _connectionState.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BtDeviceInfo>>(emptyList())
    val pairedDevices: StateFlow<List<BtDeviceInfo>> = _pairedDevices.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BtDeviceInfo>>(emptyList())
    val discoveredDevices: StateFlow<List<BtDeviceInfo>> = _discoveredDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _sendError = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val sendError: SharedFlow<String> = _sendError

    val messages: StateFlow<List<ChatMessage>> = repo.getAll()
        .map { list -> list.map { ChatMessage(it.id, it.text, it.timestamp, it.isMine, it.type, it.mediaPath) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun hasBluetoothConnectPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    }

    private val discoveryReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    if (!hasBluetoothConnectPermission()) return
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) ?: return
                    val name = try { device.name ?: "Unknown device" } catch (e: SecurityException) { "Unknown device" }
                    val info = BtDeviceInfo(name, device.address, device)
                    if (_discoveredDevices.value.none { it.address == info.address }) {
                        _discoveredDevices.value = _discoveredDevices.value + info
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _isScanning.value = false
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        val app = getApplication<Application>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            app.registerReceiver(discoveryReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            app.registerReceiver(discoveryReceiver, filter)
        }

        viewModelScope.launch {
            session.connected.collect { isConnected ->
                _connectionState.value = when {
                    isConnected -> BtConnectionState.Connected
                    _connectionState.value == BtConnectionState.Connected -> BtConnectionState.Disconnected
                    else -> _connectionState.value
                }
            }
        }
        viewModelScope.launch {
            session.incoming.collect { wire ->
                val limit = if (PlanManager.isPro) null else FREE_HISTORY_LIMIT
                when {
                    wire.type == "image" && wire.imageData != null -> {
                        val path = withContext(Dispatchers.IO) { ChatMediaUtils.saveBase64ToFile(getApplication(), wire.imageData, MediaKind.IMAGE) }
                        if (path != null) repo.record(wire.text, isMine = false, freeLimit = limit, type = ChatMsgType.IMAGE, mediaPath = path)
                    }
                    wire.type == "audio" && wire.imageData != null -> {
                        val path = withContext(Dispatchers.IO) { ChatMediaUtils.saveBase64ToFile(getApplication(), wire.imageData, MediaKind.AUDIO) }
                        if (path != null) repo.record(wire.text, isMine = false, freeLimit = limit, type = ChatMsgType.AUDIO, mediaPath = path)
                    }
                    wire.type == "video" && wire.imageData != null -> {
                        val path = withContext(Dispatchers.IO) { ChatMediaUtils.saveBase64ToFile(getApplication(), wire.imageData, MediaKind.VIDEO) }
                        if (path != null) repo.record(wire.text, isMine = false, freeLimit = limit, type = ChatMsgType.VIDEO, mediaPath = path)
                    }
                    else -> repo.record(wire.text, isMine = false, freeLimit = limit)
                }
            }
        }
    }

    fun isBluetoothSupported(): Boolean = adapter != null
    fun isBluetoothEnabled(): Boolean = adapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun loadPairedDevices() {
        if (!hasBluetoothConnectPermission()) return
        val bonded = try { adapter?.bondedDevices } catch (e: SecurityException) { null } ?: return
        _pairedDevices.value = bonded.map {
            val name = try { it.name ?: "Unknown" } catch (e: SecurityException) { "Unknown" }
            BtDeviceInfo(name, it.address, it)
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (!hasBluetoothConnectPermission()) return
        val a = adapter ?: return
        _discoveredDevices.value = emptyList()
        try {
            if (a.isDiscovering) a.cancelDiscovery()
            _isScanning.value = a.startDiscovery()
        } catch (e: SecurityException) {
            _isScanning.value = false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        try { adapter?.cancelDiscovery() } catch (e: SecurityException) { }
        _isScanning.value = false
    }

    fun startHosting() {
        val a = adapter ?: return
        _connectionState.value = BtConnectionState.Waiting
        session.startHost(a)
    }

    fun connectTo(deviceInfo: BtDeviceInfo) {
        val a = adapter ?: return
        _connectionState.value = BtConnectionState.Connecting
        session.startJoin(deviceInfo.device, a)
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
            try {
                val base64 = withContext(Dispatchers.IO) { ImageCompressUtils.uriToBase64Jpeg(getApplication(), uri) }
                if (base64 == null) { _sendError.tryEmit("Could not send photo"); return@launch }
                val path = withContext(Dispatchers.IO) { ChatMediaUtils.saveBase64ToFile(getApplication(), base64, MediaKind.IMAGE) }
                if (path == null) { _sendError.tryEmit("Could not save photo"); return@launch }
                withContext(Dispatchers.IO) { session.sendImage(base64) }
                repo.record("[Photo]", isMine = true, freeLimit = null, type = ChatMsgType.IMAGE, mediaPath = path)
            } catch (e: Throwable) {
                _sendError.tryEmit("Could not send photo")
            }
        }
    }

    fun sendAudio(base64: String) {
        if (!PlanManager.isPro) return
        viewModelScope.launch {
            try {
                val path = withContext(Dispatchers.IO) { ChatMediaUtils.saveBase64ToFile(getApplication(), base64, MediaKind.AUDIO) }
                if (path == null) { _sendError.tryEmit("Could not save voice message"); return@launch }
                withContext(Dispatchers.IO) { session.sendAudio(base64) }
                repo.record("[Voice message]", isMine = true, freeLimit = null, type = ChatMsgType.AUDIO, mediaPath = path)
            } catch (e: Throwable) {
                _sendError.tryEmit("Could not send voice message")
            }
        }
    }

    fun sendVideo(uri: Uri) {
        if (!PlanManager.isPro) return
        val size = VideoUtils.getSizeBytes(getApplication(), uri)
        if (size <= 0 || size > VideoUtils.MAX_VIDEO_BYTES) { _sendError.tryEmit("Video must be under 5 MB"); return }
        viewModelScope.launch {
            try {
                val base64 = withContext(Dispatchers.IO) { VideoUtils.uriToBase64(getApplication(), uri) }
                if (base64 == null) { _sendError.tryEmit("Could not send video — file too large or unreadable"); return@launch }
                val path = withContext(Dispatchers.IO) { ChatMediaUtils.saveBase64ToFile(getApplication(), base64, MediaKind.VIDEO) }
                if (path == null) { _sendError.tryEmit("Could not save video"); return@launch }
                withContext(Dispatchers.IO) { session.sendVideo(base64) }
                repo.record("[Video]", isMine = true, freeLimit = null, type = ChatMsgType.VIDEO, mediaPath = path)
            } catch (e: Throwable) {
                _sendError.tryEmit("Could not send video — try a smaller file")
            }
        }
    }

    fun disconnect() {
        session.close()
        _connectionState.value = BtConnectionState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        session.close()
        try { getApplication<Application>().unregisterReceiver(discoveryReceiver) } catch (e: Exception) { }
    }
}
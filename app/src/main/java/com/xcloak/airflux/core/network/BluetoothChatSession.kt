package com.xcloak.airflux.core.network

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

data class BtWireMessage(
    val text: String,
    val timestamp: Long,
    val type: String = "text",
    val imageData: String? = null
)

class BluetoothChatSession(private val scope: CoroutineScope) {

    companion object {
        val AIRFLUX_UUID: UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66")
    }

    private var socket: BluetoothSocket? = null
    private var serverSocket: BluetoothServerSocket? = null
    private var writer: PrintWriter? = null
    private val active = AtomicBoolean(false)

    // See ChatSession's writeMutex for why this is needed: without it, concurrent
    // send()/sendImage()/sendAudio()/sendVideo() calls can interleave their println()
    // writes and corrupt the line-based wire protocol.
    private val writeMutex = Mutex()

    private val _incoming = MutableSharedFlow<BtWireMessage>(extraBufferCapacity = 64)
    val incoming: SharedFlow<BtWireMessage> = _incoming

    private val _connected = MutableSharedFlow<Boolean>(replay = 1)
    val connected: SharedFlow<Boolean> = _connected

    @SuppressLint("MissingPermission")
    fun startHost(adapter: BluetoothAdapter) {
        scope.launch(Dispatchers.IO) {
            try {
                val server = adapter.listenUsingRfcommWithServiceRecord("AirFluxChat", AIRFLUX_UUID)
                serverSocket = server
                val client = server.accept()
                socket = client
                server.close()
                serverSocket = null
                beginStreams(client)
            } catch (e: Exception) {
                _connected.emit(false)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startJoin(device: BluetoothDevice, adapter: BluetoothAdapter) {
        scope.launch(Dispatchers.IO) {
            try {
                adapter.cancelDiscovery()
                val client = device.createRfcommSocketToServiceRecord(AIRFLUX_UUID)
                client.connect()
                socket = client
                beginStreams(client)
            } catch (e: Exception) {
                _connected.emit(false)
            }
        }
    }

    private suspend fun beginStreams(client: BluetoothSocket) {
        val r = BufferedReader(InputStreamReader(client.inputStream))
        val w = PrintWriter(client.outputStream, true)
        writer = w
        active.set(true)
        _connected.emit(true)
        try {
            while (active.get()) {
                val line = r.readLine() ?: break
                try {
                    val obj = JSONObject(line)
                    _incoming.emit(
                        BtWireMessage(
                            text = obj.optString("text", ""),
                            timestamp = obj.getLong("ts"),
                            type = obj.optString("type", "text"),
                            imageData = if (obj.has("img")) obj.optString("img", null) else null
                        )
                    )
                } catch (e: Exception) { }
            }
        } catch (e: Exception) {
        } finally {
            active.set(false)
            _connected.emit(false)
        }
    }

    private fun sendWireLine(json: JSONObject) {
        val w = writer ?: return
        scope.launch(Dispatchers.IO) {
            writeMutex.withLock {
                try {
                    w.println(json.toString())
                } catch (e: Exception) { }
            }
        }
    }

    fun send(text: String) {
        sendWireLine(JSONObject().put("text", text).put("ts", System.currentTimeMillis()).put("type", "text"))
    }

    fun sendImage(base64: String) {
        sendWireLine(JSONObject().put("text", "[Photo]").put("ts", System.currentTimeMillis()).put("type", "image").put("img", base64))
    }

    fun sendVideo(base64: String) {
        sendWireLine(JSONObject().put("text", "[Video]").put("ts", System.currentTimeMillis()).put("type", "video").put("img", base64))
    }

    fun sendAudio(base64: String) {
        sendWireLine(JSONObject().put("text", "[Voice message]").put("ts", System.currentTimeMillis()).put("type", "audio").put("img", base64))
    }

    fun close() {
        active.set(false)
        try { socket?.close() } catch (e: Exception) { }
        try { serverSocket?.close() } catch (e: Exception) { }
    }
}
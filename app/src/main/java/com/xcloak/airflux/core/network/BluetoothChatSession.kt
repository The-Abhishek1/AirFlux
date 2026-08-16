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
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

data class BtWireMessage(
    val text: String,
    val timestamp: Long,
    val type: String = "text", // "text" or "image"
    val imageData: String? = null
)

class BluetoothChatSession(private val scope: CoroutineScope) {

    companion object {
        // Fixed, well-known UUID for AirFlux's own RFCOMM service — both host and joiner must match.
        val AIRFLUX_UUID: UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66")
    }

    private var socket: BluetoothSocket? = null
    private var serverSocket: BluetoothServerSocket? = null
    private var writer: PrintWriter? = null
    private val active = AtomicBoolean(false)

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
                val client = server.accept() // blocks until a device connects
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
                adapter.cancelDiscovery() // discovery drastically slows the connection attempt
                val client = device.createRfcommSocketToServiceRecord(AIRFLUX_UUID)
                client.connect() // blocking connect
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

    fun send(text: String) {
        val w = writer ?: return
        scope.launch(Dispatchers.IO) {
            try {
                val json = JSONObject().put("text", text).put("ts", System.currentTimeMillis()).put("type", "text")
                w.println(json.toString())
            } catch (e: Exception) { }
        }
    }

    fun sendImage(base64: String) {
        val w = writer ?: return
        scope.launch(Dispatchers.IO) {
            try {
                val json = JSONObject().put("text", "[Photo]").put("ts", System.currentTimeMillis()).put("type", "image").put("img", base64)
                w.println(json.toString())
            } catch (e: Exception) { }
        }
    }

    fun close() {
        active.set(false)
        try { socket?.close() } catch (e: Exception) { }
        try { serverSocket?.close() } catch (e: Exception) { }
    }
}
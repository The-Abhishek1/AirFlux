package com.xcloak.airflux.core.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

data class ChatWireMessage(val text: String, val timestamp: Long)

class ChatSession(private val scope: CoroutineScope) {

    private var socket: Socket? = null
    private var serverSocket: ServerSocket? = null
    private var writer: PrintWriter? = null

    private val _incoming = MutableSharedFlow<ChatWireMessage>(extraBufferCapacity = 64)
    val incoming: SharedFlow<ChatWireMessage> = _incoming

    private val _connected = MutableSharedFlow<Boolean>(replay = 1)
    val connected: SharedFlow<Boolean> = _connected

    private val active = AtomicBoolean(false)

    /** Host mode: opens a server socket, waits for exactly one client, verifies the token. */
    fun startHost(port: Int, expectedToken: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val server = ServerSocket(port)
                serverSocket = server
                val client = server.accept()
                socket = client
                val r = BufferedReader(InputStreamReader(client.getInputStream()))
                val w = PrintWriter(client.getOutputStream(), true)

                val authLine = r.readLine() ?: throw Exception("No auth received")
                if (authLine != "AUTH:$expectedToken") {
                    w.println("REJECTED")
                    client.close()
                    _connected.emit(false)
                    return@launch
                }
                w.println("OK")
                writer = w
                active.set(true)
                _connected.emit(true)
                listenLoop(r)
            } catch (e: Exception) {
                _connected.emit(false)
            }
        }
    }

    /** Join mode: connects to a host's IP/port and authenticates with the token. */
    fun startJoin(ip: String, port: Int, token: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val s = Socket(ip, port)
                socket = s
                val r = BufferedReader(InputStreamReader(s.getInputStream()))
                val w = PrintWriter(s.getOutputStream(), true)
                w.println("AUTH:$token")
                if (r.readLine() != "OK") {
                    s.close()
                    _connected.emit(false)
                    return@launch
                }
                writer = w
                active.set(true)
                _connected.emit(true)
                listenLoop(r)
            } catch (e: Exception) {
                _connected.emit(false)
            }
        }
    }

    private suspend fun listenLoop(r: BufferedReader) {
        try {
            while (active.get()) {
                val line = r.readLine() ?: break
                try {
                    val obj = JSONObject(line)
                    _incoming.emit(ChatWireMessage(obj.getString("text"), obj.getLong("ts")))
                } catch (e: Exception) { /* skip malformed line */ }
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
                val json = JSONObject().put("text", text).put("ts", System.currentTimeMillis())
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
package com.xcloak.airflux.feature.sharing.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import com.xcloak.airflux.core.common.FileUtils
import com.xcloak.airflux.core.network.LocalFileServer
import com.xcloak.airflux.core.network.NetworkUtils
import com.xcloak.airflux.domain.model.SelectedFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException

sealed class ServerStatus {
    object Stopped : ServerStatus()
    data class Running(val url: String) : ServerStatus()
    data class Error(val message: String) : ServerStatus()
}

class SharingViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val PORT = 8080
    }

    private val _selectedFiles = MutableStateFlow<List<SelectedFile>>(emptyList())
    val selectedFiles: StateFlow<List<SelectedFile>> = _selectedFiles.asStateFlow()

    private val _serverStatus = MutableStateFlow<ServerStatus>(ServerStatus.Stopped)
    val serverStatus: StateFlow<ServerStatus> = _serverStatus.asStateFlow()

    private var server: LocalFileServer? = null

    fun addFiles(context: Context, uris: List<Uri>) {
        val resolved = uris.mapNotNull { FileUtils.resolveSelectedFile(context, it) }
        _selectedFiles.value = _selectedFiles.value + resolved
    }

    fun removeFile(file: SelectedFile) {
        _selectedFiles.value = _selectedFiles.value.filter { it.uri != file.uri }
    }

    fun clearAll() {
        _selectedFiles.value = emptyList()
    }

    fun startServer() {
        if (server != null) return

        val ip = NetworkUtils.getLocalIpAddress()
        if (ip == null) {
            _serverStatus.value = ServerStatus.Error("No Wi-Fi connection detected")
            return
        }

        try {
            val newServer = LocalFileServer(
                context = getApplication(),
                port = PORT,
                filesProvider = { _selectedFiles.value }
            )
            newServer.start(fi.iki.elonen.NanoHTTPD.SOCKET_READ_TIMEOUT, false)
            server = newServer
            _serverStatus.value = ServerStatus.Running("http://$ip:$PORT")
        } catch (e: IOException) {
            _serverStatus.value = ServerStatus.Error(e.message ?: "Failed to start server")
        }
    }

    fun stopServer() {
        server?.stop()
        server = null
        _serverStatus.value = ServerStatus.Stopped
    }

    override fun onCleared() {
        super.onCleared()
        stopServer()
    }
}
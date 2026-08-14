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
import kotlinx.coroutines.launch

sealed class ServerStatus {
    object Stopped : ServerStatus()
    data class Running(val url: String) : ServerStatus()
    data class Error(val message: String) : ServerStatus()
}

class SharingViewModel(application: Application) : AndroidViewModel(application) {

    private val _selectedFiles = MutableStateFlow<List<SelectedFile>>(emptyList())
    val selectedFiles: StateFlow<List<SelectedFile>> = _selectedFiles.asStateFlow()

    private val _serverStatus = MutableStateFlow<ServerStatus>(ServerStatus.Stopped)
    val serverStatus: StateFlow<ServerStatus> = _serverStatus.asStateFlow()

    private var server: LocalFileServer? = null

    fun addFiles(context: Context, uris: List<Uri>) {
        val newFiles = uris.mapNotNull { FileUtils.resolveSelectedFile(context, it) }
        _selectedFiles.value = _selectedFiles.value + newFiles
    }

    fun removeFile(file: SelectedFile) {
        _selectedFiles.value = _selectedFiles.value - file
    }

    fun startServer() {
        if (server != null) return

        val ip = NetworkUtils.getLocalIpAddress()
        if (ip == null) {
            _serverStatus.value = ServerStatus.Error("No Wi-Fi connection found")
            return
        }

        try {
            val port = 8080
            server = LocalFileServer(getApplication(), port) {
                _selectedFiles.value
            }
            server?.start()
            _serverStatus.value = ServerStatus.Running("http://$ip:$port")
        } catch (e: Exception) {
            _serverStatus.value = ServerStatus.Error(e.message ?: "Failed to start server")
            server = null
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

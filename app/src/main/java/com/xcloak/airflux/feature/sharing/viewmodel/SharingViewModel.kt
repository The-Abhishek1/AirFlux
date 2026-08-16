package com.xcloak.airflux.feature.sharing.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import com.xcloak.airflux.core.billing.PlanManager
import com.xcloak.airflux.core.common.FileUtils
import com.xcloak.airflux.core.network.LocalFileServer
import com.xcloak.airflux.core.network.NetworkUtils
import com.xcloak.airflux.core.security.SecurityUtils
import com.xcloak.airflux.domain.model.SelectedFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

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

    private val _encryptionEnabled = MutableStateFlow(false)
    val encryptionEnabled: StateFlow<Boolean> = _encryptionEnabled.asStateFlow()

    private val _connectedDeviceCount = MutableStateFlow(0)
    val connectedDeviceCount: StateFlow<Int> = _connectedDeviceCount.asStateFlow()

    // Distinct client IPs seen since sharing started — approximates "devices connected".
    private val seenClients = ConcurrentHashMap.newKeySet<String>()

    private var server: LocalFileServer? = null

    fun setEncryptionEnabled(enabled: Boolean) {
        if (!PlanManager.isPro) return
        _encryptionEnabled.value = enabled
    }

    fun addFiles(context: Context, uris: List<Uri>) {
        val resolved = uris.mapNotNull { FileUtils.resolveSelectedFile(context, it) }
        _selectedFiles.value = _selectedFiles.value + resolved
    }

    fun addFolder(context: Context, treeUri: Uri) {
        if (!PlanManager.isPro) return
        try {
            context.contentResolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: SecurityException) { }

        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return
        val collected = mutableListOf<SelectedFile>()
        collectFiles(root, collected, depth = 0)
        _selectedFiles.value = _selectedFiles.value + collected
    }

    private fun collectFiles(dir: DocumentFile, out: MutableList<SelectedFile>, depth: Int) {
        if (depth > 5) return
        dir.listFiles().forEach { child ->
            when {
                child.isDirectory -> collectFiles(child, out, depth + 1)
                child.isFile -> FileUtils.resolveFromDocumentFile(child)?.let { out.add(it) }
            }
        }
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
            seenClients.clear()
            _connectedDeviceCount.value = 0

            val token = SecurityUtils.generateSessionToken()
            val maxReceivers = if (PlanManager.isPro) Int.MAX_VALUE else 1
            val newServer = LocalFileServer(
                context = getApplication(),
                port = PORT,
                sessionToken = token,
                encryptionEnabled = _encryptionEnabled.value,
                maxReceivers = maxReceivers,
                filesProvider = { _selectedFiles.value },
                onClientSeen = { clientIp ->
                    if (seenClients.add(clientIp)) {
                        _connectedDeviceCount.value = seenClients.size
                    }
                }
            )
            newServer.start(fi.iki.elonen.NanoHTTPD.SOCKET_READ_TIMEOUT, false)
            server = newServer
            val encFlag = if (_encryptionEnabled.value) "&enc=1" else ""
            _serverStatus.value = ServerStatus.Running("http://$ip:$PORT?token=$token$encFlag")
        } catch (e: IOException) {
            _serverStatus.value = ServerStatus.Error(e.message ?: "Failed to start server")
        }
    }

    fun stopServer() {
        server?.stop()
        server = null
        seenClients.clear()
        _connectedDeviceCount.value = 0
        _serverStatus.value = ServerStatus.Stopped
    }

    override fun onCleared() {
        super.onCleared()
        stopServer()
    }
}
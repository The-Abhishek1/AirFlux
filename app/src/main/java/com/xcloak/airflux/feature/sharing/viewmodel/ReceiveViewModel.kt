package com.xcloak.airflux.feature.sharing.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xcloak.airflux.core.common.DownloadUtils
import com.xcloak.airflux.domain.model.RemoteFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

sealed class ConnectionState {
    object Idle : ConnectionState()
    object Loading : ConnectionState()
    data class Connected(val files: List<RemoteFile>) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

data class DownloadProgress(val fileIndex: Int, val progress: Float, val done: Boolean)

class ReceiveViewModel(application: Application) : AndroidViewModel(application) {

    private val client = OkHttpClient()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Map<Int, DownloadProgress>>(emptyMap())
    val downloadProgress: StateFlow<Map<Int, DownloadProgress>> = _downloadProgress.asStateFlow()

    private var baseUrl: String = ""

    fun connect(address: String) {
        baseUrl = if (address.startsWith("http")) address else "http://$address"
        _connectionState.value = ConnectionState.Loading

        viewModelScope.launch {
            try {
                val files = withContext(Dispatchers.IO) { fetchFileList(baseUrl) }
                _connectionState.value = ConnectionState.Connected(files)
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.Error(e.message ?: "Failed to connect")
            }
        }
    }

    private fun fetchFileList(base: String): List<RemoteFile> {
        val request = Request.Builder().url("$base/files.json").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Server returned ${response.code}")
            val bodyText = response.body?.string() ?: throw Exception("Empty response")
            val array = JSONArray(bodyText)
            return (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                RemoteFile(
                    index = obj.getInt("index"),
                    name = obj.getString("name"),
                    sizeBytes = obj.getLong("size"),
                    mimeType = obj.getString("mimeType")
                )
            }
        }
    }

    fun downloadFile(file: RemoteFile) {
        viewModelScope.launch {
            _downloadProgress.value = _downloadProgress.value + (file.index to DownloadProgress(file.index, 0f, false))

            val success = withContext(Dispatchers.IO) {
                DownloadUtils.downloadFile(
                    context = getApplication(),
                    url = "$baseUrl/file/${file.index}",
                    fileName = file.name,
                    mimeType = file.mimeType
                ) { bytesRead, totalBytes ->
                    val progress = if (totalBytes > 0) bytesRead.toFloat() / totalBytes else 0f
                    _downloadProgress.value = _downloadProgress.value + (file.index to DownloadProgress(file.index, progress, false))
                }
            }

            _downloadProgress.value = _downloadProgress.value + (file.index to DownloadProgress(file.index, 1f, success))
        }
    }
}
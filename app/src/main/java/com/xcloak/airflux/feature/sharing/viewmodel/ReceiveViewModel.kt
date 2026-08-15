package com.xcloak.airflux.feature.sharing.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xcloak.airflux.core.billing.PlanManager
import com.xcloak.airflux.core.common.DownloadUtils
import com.xcloak.airflux.domain.model.RemoteFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.ConcurrentLinkedQueue

sealed class ConnectionState {
    object Idle : ConnectionState()
    object Loading : ConnectionState()
    data class Connected(val files: List<RemoteFile>) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

enum class TransferStatus { QUEUED, DOWNLOADING, DONE, FAILED, CANCELLED }

data class DownloadProgress(
    val fileIndex: Int,
    val progress: Float = 0f,
    val status: TransferStatus = TransferStatus.QUEUED,
    val speedBytesPerSec: Long = 0,
    val etaSeconds: Long = -1
)

class ReceiveViewModel(application: Application) : AndroidViewModel(application) {

    private val client = OkHttpClient()

    private val historyRepo = com.xcloak.airflux.data.repository.HistoryRepository(application)

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Map<Int, DownloadProgress>>(emptyMap())
    val downloadProgress: StateFlow<Map<Int, DownloadProgress>> = _downloadProgress.asStateFlow()

    private var baseUrl: String = ""
    private var sessionToken: String = ""
    private val activeCalls = mutableMapOf<Int, Call>()
    private val activeJobs = mutableMapOf<Int, Job>()
    private val pendingQueue = ConcurrentLinkedQueue<RemoteFile>()
    private var activeCount = 0

    fun connect(address: String) {
        val full = if (address.startsWith("http")) address else "http://$address"
        val parts = full.split("?token=")
        baseUrl = parts[0]
        sessionToken = parts.getOrNull(1) ?: ""

        _connectionState.value = ConnectionState.Loading

        viewModelScope.launch {
            try {
                val files = withContext(Dispatchers.IO) { fetchFileList() }
                _connectionState.value = ConnectionState.Connected(files)
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.Error(e.message ?: "Failed to connect")
            }
        }
    }

    private fun fetchFileList(): List<RemoteFile> {
        val request = Request.Builder().url("$baseUrl/files.json?token=$sessionToken").build()
        client.newCall(request).execute().use { response ->
            if (response.code == 401) throw Exception("Invalid or expired session token")
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
        val current = _downloadProgress.value[file.index]
        if (current != null && (current.status == TransferStatus.DOWNLOADING || current.status == TransferStatus.QUEUED)) return

        if (activeCount < PlanManager.maxConcurrentTransfers()) {
            startDownload(file)
        } else {
            updateProgress(file.index, DownloadProgress(file.index, 0f, TransferStatus.QUEUED))
            pendingQueue.add(file)
        }
    }

    fun cancelDownload(fileIndex: Int) {
        activeCalls[fileIndex]?.cancel()
        activeJobs[fileIndex]?.cancel()
        activeCalls.remove(fileIndex)
        activeJobs.remove(fileIndex)
        updateProgress(fileIndex, DownloadProgress(fileIndex, 0f, TransferStatus.CANCELLED))
    }

    fun retryDownload(file: RemoteFile) {
        downloadFile(file)
    }

    private fun startDownload(file: RemoteFile) {
        activeCount++
        updateProgress(file.index, DownloadProgress(file.index, 0f, TransferStatus.DOWNLOADING))

        val call = DownloadUtils.buildCall(client, "$baseUrl/file/${file.index}?token=$sessionToken")
        activeCalls[file.index] = call

        val job = viewModelScope.launch {
            var lastBytes = 0L
            var lastTime = System.currentTimeMillis()

            val success = try {
                withContext(Dispatchers.IO) {
                    DownloadUtils.executeAndSave(
                        context = getApplication(),
                        call = call,
                        fileName = file.name,
                        mimeType = file.mimeType
                    ) { bytesRead, totalBytes ->
                        val now = System.currentTimeMillis()
                        val elapsed = now - lastTime
                        if (elapsed >= 500) {
                            val speed = ((bytesRead - lastBytes) * 1000L) / elapsed.coerceAtLeast(1)
                            val remaining = totalBytes - bytesRead
                            val eta = if (speed > 0) remaining / speed else -1
                            val progress = if (totalBytes > 0) bytesRead.toFloat() / totalBytes else 0f
                            updateProgress(
                                file.index,
                                DownloadProgress(file.index, progress, TransferStatus.DOWNLOADING, speed, eta)
                            )
                            lastBytes = bytesRead
                            lastTime = now
                        }
                    }
                }
            } catch (e: Exception) {
                false
            }

            activeCalls.remove(file.index)
            activeJobs.remove(file.index)
            activeCount--

            val wasCancelled = _downloadProgress.value[file.index]?.status == TransferStatus.CANCELLED
            val finalStatus = when {
                wasCancelled -> TransferStatus.CANCELLED
                success -> TransferStatus.DONE
                else -> TransferStatus.FAILED
            }
            updateProgress(file.index, DownloadProgress(file.index, if (success) 1f else 0f, finalStatus))

            if (finalStatus == TransferStatus.DONE || finalStatus == TransferStatus.FAILED) {
                historyRepo.record(file.name, file.sizeBytes, file.mimeType, com.xcloak.airflux.data.database.entity.HistoryType.RECEIVED, success)
            }

            processQueue()
        }
        activeJobs[file.index] = job
    }

    private fun processQueue() {
        while (activeCount < PlanManager.maxConcurrentTransfers()) {
            val next = pendingQueue.poll() ?: break
            startDownload(next)
        }
    }

    private fun updateProgress(index: Int, progress: DownloadProgress) {
        _downloadProgress.value = _downloadProgress.value + (index to progress)
    }
}
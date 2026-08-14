package com.xcloak.airflux.feature.downloader.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xcloak.airflux.core.billing.PlanManager
import com.xcloak.airflux.core.common.DownloadUtils
import com.xcloak.airflux.core.common.UrlUtils
import com.xcloak.airflux.core.notification.NotificationHelper
import com.xcloak.airflux.core.service.DownloadForegroundService
import com.xcloak.airflux.domain.model.UrlDownloadItem
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
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

enum class DlStatus { RESOLVING, QUEUED, DOWNLOADING, DONE, FAILED, CANCELLED }

data class DlProgress(
    val progress: Float = 0f,
    val status: DlStatus = DlStatus.RESOLVING,
    val speedBytesPerSec: Long = 0,
    val etaSeconds: Long = -1,
    val errorMessage: String? = null
)

class DownloaderViewModel(application: Application) : AndroidViewModel(application) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val _items = MutableStateFlow<List<UrlDownloadItem>>(emptyList())
    val items: StateFlow<List<UrlDownloadItem>> = _items.asStateFlow()

    private val _progress = MutableStateFlow<Map<String, DlProgress>>(emptyMap())
    val progress: StateFlow<Map<String, DlProgress>> = _progress.asStateFlow()

    private val activeCalls = mutableMapOf<String, Call>()
    private val activeJobs = mutableMapOf<String, Job>()
    private val pendingQueue = ConcurrentLinkedQueue<UrlDownloadItem>()
    private var activeCount = 0
    private var serviceRunning = false

    fun addDownload(url: String) {
        if (!UrlUtils.isValidUrl(url)) return
        val id = UUID.randomUUID().toString()

        val placeholder = UrlDownloadItem(id, url, "Resolving link...", "application/octet-stream", -1L)
        _items.value = _items.value + placeholder
        updateProgress(id, DlProgress(status = DlStatus.RESOLVING))

        viewModelScope.launch {
            val resolved = withContext(Dispatchers.IO) { resolveMetadata(url) }
            if (resolved == null) {
                updateProgress(id, DlProgress(status = DlStatus.FAILED, errorMessage = "Could not read file info from this link"))
                return@launch
            }
            val item = UrlDownloadItem(id, url, resolved.first, resolved.second, resolved.third)
            _items.value = _items.value.map { if (it.id == id) item else it }
            enqueue(item)
        }
    }

    private fun resolveMetadata(url: String): Triple<String, String, Long>? {
        try {
            val headRequest = Request.Builder().url(url).head().build()
            client.newCall(headRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val fileName = UrlUtils.extractFileName(url, response.header("Content-Disposition"))
                    val mimeType = UrlUtils.guessMimeType(fileName, response.header("Content-Type"))
                    val size = response.header("Content-Length")?.toLongOrNull() ?: -1L
                    return Triple(fileName, mimeType, size)
                }
            }
        } catch (e: Exception) { }

        return try {
            val getRequest = Request.Builder().url(url).header("Range", "bytes=0-0").build()
            client.newCall(getRequest).execute().use { response ->
                if (!response.isSuccessful && response.code != 206) return null
                val fileName = UrlUtils.extractFileName(url, response.header("Content-Disposition"))
                val mimeType = UrlUtils.guessMimeType(fileName, response.header("Content-Type"))
                val contentRange = response.header("Content-Range")
                val size = contentRange?.substringAfterLast('/')?.toLongOrNull()
                    ?: response.header("Content-Length")?.toLongOrNull()
                    ?: -1L
                Triple(fileName, mimeType, size)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun enqueue(item: UrlDownloadItem) {
        if (activeCount < PlanManager.maxConcurrentTransfers()) {
            startDownload(item)
        } else {
            updateProgress(item.id, DlProgress(status = DlStatus.QUEUED))
            pendingQueue.add(item)
        }
    }

    fun cancelDownload(id: String) {
        activeCalls[id]?.cancel()
        activeJobs[id]?.cancel()
        activeCalls.remove(id)
        activeJobs.remove(id)
        updateProgress(id, DlProgress(status = DlStatus.CANCELLED))
    }

    fun retryDownload(item: UrlDownloadItem) {
        enqueue(item)
    }

    private fun startDownload(item: UrlDownloadItem) {
        activeCount++
        ensureServiceRunning()
        updateProgress(item.id, DlProgress(status = DlStatus.DOWNLOADING))

        val call = DownloadUtils.buildCall(client, item.url)
        activeCalls[item.id] = call

        val job = viewModelScope.launch {
            var lastBytes = 0L
            var lastTime = System.currentTimeMillis()
            var lastNotifyTime = 0L

            val success = try {
                withContext(Dispatchers.IO) {
                    DownloadUtils.executeAndSave(
                        context = getApplication(),
                        call = call,
                        fileName = item.fileName,
                        mimeType = item.mimeType
                    ) { bytesRead, totalBytes ->
                        val now = System.currentTimeMillis()
                        val elapsed = now - lastTime
                        if (elapsed >= 500) {
                            val speed = ((bytesRead - lastBytes) * 1000L) / elapsed.coerceAtLeast(1)
                            val total = if (totalBytes > 0) totalBytes else item.sizeBytes
                            val remaining = total - bytesRead
                            val eta = if (speed > 0 && total > 0) remaining / speed else -1
                            val progressFraction = if (total > 0) bytesRead.toFloat() / total else 0f
                            updateProgress(item.id, DlProgress(progressFraction, DlStatus.DOWNLOADING, speed, eta))
                            lastBytes = bytesRead
                            lastTime = now
                        }
                        if (now - lastNotifyTime >= 800) {
                            val total = if (totalBytes > 0) totalBytes else item.sizeBytes
                            val percent = if (total > 0) ((bytesRead * 100) / total).toInt() else 0
                            NotificationHelper.notify(
                                getApplication(),
                                NotificationHelper.SUMMARY_NOTIFICATION_ID,
                                NotificationHelper.buildProgressNotification(getApplication(), item.fileName, percent, total <= 0)
                            )
                            lastNotifyTime = now
                        }
                    }
                }
            } catch (e: Exception) {
                false
            }

            activeCalls.remove(item.id)
            activeJobs.remove(item.id)
            activeCount--

            val wasCancelled = _progress.value[item.id]?.status == DlStatus.CANCELLED
            val finalStatus = when {
                wasCancelled -> DlStatus.CANCELLED
                success -> DlStatus.DONE
                else -> DlStatus.FAILED
            }
            val errorMsg = if (finalStatus == DlStatus.FAILED) "Download failed — check connection and retry" else null
            updateProgress(item.id, DlProgress(if (success) 1f else 0f, finalStatus, errorMessage = errorMsg))

            if (success) {
                NotificationHelper.notify(
                    getApplication(),
                    NotificationHelper.SUMMARY_NOTIFICATION_ID,
                    NotificationHelper.buildCompleteNotification(getApplication(), item.fileName)
                )
            }

            processQueue()
            stopServiceIfIdle()
        }
        activeJobs[item.id] = job
    }

    private fun processQueue() {
        while (activeCount < PlanManager.maxConcurrentTransfers()) {
            val next = pendingQueue.poll() ?: break
            startDownload(next)
        }
    }

    private fun ensureServiceRunning() {
        if (serviceRunning) return
        val context: Application = getApplication()
        val intent = Intent(context, DownloadForegroundService::class.java)
        context.startService(intent)
        serviceRunning = true
    }

    private fun stopServiceIfIdle() {
        if (activeCount > 0 || pendingQueue.isNotEmpty()) return
        val context: Application = getApplication()
        context.stopService(Intent(context, DownloadForegroundService::class.java))
        serviceRunning = false
    }

    private fun updateProgress(id: String, progress: DlProgress) {
        _progress.value = _progress.value + (id to progress)
    }
}
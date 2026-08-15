package com.xcloak.airflux.feature.downloader.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xcloak.airflux.core.billing.PlanManager
import com.xcloak.airflux.core.common.DownloadResult
import com.xcloak.airflux.core.common.DownloadUtils
import com.xcloak.airflux.core.common.StorageUtils
import com.xcloak.airflux.core.common.UrlUtils
import com.xcloak.airflux.core.network.HttpClientProvider
import com.xcloak.airflux.core.notification.NotificationHelper
import com.xcloak.airflux.core.service.DownloadForegroundService
import com.xcloak.airflux.data.database.entity.HistoryType
import com.xcloak.airflux.data.repository.HistoryRepository
import com.xcloak.airflux.domain.model.UrlDownloadItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Request
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

enum class DlStatus { RESOLVING, QUEUED, DOWNLOADING, PAUSED, DONE, FAILED, CANCELLED }

data class DlProgress(
    val progress: Float = 0f,
    val status: DlStatus = DlStatus.RESOLVING,
    val speedBytesPerSec: Long = 0,
    val etaSeconds: Long = -1,
    val errorMessage: String? = null,
    val bytesDownloaded: Long = 0
)

class DownloaderViewModel(application: Application) : AndroidViewModel(application) {

    private val client = HttpClientProvider.client
    private val historyRepo = HistoryRepository(application)

    private val _items = MutableStateFlow<List<UrlDownloadItem>>(emptyList())
    val items: StateFlow<List<UrlDownloadItem>> = _items.asStateFlow()

    private val _progress = MutableStateFlow<Map<String, DlProgress>>(emptyMap())
    val progress: StateFlow<Map<String, DlProgress>> = _progress.asStateFlow()

    private val activeCalls = mutableMapOf<String, Call>()
    private val activeJobs = mutableMapOf<String, Job>()
    private val pendingQueue = ConcurrentLinkedQueue<UrlDownloadItem>()
    private var activeCount = 0

    private val savedMediaUri = mutableMapOf<String, Uri>()
    // Unthrottled byte checkpoint — read on pause so resume never duplicates bytes.
    private val liveBytes = ConcurrentHashMap<String, Long>()

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

    fun addBatch(urls: List<String>) {
        if (!PlanManager.isPro) return
        urls.map { it.trim() }.filter { it.startsWith("http") }.forEach { addDownload(it) }
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
            startDownload(item, resumeFromByte = 0)
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
        savedMediaUri.remove(id)
        liveBytes.remove(id)
        updateProgress(id, DlProgress(status = DlStatus.CANCELLED))
    }

    /** Pauses an in-progress download (Pro only). Keeps the partial file + exact byte offset. */
    fun pauseDownload(id: String) {
        if (!PlanManager.isPro) return
        val current = _progress.value[id] ?: return
        activeCalls[id]?.cancel()
        activeJobs[id]?.cancel()
        activeCalls.remove(id)
        activeJobs.remove(id)
        val checkpoint = liveBytes[id] ?: current.bytesDownloaded
        updateProgress(id, current.copy(status = DlStatus.PAUSED, bytesDownloaded = checkpoint))
    }

    fun resumeDownload(item: UrlDownloadItem) {
        if (!PlanManager.isPro) {
            retryDownload(item)
            return
        }
        val resumeFrom = _progress.value[item.id]?.bytesDownloaded ?: 0L
        if (activeCount < PlanManager.maxConcurrentTransfers()) {
            startDownload(item, resumeFromByte = resumeFrom)
        } else {
            updateProgress(item.id, DlProgress(status = DlStatus.QUEUED, bytesDownloaded = resumeFrom))
            pendingQueue.add(item)
        }
    }

    fun retryDownload(item: UrlDownloadItem) {
        savedMediaUri.remove(item.id)
        liveBytes.remove(item.id)
        enqueue(item)
    }

    private fun startDownload(item: UrlDownloadItem, resumeFromByte: Long) {
        if (!StorageUtils.hasEnoughSpace(item.sizeBytes)) {
            updateProgress(item.id, DlProgress(status = DlStatus.FAILED, errorMessage = "Not enough storage space"))
            return
        }

        activeCount++
        ensureServiceRunning()
        updateProgress(item.id, DlProgress(status = DlStatus.DOWNLOADING, bytesDownloaded = resumeFromByte))
        liveBytes[item.id] = resumeFromByte

        val call = DownloadUtils.buildCall(client, item.url, resumeFromByte)
        activeCalls[item.id] = call

        val job = viewModelScope.launch {
            var lastBytes = resumeFromByte
            var lastTime = System.currentTimeMillis()
            var lastNotifyTime = 0L

            val result = try {
                withContext(Dispatchers.IO) {
                    DownloadUtils.executeAndSave(
                        context = getApplication(),
                        call = call,
                        fileName = item.fileName,
                        mimeType = item.mimeType,
                        existingUri = savedMediaUri[item.id],
                        resumeFromByte = resumeFromByte
                    ) { bytesRead, totalBytes ->
                        liveBytes[item.id] = bytesRead
                        val now = System.currentTimeMillis()
                        val elapsed = now - lastTime
                        if (elapsed >= 500) {
                            val speed = ((bytesRead - lastBytes) * 1000L) / elapsed.coerceAtLeast(1)
                            val total = if (totalBytes > 0) totalBytes else item.sizeBytes
                            val remaining = total - bytesRead
                            val eta = if (speed > 0 && total > 0) remaining / speed else -1
                            val progressFraction = if (total > 0) bytesRead.toFloat() / total else 0f
                            updateProgress(item.id, DlProgress(progressFraction, DlStatus.DOWNLOADING, speed, eta, bytesDownloaded = bytesRead))
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
                DownloadResult(false)
            }

            activeCalls.remove(item.id)
            activeJobs.remove(item.id)
            activeCount--

            val currentStatus = _progress.value[item.id]?.status
            val wasCancelled = currentStatus == DlStatus.CANCELLED
            val wasPaused = currentStatus == DlStatus.PAUSED

            if (result.mediaUri != null) savedMediaUri[item.id] = result.mediaUri

            if (!wasCancelled && !wasPaused) {
                val finalStatus = if (result.success) DlStatus.DONE else DlStatus.FAILED
                val errorMsg = if (finalStatus == DlStatus.FAILED) "Download failed — check connection and retry" else null
                updateProgress(item.id, DlProgress(if (result.success) 1f else 0f, finalStatus, errorMessage = errorMsg, bytesDownloaded = result.bytesWritten))
                liveBytes.remove(item.id)

                historyRepo.record(item.fileName, item.sizeBytes, item.mimeType, HistoryType.DOWNLOADED, result.success)

                if (result.success) {
                    NotificationHelper.notify(
                        getApplication(),
                        NotificationHelper.SUMMARY_NOTIFICATION_ID,
                        NotificationHelper.buildCompleteNotification(getApplication(), item.fileName)
                    )
                }
            }

            processQueue()
            stopServiceIfIdle()
        }
        activeJobs[item.id] = job
    }

    private fun processQueue() {
        while (activeCount < PlanManager.maxConcurrentTransfers()) {
            val next = pendingQueue.poll() ?: break
            val resumeFrom = _progress.value[next.id]?.bytesDownloaded ?: 0L
            startDownload(next, resumeFrom)
        }
    }

    private fun ensureServiceRunning() {
        if (serviceRunning) return
        val context: Application = getApplication()
        context.startService(Intent(context, DownloadForegroundService::class.java))
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
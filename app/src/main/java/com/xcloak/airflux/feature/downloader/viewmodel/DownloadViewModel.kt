package com.xcloak.airflux.feature.downloader.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.xcloak.airflux.core.ads.InterstitialAdManager
import com.xcloak.airflux.core.ads.SpeedBoostManager
import com.xcloak.airflux.core.billing.PlanManager
import com.xcloak.airflux.core.common.DownloadResult
import com.xcloak.airflux.core.common.DownloadUtils
import com.xcloak.airflux.core.common.MetadataResolver
import com.xcloak.airflux.core.common.NetworkStateUtils
import com.xcloak.airflux.core.common.StorageUtils
import com.xcloak.airflux.core.common.UrlUtils
import com.xcloak.airflux.core.network.HttpClientProvider
import com.xcloak.airflux.core.notification.NotificationHelper
import com.xcloak.airflux.core.service.DownloadForegroundService
import com.xcloak.airflux.core.work.ScheduledDownloadWorker
import com.xcloak.airflux.data.database.entity.HistoryType
import com.xcloak.airflux.data.repository.HistoryRepository
import com.xcloak.airflux.data.repository.ScheduledDownloadRepository
import com.xcloak.airflux.domain.model.ScheduledDownload
import com.xcloak.airflux.domain.model.UrlDownloadItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

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
    private val scheduledRepo = ScheduledDownloadRepository(application)

    private val _items = MutableStateFlow<List<UrlDownloadItem>>(emptyList())
    val items: StateFlow<List<UrlDownloadItem>> = _items.asStateFlow()

    private val _progress = MutableStateFlow<Map<String, DlProgress>>(emptyMap())
    val progress: StateFlow<Map<String, DlProgress>> = _progress.asStateFlow()

    private val _scheduled = MutableStateFlow<List<ScheduledDownload>>(emptyList())
    val scheduled: StateFlow<List<ScheduledDownload>> = _scheduled.asStateFlow()

    private val _pendingQueue = MutableStateFlow<List<UrlDownloadItem>>(emptyList())
    val pendingQueue: StateFlow<List<UrlDownloadItem>> = _pendingQueue.asStateFlow()

    init {
        // Rehydrate the "Scheduled" list from Room on (re)creation.
        viewModelScope.launch {
            val pending = withContext(Dispatchers.IO) { scheduledRepo.getPending() }
            _scheduled.value = pending
            pending.forEach { observeWork(it.id) }
        }

        // Auto-resume logic
        viewModelScope.launch {
            NetworkStateUtils.observeNetworkChanges(application).collect { isConnected ->
                if (isConnected) {
                    val isOnWifi = NetworkStateUtils.isOnWifi(application)
                    
                    // 1. Resume Wi-Fi Only downloads that were paused due to network switch
                    _items.value.filter { it.wifiOnly && isOnWifi }.forEach { item ->
                        val current = _progress.value[item.id]
                        if (current?.status == DlStatus.PAUSED && current.errorMessage == "Waiting for Wi-Fi") {
                            resumeDownload(item)
                        }
                    }

                    // 2. Retry failed downloads that failed due to connection error
                    _items.value.forEach { item ->
                        val current = _progress.value[item.id]
                        if (current?.status == DlStatus.FAILED && (current.errorMessage?.contains("connection", ignoreCase = true) == true || current.errorMessage?.contains("fetch", ignoreCase = true) == true)) {
                            retryDownload(item)
                        }
                    }
                    
                    // 3. Process queue if slots opened up
                    processQueue()
                } else {
                    // Auto-pause Wi-Fi only downloads if network is lost or switched to mobile
                    _items.value.filter { it.wifiOnly }.forEach { item ->
                        val current = _progress.value[item.id]
                        if (current?.status == DlStatus.DOWNLOADING) {
                            pauseDownload(item.id, "Waiting for Wi-Fi")
                        }
                    }
                }
            }
        }
    }

    private val _showInterstitialEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val showInterstitialEvent: SharedFlow<Unit> = _showInterstitialEvent

    private val activeCalls = mutableMapOf<String, Call>()
    private val activeJobs = mutableMapOf<String, Job>()
    private var activeCount = 0

    private val savedMediaUri = mutableMapOf<String, Uri>()
    private val liveBytes = ConcurrentHashMap<String, Long>()

    private var serviceRunning = false

    fun addDownload(url: String, wifiOnly: Boolean = false) {
        if (!UrlUtils.isValidUrl(url)) return
        val id = UUID.randomUUID().toString()

        val placeholder = UrlDownloadItem(id, url, "Resolving link...", "application/octet-stream", -1L, wifiOnly)
        _items.value = _items.value + placeholder
        updateProgress(id, DlProgress(status = DlStatus.RESOLVING))

        viewModelScope.launch {
            val resolved = withContext(Dispatchers.IO) { MetadataResolver.resolve(client, url) }
            if (resolved == null) {
                updateProgress(id, DlProgress(status = DlStatus.FAILED, errorMessage = "Could not read file info from this link"))
                return@launch
            }
            val item = UrlDownloadItem(id, url, resolved.first, resolved.second, resolved.third, wifiOnly)
            _items.value = _items.value.map { if (it.id == id) item else it }
            enqueue(item)
        }
    }

    fun addBatch(urls: List<String>) {
        if (!PlanManager.isPro) return
        urls.map { it.trim() }.filter { it.startsWith("http") }.forEach { addDownload(it) }
    }

    fun scheduleDownload(url: String, delayMinutes: Int, wifiOnly: Boolean) {
        if (!PlanManager.isPro) return
        if (!UrlUtils.isValidUrl(url)) return

        val id = UUID.randomUUID().toString()
        val triggerAt = System.currentTimeMillis() + (delayMinutes * 60_000L)
        val scheduledItem = ScheduledDownload(id, url, triggerAt, wifiOnly)
        _scheduled.value = _scheduled.value + scheduledItem
        viewModelScope.launch(Dispatchers.IO) { scheduledRepo.insert(scheduledItem) }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<ScheduledDownloadWorker>()
            .setInitialDelay(delayMinutes.toLong(), TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setInputData(
                workDataOf(
                    ScheduledDownloadWorker.KEY_URL to url,
                    ScheduledDownloadWorker.KEY_WIFI_ONLY to wifiOnly
                )
            )
            .build()

        val workManager = WorkManager.getInstance(getApplication())
        workManager.enqueueUniqueWork("scheduled_download_$id", androidx.work.ExistingWorkPolicy.REPLACE, request)
        observeWork(id, request.id)
    }

    private fun observeWork(id: String, requestId: java.util.UUID? = null) {
        val workManager = WorkManager.getInstance(getApplication())
        val infoFlow = if (requestId != null) {
            workManager.getWorkInfoByIdFlow(requestId)
        } else {
            workManager.getWorkInfosForUniqueWorkFlow("scheduled_download_$id")
                .map { it.firstOrNull() }
        }
        viewModelScope.launch {
            infoFlow.collect { info ->
                if (info?.state == WorkInfo.State.SUCCEEDED || info?.state == WorkInfo.State.FAILED) {
                    _scheduled.value = _scheduled.value.map { if (it.id == id) it.copy(fired = true) else it }
                    withContext(Dispatchers.IO) { scheduledRepo.markFired(id) }
                }
            }
        }
    }

    fun cancelScheduled(id: String) {
        WorkManager.getInstance(getApplication()).cancelUniqueWork("scheduled_download_$id")
        _scheduled.value = _scheduled.value.filter { it.id != id }
        viewModelScope.launch(Dispatchers.IO) { scheduledRepo.delete(id) }
    }

    private fun enqueue(item: UrlDownloadItem) {
        if (activeCount < PlanManager.maxConcurrentTransfers()) {
            startDownload(item, resumeFromByte = 0)
        } else {
            updateProgress(item.id, DlProgress(status = DlStatus.QUEUED))
            _pendingQueue.value = _pendingQueue.value + item
        }
    }

    fun cancelDownload(id: String) {
        activeCalls[id]?.cancel()
        activeJobs[id]?.cancel()
        activeCalls.remove(id)
        activeJobs.remove(id)
        savedMediaUri.remove(id)
        liveBytes.remove(id)
        _pendingQueue.value = _pendingQueue.value.filter { it.id != id }
        updateProgress(id, DlProgress(status = DlStatus.CANCELLED))
        processQueue()
    }

    fun pauseDownload(id: String, reason: String? = null) {
        if (!PlanManager.isPro) return
        val current = _progress.value[id] ?: return
        activeCalls[id]?.cancel()
        activeJobs[id]?.cancel()
        activeCalls.remove(id)
        activeJobs.remove(id)
        activeCount--
        val checkpoint = liveBytes[id] ?: current.bytesDownloaded
        updateProgress(id, current.copy(status = DlStatus.PAUSED, bytesDownloaded = checkpoint, errorMessage = reason))
        processQueue()
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
            if (_pendingQueue.value.none { it.id == item.id }) {
                _pendingQueue.value = _pendingQueue.value + item
            }
        }
    }

    fun retryDownload(item: UrlDownloadItem) {
        savedMediaUri.remove(item.id)
        liveBytes.remove(item.id)
        _pendingQueue.value = _pendingQueue.value.filter { it.id != item.id }
        enqueue(item)
    }

    fun moveQueueItem(from: Int, to: Int) {
        val current = _pendingQueue.value.toMutableList()
        if (from !in current.indices || to !in current.indices) return
        val item = current.removeAt(from)
        current.add(to, item)
        _pendingQueue.value = current
    }

    private fun startDownload(item: UrlDownloadItem, resumeFromByte: Long) {
        if (item.wifiOnly && !NetworkStateUtils.isOnWifi(getApplication())) {
            updateProgress(item.id, DlProgress(status = DlStatus.PAUSED, errorMessage = "Waiting for Wi-Fi", bytesDownloaded = resumeFromByte))
            return
        }

        if (!StorageUtils.hasEnoughSpace(item.sizeBytes)) {
            updateProgress(item.id, DlProgress(status = DlStatus.FAILED, errorMessage = "Not enough storage space"))
            return
        }

        activeCount++
        ensureServiceRunning()
        InterstitialAdManager.preload(getApplication())
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
                        resumeFromByte = resumeFromByte,
                        throttleBytesPerSec = if (PlanManager.isPro || SpeedBoostManager.isBoostActive()) null else SpeedBoostManager.FREE_TIER_CAP_BYTES_PER_SEC
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
                DownloadResult(false, errorMessage = e.message ?: "Connection failure")
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
                val errorMsg = if (finalStatus == DlStatus.FAILED) {
                    result.errorMessage ?: "Download failed — check connection and retry"
                } else null
                updateProgress(item.id, DlProgress(if (result.success) 1f else 0f, finalStatus, errorMessage = errorMsg, bytesDownloaded = result.bytesWritten))
                liveBytes.remove(item.id)

                historyRepo.record(item.fileName, item.sizeBytes, item.mimeType, HistoryType.DOWNLOADED, result.success, result.mediaUri?.toString())

                if (result.success) {
                    NotificationHelper.notify(
                        getApplication(),
                        NotificationHelper.SUMMARY_NOTIFICATION_ID,
                        NotificationHelper.buildCompleteNotification(getApplication(), item.fileName)
                    )
                    _showInterstitialEvent.tryEmit(Unit)
                }
            }

            processQueue()
            stopServiceIfIdle()
        }
        activeJobs[item.id] = job
    }

    private fun processQueue() {
        while (activeCount < PlanManager.maxConcurrentTransfers()) {
            val queue = _pendingQueue.value
            if (queue.isEmpty()) break
            val next = queue.first()
            _pendingQueue.value = queue.drop(1)
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
        if (activeCount > 0 || _pendingQueue.value.isNotEmpty()) return
        val context: Application = getApplication()
        context.stopService(Intent(context, DownloadForegroundService::class.java))
        serviceRunning = false
    }

    private fun updateProgress(id: String, progress: DlProgress) {
        _progress.value = _progress.value + (id to progress)
    }
}

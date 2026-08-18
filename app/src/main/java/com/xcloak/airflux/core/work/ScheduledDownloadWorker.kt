package com.xcloak.airflux.core.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.xcloak.airflux.core.ads.SpeedBoostManager
import com.xcloak.airflux.core.billing.PlanManager
import com.xcloak.airflux.core.common.DownloadUtils
import com.xcloak.airflux.core.common.MetadataResolver
import com.xcloak.airflux.core.common.NetworkStateUtils
import com.xcloak.airflux.core.common.StorageUtils
import com.xcloak.airflux.core.network.HttpClientProvider
import com.xcloak.airflux.core.notification.NotificationHelper
import com.xcloak.airflux.data.database.entity.HistoryType
import com.xcloak.airflux.data.repository.HistoryRepository

class ScheduledDownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_URL = "url"
        const val KEY_WIFI_ONLY = "wifi_only"
    }

    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val wifiOnly = inputData.getBoolean(KEY_WIFI_ONLY, false)

        // Check actual Wi-Fi state ourselves rather than relying on WorkManager's
        // NetworkType.UNMETERED constraint, which many routers/carriers never satisfy
        // even when genuinely connected to Wi-Fi -- that made scheduled downloads
        // silently never run.
        if (wifiOnly && !NetworkStateUtils.isOnWifi(applicationContext)) {
            return Result.retry()
        }

        val client = HttpClientProvider.client
        val metadata = MetadataResolver.resolve(client, url) ?: return Result.retry()
        val (fileName, mimeType, sizeBytes) = metadata

        if (!StorageUtils.hasEnoughSpace(sizeBytes)) return Result.failure()

        val call = DownloadUtils.buildCall(client, url)
        val throttle = if (PlanManager.isPro || SpeedBoostManager.isBoostActive()) null else SpeedBoostManager.FREE_TIER_CAP_BYTES_PER_SEC

        val result = DownloadUtils.executeAndSave(
            context = applicationContext,
            call = call,
            fileName = fileName,
            mimeType = mimeType,
            throttleBytesPerSec = throttle
        ) { _, _ -> }

        HistoryRepository(applicationContext).record(fileName, sizeBytes, mimeType, HistoryType.DOWNLOADED, result.success)

        if (result.success) {
            NotificationHelper.ensureChannel(applicationContext)
            NotificationHelper.notify(
                applicationContext,
                NotificationHelper.SUMMARY_NOTIFICATION_ID + 1,
                NotificationHelper.buildCompleteNotification(applicationContext, fileName)
            )
            return Result.success()
        }
        return Result.failure()
    }
}
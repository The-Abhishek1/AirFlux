package com.xcloak.airflux.core.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.xcloak.airflux.core.notification.NotificationHelper

class DownloadForegroundService : Service() {

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationHelper.buildProgressNotification(
            context = this,
            title = "AirFlux is downloading",
            progressPercent = 0,
            indeterminate = true
        )
        startForeground(NotificationHelper.SUMMARY_NOTIFICATION_ID, notification)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
package com.xcloak.airflux

import android.app.Application
import androidx.work.Configuration
import com.google.android.gms.ads.MobileAds

class App : Application(), Configuration.Provider {
    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this) {}
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setMinimumLoggingLevel(android.util.Log.INFO).build()
}
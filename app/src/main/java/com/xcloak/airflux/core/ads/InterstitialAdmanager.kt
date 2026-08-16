package com.xcloak.airflux.core.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.xcloak.airflux.core.billing.PlanManager

/** Loads one interstitial at a time and shows it on request. Free tier only. */
object InterstitialAdManager {

    private var loadedAd: InterstitialAd? = null
    private var isLoading = false

    fun preload(context: Context) {
        if (PlanManager.isPro || loadedAd != null || isLoading) return
        isLoading = true
        InterstitialAd.load(
            context,
            AdConfig.INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    loadedAd = ad
                    isLoading = false
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    loadedAd = null
                    isLoading = false
                }
            }
        )
    }

    fun showIfReady(activity: Activity, onDismissed: () -> Unit = {}) {
        if (PlanManager.isPro) {
            onDismissed()
            return
        }
        val ad = loadedAd
        if (ad == null) {
            onDismissed()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                loadedAd = null
                onDismissed()
                preload(activity)
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                loadedAd = null
                onDismissed()
            }
        }
        ad.show(activity)
    }
}
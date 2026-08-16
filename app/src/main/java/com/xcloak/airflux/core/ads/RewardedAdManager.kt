package com.xcloak.airflux.core.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object RewardedAdManager {

    private var loadedAd: RewardedAd? = null
    private var isLoading = false

    fun preload(context: Context) {
        if (loadedAd != null || isLoading) return
        isLoading = true
        RewardedAd.load(
            context,
            AdConfig.REWARDED_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
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

    fun isReady(): Boolean = loadedAd != null

    fun show(activity: Activity, onRewardEarned: () -> Unit, onDismissed: () -> Unit = {}) {
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
        ad.show(activity) { onRewardEarned() }
    }
}
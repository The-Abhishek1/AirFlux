package com.xcloak.airflux.core.ads

import android.util.DisplayMetrics
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.xcloak.airflux.core.billing.PlanManager

/** Shows a full-width adaptive banner ad for free-tier users only.
 *  Adaptive banners are taller and clearer than the old fixed 320x50 size,
 *  and scale to the device's actual screen width. */
@Composable
fun BannerAdView(modifier: Modifier = Modifier) {
    val isPro by PlanManager.isProFlow.collectAsState()
    if (isPro) return

    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    // Compute the adaptive ad size ahead of time so Compose knows exactly how
    // much vertical space to reserve -- this is what prevents the "too small /
    // gets clipped" look, since the AndroidView now has a real height from the start.
    val screenWidthDp = configuration.screenWidthDp
    val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, screenWidthDp)

    AndroidView(
        modifier = modifier.fillMaxWidth().height(adSize.height.dp),
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(adSize)
                adUnitId = AdConfig.BANNER_AD_UNIT_ID
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
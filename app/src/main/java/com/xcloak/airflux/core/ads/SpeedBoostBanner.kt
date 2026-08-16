package com.xcloak.airflux.core.ads

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.xcloak.airflux.core.billing.PlanManager
import com.xcloak.airflux.core.designsystem.GlassCard
import com.xcloak.airflux.ui.theme.ElectricCyan
import com.xcloak.airflux.ui.theme.SuccessGreen
import com.xcloak.airflux.ui.theme.TextMuted
import com.xcloak.airflux.ui.theme.TextPrimary
import com.xcloak.airflux.ui.theme.TextSecondary
import kotlinx.coroutines.delay

/** Shows nothing for Pro users. For free users: either an active-boost countdown,
 *  or a card offering "watch ad to boost" / "go Pro" — depending on ad availability. */
@Composable
fun SpeedBoostBanner(onUpgradeClick: () -> Unit) {
    val isPro by PlanManager.isProFlow.collectAsState()
    if (isPro) return

    val context = LocalContext.current
    var tick by remember { mutableStateOf(0) } // forces recomposition every second while boost is active
    val boostActive = SpeedBoostManager.isBoostActive()

    LaunchedEffect(boostActive) {
        while (SpeedBoostManager.isBoostActive()) {
            delay(1000)
            tick++
        }
    }

    LaunchedEffect(Unit) {
        RewardedAdManager.preload(context)
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (boostActive) {
                val remaining = SpeedBoostManager.remainingSeconds()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = SuccessGreen, modifier = Modifier.height(18.dp))
                    Text(
                        "  Full speed unlocked — ${remaining / 60}m ${remaining % 60}s left",
                        color = SuccessGreen,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                Text(
                    "Free downloads are speed-limited (300 KB/s).",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            val activity = context as? Activity ?: return@Button
                            RewardedAdManager.show(
                                activity = activity,
                                onRewardEarned = { SpeedBoostManager.activateBoost() }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color.Black, modifier = Modifier.height(16.dp))
                        Text("  Watch Ad", color = Color.Black, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onUpgradeClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Go Pro", color = ElectricCyan, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
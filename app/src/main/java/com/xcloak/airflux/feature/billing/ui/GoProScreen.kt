package com.xcloak.airflux.feature.billing.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.xcloak.airflux.core.billing.PlanManager
import com.xcloak.airflux.core.billing.PricingUtils
import com.xcloak.airflux.core.designsystem.AppBackground
import com.xcloak.airflux.core.designsystem.GlassCard
import com.xcloak.airflux.ui.theme.ElectricCyan
import com.xcloak.airflux.ui.theme.SuccessGreen
import com.xcloak.airflux.ui.theme.TextMuted
import com.xcloak.airflux.ui.theme.TextPrimary
import com.xcloak.airflux.ui.theme.TextSecondary

private val proFeatures = listOf(
    "Unlimited simultaneous transfers & downloads",
    "Unlimited devices per share session",
    "Pause & resume downloads",
    "Folder transfer & batch URL downloads",
    "Scheduled downloads",
    "Transfer encryption",
    "Photo sharing in chat",
    "Unlimited chat history",
    "Full-speed downloads, no throttling",
    "No ads"
)

@Composable
fun GoProScreen(onBack: () -> Unit = {}) {
    val isPro by PlanManager.isProFlow.collectAsState()
    val pricing = PricingUtils.getDisplayPricing()

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text("AirFlux Pro", color = TextPrimary, style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(20.dp))

            if (isPro) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = SuccessGreen, modifier = Modifier.height(40.dp))
                        Text(
                            "You're on AirFlux Pro",
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                        Text(
                            "Thanks for supporting AirFlux — every feature below is unlocked.",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(pricing.display, color = ElectricCyan, style = MaterialTheme.typography.displaySmall)
                        Text(pricing.currencyNote, color = TextMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    proFeatures.forEach { feature ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ElectricCyan, modifier = Modifier.height(18.dp))
                            Spacer(modifier = Modifier.height(0.dp))
                            Text(
                                feature,
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 10.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!isPro) {
                Button(
                    onClick = {
                        // TODO: replace with real Play Billing purchase flow once
                        // the product is live in Play Console. For now this is a
                        // placeholder so the purchase UI can be reviewed end-to-end.
                        PlanManager.setPro(true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Upgrade for ${pricing.display}", color = Color.Black, style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    "One-time payment. No subscription, no recurring charges.",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                Button(
                    onClick = { PlanManager.setPro(false) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset Pro status (testing only)", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
package com.xcloak.airflux.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xcloak.airflux.core.billing.PricingUtils
import com.xcloak.airflux.core.designsystem.AppBackground
import com.xcloak.airflux.core.designsystem.GlassCard
import com.xcloak.airflux.core.designsystem.accessibleClickableCard
import com.xcloak.airflux.core.designsystem.GlassCard
import com.xcloak.airflux.feature.settings.viewmodel.SettingsViewModel
import com.xcloak.airflux.ui.theme.ElectricCyan
import com.xcloak.airflux.ui.theme.ErrorRed
import com.xcloak.airflux.ui.theme.SuccessGreen
import com.xcloak.airflux.ui.theme.TextMuted
import com.xcloak.airflux.ui.theme.TextPrimary
import com.xcloak.airflux.ui.theme.TextSecondary

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel(), onGoProClick: () -> Unit = {}) {
    val isPro by viewModel.isPro.collectAsState()
    val pricing = PricingUtils.getDisplayPricing()

    AppBackground {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {

            Text("Settings", color = TextPrimary, style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(20.dp))
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .accessibleClickableCard(
                        description = if (isPro) "AirFlux Pro active, all features unlocked" else "Upgrade to AirFlux Pro",
                        onClick = onGoProClick
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = if (isPro) SuccessGreen else ElectricCyan,
                        modifier = Modifier.height(28.dp)
                    )
                    Spacer(modifier = Modifier.height(0.dp))
                    Column(modifier = Modifier.padding(start = 14.dp)) {
                        Text(
                            if (isPro) "AirFlux Pro — Active" else "Upgrade to AirFlux Pro",
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            if (isPro) "All features unlocked" else "${pricing.display} one-time — unlock everything",
                            color = TextMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Pro (dev testing toggle)", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "For internal testing only — real purchases happen via the card above",
                                color = TextMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(
                            checked = isPro,
                            onCheckedChange = { viewModel.setProForTesting(it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = ElectricCyan)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Clear transfer history", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Removes all sent, received, and downloaded entries from History",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 12.dp, top = 2.dp)
                    )
                    Button(
                        onClick = { viewModel.clearHistory() },
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Clear History", color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("About", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                    Text("AirFlux v${viewModel.appVersion()}", color = TextSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                    Text("by XCloak", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
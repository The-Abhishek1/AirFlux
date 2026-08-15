package com.xcloak.airflux.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.xcloak.airflux.core.designsystem.AppBackground
import com.xcloak.airflux.core.designsystem.GlassCard
import com.xcloak.airflux.core.designsystem.GradientAppTitle
import com.xcloak.airflux.feature.chat.ui.ChatScreen
import com.xcloak.airflux.feature.downloader.ui.DownloaderHomeScreen
import com.xcloak.airflux.feature.history.ui.HistoryScreen
import com.xcloak.airflux.feature.settings.ui.SettingsScreen
import com.xcloak.airflux.feature.sharing.ui.ReceiveScreen
import com.xcloak.airflux.feature.sharing.ui.SendScreen
import com.xcloak.airflux.ui.theme.ElectricCyan
import com.xcloak.airflux.ui.theme.TextPrimary
import com.xcloak.airflux.ui.theme.TextSecondary

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = "home") {

        composable("home") {
            HomeScreen(
                onShareClick = { navController.navigate("sharing_graph") },
                onDownloadClick = { navController.navigate("downloader_graph") },
                onHistoryClick = { navController.navigate("history") },
                onSettingsClick = { navController.navigate("settings") },
                onChatClick = { navController.navigate("chat") }
            )
        }

        composable("history") { HistoryScreen() }
        composable("settings") { SettingsScreen() }
        composable("chat") { ChatScreen() }

        navigation(startDestination = "sharing_home", route = "sharing_graph") {
            composable("sharing_home") {
                SharingModePicker(
                    onSendClick = { navController.navigate("sharing_send") },
                    onReceiveClick = { navController.navigate("sharing_receive") }
                )
            }
            composable("sharing_send") { SendScreen() }
            composable("sharing_receive") { ReceiveScreen() }
        }

        navigation(startDestination = "downloader_home", route = "downloader_graph") {
            composable("downloader_home") { DownloaderHomeScreen() }
        }
    }
}

@Composable
fun HomeScreen(
    onShareClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onChatClick: () -> Unit
) {
    AppBackground {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GradientAppTitle("AirFlux")
            Text(
                text = "Share files instantly. Download anything.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
            )

            GlassCard(modifier = Modifier.fillMaxWidth().clickable { onShareClick() }) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = ElectricCyan)
                    Text("Share / Receive", color = TextPrimary, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                    Text("Send files over Wi-Fi, no internet needed", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            GlassCard(modifier = Modifier.fillMaxWidth().clickable { onDownloadClick() }) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = ElectricCyan)
                    Text("Download from Link", color = TextPrimary, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                    Text("Paste a URL, download fast", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            GlassCard(modifier = Modifier.fillMaxWidth().clickable { onChatClick() }) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Icon(Icons.Default.ChatBubble, contentDescription = null, tint = ElectricCyan)
                    Text("Wi-Fi Chat", color = TextPrimary, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                    Text("Message a nearby device, no internet needed", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            GlassCard(modifier = Modifier.fillMaxWidth().clickable { onHistoryClick() }) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Icon(Icons.Default.History, contentDescription = null, tint = ElectricCyan)
                    Text("History", color = TextPrimary, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                    Text("View past transfers and downloads", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            GlassCard(modifier = Modifier.fillMaxWidth().clickable { onSettingsClick() }) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = ElectricCyan)
                    Text("Settings", color = TextPrimary, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                    Text("Preferences, plan, and app info", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun SharingModePicker(onSendClick: () -> Unit, onReceiveClick: () -> Unit) {
    AppBackground {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Share / Receive", color = TextPrimary, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 32.dp))

            GlassCard(modifier = Modifier.fillMaxWidth().clickable { onSendClick() }) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = ElectricCyan)
                    Text("Send", color = TextPrimary, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                    Text("Choose files and share them from this device", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            GlassCard(modifier = Modifier.fillMaxWidth().clickable { onReceiveClick() }) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = ElectricCyan)
                    Text("Receive", color = TextPrimary, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                    Text("Connect to another device and download files", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
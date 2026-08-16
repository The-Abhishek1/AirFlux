package com.xcloak.airflux.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.xcloak.airflux.core.ads.BannerAdView
import com.xcloak.airflux.core.common.IncomingShareHolder
import com.xcloak.airflux.core.common.OnboardingPrefs
import com.xcloak.airflux.core.designsystem.AppBackground
import com.xcloak.airflux.core.designsystem.GlassCard
import com.xcloak.airflux.core.designsystem.GradientAppTitle
import com.xcloak.airflux.feature.btchat.ui.BtChatScreen
import com.xcloak.airflux.feature.chat.ui.ChatScreen
import com.xcloak.airflux.feature.downloader.ui.DownloaderHomeScreen
import com.xcloak.airflux.feature.history.ui.HistoryScreen
import com.xcloak.airflux.feature.onboarding.ui.OnboardingScreen
import com.xcloak.airflux.feature.settings.ui.SettingsScreen
import com.xcloak.airflux.feature.sharing.ui.ReceiveScreen
import com.xcloak.airflux.feature.sharing.ui.SendScreen
import com.xcloak.airflux.ui.theme.ElectricCyan
import com.xcloak.airflux.ui.theme.MidnightBlue
import com.xcloak.airflux.ui.theme.TextPrimary
import com.xcloak.airflux.ui.theme.TextSecondary

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current
    val startDest = if (OnboardingPrefs.isComplete(context)) "home" else "onboarding"

    LaunchedEffect(Unit) {
        if (IncomingShareHolder.hasPending()) {
            navController.navigate("sharing_send")
        }
    }

    NavHost(navController = navController, startDestination = startDest) {

        composable("onboarding") {
            OnboardingScreen(
                onComplete = {
                    OnboardingPrefs.markComplete(context)
                    navController.navigate("home") { popUpTo("onboarding") { inclusive = true } }
                }
            )
        }

        composable("home") {
            HomeScreen(
                onShareClick = { navController.navigate("sharing_graph") },
                onDownloadClick = { navController.navigate("downloader_graph") },
                onHistoryClick = { navController.navigate("history") },
                onSettingsClick = { navController.navigate("settings") },
                onChatClick = { navController.navigate("chat") },
                onBtChatClick = { navController.navigate("bt_chat") }
            )
        }

        composable("history") { HistoryScreen() }
        composable("settings") { SettingsScreen() }
        composable("chat") { ChatScreen() }
        composable("bt_chat") { BtChatScreen() }

        navigation(startDestination = "sharing_home", route = "sharing_graph") {
            composable("sharing_home") {
                SharingModePicker(
                    onSendClick = { navController.navigate("sharing_send") },
                    onReceiveClick = { navController.navigate("sharing_receive") }
                )
            }
            composable("sharing_send") { SendScreen() }
            composable("sharing_receive") { ReceiveScreen(onGoPro = { navController.navigate("settings") }) }
        }

        navigation(startDestination = "downloader_home", route = "downloader_graph") {
            composable("downloader_home") { DownloaderHomeScreen(onGoPro = { navController.navigate("settings") }) }
        }
    }
}

@Composable
fun HomeScreen(
    onShareClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onChatClick: () -> Unit,
    onBtChatClick: () -> Unit
) {
    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            GradientAppTitle("AirFlux")
            Text(
                text = "Share files instantly. Download anything.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 28.dp)
            )

            HomeMenuCard(
                icon = Icons.Default.Share,
                title = "Share / Receive",
                description = "Send files over Wi-Fi, no internet needed",
                onClick = onShareClick
            )
            Spacer(modifier = Modifier.height(12.dp))

            HomeMenuCard(
                icon = Icons.Default.Download,
                title = "Download from Link",
                description = "Paste a URL, download fast",
                onClick = onDownloadClick
            )
            Spacer(modifier = Modifier.height(12.dp))

            HomeMenuCard(
                icon = Icons.Default.ChatBubble,
                title = "Wi-Fi Chat",
                description = "Message a nearby device, no internet needed",
                onClick = onChatClick
            )
            Spacer(modifier = Modifier.height(12.dp))

            HomeMenuCard(
                icon = Icons.Default.Bluetooth,
                title = "Bluetooth Chat",
                description = "Message nearby, zero network needed",
                onClick = onBtChatClick
            )
            Spacer(modifier = Modifier.height(12.dp))

            HomeMenuCard(
                icon = Icons.Default.History,
                title = "History",
                description = "View past transfers and downloads",
                onClick = onHistoryClick
            )
            Spacer(modifier = Modifier.height(12.dp))

            HomeMenuCard(
                icon = Icons.Default.Settings,
                title = "Settings",
                description = "Preferences, plan, and app info",
                onClick = onSettingsClick
            )

            Spacer(modifier = Modifier.height(24.dp))
            BannerAdView()
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun HomeMenuCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MidnightBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(title, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                Text(description, color = TextSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
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

            HomeMenuCard(
                icon = Icons.Default.Share,
                title = "Send",
                description = "Choose files and share them from this device",
                onClick = onSendClick
            )
            Spacer(modifier = Modifier.height(16.dp))

            HomeMenuCard(
                icon = Icons.Default.Download,
                title = "Receive",
                description = "Connect to another device and download files",
                onClick = onReceiveClick
            )
        }
    }
}
package com.xcloak.airflux.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.xcloak.airflux.core.designsystem.AppBackground
import com.xcloak.airflux.core.designsystem.GlassCard
import com.xcloak.airflux.core.designsystem.GradientAppTitle
import com.xcloak.airflux.ui.theme.ElectricCyan
import com.xcloak.airflux.ui.theme.TextPrimary
import com.xcloak.airflux.ui.theme.TextSecondary
import com.xcloak.airflux.feature.sharing.ui.SharingHomeScreen
@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = "home") {

        composable("home") {
            HomeScreen(
                onShareClick = { navController.navigate("sharing_graph") },
                onDownloadClick = { navController.navigate("downloader_graph") }
            )
        }

        navigation(startDestination = "sharing_home", route = "sharing_graph") {
            composable("sharing_home") { SharingHomeScreen() }
        }

        navigation(startDestination = "downloader_home", route = "downloader_graph") {
            composable("downloader_home") { DownloaderPlaceholderScreen() }
        }
    }
}

@Composable
fun HomeScreen(onShareClick: () -> Unit, onDownloadClick: () -> Unit) {
    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GradientAppTitle("AirFlux")
            Text(
                text = "Share files instantly. Download anything.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 40.dp)
            )

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onShareClick() }
                    .padding(0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = ElectricCyan)
                    Text(
                        "Share / Receive",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        "Send files over Wi-Fi, no internet needed",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDownloadClick() }
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = ElectricCyan)
                    Text(
                        "Download from Link",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        "Paste a URL, download fast",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}


@Composable
fun DownloaderPlaceholderScreen() {
    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Downloader")
        }
    }
}
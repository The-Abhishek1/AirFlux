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
            composable("sharing_home") { SharingPlaceholderScreen() }
        }

        navigation(startDestination = "downloader_home", route = "downloader_graph") {
            composable("downloader_home") { DownloaderPlaceholderScreen() }
        }
    }
}

@Composable
fun HomeScreen(onShareClick: () -> Unit, onDownloadClick: () -> Unit) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("AirFlux")
            Button(onClick = onShareClick, modifier = Modifier.padding(top = 24.dp)) {
                Text("Share / Receive")
            }
            Button(onClick = onDownloadClick, modifier = Modifier.padding(top = 12.dp)) {
                Text("Download from Link")
            }
        }
    }
}

@Composable
fun SharingPlaceholderScreen() {
    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Sharing")
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
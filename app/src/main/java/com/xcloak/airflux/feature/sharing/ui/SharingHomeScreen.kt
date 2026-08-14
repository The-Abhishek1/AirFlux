package com.xcloak.airflux.feature.sharing.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xcloak.airflux.core.common.FileUtils
import com.xcloak.airflux.core.designsystem.AppBackground
import com.xcloak.airflux.core.designsystem.GlassCard
import com.xcloak.airflux.domain.model.SelectedFile
import com.xcloak.airflux.feature.sharing.viewmodel.ServerStatus
import com.xcloak.airflux.feature.sharing.viewmodel.SharingViewModel
import com.xcloak.airflux.ui.theme.ElectricCyan
import com.xcloak.airflux.ui.theme.ErrorRed
import com.xcloak.airflux.ui.theme.SuccessGreen
import com.xcloak.airflux.ui.theme.TextMuted
import com.xcloak.airflux.ui.theme.TextPrimary
import com.xcloak.airflux.ui.theme.TextSecondary

@Composable
fun SharingHomeScreen(viewModel: SharingViewModel = viewModel()) {
    val context = LocalContext.current
    val files by viewModel.selectedFiles.collectAsState()
    val serverStatus by viewModel.serverStatus.collectAsState()

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.addFiles(context, uris)
    }

    AppBackground {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Share Files", color = TextPrimary, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "${files.size} file${if (files.size == 1) "" else "s"} selected",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ElectricCyan)
                        .clickable { pickerLauncher.launch("*/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add files", tint = Color.Black)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    when (val status = serverStatus) {
                        is ServerStatus.Stopped -> {
                            Text("Not sharing", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        }
                        is ServerStatus.Running -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Wifi,
                                    contentDescription = null,
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    "  Sharing live",
                                    color = SuccessGreen,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Text(
                                status.url,
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Text(
                                "Open this address in a browser on another device on the same Wi-Fi",
                                color = TextMuted,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        is ServerStatus.Error -> {
                            Text(status.message, color = ErrorRed, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (serverStatus is ServerStatus.Running) viewModel.stopServer()
                            else viewModel.startServer()
                        },
                        enabled = files.isNotEmpty() || serverStatus is ServerStatus.Running,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (serverStatus is ServerStatus.Running) "Stop Sharing" else "Start Sharing",
                            color = Color.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (files.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No files selected yet.\nTap + to choose files to share.",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 40.dp)
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(files, key = { it.uri }) { file ->
                        FileRow(file = file, onRemove = { viewModel.removeFile(file) })
                    }
                }
            }
        }
    }
}

@Composable
private fun FileRow(file: SelectedFile, onRemove: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = iconForMimeType(file.mimeType),
                contentDescription = null,
                tint = ElectricCyan,
                modifier = Modifier.size(28.dp)
            )
            Column(
                modifier = Modifier.weight(1f).padding(start = 14.dp)
            ) {
                Text(
                    file.name,
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
                Text(
                    FileUtils.formatSize(file.sizeBytes),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Remove", tint = TextMuted)
            }
        }
    }
}

private fun iconForMimeType(mimeType: String): ImageVector = when {
    mimeType.startsWith("image/") -> Icons.Default.Image
    mimeType.startsWith("video/") -> Icons.Default.Movie
    mimeType.startsWith("audio/") -> Icons.Default.MusicNote
    mimeType == "application/pdf" -> Icons.Default.Description
    mimeType.contains("zip") || mimeType.contains("compressed") -> Icons.Default.FolderZip
    else -> Icons.Default.InsertDriveFile
}
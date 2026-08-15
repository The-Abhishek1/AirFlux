package com.xcloak.airflux.feature.sharing.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image as ImageIcon
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xcloak.airflux.core.billing.PlanManager
import com.xcloak.airflux.core.common.FileUtils
import com.xcloak.airflux.core.common.QrUtils
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
fun SendScreen(viewModel: SharingViewModel = viewModel()) {
    val context = LocalContext.current
    val files by viewModel.selectedFiles.collectAsState()
    val serverStatus by viewModel.serverStatus.collectAsState()
    val isPro by PlanManager.isProFlow.collectAsState()
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris -> if (uris.isNotEmpty()) viewModel.addFiles(context, uris) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri -> if (treeUri != null) viewModel.addFolder(context, treeUri) }

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
                Row {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (isPro) ElectricCyan.copy(alpha = 0.9f) else ElectricCyan.copy(alpha = 0.4f))
                            .clickable {
                                if (isPro) folderPickerLauncher.launch(null)
                                else Toast.makeText(context, "Folder transfer is a Pro feature", Toast.LENGTH_SHORT).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "Add folder", tint = Color.Black)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
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
            }

            Spacer(modifier = Modifier.height(16.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    when (val status = serverStatus) {
                        is ServerStatus.Stopped -> {
                            Text("Not sharing", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        }
                        is ServerStatus.Running -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Wifi, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                                Text("  Sharing live", color = SuccessGreen, style = MaterialTheme.typography.bodyMedium)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                Text(
                                    status.url,
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                                        clipboard.setPrimaryClip(ClipData.newPlainText("AirFlux share link", status.url))
                                        Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy link", tint = ElectricCyan, modifier = Modifier.size(18.dp))
                                }
                            }

                            LaunchedEffect(status.url) {
                                qrBitmap = QrUtils.generateQrBitmap(status.url)
                            }
                            qrBitmap?.let { bmp ->
                                Box(
                                    modifier = Modifier.padding(top = 12.dp).size(180.dp).clip(RoundedCornerShape(12.dp)).background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(bitmap = bmp.asImageBitmap(), contentDescription = "QR code", modifier = Modifier.size(160.dp))
                                }
                            }
                            Text(
                                "Scan with AirFlux's Receive screen, or share the copied link",
                                color = TextMuted,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        is ServerStatus.Error -> {
                            Text(status.message, color = ErrorRed, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (serverStatus is ServerStatus.Running) {
                                viewModel.stopServer()
                                qrBitmap = null
                            } else {
                                viewModel.startServer()
                            }
                        },
                        enabled = files.isNotEmpty() || serverStatus is ServerStatus.Running,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (serverStatus is ServerStatus.Running) "Stop Sharing" else "Start Sharing", color = Color.Black)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (files.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No files selected yet.\nTap + to choose files, or the folder icon to share a whole folder (Pro).",
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
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = iconForMimeType(file.mimeType), contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(28.dp))
            Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                Text(file.name, color = TextPrimary, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                Text(FileUtils.formatSize(file.sizeBytes), color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Remove", tint = TextMuted)
            }
        }
    }
}

private fun iconForMimeType(mimeType: String): ImageVector = when {
    mimeType.startsWith("image/") -> ImageIcon
    mimeType.startsWith("video/") -> Icons.Default.Movie
    mimeType.startsWith("audio/") -> Icons.Default.MusicNote
    mimeType == "application/pdf" -> Icons.Default.Description
    mimeType.contains("zip") || mimeType.contains("compressed") -> Icons.Default.FolderZip
    else -> Icons.Default.InsertDriveFile
}
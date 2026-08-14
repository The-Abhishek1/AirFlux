package com.xcloak.airflux.feature.sharing.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.xcloak.airflux.core.common.FileUtils
import com.xcloak.airflux.core.designsystem.AppBackground
import com.xcloak.airflux.core.designsystem.GlassCard
import com.xcloak.airflux.domain.model.RemoteFile
import com.xcloak.airflux.feature.sharing.viewmodel.ConnectionState
import com.xcloak.airflux.feature.sharing.viewmodel.DownloadProgress
import com.xcloak.airflux.feature.sharing.viewmodel.ReceiveViewModel
import com.xcloak.airflux.feature.sharing.viewmodel.TransferStatus
import com.xcloak.airflux.ui.theme.ElectricCyan
import com.xcloak.airflux.ui.theme.ErrorRed
import com.xcloak.airflux.ui.theme.SuccessGreen
import com.xcloak.airflux.ui.theme.TextMuted
import com.xcloak.airflux.ui.theme.TextPrimary
import com.xcloak.airflux.ui.theme.TextSecondary
import com.xcloak.airflux.ui.theme.WarningAmber

@Composable
fun ReceiveScreen(viewModel: ReceiveViewModel = viewModel()) {
    val context = LocalContext.current
    var address by remember { mutableStateOf("") }
    val connectionState by viewModel.connectionState.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { scanned ->
            address = scanned
            viewModel.connect(scanned)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scanLauncher.launch(ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE).setBeepEnabled(false))
        }
    }

    fun startScan() {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            scanLauncher.launch(ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE).setBeepEnabled(false))
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    AppBackground {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {

            Text("Receive Files", color = TextPrimary, style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = { startScan() },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color.Black)
                        Text("  Scan QR Code", color = Color.Black)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("or enter address manually", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        placeholder = { Text("192.168.x.x:8080", color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = ElectricCyan
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.connect(address) },
                        enabled = address.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Connect", color = Color.Black)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (val state = connectionState) {
                is ConnectionState.Idle -> {}
                is ConnectionState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ElectricCyan)
                    }
                }
                is ConnectionState.Error -> {
                    Text(state.message, color = ErrorRed, style = MaterialTheme.typography.bodyMedium)
                }
                is ConnectionState.Connected -> {
                    Text(
                        "${state.files.size} file(s) available",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(state.files, key = { it.index }) { file ->
                            RemoteFileRow(
                                file = file,
                                progress = downloadProgress[file.index],
                                onDownload = { viewModel.downloadFile(file) },
                                onCancel = { viewModel.cancelDownload(file.index) },
                                onRetry = { viewModel.retryDownload(file) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RemoteFileRow(
    file: RemoteFile,
    progress: DownloadProgress?,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(file.name, color = TextPrimary, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                    Text(FileUtils.formatSize(file.sizeBytes), color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }

                when (progress?.status) {
                    TransferStatus.DONE -> Icon(Icons.Default.CheckCircle, contentDescription = "Done", tint = SuccessGreen)
                    TransferStatus.DOWNLOADING -> IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = ErrorRed)
                    }
                    TransferStatus.QUEUED -> Text("Queued", color = WarningAmber, style = MaterialTheme.typography.bodySmall)
                    TransferStatus.FAILED -> IconButton(onClick = onRetry) {
                        Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = WarningAmber)
                    }
                    TransferStatus.CANCELLED -> IconButton(onClick = onRetry) {
                        Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = TextMuted)
                    }
                    null -> IconButton(onClick = onDownload) {
                        Icon(Icons.Default.Download, contentDescription = "Download", tint = ElectricCyan)
                    }
                }
            }

            if (progress?.status == TransferStatus.DOWNLOADING) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress.progress },
                    color = ElectricCyan,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(FileUtils.formatSpeed(progress.speedBytesPerSec), color = TextMuted, style = MaterialTheme.typography.bodySmall)
                    Text("ETA ${FileUtils.formatEta(progress.etaSeconds)}", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                }
            }

            if (progress?.status == TransferStatus.FAILED) {
                Text("Download failed", color = ErrorRed, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
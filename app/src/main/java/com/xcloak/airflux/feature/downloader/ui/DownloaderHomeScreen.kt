package com.xcloak.airflux.feature.downloader.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xcloak.airflux.core.billing.PlanManager
import com.xcloak.airflux.core.common.FileUtils
import com.xcloak.airflux.core.designsystem.AppBackground
import com.xcloak.airflux.core.designsystem.GlassCard
import com.xcloak.airflux.domain.model.UrlDownloadItem
import com.xcloak.airflux.feature.downloader.viewmodel.DlProgress
import com.xcloak.airflux.feature.downloader.viewmodel.DlStatus
import com.xcloak.airflux.feature.downloader.viewmodel.DownloaderViewModel
import com.xcloak.airflux.ui.theme.ElectricCyan
import com.xcloak.airflux.ui.theme.ErrorRed
import com.xcloak.airflux.ui.theme.SuccessGreen
import com.xcloak.airflux.ui.theme.TextMuted
import com.xcloak.airflux.ui.theme.TextPrimary
import com.xcloak.airflux.ui.theme.TextSecondary
import com.xcloak.airflux.ui.theme.WarningAmber

@Composable
fun DownloaderHomeScreen(viewModel: DownloaderViewModel = viewModel()) {
    var url by remember { mutableStateOf("") }
    var showBatchInput by remember { mutableStateOf(false) }
    var batchText by remember { mutableStateOf("") }
    val items by viewModel.items.collectAsState()
    val progressMap by viewModel.progress.collectAsState()
    val isPro by PlanManager.isProFlow.collectAsState()

    AppBackground {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {

            Text("Download from Link", color = TextPrimary, style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        placeholder = { Text("https://example.com/file.zip", color = TextMuted) },
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
                        onClick = {
                            viewModel.addDownload(url.trim())
                            url = ""
                        },
                        enabled = url.trim().startsWith("http"),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Download", color = Color.Black)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = { showBatchInput = !showBatchInput }) {
                        if (!isPro) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            if (showBatchInput) "Hide batch input" else "Paste multiple links (Pro)",
                            color = if (isPro) ElectricCyan else TextMuted
                        )
                    }

                    if (showBatchInput) {
                        if (isPro) {
                            OutlinedTextField(
                                value = batchText,
                                onValueChange = { batchText = it },
                                placeholder = { Text("One link per line", color = TextMuted) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = ElectricCyan
                                ),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    viewModel.addBatch(batchText.split("\n"))
                                    batchText = ""
                                    showBatchInput = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Add All", color = Color.Black)
                            }
                        } else {
                            Text(
                                "Batch downloading multiple links at once is a Pro feature.",
                                color = TextMuted,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No downloads yet.\nPaste a link above to get started.",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 40.dp)
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(items, key = { it.id }) { item ->
                        DownloadRow(
                            item = item,
                            progress = progressMap[item.id],
                            isPro = isPro,
                            onCancel = { viewModel.cancelDownload(item.id) },
                            onRetry = { viewModel.retryDownload(item) },
                            onPause = { viewModel.pauseDownload(item.id) },
                            onResume = { viewModel.resumeDownload(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadRow(
    item: UrlDownloadItem,
    progress: DlProgress?,
    isPro: Boolean,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.fileName, color = TextPrimary, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                    Text(
                        if (item.sizeBytes > 0) FileUtils.formatSize(item.sizeBytes) else "Size unknown",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                when (progress?.status) {
                    DlStatus.DONE -> Icon(Icons.Default.CheckCircle, contentDescription = "Done", tint = SuccessGreen)
                    DlStatus.DOWNLOADING -> Row {
                        if (isPro) {
                            IconButton(onClick = onPause) {
                                Icon(Icons.Default.Pause, contentDescription = "Pause", tint = WarningAmber)
                            }
                        }
                        IconButton(onClick = onCancel) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = ErrorRed)
                        }
                    }
                    DlStatus.PAUSED -> Row {
                        IconButton(onClick = onResume) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = SuccessGreen)
                        }
                        IconButton(onClick = onCancel) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = ErrorRed)
                        }
                    }
                    DlStatus.QUEUED -> Text("Queued", color = WarningAmber, style = MaterialTheme.typography.bodySmall)
                    DlStatus.RESOLVING -> CircularProgressIndicator(color = ElectricCyan, modifier = Modifier.size(20.dp))
                    DlStatus.FAILED -> IconButton(onClick = onRetry) {
                        Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = WarningAmber)
                    }
                    DlStatus.CANCELLED -> IconButton(onClick = onRetry) {
                        Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = TextMuted)
                    }
                    null -> {}
                }
            }

            if (progress?.status == DlStatus.DOWNLOADING) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(progress = { progress.progress }, color = ElectricCyan, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(FileUtils.formatSpeed(progress.speedBytesPerSec), color = TextMuted, style = MaterialTheme.typography.bodySmall)
                    Text("ETA ${FileUtils.formatEta(progress.etaSeconds)}", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                }
            }

            if (progress?.status == DlStatus.PAUSED) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(progress = { progress.progress }, color = TextMuted, modifier = Modifier.fillMaxWidth())
                Text("Paused", color = TextMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            }

            if (progress?.status == DlStatus.FAILED) {
                Text(
                    progress.errorMessage ?: "Download failed",
                    color = ErrorRed,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
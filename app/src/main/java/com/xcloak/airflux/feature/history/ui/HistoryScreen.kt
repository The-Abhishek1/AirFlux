package com.xcloak.airflux.feature.history.ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xcloak.airflux.core.common.FileUtils
import com.xcloak.airflux.core.designsystem.AppBackground
import com.xcloak.airflux.core.designsystem.GlassCard
import com.xcloak.airflux.data.database.entity.HistoryEntity
import com.xcloak.airflux.data.database.entity.HistoryType
import com.xcloak.airflux.feature.history.viewmodel.HistoryViewModel
import com.xcloak.airflux.ui.theme.ElectricCyan
import com.xcloak.airflux.ui.theme.ErrorRed
import com.xcloak.airflux.ui.theme.SuccessGreen
import com.xcloak.airflux.ui.theme.TextMuted
import com.xcloak.airflux.ui.theme.TextPrimary
import com.xcloak.airflux.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = viewModel()) {
    val history by viewModel.history.collectAsState()

    AppBackground {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("History", color = TextPrimary, style = MaterialTheme.typography.headlineSmall)
                if (history.isNotEmpty()) {
                    Text(
                        "Clear",
                        color = ElectricCyan,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .padding(8.dp)
                            .clickable { viewModel.clearHistory() }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (history.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No transfers yet.\nSent, received, and downloaded files will show up here.",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 40.dp)
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(history, key = { it.id }) { entry ->
                        HistoryRow(entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(entry: HistoryEntity) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(iconForMimeType(entry.mimeType), contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(28.dp))
            Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                Text(entry.fileName, color = TextPrimary, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                Text(
                    "${labelFor(entry.type)} · ${FileUtils.formatSize(entry.sizeBytes)} · ${formatDate(entry.timestamp)}",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Icon(
                if (entry.success) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = if (entry.success) "Success" else "Failed",
                tint = if (entry.success) SuccessGreen else ErrorRed
            )
        }
    }
}

private fun labelFor(type: HistoryType): String = when (type) {
    HistoryType.SENT -> "Sent"
    HistoryType.RECEIVED -> "Received"
    HistoryType.DOWNLOADED -> "Downloaded"
}

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(timestamp))

private fun iconForMimeType(mimeType: String): ImageVector = when {
    mimeType.startsWith("image/") -> Icons.Default.Image
    mimeType.startsWith("video/") -> Icons.Default.Movie
    mimeType.startsWith("audio/") -> Icons.Default.MusicNote
    mimeType == "application/pdf" -> Icons.Default.Description
    mimeType.contains("zip") || mimeType.contains("compressed") -> Icons.Default.FolderZip
    else -> Icons.Default.InsertDriveFile
}
package com.xcloak.airflux.feature.history.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xcloak.airflux.core.common.FileUtils
import com.xcloak.airflux.core.designsystem.AppBackground
import com.xcloak.airflux.core.designsystem.GlassCard
import com.xcloak.airflux.data.database.entity.HistoryEntity
import com.xcloak.airflux.data.database.entity.HistoryType
import com.xcloak.airflux.feature.history.viewmodel.GalleryViewModel
import com.xcloak.airflux.ui.theme.ElectricCyan
import com.xcloak.airflux.ui.theme.TextMuted
import com.xcloak.airflux.ui.theme.TextPrimary
import com.xcloak.airflux.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GalleryScreen(viewModel: GalleryViewModel = viewModel()) {
    val scope = rememberCoroutineScope()
    val tabs = listOf("Images", "Videos", "Files")
    val pagerState = rememberPagerState { tabs.size }

    AppBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Gallery", color = TextPrimary, style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Clear All",
                    color = ElectricCyan,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable { viewModel.clearHistory() }
                )
            }

            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color.Transparent,
                contentColor = ElectricCyan,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                        color = ElectricCyan
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(title, style = MaterialTheme.typography.bodyMedium) }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(top = 16.dp)
            ) { page ->
                when (page) {
                    0 -> ImagesTab(viewModel)
                    1 -> VideosTab(viewModel)
                    2 -> FilesTab(viewModel)
                }
            }
        }
    }
}

@Composable
private fun ImagesTab(viewModel: GalleryViewModel) {
    val images by viewModel.images.collectAsState()
    val context = LocalContext.current

    if (images.isEmpty()) {
        EmptyState("No shared images yet")
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(images, key = { it.id }) { item ->
            ImageItem(item) { openFile(context, item) }
        }
    }
}

@Composable
private fun ImageItem(item: HistoryEntity, onClick: () -> Unit) {
    val context = LocalContext.current
    val bitmap = remember(item.mediaPath) {
        item.mediaPath?.let { path ->
            try {
                val uri = Uri.parse(path)
                val stream = context.contentResolver.openInputStream(uri)
                BitmapFactory.decodeStream(stream)
            } catch (e: Exception) {
                null
            }
        }
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x1AFFFFFF))
            .clickable { onClick() }
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + scaleIn(initialScale = 0.8f)
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = item.fileName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.InsertDriveFile,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.align(Alignment.Center).size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun VideosTab(viewModel: GalleryViewModel) {
    val videos by viewModel.videos.collectAsState()
    val context = LocalContext.current

    if (videos.isEmpty()) {
        EmptyState("No shared videos yet")
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(videos, key = { it.id }) { item ->
            FileRow(item, Icons.Default.Movie) { openFile(context, item) }
        }
    }
}

@Composable
private fun FilesTab(viewModel: GalleryViewModel) {
    val files by viewModel.files.collectAsState()
    val context = LocalContext.current

    if (files.isEmpty()) {
        EmptyState("No shared files yet")
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(files, key = { it.id }) { item ->
            val icon = when {
                item.mimeType.startsWith("audio/") -> Icons.Default.MusicNote
                item.mimeType == "application/pdf" -> Icons.Default.Description
                item.mimeType.contains("zip") -> Icons.Default.FolderZip
                else -> Icons.AutoMirrored.Filled.InsertDriveFile
            }
            FileRow(item, icon) { openFile(context, item) }
        }
    }
}

@Composable
private fun FileRow(entry: HistoryEntity, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                Text(entry.fileName, color = TextPrimary, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                Text(
                    "${labelFor(entry.type)} · ${FileUtils.formatSize(entry.sizeBytes)} · ${formatDate(entry.timestamp)}",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize().padding(40.dp), contentAlignment = Alignment.Center) {
        Text(message, color = TextMuted, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun openFile(context: android.content.Context, entry: HistoryEntity) {
    val path = entry.mediaPath ?: return
    try {
        val uri = Uri.parse(path)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, entry.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot open this file", Toast.LENGTH_SHORT).show()
    }
}

private fun labelFor(type: HistoryType): String = when (type) {
    HistoryType.SENT -> "Sent"
    HistoryType.RECEIVED -> "Received"
    HistoryType.DOWNLOADED -> "Downloaded"
}

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(timestamp))

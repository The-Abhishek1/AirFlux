package com.xcloak.airflux.feature.chat.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.xcloak.airflux.core.billing.PlanManager
import com.xcloak.airflux.core.common.ImageCompressUtils
import com.xcloak.airflux.core.common.QrUtils
import com.xcloak.airflux.core.designsystem.AppBackground
import com.xcloak.airflux.core.designsystem.GlassCard
import com.xcloak.airflux.data.database.entity.ChatMsgType
import com.xcloak.airflux.domain.model.ChatMessage
import com.xcloak.airflux.feature.chat.viewmodel.ChatConnectionState
import com.xcloak.airflux.feature.chat.viewmodel.ChatViewModel
import com.xcloak.airflux.ui.theme.ElectricCyan
import com.xcloak.airflux.ui.theme.ErrorRed
import com.xcloak.airflux.ui.theme.SuccessGreen
import com.xcloak.airflux.ui.theme.TextMuted
import com.xcloak.airflux.ui.theme.TextPrimary
import com.xcloak.airflux.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.platform.LocalContext
@Composable
fun ChatScreen(viewModel: ChatViewModel = viewModel()) {
    val connectionState by viewModel.connectionState.collectAsState()
    val hostInfo by viewModel.hostInfo.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isPro by PlanManager.isProFlow.collectAsState()

    AppBackground {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Wi-Fi Chat", color = TextPrimary, style = MaterialTheme.typography.headlineSmall)
                if (connectionState is ChatConnectionState.Connected) {
                    IconButton(onClick = { viewModel.disconnect() }) {
                        Icon(Icons.Default.Close, contentDescription = "Disconnect", tint = ErrorRed)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (val state = connectionState) {
                is ChatConnectionState.Idle -> IdleChooser(viewModel)
                is ChatConnectionState.Waiting -> WaitingHost(hostInfo)
                is ChatConnectionState.Connecting -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ElectricCyan)
                    }
                }
                is ChatConnectionState.Error -> {
                    Text(state.message, color = ErrorRed, style = MaterialTheme.typography.bodyMedium)
                }
                is ChatConnectionState.Disconnected -> {
                    Text("Peer disconnected", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.disconnect() }, colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)) {
                        Text("Back", color = Color.Black)
                    }
                }
                is ChatConnectionState.Connected -> {
                    if (!isPro) {
                        Text(
                            "Free plan keeps your last ${ChatViewModel.FREE_HISTORY_LIMIT} messages. Upgrade for unlimited history.",
                            color = TextMuted,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    ChatConversation(
                        messages = messages,
                        isPro = isPro,
                        onSend = { viewModel.sendMessage(it) },
                        onSendImage = { viewModel.sendImage(it) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun IdleChooser(viewModel: ChatViewModel) {
    val context = LocalContext.current
    var joinAddress by remember { mutableStateOf("") }
    var showJoinInput by remember { mutableStateOf(false) }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { viewModel.joinChat(it) }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) scanLauncher.launch(ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE).setBeepEnabled(false))
    }

    fun startScan() {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) scanLauncher.launch(ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE).setBeepEnabled(false))
        else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Start a chat with a nearby device over Wi-Fi — no internet needed.", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.startHosting() },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Host a Chat", color = Color.Black)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = { startScan() },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color.Black)
                Text("  Scan to Join", color = Color.Black)
            }
            Spacer(modifier = Modifier.height(10.dp))
            TextButton(onClick = { showJoinInput = !showJoinInput }) {
                Text(if (showJoinInput) "Hide manual entry" else "Or enter address manually", color = ElectricCyan)
            }
            if (showJoinInput) {
                OutlinedTextField(
                    value = joinAddress,
                    onValueChange = { joinAddress = it },
                    placeholder = { Text("192.168.x.x:8082?token=...", color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = ElectricCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.joinChat(joinAddress.trim()) },
                    enabled = joinAddress.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Join", color = Color.Black)
                }
            }
        }
    }
}

@Composable
private fun WaitingHost(hostInfo: String?) {
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(hostInfo) { hostInfo?.let { qrBitmap = QrUtils.generateQrBitmap(it) } }
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = SuccessGreen, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Text("  Waiting for a device to join...", color = SuccessGreen, style = MaterialTheme.typography.bodyMedium)
            }
            qrBitmap?.let { bmp ->
                Box(
                    modifier = Modifier.padding(top = 16.dp).size(180.dp).clip(RoundedCornerShape(12.dp)).background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Image(bitmap = bmp.asImageBitmap(), contentDescription = "QR code", modifier = Modifier.size(160.dp))
                }
            }
            Text("Have the other device scan this to join", color = TextMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        }
    }
}
@Composable
private fun ChatConversation(
    messages: List<ChatMessage>,
    isPro: Boolean,
    onSend: (String) -> Unit,
    onSendImage: (android.net.Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onSendImage(uri)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        LazyColumn(state = listState, modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(messages, key = { it.id }) { msg -> MessageBubble(msg) }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = {
                    if (isPro) imagePicker.launch("image/*")
                    else android.widget.Toast.makeText(context, "Photo sharing is a Pro feature", android.widget.Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(
                    Icons.Default.AddPhotoAlternate,
                    contentDescription = "Send photo",
                    tint = if (isPro) ElectricCyan else TextMuted
                )
            }
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Message", color = TextMuted) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = ElectricCyan
                ),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { if (input.isNotBlank()) { onSend(input.trim()); input = "" } }) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = ElectricCyan)
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: ChatMessage) {
    val alignment = if (msg.isMine) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (msg.isMine) ElectricCyan else Color(0x1FFFFFFF)
    val textColor = if (msg.isMine) Color.Black else TextPrimary

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(
            modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(bubbleColor).padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            if (msg.type == ChatMsgType.IMAGE && msg.imageData != null) {
                val bmp = remember(msg.imageData) { ImageCompressUtils.base64ToBitmap(msg.imageData) }
                bmp?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Shared photo",
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                }
            } else {
                Text(msg.text, color = textColor, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(msg.timestamp)),
                color = textColor.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
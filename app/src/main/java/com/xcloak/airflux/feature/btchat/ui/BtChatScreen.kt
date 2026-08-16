package com.xcloak.airflux.feature.btchat.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Close
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
import com.xcloak.airflux.core.billing.PlanManager
import com.xcloak.airflux.core.common.ImageCompressUtils
import com.xcloak.airflux.core.designsystem.AppBackground
import com.xcloak.airflux.core.designsystem.GlassCard
import com.xcloak.airflux.data.database.entity.ChatMsgType
import com.xcloak.airflux.domain.model.ChatMessage
import com.xcloak.airflux.feature.btchat.viewmodel.BtChatViewModel
import com.xcloak.airflux.feature.btchat.viewmodel.BtConnectionState
import com.xcloak.airflux.feature.btchat.viewmodel.BtDeviceInfo
import com.xcloak.airflux.ui.theme.ElectricCyan
import com.xcloak.airflux.ui.theme.ErrorRed
import com.xcloak.airflux.ui.theme.TextMuted
import com.xcloak.airflux.ui.theme.TextPrimary
import com.xcloak.airflux.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.platform.LocalContext
private fun requiredBtPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}

@Composable
fun BtChatScreen(viewModel: BtChatViewModel = viewModel()) {
    val context = LocalContext.current
    val connectionState by viewModel.connectionState.collectAsState()
    val paired by viewModel.pairedDevices.collectAsState()
    val discovered by viewModel.discoveredDevices.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isPro by PlanManager.isProFlow.collectAsState()

    var hasPermissions by remember {
        mutableStateOf(requiredBtPermissions().all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        })
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermissions = results.values.all { it }
        if (hasPermissions) viewModel.loadPairedDevices()
    }

    LaunchedEffect(Unit) {
        if (!hasPermissions) permissionLauncher.launch(requiredBtPermissions())
        else viewModel.loadPairedDevices()
    }

    AppBackground {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Bluetooth Chat", color = TextPrimary, style = MaterialTheme.typography.headlineSmall)
                if (connectionState is BtConnectionState.Connected) {
                    IconButton(onClick = { viewModel.disconnect() }) {
                        Icon(Icons.Default.Close, contentDescription = "Disconnect", tint = ErrorRed)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (!viewModel.isBluetoothSupported()) {
                Text("This device doesn't support Bluetooth.", color = ErrorRed, style = MaterialTheme.typography.bodyMedium)
                return@Column
            }
            if (!hasPermissions) {
                Text("Bluetooth permission is required to use this feature.", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { permissionLauncher.launch(requiredBtPermissions()) }, colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)) {
                    Text("Grant Permission", color = Color.Black)
                }
                return@Column
            }
            if (!viewModel.isBluetoothEnabled()) {
                Text("Please turn on Bluetooth in system settings to continue.", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                return@Column
            }

            when (val state = connectionState) {
                is BtConnectionState.Idle -> DeviceChooser(viewModel, paired, discovered, isScanning)
                is BtConnectionState.Waiting -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = ElectricCyan, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("  Waiting for a device to connect...", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        "Make sure the other device is paired with this one, then has it choose you from its list.",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                is BtConnectionState.Connecting -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ElectricCyan)
                    }
                }
                is BtConnectionState.Error -> {
                    Text(state.message, color = ErrorRed, style = MaterialTheme.typography.bodyMedium)
                }
                is BtConnectionState.Disconnected -> {
                    Text("Peer disconnected", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.disconnect() }, colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)) {
                        Text("Back", color = Color.Black)
                    }
                }
                is BtConnectionState.Connected -> {
                    if (!isPro) {
                        Text(
                            "Free plan keeps your last ${BtChatViewModel.FREE_HISTORY_LIMIT} messages. Upgrade for unlimited history.",
                            color = TextMuted,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    BtChatConversation(
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
private fun DeviceChooser(
    viewModel: BtChatViewModel,
    paired: List<BtDeviceInfo>,
    discovered: List<BtDeviceInfo>,
    isScanning: Boolean
) {
    Column {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Others need Bluetooth on and this device paired first.", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.startHosting() },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Bluetooth, contentDescription = null, tint = Color.Black)
                    Text("  Wait for Incoming Connection", color = Color.Black)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Paired Devices", color = TextPrimary, style = MaterialTheme.typography.titleSmall)
            Button(
                onClick = { viewModel.startScan() },
                enabled = !isScanning,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isScanning) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                } else {
                    Text("Scan", color = Color.Black, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (paired.isEmpty()) {
            Text("No paired devices. Pair one in system Bluetooth settings first.", color = TextMuted, style = MaterialTheme.typography.bodySmall)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(paired, key = { it.address }) { d -> DeviceRow(d, onClick = { viewModel.connectTo(d) }) }
            }
        }

        if (discovered.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Nearby Devices", color = TextPrimary, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(discovered, key = { it.address }) { d -> DeviceRow(d, onClick = { viewModel.connectTo(d) }) }
            }
        }
    }
}

@Composable
private fun DeviceRow(device: BtDeviceInfo, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(device.name, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                Text(device.address, color = TextMuted, style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan), shape = RoundedCornerShape(10.dp)) {
                Text("Connect", color = Color.Black, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun BtChatConversation(
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
            items(messages, key = { it.id }) { msg -> BtMessageBubble(msg) }
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
private fun BtMessageBubble(msg: ChatMessage) {
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
                        modifier = Modifier.size(200.dp).clip(RoundedCornerShape(10.dp))
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
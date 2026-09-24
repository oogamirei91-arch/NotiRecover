package com.notirecover.app.ui.screen

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notirecover.app.data.model.ConversationEntity
import com.notirecover.app.data.model.MessageEntity
import com.notirecover.app.ui.theme.*
import com.notirecover.app.util.ExportChatHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    conversation: ConversationEntity,
    messages: List<MessageEntity>,
    onBackClick: () -> Unit,
    onDeleteConversation: () -> Unit = {},
    onDeleteMessage: (MessageEntity) -> Unit = {}
) {
    val context = LocalContext.current
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var messageToDelete by remember { mutableStateOf<MessageEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = conversation.chatTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = when {
                                conversation.packageName.contains("whatsapp") -> "WhatsApp"
                                conversation.packageName.contains("instagram") -> "Instagram"
                                conversation.packageName.contains("telegram") -> "Telegram"
                                else -> "Social Chat"
                            },
                            fontSize = 12.sp,
                            color = TextSecondaryLight
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        ExportChatHelper.exportChatAsHtml(context, conversation, messages)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Ekspor Chat (PDF/HTML)",
                            tint = Color(0xFF2563EB)
                        )
                    }

                    IconButton(onClick = { showDeleteAllDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus Riwayat Chat",
                            tint = Color(0xFFDC2626)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Belum ada riwayat pesan untuk percakapan ini.",
                    color = TextSecondaryLight,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    // Baris Pesan: Bubble di sebelah kiri/tengah dan Tombol Hapus tepat di sebelah kanan bubble
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            MessageBubble(message = message)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Tombol Hapus Satuan di Sebelah Kanan Bubble Chat
                        IconButton(
                            onClick = { messageToDelete = message },
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0xFFF1F5F9), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus Pesan",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
        }

        // Dialog Konfirmasi Hapus Semua Pesan
        if (showDeleteAllDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteAllDialog = false },
                title = { Text("Hapus Semua Riwayat?", fontWeight = FontWeight.Bold) },
                text = { Text("Semua pesan dan foto yang tersimpan dari percakapan '${conversation.chatTitle}' akan dihapus permanen.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteAllDialog = false
                            onDeleteConversation()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) {
                        Text("Hapus Semua", color = Color.White)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showDeleteAllDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }

        // Dialog Konfirmasi Hapus Pesan Tunggal
        messageToDelete?.let { msg ->
            val previewText = if (msg.messageText.length > 50) msg.messageText.take(50) + "..." else msg.messageText
            AlertDialog(
                onDismissRequest = { messageToDelete = null },
                title = { Text("Hapus Pesan Ini?", fontWeight = FontWeight.Bold) },
                text = { Text("Pesan '$previewText' akan dihapus permanen dari riwayat percakapan.") },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteMessage(msg)
                            messageToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) {
                        Text("Hapus", color = Color.White)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { messageToDelete = null }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}

@Composable
fun MessageBubble(
    message: MessageEntity
) {
    val timeReceived = remember(message.receivedAt) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.receivedAt))
    }

    val timeDeleted = remember(message.deletedAt) {
        message.deletedAt?.let {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it))
        }
    }

    val bitmap = remember(message.mediaUri) {
        message.mediaUri?.let { path ->
            val file = File(path)
            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
        }
    }

    if (message.isDeleted) {
        // ==========================================
        // TAMPILAN KHUSUS: PESAN/FOTO YANG DIHAPUS PENGIRIM
        // ==========================================
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFFEF2F2),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (bitmap != null) "FOTO INI DIHAPUS OLEH PENGIRIM" else "PESAN INI DIHAPUS OLEH PENGIRIM",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Jika ada gambar terselamatkan, tampilkan fotonya
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Foto Terpulihkan",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Isi teks pesan
                Text(
                    text = message.messageText,
                    fontSize = 14.sp,
                    color = Color(0xFF1E293B),
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (timeDeleted != null) {
                        Text(
                            text = "Ditarik: $timeDeleted",
                            fontSize = 10.sp,
                            color = Color(0xFF991B1B)
                        )
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                    Text(
                        text = "Diterima: $timeReceived",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
    } else {
        // ==========================================
        // TAMPILAN PESAN/FOTO NORMAL
        // ==========================================
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Foto",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Text(
                    text = message.messageText,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = timeReceived,
                    fontSize = 10.sp,
                    color = TextSecondaryLight,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

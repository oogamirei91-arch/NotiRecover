package com.notirecover.app.ui.screen

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatDetailScreen(
    conversation: ConversationEntity,
    messages: List<MessageEntity>,
    onBackClick: () -> Unit,
    onDeleteConversation: () -> Unit = {},
    onDeleteMessage: (MessageEntity) -> Unit = {},
    onDeleteMultipleMessages: (Set<MessageEntity>) -> Unit = {}
) {
    val context = LocalContext.current
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var messageToDelete by remember { mutableStateOf<MessageEntity?>(null) }

    // Multi-selection states
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedMessages by remember { mutableStateOf<Set<MessageEntity>>(emptySet()) }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }

    // Tangani Tombol Back saat mode seleksi aktif
    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedMessages = emptySet()
    }

    LaunchedEffect(messages) {
        selectedMessages = selectedMessages.filter { sel -> messages.any { it.id == sel.id } }.toSet()
        if (selectedMessages.isEmpty() && isSelectionMode) {
            isSelectionMode = false
        }
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                // TopAppBar Khusus Mode Seleksi (Bulk Action)
                TopAppBar(
                    title = {
                        Text(
                            text = "${selectedMessages.size} Terpilih",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            isSelectionMode = false
                            selectedMessages = emptySet()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Batal Seleksi")
                        }
                    },
                    actions = {
                        // Tombol Pilih Semua / Batal Pilih Semua
                        IconButton(onClick = {
                            selectedMessages = if (selectedMessages.size == messages.size) {
                                emptySet()
                            } else {
                                messages.toSet()
                            }
                        }) {
                            Icon(
                                imageVector = if (selectedMessages.size == messages.size && messages.isNotEmpty())
                                    Icons.Default.Deselect
                                else
                                    Icons.Default.SelectAll,
                                contentDescription = "Pilih Semua"
                            )
                        }

                        // Tombol Hapus Massal Pesan
                        IconButton(
                            onClick = { showBulkDeleteDialog = true },
                            enabled = selectedMessages.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus Pesan Terpilih",
                                tint = if (selectedMessages.isNotEmpty()) Color(0xFFDC2626) else Color.Gray
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFFEFF6FF)
                    )
                )
            } else {
                TopAppBar(
                    title = {
                        val isGroup = remember(messages) {
                            messages.any { it.senderName.isNotBlank() && it.senderName != conversation.chatTitle }
                        }
                        val appLabel = when {
                            conversation.packageName.contains("whatsapp") -> "WhatsApp"
                            conversation.packageName.contains("instagram") -> "Instagram"
                            conversation.packageName.contains("telegram") -> "Telegram"
                            else -> "Social Chat"
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isGroup) {
                                    Icon(
                                        imageVector = Icons.Default.Groups,
                                        contentDescription = "Grup",
                                        tint = Color(0xFF2563EB),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = conversation.chatTitle,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = if (isGroup) "👥 Obrolan Grup • $appLabel" else appLabel,
                                fontSize = 12.sp,
                                color = if (isGroup) Color(0xFF0284C7) else TextSecondaryLight,
                                fontWeight = if (isGroup) FontWeight.SemiBold else FontWeight.Normal
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
                        // Tombol masuk ke mode seleksi
                        if (messages.isNotEmpty()) {
                            IconButton(onClick = { isSelectionMode = true }) {
                                Icon(
                                    imageVector = Icons.Default.Checklist,
                                    contentDescription = "Pilih Pesan",
                                    tint = Color(0xFF2563EB)
                                )
                            }
                        }

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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    val isSelected = selectedMessages.any { it.id == message.id }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .combinedClickable(
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedMessages = if (isSelected) {
                                            selectedMessages.filter { it.id != message.id }.toSet()
                                        } else {
                                            selectedMessages + message
                                        }
                                        if (selectedMessages.isEmpty()) {
                                            isSelectionMode = false
                                        }
                                    }
                                },
                                onLongClick = {
                                    if (!isSelectionMode) {
                                        isSelectionMode = true
                                        selectedMessages = setOf(message)
                                    }
                                }
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Checkbox indikator seleksi di samping kiri bubble saat mode seleksi aktif
                        if (isSelectionMode) {
                            Box(
                                modifier = Modifier
                                    .padding(end = 10.dp)
                                    .size(24.dp)
                                    .background(
                                        color = if (isSelected) Color(0xFF2563EB) else Color.LightGray.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Terpilih",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Bubble Chat penuh tanpa tombol trash di sampingnya
                        Box(modifier = Modifier.weight(1f)) {
                            MessageBubble(
                                message = message,
                                conversationChatTitle = conversation.chatTitle,
                                isSelected = isSelected && isSelectionMode
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

        // Dialog Konfirmasi Hapus Massal Pesan Terpilih (Bulk Delete)
        if (showBulkDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showBulkDeleteDialog = false },
                title = { Text("Hapus ${selectedMessages.size} Pesan?", fontWeight = FontWeight.Bold) },
                text = { Text("Pesan yang dipilih akan dihapus permanen dari riwayat percakapan ini.") },
                confirmButton = {
                    Button(
                        onClick = {
                            val itemsToDelete = selectedMessages
                            showBulkDeleteDialog = false
                            isSelectionMode = false
                            selectedMessages = emptySet()
                            onDeleteMultipleMessages(itemsToDelete)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) {
                        Text("Hapus", color = Color.White)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showBulkDeleteDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }

        // Dialog Konfirmasi Hapus Pesan Tunggal (Fallback jika ada)
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
    message: MessageEntity,
    conversationChatTitle: String = "",
    isSelected: Boolean = false
) {
    val isGroupSender = remember(message.senderName, conversationChatTitle) {
        message.senderName.isNotBlank() && message.senderName != conversationChatTitle
    }

    val senderColor = remember(message.senderName) {
        val colors = listOf(
            Color(0xFF0284C7), // Sky Blue
            Color(0xFF059669), // Emerald Green
            Color(0xFF7C3AED), // Purple
            Color(0xFFD97706), // Amber
            Color(0xFFDB2777), // Pink
            Color(0xFF4F46E5)  // Indigo
        )
        val idx = kotlin.math.abs(message.senderName.hashCode()) % colors.size
        colors[idx]
    }

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
            if (file.exists()) {
                val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                BitmapFactory.decodeFile(file.absolutePath, opts)
            } else null
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
            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF2563EB)) else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Header Nama Pengirim dalam Grup
                if (isGroupSender) {
                    Text(
                        text = "👤 ${message.senderName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = senderColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

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
            shadowElevation = if (isSelected) 3.dp else 1.dp,
            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF2563EB)) else null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Header Nama Pengirim dalam Grup
                if (isGroupSender) {
                    Text(
                        text = "👤 ${message.senderName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = senderColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

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

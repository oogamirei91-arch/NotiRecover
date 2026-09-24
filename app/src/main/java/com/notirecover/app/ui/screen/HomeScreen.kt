package com.notirecover.app.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notirecover.app.R
import com.notirecover.app.data.model.ConversationEntity
import com.notirecover.app.ui.theme.*
import com.notirecover.app.util.LanguageHelper
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    conversations: List<ConversationEntity>,
    isServiceEnabled: Boolean,
    currentLanguage: String = "ID",
    onEnableServiceClick: () -> Unit,
    onConversationClick: (ConversationEntity) -> Unit,
    onDeleteConversation: (ConversationEntity) -> Unit = {},
    onDeleteMultipleConversations: (Set<ConversationEntity>) -> Unit = {},
    onSettingsClick: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("ALL") }
    var conversationToDelete by remember { mutableStateOf<ConversationEntity?>(null) }
    
    // Multi-select state
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedConversations by remember { mutableStateOf<Set<ConversationEntity>>(emptySet()) }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }

    // Hitung statistik per-akun / per-sosmed
    val waList = remember(conversations) { conversations.filter { it.packageName.contains("whatsapp") } }
    val igList = remember(conversations) { conversations.filter { it.packageName.contains("instagram") } }
    val tgList = remember(conversations) { conversations.filter { it.packageName.contains("telegram") } }

    val filteredList = remember(conversations, selectedFilter) {
        if (selectedFilter == "ALL") {
            conversations
        } else {
            conversations.filter { it.packageName.contains(selectedFilter) }
        }
    }

    // Clean up selected items that no longer exist
    LaunchedEffect(conversations) {
        selectedConversations = selectedConversations.filter { sel -> conversations.any { it.id == sel.id } }.toSet()
        if (selectedConversations.isEmpty() && isSelectionMode) {
            isSelectionMode = false
        }
    }

    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedConversations = emptySet()
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = {
                        Text(
                            text = "${selectedConversations.size} Chat Terpilih",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            isSelectionMode = false
                            selectedConversations = emptySet()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Batal Seleksi")
                        }
                    },
                    actions = {
                        // Tombol Pilih Semua / Batal Pilih Semua
                        IconButton(onClick = {
                            selectedConversations = if (selectedConversations.size == filteredList.size) {
                                emptySet()
                            } else {
                                filteredList.toSet()
                            }
                        }) {
                            Icon(
                                imageVector = if (selectedConversations.size == filteredList.size && filteredList.isNotEmpty())
                                    Icons.Default.Deselect
                                else
                                    Icons.Default.SelectAll,
                                contentDescription = "Pilih Semua"
                            )
                        }

                        // Tombol Hapus Massal
                        IconButton(
                            onClick = { showBulkDeleteDialog = true },
                            enabled = selectedConversations.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus Terpilih",
                                tint = if (selectedConversations.isNotEmpty()) Color(0xFFDC2626) else Color.Gray
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.Transparent,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.app_logo),
                                    contentDescription = "Logo",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(10.dp))
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "ChatRestore",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp
                                )
                                Text(
                                    text = LanguageHelper.get("app_subtitle", currentLanguage),
                                    fontSize = 11.sp,
                                    color = TextSecondaryLight
                                )
                            }
                        }
                    },
                    actions = {
                        if (filteredList.isNotEmpty()) {
                            IconButton(onClick = { isSelectionMode = true }) {
                                Icon(
                                    imageVector = Icons.Default.Checklist,
                                    contentDescription = "Pilih Banyak Chat"
                                )
                            }
                        }

                        Surface(
                            color = Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = "👑 PRO",
                                color = Color(0xFFB45309),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        IconButton(onClick = onSettingsClick) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Pengaturan",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Banner Status Service
            ServiceStatusBanner(
                isEnabled = isServiceEnabled,
                currentLanguage = currentLanguage,
                onEnableClick = onEnableServiceClick
            )

            // =========================================================================
            // HUB PER-AKUN SOSMED (TAMPILAN STATISTIK & FILTER CEPAT PER AKUN)
            // =========================================================================
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    AccountCard(
                        title = "Semua Akun",
                        totalChats = conversations.size,
                        totalDeleted = conversations.sumOf { it.deletedCount },
                        badgeColor = PrimaryBlue,
                        isSelected = selectedFilter == "ALL",
                        onClick = {
                            selectedFilter = "ALL"
                            selectedConversations = emptySet()
                        }
                    )
                }
                item {
                    AccountCard(
                        title = "WhatsApp",
                        totalChats = waList.size,
                        totalDeleted = waList.sumOf { it.deletedCount },
                        badgeColor = Color(0xFF25D366),
                        isSelected = selectedFilter == "whatsapp",
                        onClick = {
                            selectedFilter = "whatsapp"
                            selectedConversations = emptySet()
                        }
                    )
                }
                item {
                    AccountCard(
                        title = "Instagram",
                        totalChats = igList.size,
                        totalDeleted = igList.sumOf { it.deletedCount },
                        badgeColor = Color(0xFFE1306C),
                        isSelected = selectedFilter == "instagram",
                        onClick = {
                            selectedFilter = "instagram"
                            selectedConversations = emptySet()
                        }
                    )
                }
                item {
                    AccountCard(
                        title = "Telegram",
                        totalChats = tgList.size,
                        totalDeleted = tgList.sumOf { it.deletedCount },
                        badgeColor = Color(0xFF0088CC),
                        isSelected = selectedFilter == "telegram",
                        onClick = {
                            selectedFilter = "telegram"
                            selectedConversations = emptySet()
                        }
                    )
                }
            }

            // Konten Utama
            if (filteredList.isEmpty()) {
                WelcomeEmptyStateView(
                    isServiceEnabled = isServiceEnabled,
                    currentLanguage = currentLanguage,
                    onEnableClick = onEnableServiceClick
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.id }) { conversation ->
                        val isSelected = selectedConversations.contains(conversation)
                        ConversationCard(
                            conversation = conversation,
                            isSelectionMode = isSelectionMode,
                            isSelected = isSelected,
                            onClick = {
                                if (isSelectionMode) {
                                    selectedConversations = if (isSelected) {
                                        selectedConversations - conversation
                                    } else {
                                        selectedConversations + conversation
                                    }
                                } else {
                                    onConversationClick(conversation)
                                }
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    isSelectionMode = true
                                    selectedConversations = setOf(conversation)
                                }
                            },
                            onDeleteClick = { conversationToDelete = conversation }
                        )
                    }
                }
            }
        }

        // Dialog Konfirmasi Hapus Chat Satuan
        conversationToDelete?.let { conv ->
            AlertDialog(
                onDismissRequest = { conversationToDelete = null },
                title = { Text("Hapus Riwayat Chat?", fontWeight = FontWeight.Bold) },
                text = { Text("Semua pesan yang tersimpan dari '${conv.chatTitle}' akan dihapus dari aplikasi.") },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteConversation(conv)
                            conversationToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) {
                        Text("Hapus", color = Color.White)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { conversationToDelete = null }) {
                        Text("Batal")
                    }
                }
            )
        }

        // Dialog Konfirmasi Hapus Massal (Bulk Delete Chat)
        if (showBulkDeleteDialog && selectedConversations.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { showBulkDeleteDialog = false },
                title = { Text("Hapus ${selectedConversations.size} Chat Terpilih?", fontWeight = FontWeight.Bold) },
                text = { Text("Semua pesan dan riwayat dari ${selectedConversations.size} percakapan yang dipilih akan dihapus permanen.") },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteMultipleConversations(selectedConversations)
                            showBulkDeleteDialog = false
                            isSelectionMode = false
                            selectedConversations = emptySet()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) {
                        Text("Hapus Semua (${selectedConversations.size})", color = Color.White)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showBulkDeleteDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}

@Composable
fun AccountCard(
    title: String,
    totalChats: Int,
    totalDeleted: Int,
    badgeColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(135.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) badgeColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) badgeColor else Color(0xFFE2E8F0)
        ),
        shadowElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (isSelected) badgeColor else MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = CircleShape,
                    color = badgeColor,
                    modifier = Modifier.size(8.dp)
                ) {}
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "$totalChats Kontak",
                fontSize = 11.sp,
                color = TextSecondaryLight
            )

            Spacer(modifier = Modifier.height(2.dp))

            if (totalDeleted > 0) {
                Text(
                    text = "🗑️ $totalDeleted Dihapus",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFDC2626)
                )
            } else {
                Text(
                    text = "0 Terhapus",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

@Composable
fun ServiceStatusBanner(
    isEnabled: Boolean,
    currentLanguage: String = "ID",
    onEnableClick: () -> Unit
) {
    val isId = currentLanguage == "ID"
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (isEnabled) Color(0xFFF0FDF4) else Color(0xFFFFFBEB),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isEnabled) Color(0xFFBBF7D0) else Color(0xFFFDE68A)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (isEnabled) AccentGreen else Color(0xFFD97706),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isEnabled) Icons.Default.CheckCircle else Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isEnabled) (if (isId) "🟢 Monitoring Aktif" else "🟢 Monitoring Active") else (if (isId) "⚠️ Izin Notifikasi Dibutuhkan" else "⚠️ Notification Permission Needed"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isEnabled) Color(0xFF166534) else Color(0xFF92400E)
                )
                Text(
                    text = if (isEnabled) (if (isId) "Siap merekam pesan & foto yang dihapus pengirim" else "Ready to capture deleted messages & photos") else (if (isId) "Aktifkan agar aplikasi dapat mencatat chat masuk" else "Enable access so the app can log incoming chats"),
                    fontSize = 11.sp,
                    color = if (isEnabled) Color(0xFF15803D) else Color(0xFFB45309)
                )
            }
            if (!isEnabled) {
                Button(
                    onClick = onEnableClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isId) "Aktifkan" else "Enable", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun WelcomeEmptyStateView(
    isServiceEnabled: Boolean,
    currentLanguage: String = "ID",
    onEnableClick: () -> Unit
) {
    val isId = currentLanguage == "ID"
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 3.dp
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF2563EB), Color(0xFF3B82F6))
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isId) "👋 Selamat Datang!" else "👋 Welcome!",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isId)
                                "ChatRestore akan otomatis mencatat dan menyelamatkan pesan serta foto dari WhatsApp, IG, & Telegram yang ditarik/dihapus pengirim."
                            else
                                "ChatRestore automatically saves and recovers messages & photos from WhatsApp, IG, and Telegram deleted by the sender.",
                            color = Color(0xFFDBEAFE),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFFEF2F2),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isId) "CONTOH PESAN YANG DIHAPUS" else "DELETED MESSAGE EXAMPLE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                        }
                        Text(if (isId) "Dihapus: 10:45" else "Deleted: 10:45", fontSize = 10.sp, color = Color(0xFF991B1B))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isId) "“Besok kita jadi ketemuan jam 7 malam ya!”" else "“Hey, let's meet tomorrow at 7 PM!”",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isId) "✨ Pesan asli tetap aman tersimpan di sini meski dihapus di WhatsApp" else "✨ Original text remains safely preserved even when deleted in WhatsApp",
                        fontSize = 11.sp,
                        color = Color(0xFF059669),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isId) "Cara Kerja & Penggunaan:" else "How It Works:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    StepRow(
                        number = "1",
                        title = if (isId) "Pastikan Izin Notifikasi Aktif" else "Ensure Notification Access is Enabled",
                        desc = if (isId) "Klik tombol 'Aktifkan' di atas untuk mengizinkan aplikasi membaca notifikasi." else "Click 'Enable' above to allow the app to read notifications."
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    StepRow(
                        number = "2",
                        title = if (isId) "Gunakan Medsos Seperti Biasa" else "Use Social Media Normally",
                        desc = if (isId) "Saat ada pesan/foto masuk dari teman, aplikasi akan otomatis menyalinnya." else "When incoming chats/photos arrive, the app logs them instantly."
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    StepRow(
                        number = "3",
                        title = if (isId) "Lihat Chat yang Dihapus" else "View Recovered Deleted Chats",
                        desc = if (isId) "Buka ChatRestore kapan saja untuk melihat teks dan foto yang telah ditarik pengirim." else "Open ChatRestore anytime to view recovered text & media."
                    )
                }
            }
        }
    }
}

@Composable
fun StepRow(number: String, title: String, desc: String) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(
            shape = CircleShape,
            color = Color(0xFFEFF6FF),
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = PrimaryBlue
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(text = desc, fontSize = 11.sp, color = TextSecondaryLight)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationCard(
    conversation: ConversationEntity,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    val appBadgeColor = when {
        conversation.packageName.contains("whatsapp") -> Color(0xFF25D366)
        conversation.packageName.contains("instagram") -> Color(0xFFE1306C)
        conversation.packageName.contains("telegram") -> Color(0xFF0088CC)
        else -> PrimaryBlue
    }

    val appName = when {
        conversation.packageName.contains("whatsapp") -> "WA"
        conversation.packageName.contains("instagram") -> "IG"
        conversation.packageName.contains("telegram") -> "TG"
        else -> "Chat"
    }

    val formattedTime = remember(conversation.updatedAt) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.format(Date(conversation.updatedAt))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFFEFF6FF) else MaterialTheme.colorScheme.surface,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, PrimaryBlue) else null,
        shadowElevation = if (isSelected) 3.dp else 1.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) PrimaryBlue else Color(0xFFE2E8F0)),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Terpilih",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = conversation.chatTitle.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimaryLight
                    )
                    Surface(
                        shape = CircleShape,
                        color = appBadgeColor,
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.BottomEnd)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(appName.take(1), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = conversation.chatTitle,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = formattedTime,
                        fontSize = 12.sp,
                        color = TextSecondaryLight
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.lastMessage,
                        fontSize = 13.sp,
                        color = TextSecondaryLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (conversation.deletedCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AlertRedContainer,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = null,
                                    tint = AlertRedText,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${conversation.deletedCount} Terhapus",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AlertRedText
                                )
                            }
                        }
                    }
                }
            }

            if (!isSelectionMode) {
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus Percakapan",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

package com.notirecover.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notirecover.app.data.model.ConversationEntity
import com.notirecover.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    conversations: List<ConversationEntity>,
    isServiceEnabled: Boolean,
    onEnableServiceClick: () -> Unit,
    onConversationClick: (ConversationEntity) -> Unit
) {
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filterOptions = listOf(
        "ALL" to "Semua",
        "com.whatsapp" to "WhatsApp",
        "com.instagram.android" to "Instagram",
        "org.telegram.messenger" to "Telegram",
        "com.facebook.orca" to "Messenger"
    )

    val filteredList = remember(conversations, selectedFilter) {
        if (selectedFilter == "ALL") {
            conversations
        } else {
            conversations.filter { it.packageName == selectedFilter }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "ChatRestore",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Universal Chat & Deleted Log",
                            fontSize = 12.sp,
                            color = TextSecondaryLight
                        )
                    }
                },
                actions = {
                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = "👑 PRO",
                            color = Color(0xFFB45309),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Status Service Card Banner
            ServiceStatusBanner(
                isEnabled = isServiceEnabled,
                onEnableClick = onEnableServiceClick
            )

            // Filter Chips Sosmed
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filterOptions) { (key, label) ->
                    FilterChip(
                        selected = selectedFilter == key,
                        onClick = { selectedFilter = key },
                        label = { Text(label) }
                    )
                }
            }

            // List Percakapan
            if (filteredList.isEmpty()) {
                EmptyStateView(isServiceEnabled = isServiceEnabled)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.id }) { conversation ->
                        ConversationCard(
                            conversation = conversation,
                            onClick = { onConversationClick(conversation) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceStatusBanner(
    isEnabled: Boolean,
    onEnableClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isEnabled) Color(0xFFECFDF5) else Color(0xFFFFFBEB),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isEnabled) Color(0xFFA7F3D0) else Color(0xFFFDE68A)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isEnabled) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isEnabled) AccentGreen else Color(0xFFD97706),
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isEnabled) "Monitoring Aktif" else "Izin Notifikasi Belum Aktif",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isEnabled) Color(0xFF065F46) else Color(0xFF92400E)
                )
                Text(
                    text = if (isEnabled) "Siap mendeteksi pesan yang dihapus" else "Aktifkan agar aplikasi bisa mencatat pesan",
                    fontSize = 12.sp,
                    color = if (isEnabled) Color(0xFF047857) else Color(0xFFB45309)
                )
            }
            if (!isEnabled) {
                Button(
                    onClick = onEnableClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Aktifkan", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun ConversationCard(
    conversation: ConversationEntity,
    onClick: () -> Unit
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
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar Placeholder dengan Badge App
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE2E8F0)),
                contentAlignment = Alignment.Center
            ) {
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

                    // Badge Pesan Terhapus (Jika Ada)
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
        }
    }
}

@Composable
fun EmptyStateView(isServiceEnabled: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.DeleteSweep,
                contentDescription = null,
                tint = Color(0xFFCBD5E1),
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Belum Ada Pesan Masuk",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isServiceEnabled)
                    "Kirim atau terima pesan di WhatsApp/IG/Telegram, maka riwayatnya akan otomatis tercatat di sini."
                else
                    "Silakan aktifkan Izin Akses Notifikasi di atas agar aplikasi dapat mulai mencatat.",
                fontSize = 13.sp,
                color = TextSecondaryLight,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

package com.notirecover.app.ui.screen

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.notirecover.app.data.model.MessageEntity
import com.notirecover.app.ui.theme.AlertRed
import com.notirecover.app.ui.theme.AlertRedContainer
import com.notirecover.app.ui.theme.AlertRedText
import com.notirecover.app.ui.theme.TextSecondaryLight
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MediaGalleryScreen(
    mediaMessages: List<MessageEntity>,
    onDeleteMedia: (MessageEntity) -> Unit = {},
    onDeleteMultipleMedia: (Set<MessageEntity>) -> Unit = {}
) {
    var selectedMediaForPreview by remember { mutableStateOf<MessageEntity?>(null) }
    var mediaToDelete by remember { mutableStateOf<MessageEntity?>(null) }
    
    // Selection mode states
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedItems by remember { mutableStateOf<Set<MessageEntity>>(emptySet()) }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }

    // Handle back button when in selection mode
    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedItems = emptySet()
    }

    LaunchedEffect(mediaMessages) {
        selectedItems = selectedItems.filter { sel -> mediaMessages.any { it.id == sel.id } }.toSet()
        if (selectedItems.isEmpty() && isSelectionMode) {
            isSelectionMode = false
        }
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = {
                        Text(
                            text = "${selectedItems.size} Terpilih",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            isSelectionMode = false
                            selectedItems = emptySet()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Batal Seleksi")
                        }
                    },
                    actions = {
                        // Tombol Pilih Semua / Batal Pilih Semua
                        IconButton(onClick = {
                            selectedItems = if (selectedItems.size == mediaMessages.size) {
                                emptySet()
                            } else {
                                mediaMessages.toSet()
                            }
                        }) {
                            Icon(
                                imageVector = if (selectedItems.size == mediaMessages.size && mediaMessages.isNotEmpty())
                                    Icons.Default.Deselect
                                else
                                    Icons.Default.SelectAll,
                                contentDescription = "Pilih Semua"
                            )
                        }

                        // Tombol Hapus Massal
                        IconButton(
                            onClick = { showBulkDeleteDialog = true },
                            enabled = selectedItems.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus Terpilih",
                                tint = if (selectedItems.isNotEmpty()) Color(0xFFDC2626) else Color.Gray
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
                        Column {
                            Text(
                                text = "Galeri Media Terpulihkan",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "${mediaMessages.size} File Terselamatkan",
                                fontSize = 12.sp,
                                color = TextSecondaryLight
                            )
                        }
                    },
                    actions = {
                        if (mediaMessages.isNotEmpty()) {
                            IconButton(onClick = { isSelectionMode = true }) {
                                Icon(
                                    imageVector = Icons.Default.Checklist,
                                    contentDescription = "Pilih Media",
                                    tint = Color(0xFF2563EB)
                                )
                            }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        if (mediaMessages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Belum Ada Media yang Diselamatkan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Foto dan gambar dari notifikasi chat akan muncul di sini.",
                        fontSize = 12.sp,
                        color = TextSecondaryLight
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(mediaMessages, key = { it.id }) { item ->
                    val isSelected = selectedItems.any { it.id == item.id }
                    MediaGridItem(
                        message = item,
                        isSelectionMode = isSelectionMode,
                        isSelected = isSelected,
                        onClick = {
                            if (isSelectionMode) {
                                selectedItems = if (isSelected) {
                                    selectedItems.filter { it.id != item.id }.toSet()
                                } else {
                                    selectedItems + item
                                }
                                if (selectedItems.isEmpty()) {
                                    isSelectionMode = false
                                }
                            } else {
                                selectedMediaForPreview = item
                            }
                        },
                        onLongClick = {
                            if (!isSelectionMode) {
                                isSelectionMode = true
                                selectedItems = setOf(item)
                            }
                        },
                        onDeleteClick = { mediaToDelete = item }
                    )
                }
            }
        }

        // Fullscreen Image Dialog Preview
        selectedMediaForPreview?.let { media ->
            ImagePreviewDialog(
                media = media,
                onDismiss = { selectedMediaForPreview = null },
                onDelete = {
                    onDeleteMedia(media)
                    selectedMediaForPreview = null
                }
            )
        }

        // Dialog Konfirmasi Hapus Media Satuan
        mediaToDelete?.let { media ->
            AlertDialog(
                onDismissRequest = { mediaToDelete = null },
                title = { Text("Hapus Gambar Ini?", fontWeight = FontWeight.Bold) },
                text = { Text("File gambar ini akan dihapus permanen dari memori internal aplikasi.") },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteMedia(media)
                            mediaToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) {
                        Text("Hapus", color = Color.White)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { mediaToDelete = null }) {
                        Text("Batal")
                    }
                }
            )
        }

        // Dialog Konfirmasi Hapus Massal (Bulk Delete)
        if (showBulkDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showBulkDeleteDialog = false },
                title = { Text("Hapus ${selectedItems.size} Gambar Terpilih?", fontWeight = FontWeight.Bold) },
                text = { Text("Semua file gambar yang dipilih akan dihapus permanen dari memori internal aplikasi.") },
                confirmButton = {
                    Button(
                        onClick = {
                            val itemsToDelete = selectedItems
                            showBulkDeleteDialog = false
                            isSelectionMode = false
                            selectedItems = emptySet()
                            onDeleteMultipleMedia(itemsToDelete)
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
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaGridItem(
    message: MessageEntity,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    val bitmap = remember(message.mediaUri) {
        message.mediaUri?.let { path ->
            val file = File(path)
            if (file.exists()) {
                val options = BitmapFactory.Options().apply { inSampleSize = 2 }
                BitmapFactory.decodeFile(file.absolutePath, options)
            } else null
        }
    }

    val formattedTime = remember(message.receivedAt) {
        SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(message.receivedAt))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = if (isSelected) 4.dp else 2.dp,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF2563EB)) else null,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Foto Terpulihkan",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }

                // Checkbox / Indikator Seleksi
                if (isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .size(26.dp)
                            .background(
                                color = if (isSelected) Color(0xFF2563EB) else Color.Black.copy(alpha = 0.45f),
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

                // Badge Jika Dihapus Pengirim
                if (message.isDeleted) {
                    Surface(
                        color = AlertRed,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = "TERHAPUS",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = message.senderName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formattedTime,
                        fontSize = 10.sp,
                        color = TextSecondaryLight
                    )
                }

                if (!isSelectionMode) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus Gambar",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ImagePreviewDialog(
    media: MessageEntity,
    onDismiss: () -> Unit,
    onDelete: () -> Unit = {}
) {
    var showConfirmDelete by remember { mutableStateOf(false) }

    val bitmap = remember(media.mediaUri) {
        media.mediaUri?.let { path ->
            val file = File(path)
            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = media.senderName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        if (media.isDeleted) {
                            Text(
                                text = "⚠️ Foto ini telah ditarik oleh pengirim",
                                color = AlertRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(onClick = { showConfirmDelete = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus Gambar",
                            tint = Color(0xFFDC2626)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Preview Gambar",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tutup")
                }
            }
        }
    }

    if (showConfirmDelete) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            title = { Text("Hapus Gambar?", fontWeight = FontWeight.Bold) },
            text = { Text("File gambar ini akan dihapus permanen dari perangkat.") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDelete = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Hapus", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmDelete = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

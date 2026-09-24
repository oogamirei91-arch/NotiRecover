package com.notirecover.app.ui.screen

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.notirecover.app.ui.theme.TextSecondaryLight
import com.notirecover.app.util.StatusMediaItem
import com.notirecover.app.util.StatusSaverHelper
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusSaverScreen() {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: WA Cache, 1: Downloaded
    var statusList by remember { mutableStateOf(emptyList<StatusMediaItem>()) }
    var downloadedList by remember { mutableStateOf(emptyList<StatusMediaItem>()) }
    var selectedStatusForPreview by remember { mutableStateOf<StatusMediaItem?>(null) }
    var statusToDelete by remember { mutableStateOf<StatusMediaItem?>(null) }
    var hasFolderConnected by remember { mutableStateOf(StatusSaverHelper.getSavedTreeUri(context) != null) }

    fun refreshStatuses() {
        statusList = StatusSaverHelper.getAllStatuses(context)
        downloadedList = StatusSaverHelper.getDownloadedStatuses(context)
        hasFolderConnected = StatusSaverHelper.getSavedTreeUri(context) != null || statusList.isNotEmpty()
    }

    // Launcher Pemilih Folder SAF Resmi Android
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            StatusSaverHelper.saveTreeUri(context, uri)
            hasFolderConnected = true
            refreshStatuses()
            Toast.makeText(context, "Folder WhatsApp Berhasil Dihubungkan! 🎉", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        refreshStatuses()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Status Saver",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = if (selectedTab == 0) "Status Teman (Incognito)" else "${downloadedList.size} Status Tersimpan",
                            fontSize = 12.sp,
                            color = TextSecondaryLight
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { refreshStatuses() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Segarkan Status")
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
            // Tab Switcher: Status WA vs Status Tersimpan (Download)
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Status WhatsApp", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        downloadedList = StatusSaverHelper.getDownloadedStatuses(context)
                    },
                    text = { Text("Tersimpan (${downloadedList.size})", fontWeight = FontWeight.SemiBold) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (selectedTab == 0) {
                // ==========================================
                // TAB 1: STATUS WHATSAPP AKTIF
                // ==========================================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    // Banner Hubungkan Folder WhatsApp (Jika Belum Terhubung di Android 11+)
                    if (!hasFolderConnected) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFFFFBEB),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.FolderOpen,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Hubungkan Folder WhatsApp",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF92400E)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Klik tombol di bawah lalu tekan 'Gunakan Folder Ini' agar status dapat terbaca otomatis.",
                                    fontSize = 12.sp,
                                    color = Color(0xFFB45309),
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { folderPickerLauncher.launch(null) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Folder, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Pilih Folder WhatsApp", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (statusList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Belum Ada Status Terdeteksi",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "1. Buka aplikasi WhatsApp -> Tonton status teman selama 1–2 detik.\n2. Kembali ke sini dan klik 'Segarkan'.",
                                    fontSize = 12.sp,
                                    color = TextSecondaryLight,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedButton(onClick = { folderPickerLauncher.launch(null) }) {
                                        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Pilih Folder")
                                    }
                                    Button(onClick = { refreshStatuses() }) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Segarkan")
                                    }
                                }
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(statusList, key = { it.uri.toString() }) { item ->
                                StatusGridCard(
                                    statusItem = item,
                                    isDownloaded = false,
                                    onClick = { selectedStatusForPreview = item },
                                    onSaveClick = {
                                        val success = StatusSaverHelper.saveStatusToGallery(context, item)
                                        if (success) {
                                            refreshStatuses()
                                            Toast.makeText(context, "Berhasil disimpan ke Galeri HP! 🎉", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Gagal menyimpan file", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                // ==========================================
                // TAB 2: STATUS TERSIMPAN (DOWNLOADED)
                // ==========================================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    if (downloadedList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.DownloadDone,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Belum Ada Status yang Diunduh",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Status yang Anda unduh dari tab 'Status WhatsApp' atau 'WhatsApp Web' akan tersimpan rapi di sini.",
                                    fontSize = 12.sp,
                                    color = TextSecondaryLight,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(downloadedList, key = { it.uri.toString() }) { item ->
                                StatusGridCard(
                                    statusItem = item,
                                    isDownloaded = true,
                                    onClick = { selectedStatusForPreview = item },
                                    onDeleteClick = { statusToDelete = item }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Dialog Preview Status
        selectedStatusForPreview?.let { item ->
            val isDownloadedItem = downloadedList.any { it.name == item.name }
            StatusPreviewDialog(
                statusItem = item,
                isDownloaded = isDownloadedItem,
                onDismiss = { selectedStatusForPreview = null },
                onSave = {
                    val success = StatusSaverHelper.saveStatusToGallery(context, item)
                    if (success) {
                        refreshStatuses()
                        Toast.makeText(context, "Berhasil disimpan ke Galeri HP! 🎉", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Gagal menyimpan file", Toast.LENGTH_SHORT).show()
                    }
                },
                onDelete = {
                    statusToDelete = item
                }
            )
        }

        // Dialog Konfirmasi Hapus Status Downloaded
        statusToDelete?.let { item ->
            AlertDialog(
                onDismissRequest = { statusToDelete = null },
                title = { Text("Hapus Status Ini?", fontWeight = FontWeight.Bold) },
                text = { Text("File status '${item.name}' akan dihapus permanen dari penyimpanan HP Anda.") },
                confirmButton = {
                    Button(
                        onClick = {
                            val deleted = StatusSaverHelper.deleteStatus(context, item)
                            if (deleted) {
                                Toast.makeText(context, "Status berhasil dihapus!", Toast.LENGTH_SHORT).show()
                                refreshStatuses()
                                if (selectedStatusForPreview == item) selectedStatusForPreview = null
                            } else {
                                Toast.makeText(context, "Gagal menghapus file", Toast.LENGTH_SHORT).show()
                            }
                            statusToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) {
                        Text("Hapus", color = Color.White)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { statusToDelete = null }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}

@Composable
fun StatusGridCard(
    statusItem: StatusMediaItem,
    isDownloaded: Boolean = false,
    onClick: () -> Unit,
    onSaveClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val bitmap = remember(statusItem.uri) {
        if (!statusItem.isVideo) {
            try {
                val inputStream: InputStream? = if (statusItem.file != null) {
                    java.io.FileInputStream(statusItem.file)
                } else {
                    context.contentResolver.openInputStream(statusItem.uri)
                }
                inputStream?.use { BitmapFactory.decodeStream(it) }
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(Color(0xFFE2E8F0)),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Status",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (statusItem.isVideo) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Video",
                        tint = Color.White,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            if (!isDownloaded) {
                IconButton(
                    onClick = onSaveClick,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(36.dp)
                        .background(Color(0xFF2563EB), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Simpan",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(36.dp)
                        .background(Color(0xFFDC2626), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StatusPreviewDialog(
    statusItem: StatusMediaItem,
    isDownloaded: Boolean = false,
    onDismiss: () -> Unit,
    onSave: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val context = LocalContext.current
    val bitmap = remember(statusItem.uri) {
        if (!statusItem.isVideo) {
            try {
                val inputStream: InputStream? = if (statusItem.file != null) {
                    java.io.FileInputStream(statusItem.file)
                } else {
                    context.contentResolver.openInputStream(statusItem.uri)
                }
                inputStream?.use { BitmapFactory.decodeStream(it) }
            } catch (e: Exception) {
                null
            }
        } else {
            null
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
                            text = if (statusItem.isVideo) "Video Status" else "Foto Status",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (isDownloaded) "Tersimpan di Galeri ChatRestore" else "Status WhatsApp Aktif",
                            fontSize = 11.sp,
                            color = Color(0xFF0369A1)
                        )
                    }

                    if (isDownloaded) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus Status",
                                tint = Color(0xFFDC2626)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Preview Status",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                } else if (statusItem.isVideo) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color.Black, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                            Text("Video Status: ${statusItem.name}", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Tutup")
                    }
                    if (!isDownloaded) {
                        Button(
                            onClick = {
                                onSave()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Simpan")
                        }
                    }
                }
            }
        }
    }
}

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
    var statusList by remember { mutableStateOf(emptyList<StatusMediaItem>()) }
    var selectedStatusForPreview by remember { mutableStateOf<StatusMediaItem?>(null) }
    var hasFolderConnected by remember { mutableStateOf(StatusSaverHelper.getSavedTreeUri(context) != null) }

    fun refreshStatuses() {
        statusList = StatusSaverHelper.getAllStatuses(context)
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
                            text = "Status Saver (Ghost Mode)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Tonton & Simpan Tanpa Ketahuan",
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
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
                                text = "Hubungkan Folder WhatsApp (Wajib 1x)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF92400E)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Agar foto & video status dapat terbaca otomatis di HP Anda, klik tombol di bawah lalu tekan 'Gunakan Folder Ini' pada pop-up sistem Android.",
                            fontSize = 12.sp,
                            color = Color(0xFFB45309),
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                folderPickerLauncher.launch(null)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pilih & Hubungkan Folder WhatsApp", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Banner Ghost Mode
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF0F9FF),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBAE6FD)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Tonton status 1-2 detik di WhatsApp agar file terunduh di HP, lalu simpan permanen di sini!",
                        fontSize = 11.sp,
                        color = Color(0xFF0369A1)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                            text = "1. Buka aplikasi WhatsApp -> Tonton status teman selama 1–2 detik.\n2. Kembali ke sini dan klik 'Segarkan Status'.",
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
                            onClick = { selectedStatusForPreview = item },
                            onSaveClick = {
                                val success = StatusSaverHelper.saveStatusToGallery(context, item)
                                if (success) {
                                    Toast.makeText(context, "Berhasil disimpan ke Galeri HP!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Gagal menyimpan file", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }

        // Dialog Preview
        selectedStatusForPreview?.let { item ->
            StatusPreviewDialog(
                statusItem = item,
                onDismiss = { selectedStatusForPreview = null },
                onSave = {
                    val success = StatusSaverHelper.saveStatusToGallery(context, item)
                    if (success) {
                        Toast.makeText(context, "Berhasil disimpan ke Galeri HP!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Gagal menyimpan file", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

@Composable
fun StatusGridCard(
    statusItem: StatusMediaItem,
    onClick: () -> Unit,
    onSaveClick: () -> Unit
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
        }
    }
}

@Composable
fun StatusPreviewDialog(
    statusItem: StatusMediaItem,
    onDismiss: () -> Unit,
    onSave: () -> Unit
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
                Text(
                    text = if (statusItem.isVideo) "Video Status WhatsApp" else "Foto Status WhatsApp",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Tersimpan lokal & ditonton tanpa terdeteksi",
                    fontSize = 11.sp,
                    color = Color(0xFF0369A1)
                )

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

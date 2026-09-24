package com.notirecover.app.ui.screen

import android.graphics.BitmapFactory
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VisibilityOff
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
import com.notirecover.app.util.StatusItem
import com.notirecover.app.util.StatusSaverHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusSaverScreen() {
    val context = LocalContext.current
    var statusList by remember { mutableStateOf(emptyList<StatusItem>()) }
    var selectedStatusForPreview by remember { mutableStateOf<StatusItem?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    fun refreshStatuses() {
        isRefreshing = true
        statusList = StatusSaverHelper.getActiveStatuses()
        isRefreshing = false
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
            // Banner Penjelasan Ghost Viewer (Mode Siluman)
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
                        text = "Buka tab Status di WA agar file terunduh di HP, lalu tonton & simpan di sini tanpa nama Anda muncul di daftar penonton!",
                        fontSize = 12.sp,
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
                            text = "Buka aplikasi WhatsApp $\\rightarrow$ buka tab Status/Pembaruan sebentar, lalu kembali ke sini dan klik tombol Segarkan (🔄).",
                            fontSize = 12.sp,
                            color = TextSecondaryLight,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { refreshStatuses() }) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Segarkan Status")
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
                    items(statusList, key = { it.file.absolutePath }) { item ->
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
}

package com.notirecover.app.ui.screen

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.notirecover.app.ui.theme.PrimaryBlue
import com.notirecover.app.ui.theme.TextSecondaryLight
import com.notirecover.app.util.PermissionHelper
import com.notirecover.app.util.StatusMediaItem
import com.notirecover.app.util.StatusSaverHelper
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusSaverScreen() {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: WA Cache, 1: IG Story Ghost, 2: Downloaded
    var statusList by remember { mutableStateOf<List<StatusMediaItem>>(emptyList()) }
    var downloadedList by remember { mutableStateOf<List<StatusMediaItem>>(emptyList()) }
    var selectedStatusForPreview by remember { mutableStateOf<StatusMediaItem?>(null) }
    
    // Status Deletion & Selection States
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedItems by remember { mutableStateOf<Set<StatusMediaItem>>(emptySet()) }
    var statusToDelete by remember { mutableStateOf<StatusMediaItem?>(null) }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    var hasAllFilesPermission by remember { mutableStateOf<Boolean>(PermissionHelper.hasAllFilesAccess()) }
    var hasFolderConnected by remember {
        mutableStateOf<Boolean>(PermissionHelper.hasAllFilesAccess() || StatusSaverHelper.getSavedTreeUri(context) != null)
    }

    val currentList = if (selectedTab == 0) statusList else if (selectedTab == 2) downloadedList else emptyList()

    fun refreshStatuses() {
        hasAllFilesPermission = PermissionHelper.hasAllFilesAccess()
        statusList = StatusSaverHelper.getAllStatuses(context)
        downloadedList = StatusSaverHelper.getDownloadedStatuses(context)
        hasFolderConnected = hasAllFilesPermission || StatusSaverHelper.getSavedTreeUri(context) != null || statusList.isNotEmpty()
        // Bersihkan item terpilih yang sudah tidak ada
        selectedItems = selectedItems.filter { item ->
            currentList.any { it.name == item.name }
        }.toSet()
        if (selectedItems.isEmpty() && isSelectionMode) {
            isSelectionMode = false
        }
    }

    // Tangani Tombol Back saat mode seleksi aktif
    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedItems = emptySet()
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

    var showPaywallDialog by remember { mutableStateOf(false) }
    val prefs = remember { com.notirecover.app.data.preference.AppPreferences(context) }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                // TopAppBar Khusus Mode Seleksi (Bulk Action)
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
                            selectedItems = if (selectedItems.size == currentList.size) {
                                emptySet()
                            } else {
                                currentList.toSet()
                            }
                        }) {
                            Icon(
                                imageVector = if (selectedItems.size == currentList.size && currentList.isNotEmpty())
                                    Icons.Default.Deselect
                                else
                                    Icons.Default.SelectAll,
                                contentDescription = "Pilih Semua"
                            )
                        }

                        // Tombol Hapus Massal (Bulk Delete)
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Status Saver",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                if (prefs.isProUser) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFFEF3C7)
                                    ) {
                                        Text(
                                            text = "👑 PRO",
                                            color = Color(0xFFB45309),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = when (selectedTab) {
                                    0 -> "${statusList.size} Status Terdeteksi (Incognito)"
                                    1 -> "Lihat & Unduh Story Anonim (Ghost)"
                                    else -> "${downloadedList.size} Status Tersimpan"
                                },
                                fontSize = 12.sp,
                                color = if (selectedTab == 1) Color(0xFF7C3AED) else TextSecondaryLight,
                                fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    },
                    actions = {
                        // 1-Click Save All Status (PRO Feature)
                        if (selectedTab == 0 && statusList.isNotEmpty()) {
                            IconButton(onClick = {
                                if (prefs.isProUser) {
                                    var count = 0
                                    statusList.forEach { item ->
                                        if (StatusSaverHelper.saveStatusToGallery(context, item)) {
                                            count++
                                        }
                                    }
                                    Toast.makeText(context, "$count status berhasil disimpan massal ke Galeri! 🎉", Toast.LENGTH_SHORT).show()
                                    refreshStatuses()
                                } else {
                                    showPaywallDialog = true
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.DownloadForOffline,
                                    contentDescription = "Simpan Semua Status",
                                    tint = Color(0xFFD97706)
                                )
                            }
                        }

                        if (currentList.isNotEmpty()) {
                            // Tombol Masuk Mode Pilih Banyak
                            IconButton(onClick = { isSelectionMode = true }) {
                                Icon(Icons.Default.Checklist, contentDescription = "Pilih Banyak")
                            }

                            // Tombol Hapus Semua di Tab Ini
                            if (selectedTab == 2) {
                                IconButton(onClick = { showDeleteAllDialog = true }) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = "Hapus Semua", tint = Color(0xFFDC2626))
                                }
                            }
                        }

                        if (selectedTab != 1) {
                            IconButton(onClick = { refreshStatuses() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Segarkan Status")
                            }
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
            // Tab Switcher: Status WA vs IG Story 👻 vs Status Tersimpan (Download)
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        isSelectionMode = false
                        selectedItems = emptySet()
                        refreshStatuses()
                    },
                    text = { Text("Status WA", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        isSelectionMode = false
                        selectedItems = emptySet()
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("IG Story", fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("👻", fontSize = 12.sp)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        isSelectionMode = false
                        selectedItems = emptySet()
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
                    // Banner Izin Akses Status (Jika Belum Terhubung)
                    if (!hasFolderConnected && !hasAllFilesPermission) {
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
                                        text = "Akses Penyimpanan Status WA",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF92400E)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Pilih salah satu metode di bawah agar ChatRestore dapat memindai folder status WhatsApp Anda secara otomatis:",
                                    fontSize = 12.sp,
                                    color = Color(0xFFB45309),
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { PermissionHelper.openAllFilesAccessSettings(context) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Security, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("⚡ Izinkan Akses File (Rekomendasi)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedButton(
                                    onClick = { folderPickerLauncher.launch(null) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Folder, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("📁 Atau Pilih Folder WhatsApp Manual", fontSize = 12.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (statusList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFEFF6FF),
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.VisibilityOff,
                                            contentDescription = null,
                                            tint = Color(0xFF2563EB),
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Belum Ada Status WhatsApp Terdeteksi",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "📌 Cara Menampilkan Status:\n1. Buka WhatsApp biasa dan tonton status teman selama 1–2 detik.\n2. Kembali ke ChatRestore lalu klik tombol 'Segarkan' di bawah.",
                                    fontSize = 12.sp,
                                    color = TextSecondaryLight,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                                    lineHeight = 18.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedButton(onClick = {
                                        if (!hasAllFilesPermission) {
                                            PermissionHelper.openAllFilesAccessSettings(context)
                                        } else {
                                            folderPickerLauncher.launch(null)
                                        }
                                    }) {
                                        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Izin / Folder")
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
                                val isSelected = selectedItems.contains(item)
                                StatusGridCard(
                                    statusItem = item,
                                    isDownloaded = false,
                                    isSelectionMode = isSelectionMode,
                                    isSelected = isSelected,
                                    onClick = {
                                        if (isSelectionMode) {
                                            selectedItems = if (isSelected) selectedItems - item else selectedItems + item
                                        } else {
                                            selectedStatusForPreview = item
                                        }
                                    },
                                    onLongClick = {
                                        if (!isSelectionMode) {
                                            isSelectionMode = true
                                            selectedItems = setOf(item)
                                        }
                                    },
                                    onSaveClick = {
                                        val customName = StatusSaverHelper.getCustomStatusName(context, item.name)
                                        val success = StatusSaverHelper.saveStatusToGallery(context, item, customName)
                                        if (success) {
                                            refreshStatuses()
                                            val msg = if (!customName.isNullOrBlank()) {
                                                "Berhasil disimpan sebagai Status $customName di Galeri! 🎉"
                                            } else {
                                                "Berhasil disimpan ke Galeri HP! 🎉"
                                            }
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Gagal menyimpan file", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onDeleteClick = { statusToDelete = item }
                                )
                            }
                        }
                    }
                }
            } else if (selectedTab == 1) {
                // ==========================================
                // TAB 2: INSTAGRAM GHOST STORY
                // ==========================================
                InstagramGhostStoryView()
            } else {
                // ==========================================
                // TAB 3: STATUS TERSIMPAN (DOWNLOADED)
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
                                val isSelected = selectedItems.contains(item)
                                StatusGridCard(
                                    statusItem = item,
                                    isDownloaded = true,
                                    isSelectionMode = isSelectionMode,
                                    isSelected = isSelected,
                                    onClick = {
                                        if (isSelectionMode) {
                                            selectedItems = if (isSelected) selectedItems - item else selectedItems + item
                                        } else {
                                            selectedStatusForPreview = item
                                        }
                                    },
                                    onLongClick = {
                                        if (!isSelectionMode) {
                                            isSelectionMode = true
                                            selectedItems = setOf(item)
                                        }
                                    },
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
                onSave = { customName ->
                    val success = StatusSaverHelper.saveStatusToGallery(context, item, customName)
                    if (success) {
                        refreshStatuses()
                        val msg = if (!customName.isNullOrBlank()) {
                            "Berhasil disimpan sebagai Status $customName di Galeri! 🎉"
                        } else {
                            "Berhasil disimpan ke Galeri HP! 🎉"
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Gagal menyimpan file", Toast.LENGTH_SHORT).show()
                    }
                },
                onDelete = {
                    statusToDelete = item
                }
            )
        }

        // Dialog Konfirmasi Hapus Satuan (Single Delete)
        statusToDelete?.let { item ->
            AlertDialog(
                onDismissRequest = { statusToDelete = null },
                title = { Text("Hapus Status Ini?", fontWeight = FontWeight.Bold) },
                text = { Text("File '${item.name}' akan dihapus permanen dari penyimpanan HP Anda.") },
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

        // Dialog Konfirmasi Hapus Terpilih (Bulk Delete)
        if (showBulkDeleteDialog && selectedItems.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { showBulkDeleteDialog = false },
                title = { Text("Hapus ${selectedItems.size} Status?", fontWeight = FontWeight.Bold) },
                text = { Text("${selectedItems.size} file yang dipilih akan dihapus permanen dari memori HP.") },
                confirmButton = {
                    Button(
                        onClick = {
                            var successCount = 0
                            selectedItems.forEach { item ->
                                if (StatusSaverHelper.deleteStatus(context, item)) {
                                    successCount++
                                }
                            }
                            Toast.makeText(context, "$successCount file status berhasil dihapus! 🗑️", Toast.LENGTH_SHORT).show()
                            showBulkDeleteDialog = false
                            isSelectionMode = false
                            selectedItems = emptySet()
                            refreshStatuses()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) {
                        Text("Hapus Semua (${selectedItems.size})", color = Color.White)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showBulkDeleteDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }

        // Dialog Konfirmasi Hapus Semua Status Tersimpan
        if (showDeleteAllDialog && downloadedList.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { showDeleteAllDialog = false },
                title = { Text("Hapus Semua Koleksi Tersimpan?", fontWeight = FontWeight.Bold) },
                text = { Text("Seluruh (${downloadedList.size}) foto & video status yang tersimpan di folder ChatRestore akan dihapus.") },
                confirmButton = {
                    Button(
                        onClick = {
                            var count = 0
                            downloadedList.forEach { item ->
                                if (StatusSaverHelper.deleteStatus(context, item)) {
                                    count++
                                }
                            }
                            Toast.makeText(context, "$count status berhasil dihapus total!", Toast.LENGTH_SHORT).show()
                            showDeleteAllDialog = false
                            refreshStatuses()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) {
                        Text("Hapus Total", color = Color.White)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showDeleteAllDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }

        // Dialog Paywall PRO
        if (showPaywallDialog) {
            com.notirecover.app.ui.dialog.ProPaywallDialog(
                onDismiss = { showPaywallDialog = false },
                onSuccessPurchase = { refreshStatuses() }
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InstagramGhostStoryView() {
    val context = LocalContext.current
    var usernameInput by remember { mutableStateOf("") }
    var currentUrl by remember { mutableStateOf("https://storiesig.info/en/") }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var progress by remember { mutableIntStateOf(0) }

    fun searchUser(name: String) {
        val clean = name.trim().removePrefix("@").trim()
        if (clean.isNotBlank()) {
            val target = "https://storiesig.info/en/profile/$clean"
            currentUrl = target
            webViewInstance?.loadUrl(target)
        } else {
            Toast.makeText(context, "Ketik username akun Instagram publik terlebih dahulu", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Banner Ghost Story
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFF3E8FF),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE9D5FF)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("👻", fontSize = 28.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Instagram Ghost Story Viewer",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF6B21A8)
                    )
                    Text(
                        text = "Tonton & simpan Story Instagram tanpa ketahuan (100% anonim, tanpa perlu login). Nama Anda tidak akan muncul di daftar penonton!",
                        fontSize = 11.sp,
                        color = Color(0xFF7E22CE),
                        lineHeight = 15.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Input Username & Tombol Buka
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = usernameInput,
                onValueChange = { usernameInput = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Username akun publik (contoh: cristiano)", fontSize = 12.sp) },
                singleLine = true,
                leadingIcon = {
                    Text("@", fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED), fontSize = 16.sp)
                },
                trailingIcon = {
                    if (usernameInput.isNotBlank()) {
                        IconButton(onClick = { usernameInput = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Hapus", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSearch = { searchUser(usernameInput) }
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { searchUser(usernameInput) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = "Cari", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Buka", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Chip Rekomendasi / Contoh Cepat
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Contoh:", fontSize = 11.sp, color = TextSecondaryLight)
            listOf("cristiano", "selenagomez", "natgeo").forEach { sample ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.clickable {
                        usernameInput = sample
                        searchUser(sample)
                    }
                ) {
                    Text(
                        text = "@$sample",
                        fontSize = 11.sp,
                        color = Color(0xFF334155),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Banner Petunjuk Akun Privat -> Tab Web
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFF8FAFC),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Untuk akun privat yang Anda ikuti, gunakan tab 'Instagram 👻' di menu Web.",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Loading Progress
        AnimatedVisibility(visible = progress in 1..99) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = Color(0xFF7C3AED),
                trackColor = Color(0xFFF3E8FF)
            )
        }

        // WebView Anonymous Story Viewer
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp)),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        builtInZoomControls = true
                        displayZoomControls = false
                        userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                    }

                    setDownloadListener { url, _, _, _, _ ->
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Membuka tautan download...", Toast.LENGTH_SHORT).show()
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            super.onProgressChanged(view, newProgress)
                            progress = newProgress
                        }
                    }

                    loadUrl(currentUrl)
                    webViewInstance = this
                }
            },
            update = {
                // Keep instance updated
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StatusGridCard(
    statusItem: StatusMediaItem,
    isDownloaded: Boolean = false,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
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
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 2 // Downsample untuk grid thumbnail (menghemat CPU & RAM)
                }
                inputStream?.use { BitmapFactory.decodeStream(it, null, options) }
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        shadowElevation = if (isSelected) 4.dp else 2.dp,
        color = MaterialTheme.colorScheme.surface,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.5.dp, PrimaryBlue) else null,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
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

            // Badge Sumber Akun & Ukuran di Pojok Kiri Atas
            Surface(
                shape = RoundedCornerShape(topStart = 12.dp, bottomEnd = 8.dp),
                color = if (statusItem.sourceApp.contains("Business")) Color(0xFF128C7E) else Color(0xFF25D366),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (statusItem.sourceApp.contains("Business")) "WA Biz" else "WA",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                    if (statusItem.formattedSize.isNotBlank()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "• ${statusItem.formattedSize}",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 9.sp
                        )
                    }
                }
            }

            // Badge Nama Teman (Jika sudah dinamai) di Pojok Kiri Bawah
            val customFriendName = remember(statusItem.name) {
                StatusSaverHelper.getCustomStatusName(context, statusItem.name)
            }
            if (!customFriendName.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(topEnd = 8.dp, bottomStart = 12.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.BottomStart)
                ) {
                    Text(
                        text = "👤 $customFriendName",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            // Mode Seleksi: Tampilkan Checklist
            if (isSelectionMode) {
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) PrimaryBlue else Color.Black.copy(alpha = 0.4f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(26.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
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
            } else {
                // Mode Normal: Tombol Download atau Hapus
                if (!isDownloaded) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus dari Cache",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = onSaveClick,
                            modifier = Modifier
                                .size(32.dp)
                                .background(PrimaryBlue, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Simpan",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
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
}

@Composable
fun StatusPreviewDialog(
    statusItem: StatusMediaItem,
    isDownloaded: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (customName: String?) -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val context = LocalContext.current
    var friendName by remember(statusItem.name) {
        mutableStateOf(StatusSaverHelper.getCustomStatusName(context, statusItem.name) ?: "")
    }
    var isEditingName by remember { mutableStateOf(false) }
    var tempName by remember(friendName) { mutableStateOf(friendName) }

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
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header Identitas Pengunggah / Akun
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (statusItem.sourceApp.contains("Business")) Color(0xFF128C7E) else Color(0xFF25D366),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (statusItem.isVideo) Icons.Default.Videocam else Icons.Default.Image,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            if (isEditingName) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = tempName,
                                        onValueChange = { tempName = it },
                                        placeholder = { Text("Nama teman...", fontSize = 12.sp) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = {
                                            StatusSaverHelper.saveCustomStatusName(context, statusItem.name, tempName)
                                            friendName = tempName.trim()
                                            isEditingName = false
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = "Simpan Nama", tint = Color(0xFF16A34A))
                                    }
                                    IconButton(
                                        onClick = { isEditingName = false },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Batal", tint = Color.Gray)
                                    }
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            tempName = friendName
                                            isEditingName = true
                                        }
                                ) {
                                    Text(
                                        text = if (friendName.isNotBlank()) "Status $friendName" else (if (statusItem.isVideo) "Video Status" else "Foto Status"),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Beri Nama Teman",
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFDCFCE7)
                                    ) {
                                        Text(
                                            text = statusItem.sourceApp,
                                            color = Color(0xFF166534),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = statusItem.formattedDate,
                                        fontSize = 11.sp,
                                        color = TextSecondaryLight
                                    )
                                }
                            }
                        }
                    }

                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus Status",
                            tint = Color(0xFFDC2626)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Preview Media
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Preview Status",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                } else if (statusItem.isVideo) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color.Black, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(52.dp))
                            Text(
                                text = "Video Status WhatsApp",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Detail Informasi File & Waktu
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("🕒 Waktu Dilihat", fontSize = 11.sp, color = TextSecondaryLight)
                            Text(statusItem.formattedDate, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("📦 Ukuran & Tipe", fontSize = 11.sp, color = TextSecondaryLight)
                            Text(
                                text = "${statusItem.formattedSize} • ${if (statusItem.isVideo) "Video MP4" else "Foto HD"}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bar Cepat Beri Nama Teman
                if (!isEditingName) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                tempName = friendName
                                isEditingName = true
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (friendName.isNotBlank()) "Nama Teman: $friendName (Klik untuk ubah)" else "✏️ Beri Nama Teman untuk Status Ini",
                                fontSize = 11.sp,
                                color = if (friendName.isNotBlank()) PrimaryBlue else TextSecondaryLight,
                                fontWeight = if (friendName.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tombol Aksi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Tombol Bagikan / Repost
                    OutlinedButton(
                        onClick = {
                            try {
                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = if (statusItem.isVideo) "video/*" else "image/*"
                                    putExtra(android.content.Intent.EXTRA_STREAM, statusItem.uri)
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(android.content.Intent.createChooser(shareIntent, "Bagikan Status"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Gagal membagikan status", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bagikan", fontSize = 12.sp)
                    }

                    if (!isDownloaded) {
                        Button(
                            onClick = {
                                onSave(friendName.ifBlank { null })
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Simpan", fontSize = 12.sp)
                        }
                    } else {
                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Tutup", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

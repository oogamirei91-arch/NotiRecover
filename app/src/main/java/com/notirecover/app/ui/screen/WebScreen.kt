package com.notirecover.app.ui.screen

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.notirecover.app.util.WebAppInterface

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebScreen() {
    val context = LocalContext.current
    var selectedWeb by remember { mutableStateOf("WHATSAPP") }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var progress by remember { mutableIntStateOf(0) }
    var isDesktopMode by remember { mutableStateOf(true) }

    val desktopUA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    val mobileUA = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    fun loadCurrentUrl() {
        val targetUrl = if (selectedWeb == "WHATSAPP") "https://web.whatsapp.com" else "https://web.telegram.org/k/"
        webViewInstance?.settings?.userAgentString = if (isDesktopMode) desktopUA else mobileUA
        webViewInstance?.loadUrl(targetUrl)
    }

    val downloadStatusJs = """
        (function() {
            try {
                // 1. Cari video status aktif
                const videos = Array.from(document.querySelectorAll('video'));
                for (let vid of videos) {
                    if (vid.src && vid.offsetWidth > 100 && vid.offsetHeight > 100) {
                        fetch(vid.src)
                            .then(r => r.blob())
                            .then(blob => {
                                const reader = new FileReader();
                                reader.onloadend = function() {
                                    window.AndroidDownloader.onMediaDownloaded(reader.result, blob.type || 'video/mp4', 'wa_status.mp4');
                                };
                                reader.readAsDataURL(blob);
                            })
                            .catch(e => {
                                console.error('Fetch video error', e);
                            });
                        return;
                    }
                }

                // 2. Cari gambar status aktif berukuran besar
                const imgs = Array.from(document.querySelectorAll('img')).filter(img => {
                    return img.src && (img.src.startsWith('blob:') || img.src.startsWith('http') || img.src.startsWith('data:')) &&
                           img.offsetWidth > 180 && img.offsetHeight > 180;
                });

                imgs.sort((a, b) => (b.offsetWidth * b.offsetHeight) - (a.offsetWidth * a.offsetHeight));

                if (imgs.length > 0) {
                    const bestImg = imgs[0];
                    if (bestImg.src.startsWith('data:')) {
                        window.AndroidDownloader.onMediaDownloaded(bestImg.src, 'image/jpeg', 'wa_status.jpg');
                        return;
                    }
                    fetch(bestImg.src)
                        .then(r => r.blob())
                        .then(blob => {
                            const reader = new FileReader();
                            reader.onloadend = function() {
                                window.AndroidDownloader.onMediaDownloaded(reader.result, blob.type || 'image/jpeg', 'wa_status.jpg');
                            };
                            reader.readAsDataURL(blob);
                        })
                        .catch(e => {
                            try {
                                const canvas = document.createElement('canvas');
                                canvas.width = bestImg.naturalWidth || bestImg.offsetWidth;
                                canvas.height = bestImg.naturalHeight || bestImg.offsetHeight;
                                const ctx = canvas.getContext('2d');
                                ctx.drawImage(bestImg, 0, 0);
                                const dataUrl = canvas.toDataURL('image/jpeg', 0.95);
                                window.AndroidDownloader.onMediaDownloaded(dataUrl, 'image/jpeg', 'wa_status.jpg');
                            } catch (err) {
                                alert('Gagal mendownload gambar: ' + err.message);
                            }
                        });
                    return;
                }

                alert('Tidak ada status foto/video yang sedang terbuka di layar. Buka status teman terlebih dahulu!');
            } catch (err) {
                alert('Terjadi kesalahan: ' + err.message);
            }
        })();
    """.trimIndent()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Web Medsos Dual Login",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = if (isDesktopMode) "Mode Tampilan Desktop (QR Code Aktif)" else "Mode Tampilan Mobile",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                },
                actions = {
                    // Tombol Ganti Mode Desktop / Mobile
                    IconButton(onClick = {
                        isDesktopMode = !isDesktopMode
                        loadCurrentUrl()
                    }) {
                        Icon(
                            imageVector = if (isDesktopMode) Icons.Default.DesktopWindows else Icons.Default.PhoneAndroid,
                            contentDescription = "Ganti Mode Tampilan",
                            tint = if (isDesktopMode) Color(0xFF2563EB) else Color(0xFF10B981)
                        )
                    }
                    IconButton(onClick = { webViewInstance?.reload() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Muat Ulang")
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedWeb == "WHATSAPP") {
                ExtendedFloatingActionButton(
                    onClick = {
                        webViewInstance?.evaluateJavascript(downloadStatusJs, null)
                    },
                    icon = { Icon(Icons.Default.FileDownload, contentDescription = "Unduh Status") },
                    text = { Text("Unduh Status di Layar", fontWeight = FontWeight.Bold) },
                    containerColor = Color(0xFF10B981),
                    contentColor = Color.White
                )
            }
        },
        bottomBar = {
            // Bilah Navigasi Kontrol WebView Cepat
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (webViewInstance?.canGoBack() == true) webViewInstance?.goBack() },
                        enabled = webViewInstance?.canGoBack() == true
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }

                    IconButton(
                        onClick = { if (webViewInstance?.canGoForward() == true) webViewInstance?.goForward() },
                        enabled = webViewInstance?.canGoForward() == true
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Maju")
                    }

                    IconButton(onClick = { webViewInstance?.zoomOut() }) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "Perkecil")
                    }

                    IconButton(onClick = { webViewInstance?.zoomIn() }) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Perbesar")
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Switcher WhatsApp Web / Telegram Web
            TabRow(
                selectedTabIndex = if (selectedWeb == "WHATSAPP") 0 else 1,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedWeb == "WHATSAPP",
                    onClick = {
                        if (selectedWeb != "WHATSAPP") {
                            selectedWeb = "WHATSAPP"
                            loadCurrentUrl()
                        }
                    },
                    text = { Text("WhatsApp Web", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedWeb == "TELEGRAM",
                    onClick = {
                        if (selectedWeb != "TELEGRAM") {
                            selectedWeb = "TELEGRAM"
                            loadCurrentUrl()
                        }
                    },
                    text = { Text("Telegram Web", fontWeight = FontWeight.SemiBold) }
                )
            }

            // Indikator Loading Bar Halus
            AnimatedVisibility(visible = progress in 1..99) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF2563EB)
                )
            }

            // WebView Responsif
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                            cacheMode = WebSettings.LOAD_DEFAULT
                            userAgentString = desktopUA
                            textZoom = 100
                        }

                        // Hubungkan JavaScript Interface Downloader
                        addJavascriptInterface(WebAppInterface(ctx), "AndroidDownloader")

                        setInitialScale(100)

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                            }

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

                        loadUrl("https://web.whatsapp.com")
                        webViewInstance = this
                    }
                }
            )
        }
    }
}

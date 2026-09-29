package com.notirecover.app.ui.screen

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebScreen() {
    val context = LocalContext.current
    var selectedWeb by remember { mutableStateOf("WHATSAPP") } // WHATSAPP, INSTAGRAM, TELEGRAM
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var progress by remember { mutableIntStateOf(0) }
    var isDesktopMode by remember { mutableStateOf(true) }
    var isGhostModeEnabled by remember { mutableStateOf(true) }

    val desktopUA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    val mobileUA = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    fun loadCurrentUrl() {
        val targetUrl = when (selectedWeb) {
            "WHATSAPP" -> "https://web.whatsapp.com"
            "INSTAGRAM" -> "https://www.instagram.com"
            else -> "https://web.telegram.org/k/"
        }
        val targetUA = if (selectedWeb == "INSTAGRAM") mobileUA else if (isDesktopMode) desktopUA else mobileUA
        webViewInstance?.settings?.userAgentString = targetUA
        webViewInstance?.loadUrl(targetUrl)
    }

    // Jeda eksekusi JavaScript & timer WebView saat pengguna berpindah tab untuk hemat baterai
    DisposableEffect(Unit) {
        webViewInstance?.onResume()
        webViewInstance?.resumeTimers()
        onDispose {
            webViewInstance?.onPause()
            webViewInstance?.pauseTimers()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = when (selectedWeb) {
                                "WHATSAPP" -> "WhatsApp Web"
                                "INSTAGRAM" -> "Instagram Web"
                                else -> "Telegram Web"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = if (selectedWeb == "INSTAGRAM") {
                                if (isGhostModeEnabled) "👻 Ghost Mode Aktif (Seen Story Diblokir)" else "Ghost Mode Nonaktif"
                            } else {
                                if (isDesktopMode) "Mode Tampilan Desktop (QR Code Aktif)" else "Mode Tampilan Mobile"
                            },
                            fontSize = 11.sp,
                            color = if (selectedWeb == "INSTAGRAM" && isGhostModeEnabled) Color(0xFF7C3AED) else Color(0xFF64748B),
                            fontWeight = if (selectedWeb == "INSTAGRAM" && isGhostModeEnabled) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                },
                actions = {
                    // Tombol Ghost Mode (Khusus Instagram Web)
                    if (selectedWeb == "INSTAGRAM") {
                        FilterChip(
                            selected = isGhostModeEnabled,
                            onClick = {
                                isGhostModeEnabled = !isGhostModeEnabled
                                Toast.makeText(
                                    context,
                                    if (isGhostModeEnabled) "👻 Ghost Mode AKTIF: Story Seen Diblokir!" else "Ghost Mode NONAKTIF: Seen Normal",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isGhostModeEnabled) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = if (isGhostModeEnabled) "👻 Ghost: ON" else "Ghost: OFF",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF7C3AED),
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            )
                        )
                    } else {
                        // Tombol Ganti Mode Desktop / Mobile untuk WhatsApp & Telegram
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
                    }

                    IconButton(onClick = { webViewInstance?.reload() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Muat Ulang")
                    }
                }
            )
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
            // Tab Switcher WhatsApp Web / Instagram Web / Telegram Web
            TabRow(
                selectedTabIndex = when (selectedWeb) {
                    "WHATSAPP" -> 0
                    "INSTAGRAM" -> 1
                    else -> 2
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedWeb == "WHATSAPP",
                    onClick = {
                        if (selectedWeb != "WHATSAPP") {
                            selectedWeb = "WHATSAPP"
                            isDesktopMode = true
                            loadCurrentUrl()
                        }
                    },
                    text = { Text("WhatsApp", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedWeb == "INSTAGRAM",
                    onClick = {
                        if (selectedWeb != "INSTAGRAM") {
                            selectedWeb = "INSTAGRAM"
                            isDesktopMode = false
                            loadCurrentUrl()
                        }
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Instagram", fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("👻", fontSize = 12.sp)
                        }
                    }
                )
                Tab(
                    selected = selectedWeb == "TELEGRAM",
                    onClick = {
                        if (selectedWeb != "TELEGRAM") {
                            selectedWeb = "TELEGRAM"
                            isDesktopMode = true
                            loadCurrentUrl()
                        }
                    },
                    text = { Text("Telegram", fontWeight = FontWeight.SemiBold) }
                )
            }

            // Banner Indikator Ghost Mode saat berada di Instagram
            AnimatedVisibility(visible = selectedWeb == "INSTAGRAM" && isGhostModeEnabled) {
                Surface(
                    color = Color(0xFFF3E8FF),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("👻", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ghost Mode Aktif: Anda bisa menonton story tanpa nama Anda muncul di daftar penonton!",
                            fontSize = 11.sp,
                            color = Color(0xFF6B21A8),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Indikator Loading Bar Halus
            AnimatedVisibility(visible = progress in 1..99) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF2563EB)
                )
            }

            // WebView Responsif dengan Interseptor Ghost Mode
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

                        setInitialScale(100)

                        webViewClient = object : WebViewClient() {
                            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                                if (isGhostModeEnabled && request != null) {
                                    val url = request.url.toString().lowercase()
                                    // Intersep dan blokir semua request seen tracking di Instagram
                                    if (url.contains("/api/v1/stories/reel/seen") ||
                                        url.contains("/api/v1/media/seen") ||
                                        url.contains("/stories/reel/seen") ||
                                        url.contains("seenmarker") ||
                                        (url.contains("/graphql/query") && url.contains("story_view")) ||
                                        (request.method.equals("POST", ignoreCase = true) && url.contains("seen"))
                                    ) {
                                        android.util.Log.i("GhostMode", "👻 Berhasil memblokir tracking seen story Instagram: $url")
                                        return WebResourceResponse(
                                            "application/json",
                                            "UTF-8",
                                            200,
                                            "OK",
                                            mapOf("Access-Control-Allow-Origin" to "*"),
                                            java.io.ByteArrayInputStream("""{"status":"ok"}""".toByteArray())
                                        )
                                    }
                                }
                                return super.shouldInterceptRequest(view, request)
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                if (selectedWeb == "INSTAGRAM" && isGhostModeEnabled) {
                                    // Injeksi pengaman fetch & XHR seen blocker di level JavaScript
                                    val ghostJs = """
                                        (function() {
                                            if (window.__ghostModeInjected) return;
                                            window.__ghostModeInjected = true;
                                            const origFetch = window.fetch;
                                            window.fetch = async function(...args) {
                                                const u = (args[0] && args[0].url) ? args[0].url : String(args[0]);
                                                if (u.toLowerCase().includes('seen')) {
                                                    console.log('👻 Ghost Mode JS: fetch seen blocked', u);
                                                    return new Response(JSON.stringify({status: 'ok'}), { status: 200, headers: {'Content-Type':'application/json'} });
                                                }
                                                return origFetch.apply(this, args);
                                            };
                                            const origOpen = XMLHttpRequest.prototype.open;
                                            XMLHttpRequest.prototype.open = function(m, u) {
                                                if (u && u.toLowerCase().includes('seen')) {
                                                    console.log('👻 Ghost Mode JS: XHR seen blocked', u);
                                                    this.send = function() {
                                                        Object.defineProperty(this, 'readyState', { value: 4 });
                                                        Object.defineProperty(this, 'status', { value: 200 });
                                                        Object.defineProperty(this, 'responseText', { value: '{"status":"ok"}' });
                                                        if (this.onload) this.onload();
                                                    };
                                                }
                                                return origOpen.apply(this, arguments);
                                            };
                                        })();
                                    """.trimIndent()
                                    view?.evaluateJavascript(ghostJs, null)
                                }
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

package com.notirecover.app.ui.screen

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.JavascriptInterface
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

class GhostBridge(private val onBlocked: (String) -> Unit) {
    @JavascriptInterface
    fun onSeenBlocked(info: String) {
        onBlocked(info)
    }
}

private fun getInstagramGhostModeScript(enabled: Boolean): String {
    return """
        (function() {
            window.__ghostModeEnabled = $enabled;
            if (window.__ghostModeInstalled) {
                return;
            }
            window.__ghostModeInstalled = true;

            function notifyBlocked(type, detail) {
                try {
                    if (window.GhostBridge && window.GhostBridge.onSeenBlocked) {
                        window.GhostBridge.onSeenBlocked(type + ': ' + (detail || ''));
                    }
                } catch(e) {}
            }

            function extractText(data) {
                if (!data) return '';
                if (typeof data === 'string') return data;
                try {
                    if (data instanceof URLSearchParams) {
                        return data.toString();
                    }
                    if (data instanceof FormData) {
                        var res = '';
                        for (var pair of data.entries()) {
                            res += ' ' + pair[0] + '=' + pair[1];
                        }
                        return res;
                    }
                    if (typeof data === 'object') {
                        return JSON.stringify(data);
                    }
                } catch(e) {}
                return String(data);
            }

            function isSeenRequest(url, body) {
                if (window.__ghostModeEnabled === false) return false;

                var sUrl = (url ? (typeof url === 'string' ? url : (url.url || String(url))) : '').toLowerCase();
                var sBody = extractText(body).toLowerCase();

                if (sBody.indexOf('polarisstoriesv3seenmutation') !== -1 ||
                    sBody.indexOf('polarisstoriesseenmutation') !== -1 ||
                    sBody.indexOf('storiesseenmutation') !== -1 ||
                    sBody.indexOf('polarisstoryseenmutation') !== -1 ||
                    sBody.indexOf('polarisstoriesseen') !== -1 ||
                    sBody.indexOf('polarisstoriesv3seen') !== -1 ||
                    sBody.indexOf('storyseen') !== -1 ||
                    sBody.indexOf('story_seen') !== -1 ||
                    sBody.indexOf('stories_seen') !== -1 ||
                    sBody.indexOf('stories/reel/seen') !== -1 ||
                    sBody.indexOf('media/seen') !== -1 ||
                    sBody.indexOf('seenmarker') !== -1 ||
                    (sBody.indexOf('seen_at') !== -1 && sBody.indexOf('reel') !== -1) ||
                    (sBody.indexOf('max_seen_at') !== -1 && sBody.indexOf('reel') !== -1) ||
                    (sBody.indexOf('reel_media_id') !== -1 && sBody.indexOf('seen') !== -1)
                ) {
                    return true;
                }

                if (sUrl.indexOf('/stories/reel/seen') !== -1 ||
                    sUrl.indexOf('/media/seen') !== -1 ||
                    sUrl.indexOf('/stories/seen') !== -1 ||
                    sUrl.indexOf('seenmarker') !== -1 ||
                    sUrl.indexOf('polarisstoriesv3seen') !== -1 ||
                    sUrl.indexOf('polarisstoriesseen') !== -1 ||
                    sUrl.indexOf('storiesseenmutation') !== -1 ||
                    sUrl.indexOf('/api/v1/stories/reel/seen') !== -1 ||
                    sUrl.indexOf('/api/v1/media/seen') !== -1
                ) {
                    return true;
                }

                return false;
            }

            function createFakeSuccessResponse() {
                return new Response(JSON.stringify({
                    data: {
                        polaris_stories_v3_seen: { status: 'OK', __typename: 'PolarisStoriesV3SeenMutationPayload' },
                        polaris_stories_seen: { status: 'OK', __typename: 'PolarisStoriesSeenMutationPayload' },
                        story_seen: { status: 'OK' }
                    },
                    status: 'ok'
                }), {
                    status: 200,
                    statusText: 'OK',
                    headers: {
                        'Content-Type': 'application/json',
                        'Access-Control-Allow-Origin': '*'
                    }
                });
            }

            var realFetch = window.fetch;
            async function ghostFetch(resource, init) {
                var url = (resource && resource.url) ? resource.url : resource;
                var body = init ? init.body : (resource && resource.body);

                if (!body && resource && typeof resource.clone === 'function' && resource.method === 'POST') {
                    try {
                        var clone = resource.clone();
                        body = await clone.text();
                    } catch(e) {}
                }

                if (isSeenRequest(url, body)) {
                    notifyBlocked('fetch', url);
                    return Promise.resolve(createFakeSuccessResponse());
                }

                return realFetch.apply(this, arguments);
            }

            try {
                Object.defineProperty(window, 'fetch', {
                    configurable: true,
                    enumerable: true,
                    get: function() { return ghostFetch; },
                    set: function(fn) {
                        if (fn !== ghostFetch) {
                            realFetch = fn;
                        }
                    }
                });
            } catch(e) {
                window.fetch = ghostFetch;
            }

            var realOpen = XMLHttpRequest.prototype.open;
            var realSend = XMLHttpRequest.prototype.send;

            XMLHttpRequest.prototype.open = function(method, url) {
                this.__ghostUrl = url;
                this.__ghostMethod = method;
                return realOpen.apply(this, arguments);
            };

            XMLHttpRequest.prototype.send = function(body) {
                if (isSeenRequest(this.__ghostUrl, body)) {
                    notifyBlocked('XHR', this.__ghostUrl);
                    var self = this;
                    setTimeout(function() {
                        try {
                            Object.defineProperty(self, 'readyState', { value: 4, writable: true });
                            Object.defineProperty(self, 'status', { value: 200, writable: true });
                            Object.defineProperty(self, 'statusText', { value: 'OK', writable: true });
                            Object.defineProperty(self, 'responseText', {
                                value: '{"data":{"polaris_stories_seen":{"status":"OK"},"polaris_stories_v3_seen":{"status":"OK"}},"status":"ok"}',
                                writable: true
                            });
                        } catch(e) {}
                        if (typeof self.onreadystatechange === 'function') self.onreadystatechange();
                        if (typeof self.onload === 'function') self.onload();
                    }, 10);
                    return;
                }
                return realSend.apply(this, arguments);
            };

            if (navigator && navigator.sendBeacon) {
                var realBeacon = navigator.sendBeacon;
                navigator.sendBeacon = function(url, data) {
                    if (isSeenRequest(url, data)) {
                        notifyBlocked('sendBeacon', url);
                        return true;
                    }
                    return realBeacon.apply(this, arguments);
                };
            }
        })();
    """.trimIndent()
}

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
    var lastToastTime by remember { mutableLongStateOf(0L) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    val desktopUA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    val mobileUA = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    fun loadCurrentUrl() {
        val targetUrl = when (selectedWeb) {
            "WHATSAPP" -> "https://web.whatsapp.com"
            "INSTAGRAM" -> "https://www.instagram.com"
            else -> "https://web.telegram.org/k/"
        }
        val targetUA = if (isDesktopMode) desktopUA else mobileUA
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
                                webViewInstance?.evaluateJavascript("window.__ghostModeEnabled = $isGhostModeEnabled;", null)
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
                    }

                    // Tombol Ganti Mode Desktop / Mobile untuk semua tab
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

                        addJavascriptInterface(GhostBridge { info ->
                            android.util.Log.i("GhostMode", "👻 JS GhostBridge Blocked: $info")
                            val now = System.currentTimeMillis()
                            if (now - lastToastTime > 3000L) {
                                lastToastTime = now
                                mainHandler.post {
                                    Toast.makeText(context, "👻 Ghost Mode: Story seen berhasil diblokir! (Anonim)", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }, "GhostBridge")

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                if (selectedWeb == "INSTAGRAM") {
                                    view?.evaluateJavascript(getInstagramGhostModeScript(isGhostModeEnabled), null)
                                }
                            }

                            override fun onLoadResource(view: WebView?, url: String?) {
                                super.onLoadResource(view, url)
                                if (selectedWeb == "INSTAGRAM") {
                                    view?.evaluateJavascript(getInstagramGhostModeScript(isGhostModeEnabled), null)
                                }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                if (selectedWeb == "INSTAGRAM") {
                                    view?.evaluateJavascript(getInstagramGhostModeScript(isGhostModeEnabled), null)
                                }
                            }

                            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                                if (isGhostModeEnabled && request != null) {
                                    val url = request.url.toString().lowercase()
                                    // Intersep dan blokir semua request seen tracking di Instagram
                                    if (url.contains("/api/v1/stories/reel/seen") ||
                                        url.contains("/api/v1/media/seen") ||
                                        url.contains("/stories/reel/seen") ||
                                        url.contains("/stories/seen") ||
                                        url.contains("/media/seen") ||
                                        url.contains("seenmarker") ||
                                        url.contains("polarisstoriesseen") ||
                                        url.contains("polarisstoriesv3seen") ||
                                        url.contains("storiesseenmutation") ||
                                        url.contains("story_view") ||
                                        (url.contains("/graphql") && (url.contains("seen") || url.contains("story_view")))
                                    ) {
                                        android.util.Log.i("GhostMode", "👻 [BLOCKED by Native Interceptor] $url")
                                        return WebResourceResponse(
                                            "application/json",
                                            "UTF-8",
                                            200,
                                            "OK",
                                            mapOf(
                                                "Access-Control-Allow-Origin" to "*",
                                                "Access-Control-Allow-Methods" to "GET, POST, OPTIONS",
                                                "Access-Control-Allow-Headers" to "*"
                                            ),
                                            java.io.ByteArrayInputStream("""{"data":{"polaris_stories_v3_seen":{"status":"OK"},"polaris_stories_seen":{"status":"OK"}},"status":"ok"}""".toByteArray())
                                        )
                                    }
                                }
                                return super.shouldInterceptRequest(view, request)
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

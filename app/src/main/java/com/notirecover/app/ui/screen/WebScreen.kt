package com.notirecover.app.ui.screen

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebScreen() {
    var selectedWeb by remember { mutableStateOf("WHATSAPP") }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val url = remember(selectedWeb) {
        if (selectedWeb == "WHATSAPP") "https://web.whatsapp.com" else "https://web.telegram.org/a/"
    }

    // User Agent Desktop khusus agar WhatsApp Web menampilkan QR Code di Android
    val desktopUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Web Medsos Dual Login",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                actions = {
                    IconButton(onClick = { webViewInstance?.reload() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Muat Ulang")
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
            // Tab Switcher WhatsApp Web / Telegram Web
            TabRow(
                selectedTabIndex = if (selectedWeb == "WHATSAPP") 0 else 1,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedWeb == "WHATSAPP",
                    onClick = {
                        selectedWeb = "WHATSAPP"
                        webViewInstance?.loadUrl("https://web.whatsapp.com")
                    },
                    text = { Text("WhatsApp Web", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedWeb == "TELEGRAM",
                    onClick = {
                        selectedWeb = "TELEGRAM"
                        webViewInstance?.loadUrl("https://web.telegram.org/a/")
                    },
                    text = { Text("Telegram Web", fontWeight = FontWeight.SemiBold) }
                )
            }

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // WebView Container
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
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
                            userAgentString = desktopUserAgent // Paksa Desktop Mode agar WhatsApp Web tidak redirect
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                            }
                        }

                        webChromeClient = WebChromeClient()
                        loadUrl(url)
                        webViewInstance = this
                    }
                },
                update = { webView ->
                    // Diperbarui saat URL berubah
                }
            )
        }
    }
}

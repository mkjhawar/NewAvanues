/**
 * BrowserScreen.kt - Voice-controlled browser interface
 *
 * Integrates WebAvanue module with voice commands.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.ui.browser

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    onNavigateBack: () -> Unit
) {
    var currentUrl by remember { mutableStateOf("https://www.google.com") }
    var urlInput by remember { mutableStateOf(currentUrl) }
    var isLoading by remember { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("WebAvanue") },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.semantics { contentDescription = "Voice: click Close Browser" }
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { webView?.reload() },
                            modifier = Modifier.semantics { contentDescription = "Voice: click Reload Page" }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reload")
                        }
                        IconButton(
                            onClick = { /* TODO: Voice command */ },
                            modifier = Modifier.semantics { contentDescription = "Voice: click Voice Command" }
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice")
                        }
                    }
                )

                // URL Bar
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .semantics { contentDescription = "Voice: enter URL or Search Query" },
                    placeholder = { Text("Enter URL or search") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            currentUrl = normalizeUrl(urlInput)
                            webView?.loadUrl(currentUrl)
                        }
                    ),
                    leadingIcon = {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Search, contentDescription = null)
                        }
                    },
                    trailingIcon = {
                        if (urlInput.isNotEmpty()) {
                            IconButton(
                                onClick = { urlInput = "" },
                                modifier = Modifier.semantics { contentDescription = "Voice: click Clear URL" }
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    }
                )

                // Navigation Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(
                        onClick = { webView?.goBack() },
                        enabled = canGoBack,
                        modifier = Modifier.semantics { contentDescription = "Voice: click Go Back" }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }

                    IconButton(
                        onClick = { webView?.goForward() },
                        enabled = canGoForward,
                        modifier = Modifier.semantics { contentDescription = "Voice: click Go Forward" }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward")
                    }

                    IconButton(
                        onClick = {
                            currentUrl = "https://www.google.com"
                            webView?.loadUrl(currentUrl)
                        },
                        modifier = Modifier.semantics { contentDescription = "Voice: click Go Home" }
                    ) {
                        Icon(Icons.Default.Home, contentDescription = "Home")
                    }

                    IconButton(
                        onClick = { /* TODO: Bookmarks */ },
                        modifier = Modifier.semantics { contentDescription = "Voice: click Bookmarks" }
                    ) {
                        Icon(Icons.Default.BookmarkBorder, contentDescription = "Bookmarks")
                    }

                    IconButton(
                        onClick = { /* TODO: Tabs */ },
                        modifier = Modifier.semantics { contentDescription = "Voice: click Tabs" }
                    ) {
                        Icon(Icons.Default.Tab, contentDescription = "Tabs")
                    }
                }

                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    ) { padding ->
        BrowserWebView(
            url = currentUrl,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            onWebViewCreated = { webView = it },
            onUrlChanged = { url ->
                currentUrl = url
                urlInput = url
            },
            onLoadingChanged = { isLoading = it },
            onNavigationChanged = { back, forward ->
                canGoBack = back
                canGoForward = forward
            }
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserWebView(
    url: String,
    modifier: Modifier = Modifier,
    onWebViewCreated: (WebView) -> Unit,
    onUrlChanged: (String) -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    onNavigationChanged: (canGoBack: Boolean, canGoForward: Boolean) -> Unit
) {
    val context = LocalContext.current

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                settings.setSupportZoom(true)

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        onLoadingChanged(true)
                        url?.let { onUrlChanged(it) }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        onLoadingChanged(false)
                        view?.let {
                            onNavigationChanged(it.canGoBack(), it.canGoForward())
                        }
                    }
                }

                loadUrl(url)
                onWebViewCreated(this)
            }
        },
        update = { webView ->
            if (webView.url != url) {
                webView.loadUrl(url)
            }
        }
    )
}

private fun normalizeUrl(input: String): String {
    val trimmed = input.trim()

    // Already a valid URL
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        return trimmed
    }

    // Looks like a domain
    if (trimmed.contains(".") && !trimmed.contains(" ")) {
        return "https://$trimmed"
    }

    // Treat as search query
    return "https://www.google.com/search?q=${java.net.URLEncoder.encode(trimmed, "UTF-8")}"
}

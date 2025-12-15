package com.augmentalis.cockpit.mvp.content

import android.webkit.HttpAuthHandler
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.avanues.cockpit.core.window.WindowContent
import com.augmentalis.cockpit.mvp.content.webview.WebViewRenderer
import com.augmentalis.cockpit.mvp.content.loading.LoadingOverlay
import com.augmentalis.cockpit.mvp.content.loading.WebViewErrorPage

/**
 * WebView content renderer for WindowContent.WebContent
 *
 * Features:
 * - Full WebView with telemetry and JavaScript bridge
 * - Loading indicator with progress
 * - Error handling with retry capability
 * - Automatic URL reload on content update
 * - Scroll position persistence (Phase 3: FR-3.1)
 * - JavaScript bridge integration (window.cockpit)
 *
 * @param webContent WebView configuration and state
 * @param onScrollChanged Callback when scroll position changes (for state persistence)
 * @param windowId Window ID for telemetry (derived from content hash if not provided)
 * @param enableBridge Whether to enable JavaScript bridge (default: true)
 */
@Composable
fun WebViewContent(
    webContent: WindowContent.WebContent,
    onScrollChanged: ((scrollX: Int, scrollY: Int) -> Unit)? = null,
    windowId: String = webContent.url.hashCode().toString(),
    enableBridge: Boolean = true,
    onMinimize: (() -> Unit)? = null,
    onMaximize: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Track content state internally (initialize with loading=true)
    var currentState by remember {
        mutableStateOf(webContent.copy(isLoading = true, loadingProgress = 0))
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Render WebView with new infrastructure
        WebViewRenderer(
            windowId = windowId,
            content = currentState,
            onContentStateChanged = { newState ->
                // Check BEFORE updating currentState to avoid comparison bug
                if (newState.scrollX != currentState.scrollX || newState.scrollY != currentState.scrollY) {
                    onScrollChanged?.invoke(newState.scrollX, newState.scrollY)
                }
                currentState = newState  // Update AFTER comparison
            },
            enableBridge = enableBridge,
            onMinimize = onMinimize,
            onMaximize = onMaximize,
            onClose = onClose,
            modifier = Modifier.fillMaxSize()
        )

        // Show loading overlay when loading
        if (currentState.isLoading) {
            LoadingOverlay(
                message = "Loading ${currentState.pageTitle ?: "page"}...",
                progress = currentState.loadingProgress.takeIf { it > 0 },
                url = currentState.url,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Show error page if error occurred
        currentState.error?.let { error ->
            WebViewErrorPage(
                error = error,
                url = currentState.url,
                onRetry = {
                    // Reset error and reload
                    currentState = currentState.copy(
                        error = null,
                        isLoading = true,
                        loadingProgress = 0
                    )
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Legacy WebViewContent implementation (keeping for backwards compatibility)
 * This version uses plain WebView without telemetry/bridge integration
 *
 * DEPRECATED: Use WebViewContent with new infrastructure instead
 */
@Composable
fun LegacyWebViewContent(
    webContent: WindowContent.WebContent,
    onScrollChanged: ((scrollX: Int, scrollY: Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var webView: WebView? by remember { mutableStateOf(null) }

    // Phase 4: HTTP Basic Auth state (FR-4.3)
    var showAuthDialog by remember { mutableStateOf(false) }
    var authHandler by remember { mutableStateOf<HttpAuthHandler?>(null) }
    var authHost by remember { mutableStateOf("") }
    var authRealm by remember { mutableStateOf("") }

    // Track URL changes to reload WebView
    LaunchedEffect(webContent.url) {
        webView?.loadUrl(webContent.url)
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            isLoading = false
                            hasError = false

                            // Phase 3: Restore scroll position after page load (FR-3.1)
                            // Use postDelayed to ensure DOM is fully rendered before scrolling
                            view?.postDelayed({
                                if (webContent.scrollX != 0 || webContent.scrollY != 0) {
                                    view.scrollTo(webContent.scrollX, webContent.scrollY)
                                }
                            }, 300L)  // 300ms delay for DOM rendering
                        }

                        @Deprecated("Deprecated in Android API")
                        override fun onReceivedError(
                            view: WebView?,
                            errorCode: Int,
                            description: String?,
                            failingUrl: String?
                        ) {
                            isLoading = false
                            hasError = true
                        }

                        // Phase 4: HTTP Basic Auth handler (FR-4.3)
                        override fun onReceivedHttpAuthRequest(
                            view: WebView?,
                            handler: HttpAuthHandler?,
                            host: String?,
                            realm: String?
                        ) {
                            authHandler = handler
                            authHost = host ?: ""
                            authRealm = realm ?: ""
                            showAuthDialog = true
                        }
                    }

                    // Phase 4: WebChromeClient for dynamic title updates (FR-4.2)
                    webChromeClient = object : WebChromeClient() {
                        override fun onReceivedTitle(view: WebView?, title: String?) {
                            // Update page title dynamically
                            title?.let {
                                // Title is received but will be updated through ManagedWebView's content state change
                                // Trigger scroll update to notify parent of any changes
                                onScrollChanged?.invoke(view?.scrollX ?: 0, view?.scrollY ?: 0)
                            }
                        }

                        // Phase 4: New tab handling (FR-4.4)
                        override fun onCreateWindow(
                            view: WebView?,
                            isDialog: Boolean,
                            isUserGesture: Boolean,
                            resultMsg: android.os.Message?
                        ): Boolean {
                            // TODO: Create new window with target URL
                            // For now, return false to let default behavior handle it
                            return false
                        }
                    }

                    // Phase 3: Listen for scroll changes to save position (FR-3.1)
                    setOnScrollChangeListener { _, scrollX, scrollY, _, _ ->
                        onScrollChanged?.invoke(scrollX, scrollY)
                    }

                    settings.apply {
                        javaScriptEnabled = webContent.javaScriptEnabled
                        domStorageEnabled = webContent.domStorageEnabled
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        allowFileAccess = false
                        allowContentAccess = true

                        // Phase 4: Desktop mode user agent (FR-4.1)
                        userAgentString = when {
                            webContent.userAgent != null -> webContent.userAgent
                            webContent.isDesktopMode -> {
                                // Desktop Chrome user agent for better rendering on AR glasses
                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                            }
                            else -> {
                                // Use default mobile user agent
                                null
                            }
                        }

                        // Phase 4: Enable support for window.open() (FR-4.4)
                        javaScriptCanOpenWindowsAutomatically = true
                        setSupportMultipleWindows(true)
                    }

                    loadUrl(webContent.url)
                    webView = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Loading indicator
        if (isLoading && !hasError) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Error view with retry
        if (hasError) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Failed to load",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = webContent.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Button(
                        onClick = {
                            isLoading = true
                            hasError = false
                            webView?.reload()
                        }
                    ) {
                        Text("Retry")
                    }
                }
            }
        }

        // Phase 4: HTTP Basic Auth dialog (FR-4.3)
        if (showAuthDialog) {
            BasicAuthDialog(
                host = authHost,
                realm = authRealm,
                onDismiss = {
                    authHandler?.cancel()
                    showAuthDialog = false
                },
                onConfirm = { username, password ->
                    authHandler?.proceed(username, password)
                    showAuthDialog = false
                }
            )
        }
    }
}

/**
 * HTTP Basic Authentication dialog (Phase 4: FR-4.3)
 * Shows username/password fields for HTTP auth challenges
 */
@Composable
private fun BasicAuthDialog(
    host: String,
    realm: String,
    onDismiss: () -> Unit,
    onConfirm: (username: String, password: String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Authentication Required",
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    text = "The site $host requires authentication",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (realm.isNotEmpty()) {
                    Text(
                        text = "Realm: $realm",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(username, password) },
                        enabled = username.isNotEmpty() && password.isNotEmpty()
                    ) {
                        Text("Sign In")
                    }
                }
            }
        }
    }
}

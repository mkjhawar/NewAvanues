package com.augmentalis.Avanues.web.universal.presentation.ui.browser

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.util.Base64
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.augmentalis.Avanues.web.universal.presentation.ui.security.CertificateUtils
import com.augmentalis.Avanues.web.universal.presentation.ui.security.HttpAuthRequest
import com.augmentalis.Avanues.web.universal.presentation.ui.security.PermissionType
import com.augmentalis.Avanues.web.universal.platform.SettingsApplicator
import com.augmentalis.Avanues.web.universal.platform.webview.WebViewLifecycle
import com.augmentalis.webavanue.domain.model.BrowserSettings
import com.augmentalis.webavanue.domain.state.SettingsStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Global WebViewLifecycle singleton for managing WebView instances across the app
 */
private val globalWebViewLifecycle = WebViewLifecycle()

/**
 * WebViewPoolManager - Android implementation
 * Provides access to the WebViewLifecycle for cleanup operations.
 */
actual object WebViewPoolManager {
    actual fun removeWebView(tabId: String) {
        globalWebViewLifecycle.removeWebView(tabId)
    }

    actual fun clearAllWebViews() {
        globalWebViewLifecycle.clearAllWebViews()
    }
}

/**
 * WebViewContainer - Android implementation
 *
 * Uses AndroidView to wrap Android WebView.
 *
 * Features:
 * - URL navigation and history preservation across tab switches
 * - JavaScript support
 * - Cookie management
 * - Progress tracking
 * - Custom user agent (desktop mode)
 *
 * FIX: Tab history is now preserved by reusing WebView instances from the pool
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun WebViewContainer(
    tabId: String,
    url: String,
    controller: WebViewController?,
    onUrlChange: (String) -> Unit,
    onLoadingChange: (Boolean) -> Unit,
    onTitleChange: (String) -> Unit,
    onProgressChange: (Float) -> Unit,
    canGoBack: (Boolean) -> Unit,
    canGoForward: (Boolean) -> Unit,
    sessionData: String?,
    onSessionDataChange: (String?) -> Unit,
    securityViewModel: com.augmentalis.Avanues.web.universal.presentation.viewmodel.SecurityViewModel?,
    onDownloadStart: ((DownloadRequest) -> Unit)?,
    initialScale: Float,
    settings: BrowserSettings?,
    modifier: Modifier
) {
    val context = LocalContext.current
    var webView: WebView? by remember(tabId) { mutableStateOf(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    // FIX L5: Track restored state per unique sessionData, not per tabId
    // Using tabId+sessionData hash ensures we restore once per unique session
    val sessionKey = "$tabId-${sessionData?.hashCode() ?: 0}"
    var hasRestoredState by remember(sessionKey) { mutableStateOf(false) }

    // FIX L3: Thread-safe settings state machine (prevents race conditions)
    // Create coroutine scope for state machine
    val settingsScope = remember(tabId) { CoroutineScope(SupervisorJob() + kotlinx.coroutines.Dispatchers.Main) }
    val settingsStateMachine = remember(tabId) { SettingsStateMachine(settingsScope) }

    // Observe settings state for UI feedback
    val settingsState by settingsStateMachine.state.collectAsState()

    // Cleanup state machine on dispose
    DisposableEffect(tabId) {
        onDispose {
            settingsScope.cancel()
        }
    }

    // File upload support - callback from onShowFileChooser
    // FIX L17: Track callback per tabId to cleanup on tab switch
    var filePathCallback by remember(tabId) { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    // FIX L17: Cleanup file picker callback on tab switch/dispose
    DisposableEffect(tabId) {
        onDispose {
            filePathCallback?.onReceiveValue(null)
            filePathCallback = null
        }
    }

    // File picker launcher - handles file selection for <input type="file">
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        // Return selected files to WebView
        filePathCallback?.onReceiveValue(uris.toTypedArray())
        filePathCallback = null
        println("📎 File upload: ${uris.size} file(s) selected")
    }

    // FIX L3: Apply settings through state machine when settings change
    // LaunchedEffect dependency on 'settings' ensures this only runs when settings actually change
    LaunchedEffect(settings) {
        settings?.let { browserSettings ->
            webView?.let { view ->
                settingsStateMachine.requestUpdate(browserSettings) { settingsToApply ->
                    val settingsApplicator = SettingsApplicator()
                    settingsApplicator.applySettings(view, settingsToApply)
                }
            }
        }
    }

    // FIX ISSUE #1: Use key(tabId) to force AndroidView recreation when tab changes
    // This ensures WebViewLifecycle.acquireWebView is called for each tab, preserving navigation history
    // Without key(), the factory lambda only runs once and never switches to other tab's WebView
    key(tabId) {
        // FIX: Handle WebView lifecycle to prevent crashes when Home button is pressed
        // ALSO: Save WebView state when app is paused (for history persistence)
        DisposableEffect(lifecycleOwner, tabId) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> {
                        // Pause WebView when activity goes to background
                        webView?.onPause()
                        webView?.pauseTimers()

                        // FIX BUG #2: Save WebView state to persist navigation history
                        webView?.let { wv ->
                            val stateString = globalWebViewLifecycle.saveState(wv)
                            onSessionDataChange(stateString)
                        }
                    }
                    Lifecycle.Event.ON_RESUME -> {
                        // Resume WebView when activity comes to foreground
                        webView?.onResume()
                        webView?.resumeTimers()
                    }
                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                // Save state one final time on dispose
                webView?.let { wv ->
                    val stateString = globalWebViewLifecycle.saveState(wv)
                    onSessionDataChange(stateString)
                }
                // NOTE: We don't destroy the WebView here - it's managed by the lifecycle
                // WebView will be destroyed when the tab is closed (via WebViewLifecycle.removeWebView())
            }
        }

        AndroidView(
        factory = { factoryContext ->
            // FIX: Use WebViewLifecycle to get or create WebView for this tab
            // This preserves navigation history when switching between tabs
            globalWebViewLifecycle.acquireWebView(tabId, factoryContext) { ctx ->
                WebView(ctx).apply {
                    // Set layout params to respect parent constraints
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    // Set black background (Ocean Blue theme)
                    setBackgroundColor(android.graphics.Color.BLACK)

                    // Set initial page scale (default 75%)
                    // 0 = auto-size, 1-100 = percentage scale
                    val scalePercent = (initialScale * 100).toInt()
                    if (scalePercent > 0) {
                        setInitialScale(scalePercent)
                    }

                    // Apply BrowserSettings using SettingsApplicator
                    // This replaces hardcoded settings with user-configurable values
                    val settingsApplicator = SettingsApplicator()

                    // Use provided settings or fallback to defaults
                    val browserSettings = settings ?: BrowserSettings()

                    // Apply all settings (privacy, display, performance, WebXR)
                    val result = settingsApplicator.applySettings(this, browserSettings)

                    // Log any errors during settings application
                    result.onFailure { exception ->
                        println("⚠️ Failed to apply settings: ${exception.message}")
                        exception.printStackTrace()
                    }

                    // Security: Always override file access (regardless of settings)
                    // No action needed - SettingsApplicator already handles this

                // WebViewClient (handles page navigation)
                webViewClient = object : WebViewClient() {
                    // FIX ANR: Track page load timeouts to prevent ANR on slow sites
                    private var loadTimeoutRunnable: Runnable? = null
                    private val loadTimeoutHandler = android.os.Handler(android.os.Looper.getMainLooper())

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        onLoadingChange(true)
                        url?.let { onUrlChange(it) }

                        // FIX ANR: Cancel load if it takes too long (prevents ANR on AOSP devices)
                        // This is especially important for WebGL sites (Babylon.js, Shadertoy) on HMT-1
                        loadTimeoutRunnable?.let { loadTimeoutHandler.removeCallbacks(it) }
                        loadTimeoutRunnable = Runnable {
                            if (view?.progress ?: 100 < 20) {
                                view?.stopLoading()
                                println("⚠️ WebView load timeout after 4s - stopping to prevent ANR")
                                println("   URL: $url")
                                println("   Progress: ${view?.progress}%")
                                onLoadingChange(false)
                            }
                        }
                        loadTimeoutHandler.postDelayed(loadTimeoutRunnable!!, 4000) // 4s (before 5s ANR threshold)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        onLoadingChange(false)

                        // Cancel timeout since page finished loading
                        loadTimeoutRunnable?.let { loadTimeoutHandler.removeCallbacks(it) }
                        loadTimeoutRunnable = null

                        view?.let {
                            canGoBack(it.canGoBack())
                            canGoForward(it.canGoForward())
                            it.title?.let { title -> onTitleChange(title) }
                        }
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        // Let WebView handle the URL
                        return false
                    }

                    // PHASE 1: SSL Error Handling (CWE-295)
                    // PHASE 3: Integrated with SecurityViewModel and SslErrorDialog
                    override fun onReceivedSslError(
                        view: WebView?,
                        handler: SslErrorHandler?,
                        error: SslError?
                    ) {
                        // SECURITY: Reject invalid certificates by default
                        if (error == null || handler == null) {
                            handler?.cancel()
                            return
                        }

                        // Convert Android SslError to our SecurityState
                        val sslErrorInfo = CertificateUtils.convertSslError(error)

                        // Log SSL error for debugging
                        println("🔒 SSL Error detected:")
                        println("   URL: ${sslErrorInfo.url}")
                        println("   Type: ${sslErrorInfo.errorType}")
                        println("   Error: ${sslErrorInfo.primaryError}")

                        // PHASE 3: Show dialog if SecurityViewModel is available
                        if (securityViewModel != null) {
                            securityViewModel.showSslErrorDialog(
                                sslErrorInfo = sslErrorInfo,
                                onGoBack = {
                                    handler.cancel()
                                    println("   → User chose: GO BACK (safe)")
                                },
                                onProceedAnyway = {
                                    handler.proceed()
                                    println("   → User chose: PROCEED ANYWAY (dangerous)")
                                }
                            )
                        } else {
                            // Fallback: Deny if no ViewModel (shouldn't happen)
                            println("   → No SecurityViewModel, denying by default")
                            handler.cancel()
                        }
                    }

                    // HTTP Authentication (Basic/Digest)
                    override fun onReceivedHttpAuthRequest(
                        view: WebView?,
                        handler: android.webkit.HttpAuthHandler?,
                        host: String?,
                        realm: String?
                    ) {
                        // SECURITY: Require user authentication for HTTP Basic/Digest
                        if (handler == null) {
                            handler?.cancel()
                            return
                        }

                        val authRequest = HttpAuthRequest(
                            host = host ?: "Unknown",
                            realm = realm ?: "",
                            scheme = "Basic" // Android doesn't distinguish Basic vs Digest
                        )

                        // Log HTTP auth request
                        println("🔐 HTTP Authentication requested:")
                        println("   Host: ${authRequest.host}")
                        println("   Realm: ${authRequest.realm}")

                        // Show dialog if SecurityViewModel is available
                        if (securityViewModel != null) {
                            securityViewModel.showHttpAuthDialog(
                                authRequest = authRequest,
                                onAuthenticate = { credentials ->
                                    handler.proceed(credentials.username, credentials.password)
                                    println("   → User authenticated")
                                },
                                onCancel = {
                                    handler.cancel()
                                    println("   → User cancelled authentication")
                                }
                            )
                        } else {
                            // Fallback: Cancel if no ViewModel
                            println("   → No SecurityViewModel, cancelling")
                            handler.cancel()
                        }
                    }

                    // FIX P0-P3: Handle network errors (DNS, connection, timeout)
                    override fun onReceivedError(
                        view: WebView?,
                        request: android.webkit.WebResourceRequest?,
                        error: android.webkit.WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)

                        val errorCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                            error?.errorCode ?: -1
                        } else -1

                        val description = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                            error?.description?.toString() ?: "Unknown error"
                        } else "Unknown error"

                        val url = request?.url?.toString() ?: "Unknown URL"
                        val isMainFrame = request?.isForMainFrame ?: false

                        // Only log main frame errors (not subresources like images)
                        if (isMainFrame) {
                            println("🔴 Page Load Error ($errorCode): $url")
                            println("   Error: $description")

                            // Map error codes to user-friendly context
                            val errorContext = when (errorCode) {
                                WebViewClient.ERROR_UNKNOWN -> "Unknown network error"
                                WebViewClient.ERROR_HOST_LOOKUP -> "Could not find server (DNS failure)"
                                WebViewClient.ERROR_UNSUPPORTED_AUTH_SCHEME -> "Unsupported authentication"
                                WebViewClient.ERROR_AUTHENTICATION -> "Authentication failed"
                                WebViewClient.ERROR_PROXY_AUTHENTICATION -> "Proxy authentication failed"
                                WebViewClient.ERROR_CONNECT -> "Could not connect to server"
                                WebViewClient.ERROR_IO -> "Network I/O error"
                                WebViewClient.ERROR_TIMEOUT -> "Connection timeout"
                                WebViewClient.ERROR_REDIRECT_LOOP -> "Too many redirects"
                                WebViewClient.ERROR_UNSUPPORTED_SCHEME -> "Unsupported URL scheme"
                                WebViewClient.ERROR_FAILED_SSL_HANDSHAKE -> "SSL handshake failed"
                                WebViewClient.ERROR_BAD_URL -> "Invalid URL format"
                                WebViewClient.ERROR_FILE -> "Generic file error"
                                WebViewClient.ERROR_FILE_NOT_FOUND -> "File not found"
                                WebViewClient.ERROR_TOO_MANY_REQUESTS -> "Too many requests"
                                else -> "Network error: $description"
                            }
                            println("   Context: $errorContext")
                        }
                    }

                    // FIX P0-P4: Handle HTTP errors (4xx, 5xx responses)
                    override fun onReceivedHttpError(
                        view: WebView?,
                        request: android.webkit.WebResourceRequest?,
                        errorResponse: android.webkit.WebResourceResponse?
                    ) {
                        super.onReceivedHttpError(view, request, errorResponse)

                        val statusCode = errorResponse?.statusCode ?: -1
                        val url = request?.url?.toString() ?: "Unknown URL"
                        val isMainFrame = request?.isForMainFrame ?: false

                        // Only log main frame HTTP errors
                        if (isMainFrame && statusCode >= 400) {
                            println("⚠️  HTTP Error $statusCode: $url")

                            // Log security-relevant errors
                            when (statusCode) {
                                401 -> println("   → Unauthorized (authentication required)")
                                403 -> println("   → Forbidden (access denied)")
                                404 -> println("   → Not Found")
                                in 500..599 -> println("   → Server error (may indicate server issues)")
                            }
                        }
                    }

                    // FIX P5: Intercept requests for ad/tracker blocking and traffic monitoring
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        val url = request?.url?.toString() ?: return null
                        val host = request?.url?.host?.lowercase() ?: return null

                        // Basic ad/tracker domain blocklist (can be expanded)
                        val blockedDomains = setOf(
                            "doubleclick.net",
                            "googlesyndication.com",
                            "googleadservices.com",
                            "google-analytics.com",
                            "facebook.com/tr",
                            "analytics.google.com",
                            "adservice.google.com",
                            "pagead2.googlesyndication.com"
                        )

                        // Check if host matches any blocked domain
                        val isBlocked = blockedDomains.any { blocked ->
                            host.endsWith(blocked) || host == blocked
                        }

                        if (isBlocked) {
                            println("🚫 Blocked request: $url")
                            // Return empty response to block the request
                            return WebResourceResponse(
                                "text/plain",
                                "UTF-8",
                                java.io.ByteArrayInputStream(ByteArray(0))
                            )
                        }

                        // Allow the request to proceed normally
                        return super.shouldInterceptRequest(view, request)
                    }
                }

                // WebChromeClient (handles JavaScript dialogs, progress, etc.)
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        onProgressChange(newProgress / 100f)
                    }

                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        super.onReceivedTitle(view, title)
                        title?.let { onTitleChange(it) }
                    }

                    // PHASE 1: JavaScript Dialog Security (CWE-1021)
                    // PHASE 3: Integrated with SecurityViewModel and JavaScriptAlertDialog

                    override fun onJsAlert(
                        view: WebView?,
                        url: String?,
                        message: String?,
                        result: JsResult?
                    ): Boolean {
                        if (result == null) return true

                        // Extract domain from URL
                        val domain = try {
                            Uri.parse(url).host ?: "Unknown"
                        } catch (e: Exception) {
                            "Unknown"
                        }

                        // Log JavaScript alert
                        println("⚠️  JavaScript Alert from $domain:")
                        println("   Message: ${message ?: "(empty)"}")

                        // PHASE 3: Show dialog if SecurityViewModel is available
                        if (securityViewModel != null) {
                            securityViewModel.showJsAlertDialog(
                                domain = domain,
                                message = message ?: "",
                                onDismiss = {
                                    result.confirm()
                                    println("   → User dismissed alert")
                                }
                            )
                        } else {
                            // Fallback: Cancel if no ViewModel
                            println("   → No SecurityViewModel, cancelling by default")
                            result.cancel()
                        }

                        return true
                    }

                    override fun onJsConfirm(
                        view: WebView?,
                        url: String?,
                        message: String?,
                        result: JsResult?
                    ): Boolean {
                        if (result == null) return true

                        // Extract domain from URL
                        val domain = try {
                            Uri.parse(url).host ?: "Unknown"
                        } catch (e: Exception) {
                            "Unknown"
                        }

                        // Log JavaScript confirm
                        println("⚠️  JavaScript Confirm from $domain:")
                        println("   Message: ${message ?: "(empty)"}")

                        // PHASE 3: Show dialog if SecurityViewModel is available
                        if (securityViewModel != null) {
                            securityViewModel.showJsConfirmDialog(
                                domain = domain,
                                message = message ?: "",
                                onConfirm = {
                                    result.confirm()
                                    println("   → User chose: OK")
                                },
                                onCancel = {
                                    result.cancel()
                                    println("   → User chose: CANCEL")
                                }
                            )
                        } else {
                            // Fallback: Cancel if no ViewModel
                            println("   → No SecurityViewModel, cancelling by default")
                            result.cancel()
                        }

                        return true
                    }

                    override fun onJsPrompt(
                        view: WebView?,
                        url: String?,
                        message: String?,
                        defaultValue: String?,
                        result: JsPromptResult?
                    ): Boolean {
                        if (result == null) return true

                        // Extract domain from URL
                        val domain = try {
                            Uri.parse(url).host ?: "Unknown"
                        } catch (e: Exception) {
                            "Unknown"
                        }

                        // Log JavaScript prompt
                        println("⚠️  JavaScript Prompt from $domain:")
                        println("   Message: ${message ?: "(empty)"}")
                        println("   Default: ${defaultValue ?: "(none)"}")

                        // PHASE 3: Show dialog if SecurityViewModel is available
                        if (securityViewModel != null) {
                            securityViewModel.showJsPromptDialog(
                                domain = domain,
                                message = message ?: "",
                                defaultValue = defaultValue ?: "",
                                onConfirm = { input ->
                                    result.confirm(input)
                                    println("   → User entered: $input")
                                },
                                onCancel = {
                                    result.cancel()
                                    println("   → User chose: CANCEL")
                                }
                            )
                        } else {
                            // Fallback: Cancel if no ViewModel
                            println("   → No SecurityViewModel, cancelling by default")
                            result.cancel()
                        }

                        return true
                    }

                    // PHASE 1: Permission Request Handling (CWE-276)
                    // PHASE 3: Integrated with SecurityViewModel and PermissionRequestDialog
                    override fun onPermissionRequest(request: PermissionRequest?) {
                        if (request == null) return

                        // Extract domain from request origin
                        val domain = request.origin?.host ?: "Unknown"

                        // Convert Android permission resources to our PermissionType
                        val permissions = request.resources.mapNotNull { resource ->
                            PermissionType.fromResourceString(resource)
                        }

                        if (permissions.isEmpty()) {
                            // Unknown permission type - deny for security
                            println("⚠️  Permission request from $domain - unknown type, denying")
                            request.deny()
                            return
                        }

                        // Log permission request
                        println("🔐 Permission request from $domain:")
                        permissions.forEach { perm ->
                            println("   - ${perm.getUserFriendlyName()}")
                        }

                        // PHASE 3: Show dialog if SecurityViewModel is available
                        if (securityViewModel != null) {
                            securityViewModel.showPermissionRequestDialog(
                                domain = domain,
                                permissions = permissions,
                                onAllow = { remember ->
                                    request.grant(request.resources)
                                    println("   → User chose: ALLOW (remember=$remember)")
                                },
                                onDeny = {
                                    request.deny()
                                    println("   → User chose: DENY")
                                }
                            )
                        } else {
                            // Fallback: Deny if no ViewModel (shouldn't happen)
                            println("   → No SecurityViewModel, denying by default")
                            request.deny()
                        }
                    }

                    // File upload support - onShowFileChooser for <input type="file">
                    override fun onShowFileChooser(
                        webView: WebView?,
                        newFilePathCallback: ValueCallback<Array<Uri>>?,
                        fileChooserParams: FileChooserParams?
                    ): Boolean {
                        if (newFilePathCallback == null) return false

                        // Cancel previous callback if any
                        filePathCallback?.onReceiveValue(null)

                        // Store callback for file picker result
                        filePathCallback = newFilePathCallback

                        // Extract MIME types from params
                        val acceptTypes = fileChooserParams?.acceptTypes?.firstOrNull() ?: "*/*"

                        println("📎 File upload requested:")
                        println("   Accept types: $acceptTypes")
                        println("   Multiple: ${fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE}")

                        // Launch file picker with appropriate MIME type
                        filePickerLauncher.launch(acceptTypes)

                        return true
                    }
                }

                // FIX BUG #2: Add download listener for file downloads
                // Updated to use onDownloadStart callback for integration with DownloadViewModel
                setDownloadListener { downloadUrl, userAgent, contentDisposition, mimeType, contentLength ->
                    println("📥 Download requested:")
                    println("   URL: $downloadUrl")
                    println("   MIME: $mimeType")
                    println("   Size: $contentLength bytes")
                    println("   Content-Disposition: $contentDisposition")

                    // Extract filename from content disposition or URL
                    val filename = android.webkit.URLUtil.guessFileName(downloadUrl, contentDisposition, mimeType)
                    println("   Filename: $filename")

                    // Create DownloadRequest and call callback if provided
                    if (onDownloadStart != null) {
                        val downloadRequest = DownloadRequest(
                            url = downloadUrl,
                            filename = filename,
                            mimeType = mimeType,
                            contentLength = contentLength,
                            userAgent = userAgent,
                            contentDisposition = contentDisposition
                        )
                        onDownloadStart(downloadRequest)
                        println("   ✅ Download request sent to ViewModel")
                    } else {
                        // Fallback: Direct download if no callback
                        try {
                            val request = android.app.DownloadManager.Request(android.net.Uri.parse(downloadUrl))
                                .setTitle(filename)
                                .setDescription("Downloading file...")
                                .setMimeType(mimeType)
                                .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                .setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, filename)
                                .addRequestHeader("User-Agent", userAgent)

                            val downloadManager = ctx.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                            val downloadId = downloadManager.enqueue(request)
                            println("   ✅ Download started (fallback): ID=$downloadId")

                            android.widget.Toast.makeText(
                                ctx,
                                "Downloading: $filename",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        } catch (e: Exception) {
                            println("   ❌ Download failed: ${e.message}")
                            e.printStackTrace()

                            android.widget.Toast.makeText(
                                ctx,
                                "Download failed: ${e.message}",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }

                // FIX BUG #2 (original): Restore WebView state if available (navigation history)
                // Only restore once per tab instance
                if (!hasRestoredState && sessionData != null) {
                    val restored = globalWebViewLifecycle.restoreState(this, sessionData)
                    if (restored) {
                        println("WebView: Restored state for tab $tabId (navigation history)")
                        hasRestoredState = true
                    } else {
                        // If restore failed, load initial URL
                        if (url.isNotBlank()) {
                            loadUrl(url)
                        }
                        hasRestoredState = true
                    }
                } else {
                    // Load initial URL (only for new WebViews or if no session data)
                    if (url.isNotBlank() && this.url == null) {
                        loadUrl(url)
                    }
                }

                // Save reference
                webView = this

                // Set up controller
                controller?.setWebView(this)
                }
            }.also { view ->
                // Save reference for lifecycle management
                webView = view
                // Set up controller
                controller?.setWebView(view)
                // Update navigation state
                canGoBack(view.canGoBack())
                canGoForward(view.canGoForward())
            }
        },
        update = { view ->
            // FIX: More robust URL comparison to prevent reload loops
            // Normalize URLs before comparing (ignore trailing slash, protocol case)
            fun normalizeUrl(u: String?): String {
                if (u.isNullOrBlank()) return ""
                return u.lowercase()
                    .trimEnd('/')
                    .removePrefix("https://")
                    .removePrefix("http://")
                    .removePrefix("www.")
            }

            val normalizedViewUrl = normalizeUrl(view.url)
            val normalizedUrl = normalizeUrl(url)

            // Only reload if URLs are meaningfully different and not during initial load
            if (url.isNotBlank() && normalizedViewUrl != normalizedUrl && view.url != null) {
                println("WebView update: Loading new URL - old='${view.url}' new='$url'")
                view.loadUrl(url)
            }

            // FIX L3: Settings updates handled by LaunchedEffect (see below)
            // This avoids applying settings twice (once here, once in LaunchedEffect)
            // The update block only updates controller and navigation state

            // Update controller reference (in case it changed)
            controller?.setWebView(view)
            // Update navigation state
            canGoBack(view.canGoBack())
            canGoForward(view.canGoForward())
        },
            modifier = modifier
        )
    } // End of key(tabId) block
}

/**
 * WebViewController - Android implementation
 */
actual class WebViewController {
    private var webView: WebView? = null

    /**
     * Set WebView instance (called from WebViewContainer)
     */
    fun setWebView(webView: WebView) {
        this.webView = webView
    }

    actual fun goBack() {
        webView?.goBack()
    }

    actual fun goForward() {
        webView?.goForward()
    }

    actual fun reload() {
        webView?.reload()
    }

    actual fun stopLoading() {
        webView?.stopLoading()
    }

    actual fun loadUrl(url: String) {
        webView?.loadUrl(url)
    }

    @SuppressLint("JavascriptInterface")
    actual fun evaluateJavaScript(script: String, callback: (String?) -> Unit) {
        webView?.evaluateJavascript(script) { result ->
            callback(result)
        }
    }

    actual fun clearCache() {
        webView?.clearCache(true)
    }

    actual fun clearCookies() {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
    }

    actual fun clearHistory() {
        webView?.clearHistory()
    }

    actual fun setUserAgent(userAgent: String) {
        webView?.settings?.userAgentString = userAgent
    }

    actual fun setJavaScriptEnabled(enabled: Boolean) {
        webView?.settings?.javaScriptEnabled = enabled
    }

    actual fun setCookiesEnabled(enabled: Boolean) {
        CookieManager.getInstance().setAcceptCookie(enabled)
    }

    actual fun setDesktopMode(enabled: Boolean) {
        webView?.settings?.apply {
            if (enabled) {
                // Desktop user agent (Chrome on Windows)
                userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/120.0.0.0 Safari/537.36"
                useWideViewPort = true
                loadWithOverviewMode = true
            } else {
                // Reset to default (mobile)
                userAgentString = null
            }
        }

        // FIX ANR: Post reload to next frame to avoid blocking current touch event
        // This prevents ANR on slow devices (AOSP Android 10, HMT-1) when toggling desktop mode
        // on heavy sites (Babylon.js, Shadertoy, WebGL content)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            webView?.reload()
        }, 150) // 150ms delay allows touch event to complete
    }

    // ========== Scrolling Controls ==========

    private var currentZoomLevel = 3 // Default zoom level (100%)

    actual fun scrollUp() {
        webView?.let { view ->
            val scrollAmount = view.height / 2
            view.scrollBy(0, -scrollAmount)
        }
    }

    actual fun scrollDown() {
        webView?.let { view ->
            val scrollAmount = view.height / 2
            view.scrollBy(0, scrollAmount)
        }
    }

    actual fun scrollLeft() {
        webView?.let { view ->
            val scrollAmount = view.width / 2
            view.scrollBy(-scrollAmount, 0)
        }
    }

    actual fun scrollRight() {
        webView?.let { view ->
            val scrollAmount = view.width / 2
            view.scrollBy(scrollAmount, 0)
        }
    }

    actual fun scrollToTop() {
        webView?.scrollTo(0, 0)
    }

    actual fun scrollToBottom() {
        webView?.let { view ->
            view.evaluateJavascript("window.scrollTo(0, document.body.scrollHeight);", null)
        }
    }

    // ========== Zoom Controls ==========

    actual fun zoomIn() {
        webView?.let { view ->
            view.settings.textZoom = (view.settings.textZoom * 1.1).toInt()
            if (currentZoomLevel < 5) currentZoomLevel++
        }
    }

    actual fun zoomOut() {
        webView?.let { view ->
            view.settings.textZoom = (view.settings.textZoom * 0.9).toInt()
            if (currentZoomLevel > 1) currentZoomLevel--
        }
    }

    actual fun setZoomLevel(level: Int) {
        webView?.let { view ->
            currentZoomLevel = level.coerceIn(1, 5)
            // Map levels 1-5 to text zoom percentages (50%, 75%, 100%, 125%, 150%)
            val zoomPercent = when (currentZoomLevel) {
                1 -> 50
                2 -> 75
                3 -> 100
                4 -> 125
                5 -> 150
                else -> 100
            }
            view.settings.textZoom = zoomPercent
        }
    }

    actual fun getZoomLevel(): Int = currentZoomLevel

    // ========== Auto-Fit Zoom for Landscape ==========
    private var autoFitZoomEnabled = false
    private var lastViewportWidth = 0

    actual fun setAutoFitZoom(enabled: Boolean) {
        autoFitZoomEnabled = enabled
        if (enabled) {
            applyAutoFitZoom()
        }
    }

    /**
     * Apply auto-fit zoom to show full page width in landscape mode.
     * Uses a combination of WebView settings and JavaScript for reliable zoom.
     *
     * Strategy (from legacy avenue-redux-browser):
     * 1. Enable wide viewport mode - allows WebView to use viewport meta tag
     * 2. Enable overview mode - starts zoomed out to fit page width
     * 3. Calculate and apply scale based on actual content width
     * 4. Use a small delay to ensure page has rendered
     */
    actual fun applyAutoFitZoom() {
        webView?.let { view ->
            val viewWidth = view.width

            // Skip if view not measured yet
            if (viewWidth <= 0) {
                println("AutoFitZoom: View not measured yet, skipping")
                return
            }

            // Skip if width hasn't changed (avoid unnecessary recalculation)
            if (viewWidth == lastViewportWidth && !autoFitZoomEnabled) {
                return
            }
            lastViewportWidth = viewWidth

            // Step 1: Configure WebView settings for wide viewport
            view.settings.apply {
                useWideViewPort = true
                loadWithOverviewMode = true
                // Enable built-in zoom for user manual adjustments after auto-fit
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
            }

            // Step 2: Use JavaScript to get content dimensions and apply optimal zoom
            // Wait a short delay to ensure the page has rendered properly
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                view.evaluateJavascript(
                    """
                    (function() {
                        // Get various width measurements
                        var bodyWidth = document.body ? document.body.scrollWidth : 0;
                        var docWidth = document.documentElement ? document.documentElement.scrollWidth : 0;
                        var bodyOffset = document.body ? document.body.offsetWidth : 0;
                        var docOffset = document.documentElement ? document.documentElement.offsetWidth : 0;

                        // Use the maximum as the actual content width
                        var contentWidth = Math.max(bodyWidth, docWidth, bodyOffset, docOffset);

                        // Also check for viewport meta tag
                        var viewportMeta = document.querySelector('meta[name="viewport"]');
                        var hasViewport = viewportMeta != null;

                        return JSON.stringify({
                            contentWidth: contentWidth,
                            hasViewport: hasViewport,
                            windowWidth: window.innerWidth
                        });
                    })()
                    """.trimIndent()
                ) { result ->
                    try {
                        // Parse the JSON result
                        val jsonStr = result?.replace("\\\"", "\"")?.trim('"') ?: "{}"
                        val contentWidth = Regex("contentWidth\"?:\\s*(\\d+)").find(jsonStr)
                            ?.groupValues?.get(1)?.toIntOrNull() ?: 0
                        val hasViewport = jsonStr.contains("hasViewport\":true") ||
                                          jsonStr.contains("hasViewport\": true")

                        println("AutoFitZoom: Content width=$contentWidth, View width=$viewWidth, Has viewport=$hasViewport")

                        if (contentWidth > 0 && viewWidth > 0) {
                            if (contentWidth > viewWidth) {
                                // Content is wider than view - zoom out to fit
                                val scale = ((viewWidth.toFloat() / contentWidth.toFloat()) * 100).toInt()
                                // Clamp scale between 25% and 100%
                                val clampedScale = scale.coerceIn(25, 100)

                                // Apply the calculated scale
                                view.setInitialScale(clampedScale)

                                // For pages without viewport meta, also use zoomOut
                                if (!hasViewport) {
                                    // Zoom out multiple times if needed for very wide content
                                    val zoomOutTimes = ((contentWidth.toFloat() / viewWidth.toFloat()) - 1).toInt()
                                        .coerceIn(1, 3)
                                    repeat(zoomOutTimes) {
                                        view.zoomOut()
                                    }
                                }

                                println("AutoFitZoom: Applied scale $clampedScale% (ratio: ${contentWidth}/${viewWidth})")
                            } else if (hasViewport) {
                                // Content fits and has viewport - let viewport handle it
                                view.setInitialScale(0) // 0 = use default based on viewport
                                println("AutoFitZoom: Content fits, using viewport meta")
                            } else {
                                // Content fits, no viewport - use overview mode
                                view.setInitialScale(0)
                                println("AutoFitZoom: Content fits, using overview mode")
                            }
                        } else {
                            // Fallback: Use 0 (auto) scale
                            view.setInitialScale(0)
                            println("AutoFitZoom: Could not determine content size, using auto scale")
                        }
                    } catch (e: Exception) {
                        // Fallback: Use overview mode
                        view.setInitialScale(0)
                        println("AutoFitZoom: Error - ${e.message}, using auto scale")
                    }
                }
            }, 150) // Small delay to ensure page render
        }
    }

    // ========== Touch/Interaction Controls ==========

    actual fun setScrollFrozen(frozen: Boolean) {
        webView?.let { view ->
            if (frozen) {
                // Disable scrolling
                view.setOnTouchListener { _, _ -> true }
            } else {
                // Re-enable scrolling
                view.setOnTouchListener(null)
            }
        }
    }

    actual fun performClick() {
        webView?.let { view ->
            val x = view.width / 2f
            val y = view.height / 2f
            val downTime = android.os.SystemClock.uptimeMillis()
            val eventTime = android.os.SystemClock.uptimeMillis()

            val downEvent = android.view.MotionEvent.obtain(
                downTime, eventTime,
                android.view.MotionEvent.ACTION_DOWN,
                x, y, 0
            )
            val upEvent = android.view.MotionEvent.obtain(
                downTime, eventTime + 100,
                android.view.MotionEvent.ACTION_UP,
                x, y, 0
            )

            view.dispatchTouchEvent(downEvent)
            view.dispatchTouchEvent(upEvent)

            downEvent.recycle()
            upEvent.recycle()
        }
    }

    actual fun performDoubleClick() {
        webView?.let { view ->
            val x = view.width / 2f
            val y = view.height / 2f
            val downTime = android.os.SystemClock.uptimeMillis()

            // First click
            val downEvent1 = android.view.MotionEvent.obtain(
                downTime, downTime,
                android.view.MotionEvent.ACTION_DOWN,
                x, y, 0
            )
            val upEvent1 = android.view.MotionEvent.obtain(
                downTime, downTime + 50,
                android.view.MotionEvent.ACTION_UP,
                x, y, 0
            )

            // Second click
            val downEvent2 = android.view.MotionEvent.obtain(
                downTime + 100, downTime + 100,
                android.view.MotionEvent.ACTION_DOWN,
                x, y, 0
            )
            val upEvent2 = android.view.MotionEvent.obtain(
                downTime + 100, downTime + 150,
                android.view.MotionEvent.ACTION_UP,
                x, y, 0
            )

            view.dispatchTouchEvent(downEvent1)
            view.dispatchTouchEvent(upEvent1)
            view.dispatchTouchEvent(downEvent2)
            view.dispatchTouchEvent(upEvent2)

            downEvent1.recycle()
            upEvent1.recycle()
            downEvent2.recycle()
            upEvent2.recycle()
        }
    }
}

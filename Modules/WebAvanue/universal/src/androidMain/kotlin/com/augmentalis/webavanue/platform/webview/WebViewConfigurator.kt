package com.augmentalis.webavanue.platform.webview

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.net.http.SslError
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import com.augmentalis.webavanue.platform.SettingsApplicator
import com.augmentalis.webavanue.feature.download.DownloadRequest
import com.augmentalis.webavanue.ui.screen.security.CertificateUtils
import com.augmentalis.webavanue.ui.screen.security.HttpAuthRequest
import com.augmentalis.webavanue.ui.screen.security.PermissionType
import com.augmentalis.webavanue.ui.viewmodel.SecurityViewModel
import com.augmentalis.webavanue.domain.model.BrowserSettings

/**
 * WebViewConfigurator - Configures WebView instances based on browser settings
 *
 * Single Responsibility: Translating BrowserSettings to WebView configuration
 *
 * Features:
 * - Initial WebView setup (settings, layout, background)
 * - Settings application via SettingsApplicator
 * - WebViewClient configuration (navigation, security, ad blocking)
 * - WebChromeClient configuration (progress, dialogs, permissions)
 * - Download listener setup
 * - Incremental settings updates (performance optimization)
 *
 * Security Features:
 * - SSL error handling (CWE-295)
 * - HTTP authentication (Basic/Digest)
 * - JavaScript dialog security (CWE-1021)
 * - Permission request handling (CWE-276)
 * - Ad/tracker blocking
 */
class WebViewConfigurator {

    /**
     * Configure WebView instance with initial settings.
     * Called once during WebView creation.
     *
     * @param webView WebView instance to configure
     * @param context Android context
     * @param settings Browser settings to apply
     * @param initialScale Initial page scale (0-1.0, 0 = auto)
     * @param securityViewModel ViewModel for security dialogs (optional)
     * @param onDownloadStart Callback for download requests (optional)
     * @param onUrlChange Callback when URL changes
     * @param onLoadingChange Callback when loading state changes
     * @param onTitleChange Callback when page title changes
     * @param onProgressChange Callback when load progress changes
     * @param canGoBack Callback with back navigation state
     * @param canGoForward Callback with forward navigation state
     * @param filePathCallback Callback for file upload results
     * @param filePickerLauncher Launcher for file picker activity
     */
    @SuppressLint("SetJavaScriptEnabled")
    fun configure(
        webView: WebView,
        context: Context,
        settings: BrowserSettings?,
        initialScale: Float,
        securityViewModel: SecurityViewModel?,
        onDownloadStart: ((DownloadRequest) -> Unit)?,
        onUrlChange: (String) -> Unit,
        onLoadingChange: (Boolean) -> Unit,
        onTitleChange: (String) -> Unit,
        onProgressChange: (Float) -> Unit,
        canGoBack: (Boolean) -> Unit,
        canGoForward: (Boolean) -> Unit,
        filePathCallback: () -> ValueCallback<Array<Uri>>?,
        setFilePathCallback: (ValueCallback<Array<Uri>>?) -> Unit,
        filePickerLauncher: ManagedActivityResultLauncher<String, List<Uri>>
    ) {
        // Set layout params to respect parent constraints
        webView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        // Set black background (Ocean Blue theme)
        webView.setBackgroundColor(Color.BLACK)

        // Set initial page scale (default 75%)
        // 0 = auto-size, 1-100 = percentage scale
        val scalePercent = (initialScale * 100).toInt()
        if (scalePercent > 0) {
            webView.setInitialScale(scalePercent)
        }

        // Apply BrowserSettings using SettingsApplicator
        applySettings(webView, settings)

        // Configure WebViewClient (navigation, security)
        configureWebViewClient(
            webView,
            securityViewModel,
            onUrlChange,
            onLoadingChange,
            onTitleChange,
            canGoBack,
            canGoForward
        )

        // Configure WebChromeClient (progress, dialogs, permissions)
        configureWebChromeClient(
            webView,
            securityViewModel,
            onProgressChange,
            onTitleChange,
            filePathCallback,
            setFilePathCallback,
            filePickerLauncher
        )

        // Configure download listener
        configureDownloadListener(webView, context, onDownloadStart)
    }

    /**
     * Apply browser settings to WebView.
     * Uses SettingsApplicator for comprehensive settings application.
     *
     * @param webView WebView instance
     * @param settings Browser settings to apply (null = use defaults)
     */
    private fun applySettings(webView: WebView, settings: BrowserSettings?) {
        val settingsApplicator = SettingsApplicator()
        val browserSettings = settings ?: BrowserSettings()

        // Apply all settings (privacy, display, performance, WebXR)
        val result = settingsApplicator.applySettings(webView, browserSettings)

        // Log any errors during settings application
        result.onFailure { exception ->
            println("⚠️ WebViewConfigurator: Failed to apply settings: ${exception.message}")
            exception.printStackTrace()
        }
    }

    /**
     * Reconfigure WebView when settings change.
     * Only updates changed settings for performance.
     *
     * @param webView WebView instance
     * @param newSettings New browser settings
     * @param previousSettings Previous browser settings (for diff)
     */
    fun reconfigure(
        webView: WebView,
        newSettings: BrowserSettings,
        previousSettings: BrowserSettings?
    ) {
        if (newSettings != previousSettings) {
            val settingsApplicator = SettingsApplicator()
            val result = settingsApplicator.applySettings(webView, newSettings)

            result.onFailure { exception ->
                println("⚠️ WebViewConfigurator: Failed to apply updated settings: ${exception.message}")
            }
        }
    }

    /**
     * Configure WebViewClient for navigation and security.
     *
     * Handles:
     * - Page navigation events (start, finish)
     * - SSL errors (CWE-295)
     * - HTTP authentication (Basic/Digest)
     * - Network errors (DNS, connection, timeout)
     * - HTTP errors (4xx, 5xx)
     * - Request interception (ad/tracker blocking)
     * - ANR prevention (load timeout)
     */
    private fun configureWebViewClient(
        webView: WebView,
        securityViewModel: SecurityViewModel?,
        onUrlChange: (String) -> Unit,
        onLoadingChange: (Boolean) -> Unit,
        onTitleChange: (String) -> Unit,
        canGoBack: (Boolean) -> Unit,
        canGoForward: (Boolean) -> Unit
    ) {
        webView.webViewClient = object : WebViewClient() {
            // FIX ANR: Track page load timeouts to prevent ANR on slow sites
            private var loadTimeoutRunnable: Runnable? = null
            private val loadTimeoutHandler = Handler(Looper.getMainLooper())

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
                handler: HttpAuthHandler?,
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
                request: WebResourceRequest?,
                error: WebResourceError?
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
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
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
    }

    /**
     * Configure WebChromeClient for progress, dialogs, and permissions.
     *
     * Handles:
     * - Progress tracking
     * - Page title updates
     * - JavaScript alerts (CWE-1021)
     * - JavaScript confirms
     * - JavaScript prompts
     * - Permission requests (CWE-276) - camera, microphone, location
     * - File upload picker
     */
    private fun configureWebChromeClient(
        webView: WebView,
        securityViewModel: SecurityViewModel?,
        onProgressChange: (Float) -> Unit,
        onTitleChange: (String) -> Unit,
        filePathCallback: () -> ValueCallback<Array<Uri>>?,
        setFilePathCallback: (ValueCallback<Array<Uri>>?) -> Unit,
        filePickerLauncher: ManagedActivityResultLauncher<String, List<Uri>>
    ) {
        webView.webChromeClient = object : WebChromeClient() {
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
                filePathCallback()?.onReceiveValue(null)

                // Store callback for file picker result
                setFilePathCallback(newFilePathCallback)

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
    }

    /**
     * Configure download listener for file downloads.
     *
     * @param webView WebView instance
     * @param context Android context
     * @param onDownloadStart Callback for download requests (optional)
     */
    private fun configureDownloadListener(
        webView: WebView,
        context: Context,
        onDownloadStart: ((DownloadRequest) -> Unit)?
    ) {
        webView.setDownloadListener { downloadUrl, userAgent, contentDisposition, mimeType, contentLength ->
            println("📥 Download requested:")
            println("   URL: $downloadUrl")
            println("   MIME: $mimeType")
            println("   Size: $contentLength bytes")
            println("   Content-Disposition: $contentDisposition")

            // Extract filename from content disposition or URL
            val filename = URLUtil.guessFileName(downloadUrl, contentDisposition, mimeType)
            println("   Filename: $filename")

            // Create DownloadRequest and call callback if provided
            if (onDownloadStart != null) {
                val downloadRequest = DownloadRequest(
                    url = downloadUrl,
                    filename = filename,
                    mimeType = mimeType,
                    expectedSize = contentLength,
                    userAgent = userAgent
                )
                onDownloadStart(downloadRequest)
                println("   ✅ Download request sent to ViewModel")
            } else {
                // Fallback: Direct download if no callback
                try {
                    val request = DownloadManager.Request(Uri.parse(downloadUrl))
                        .setTitle(filename)
                        .setDescription("Downloading file...")
                        .setMimeType(mimeType)
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
                        .addRequestHeader("User-Agent", userAgent)

                    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    val downloadId = downloadManager.enqueue(request)
                    println("   ✅ Download started (fallback): ID=$downloadId")

                    Toast.makeText(
                        context,
                        "Downloading: $filename",
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Exception) {
                    println("   ❌ Download failed: ${e.message}")
                    e.printStackTrace()

                    Toast.makeText(
                        context,
                        "Download failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}

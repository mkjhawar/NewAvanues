package com.augmentalis.cockpit.mvp.content.webview

import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.avanues.cockpit.core.window.WindowContent
import com.augmentalis.cockpit.mvp.content.bridge.CockpitJsBridge
import org.json.JSONObject

/**
 * WebViewRenderer - Composable wrapper for ManagedWebView
 *
 * Renders WebView content in Flat workspace mode with:
 * - Automatic WebView lifecycle management
 * - JavaScript bridge integration
 * - Content state synchronization
 * - Scroll position restoration
 * - Loading and error handling
 *
 * Usage:
 * ```kotlin
 * WebViewRenderer(
 *     windowId = window.id,
 *     content = window.content as WindowContent.WebContent,
 *     onContentStateChanged = { newContent ->
 *         viewModel.updateWindowContent(window.id, newContent)
 *     },
 *     modifier = Modifier.fillMaxSize()
 * )
 * ```
 */
@Composable
fun WebViewRenderer(
    windowId: String,
    content: WindowContent.WebContent,
    onContentStateChanged: (WindowContent.WebContent) -> Unit,
    modifier: Modifier = Modifier,
    enableBridge: Boolean = true,
    onMinimize: (() -> Unit)? = null,
    onMaximize: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null
) {
    val context = LocalContext.current

    // Remember WebView across recompositions
    val webView = remember(windowId) {
        ManagedWebView(
            context = context,
            windowId = windowId
        ).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    // Create JavaScript bridge implementation
    val jsBridge = remember(windowId) {
        if (enableBridge) {
            CockpitJsBridgeImpl(
                windowId = windowId,
                onWindowAction = { action, params ->
                    // Handle window actions from JavaScript
                    handleWindowAction(
                        action = action,
                        params = params,
                        onContentStateChanged = onContentStateChanged,
                        currentContent = content,
                        onMinimize = onMinimize,
                        onMaximize = onMaximize,
                        onClose = onClose
                    )
                }
            )
        } else {
            null
        }
    }

    // Attach bridge when available
    LaunchedEffect(jsBridge) {
        jsBridge?.let { bridge ->
            webView.attachBridge(bridge)
        }
    }

    // Observe content state changes from WebView
    LaunchedEffect(windowId) {
        webView.observeContentState { newState ->
            onContentStateChanged(newState)
        }
    }

    // Track last loaded URL to prevent redundant loads
    var lastLoadedUrl by remember { mutableStateOf<String?>(null) }

    // Load URL when content changes
    LaunchedEffect(content.url) {
        if (content.url.isNotEmpty() && content.url != lastLoadedUrl) {
            webView.loadUrl(content.url)
            lastLoadedUrl = content.url
        }
    }

    // Restore scroll position
    LaunchedEffect(content.scrollX, content.scrollY) {
        if (content.scrollX > 0 || content.scrollY > 0) {
            webView.scrollTo(content.scrollX, content.scrollY)
        }
    }

    // Cleanup on dispose
    DisposableEffect(windowId) {
        onDispose {
            // Ensure WebView is removed from parent before destroying
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.destroy()
        }
    }

    // Render WebView
    AndroidView(
        factory = { webView },
        modifier = modifier.fillMaxSize()
    )
}

/**
 * Handle window actions triggered from JavaScript bridge
 */
private fun handleWindowAction(
    action: String,
    @Suppress("UNUSED_PARAMETER") params: Map<String, Any>,
    @Suppress("UNUSED_PARAMETER") onContentStateChanged: (WindowContent.WebContent) -> Unit,
    @Suppress("UNUSED_PARAMETER") currentContent: WindowContent.WebContent,
    onMinimize: (() -> Unit)?,
    onMaximize: (() -> Unit)?,
    onClose: (() -> Unit)?
) {
    when (action) {
        "resize" -> {
            // Window resize is handled by WorkspaceViewModel
            // JavaScript can only request size via bridge
            // params would contain: width, height, isLarge (reserved for future use)
        }
        "minimize" -> {
            onMinimize?.invoke()
        }
        "maximize" -> {
            onMaximize?.invoke()
        }
        "close" -> {
            onClose?.invoke()
        }
    }
}

// ========================================
// JavaScript Bridge Implementation
// ========================================

/**
 * Default implementation of CockpitJsBridge
 */
private class CockpitJsBridgeImpl(
    private val windowId: String,
    private val onWindowAction: (action: String, params: Map<String, Any>) -> Unit
) : CockpitJsBridge {

    override fun requestSize(width: Int, height: Int, isLarge: Boolean): String {
        onWindowAction("resize", mapOf(
            "width" to width,
            "height" to height,
            "isLarge" to isLarge
        ))
        return success()
    }

    override fun minimize(): String {
        onWindowAction("minimize", emptyMap())
        return success()
    }

    override fun maximize(): String {
        onWindowAction("maximize", emptyMap())
        return success()
    }

    override fun close(): String {
        onWindowAction("close", emptyMap())
        return success()
    }

    override fun getLocation(): String {
        // TODO: Request location permission and get actual location
        return error("Location not implemented yet")
    }

    override fun voiceSearch(query: String): String {
        // TODO: Launch VoiceOS voice search
        return error("Voice search not implemented yet")
    }

    override fun navigate(screenId: String): String {
        // TODO: Navigate to Cockpit screen
        return error("Navigation not implemented yet")
    }

    override fun shareContent(title: String, text: String, url: String): String {
        // TODO: Launch Android share sheet
        return error("Share not implemented yet")
    }

    override fun openWindow(type: String, url: String, title: String): String {
        // TODO: Open new window in workspace
        return error("Open window not implemented yet")
    }

    override fun sendMessage(windowId: String, message: String): String {
        // TODO: Send message to another window
        return error("Inter-window messaging not implemented yet")
    }

    override fun log(level: String, message: String): String {
        android.util.Log.println(
            when (level) {
                "DEBUG" -> android.util.Log.DEBUG
                "INFO" -> android.util.Log.INFO
                "WARN" -> android.util.Log.WARN
                "ERROR" -> android.util.Log.ERROR
                else -> android.util.Log.VERBOSE
            },
            "JS[$windowId]",
            message
        )
        return success()
    }

    override fun reportError(error: String, context: String): String {
        android.util.Log.e("JS[$windowId]", "Error: $error | Context: $context")
        // TODO: Send to Firebase Crashlytics
        return success()
    }

    // Helpers
    private fun success(data: Map<String, Any>? = null): String {
        val json = JSONObject().apply {
            put("success", true)
            data?.let { d ->
                put("data", JSONObject(d))
            }
        }
        return json.toString()
    }

    private fun error(message: String): String {
        val json = JSONObject().apply {
            put("success", false)
            put("error", message)
        }
        return json.toString()
    }
}

package com.augmentalis.webavanue

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.layout.BorderPane
import javafx.scene.web.WebEngine
import javafx.scene.web.WebView as JFXWebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.awt.BorderLayout
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JPanel

/**
 * Desktop implementation of WebView using JavaFX WebView
 */
class DesktopWebView(
    private val config: WebViewConfig
) : WebView {

    private val jfxPanel = JFXPanel()
    private lateinit var webView: JFXWebView
    private lateinit var webEngine: WebEngine

    private val _currentUrl = MutableStateFlow(config.initialUrl)
    override val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()

    private val _pageTitle = MutableStateFlow("")
    override val pageTitle: StateFlow<String> = _pageTitle.asStateFlow()

    private val _loadingProgress = MutableStateFlow(0)
    override val loadingProgress: StateFlow<Int> = _loadingProgress.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _canGoBack = MutableStateFlow(false)
    override val canGoBack: StateFlow<Boolean> = _canGoBack.asStateFlow()

    private val _canGoForward = MutableStateFlow(false)
    override val canGoForward: StateFlow<Boolean> = _canGoForward.asStateFlow()

    init {
        initializeJavaFX()
    }

    private fun initializeJavaFX() {
        Platform.setImplicitExit(false)
        Platform.runLater {
            webView = JFXWebView()
            webEngine = webView.engine

            // Apply configuration
            applyConfiguration()

            // Setup listeners
            setupListeners()

            // Create scene
            val borderPane = BorderPane()
            borderPane.center = webView
            val scene = Scene(borderPane)
            jfxPanel.scene = scene

            // Load initial URL
            if (config.initialUrl.isNotBlank() && config.initialUrl != "about:blank") {
                loadUrl(config.initialUrl)
            }
        }
    }

    private fun applyConfiguration() {
        Platform.runLater {
            // JavaScript configuration
            webEngine.isJavaScriptEnabled = config.javaScriptEnabled

            // User agent
            config.userAgent?.let {
                webEngine.userAgent = it
            }

            // Zoom level based on font size
            webView.zoom = when (config.fontSize) {
                BrowserSettings.FontSize.TINY -> 0.75
                BrowserSettings.FontSize.SMALL -> 0.875
                BrowserSettings.FontSize.MEDIUM -> 1.0
                BrowserSettings.FontSize.LARGE -> 1.125
                BrowserSettings.FontSize.HUGE -> 1.25
            }.toDouble()

            // Error handler
            webEngine.onError = { event ->
                println("WebView Error: ${event.message}")
            }

            // Alert handler
            webEngine.onAlert = { event ->
                println("JavaScript Alert: ${event.data}")
            }

            // Confirm handler
            webEngine.confirmHandler = { message ->
                println("JavaScript Confirm: $message")
                true // Auto-confirm for now
            }

            // Prompt handler
            webEngine.promptHandler = { promptData ->
                println("JavaScript Prompt: ${promptData.message}")
                promptData.defaultValue // Return default value
            }
        }
    }

    private fun setupListeners() {
        Platform.runLater {
            // Location listener
            webEngine.locationProperty().addListener { _, _, newValue ->
                _currentUrl.value = newValue ?: ""
                updateNavigationState()
            }

            // Title listener
            webEngine.titleProperty().addListener { _, _, newValue ->
                _pageTitle.value = newValue ?: ""
            }

            // Loading state listener
            webEngine.loadWorker.stateProperty().addListener { _, _, newValue ->
                _isLoading.value = newValue == Worker.State.RUNNING
                if (newValue == Worker.State.SUCCEEDED || newValue == Worker.State.FAILED) {
                    updateNavigationState()
                }
            }

            // Progress listener
            webEngine.loadWorker.progressProperty().addListener { _, _, newValue ->
                _loadingProgress.value = (newValue.toDouble() * 100).toInt()
            }

            // Error listener
            webEngine.loadWorker.exceptionProperty().addListener { _, _, newValue ->
                newValue?.let {
                    println("Loading error: ${it.message}")
                }
            }
        }
    }

    private fun updateNavigationState() {
        Platform.runLater {
            val history = webEngine.history
            _canGoBack.value = history.currentIndex > 0
            _canGoForward.value = history.currentIndex < history.entries.size - 1
        }
    }

    override fun loadUrl(url: String) {
        Platform.runLater {
            webEngine.load(url)
        }
    }

    override fun reload() {
        Platform.runLater {
            webEngine.reload()
        }
    }

    override fun stopLoading() {
        Platform.runLater {
            webEngine.loadWorker.cancel()
        }
    }

    override fun goBack() {
        Platform.runLater {
            val history = webEngine.history
            if (history.currentIndex > 0) {
                history.go(-1)
            }
        }
    }

    override fun goForward() {
        Platform.runLater {
            val history = webEngine.history
            if (history.currentIndex < history.entries.size - 1) {
                history.go(1)
            }
        }
    }

    override fun clearHistory() {
        Platform.runLater {
            webEngine.history.entries.clear()
        }
    }

    override fun clearCache(includeDiskFiles: Boolean) {
        // JavaFX doesn't provide direct cache clearing
        // Would need to clear JavaFX cache directory
        if (includeDiskFiles) {
            val cacheDir = File(System.getProperty("user.home"), ".java/.userPrefs/javafx/webview")
            if (cacheDir.exists()) {
                cacheDir.deleteRecursively()
            }
        }
    }

    override suspend fun evaluateJavaScript(script: String): String? = withContext(Dispatchers.IO) {
        var result: String? = null
        Platform.runLater {
            try {
                val jsResult = webEngine.executeScript(script)
                result = jsResult?.toString()
            } catch (e: Exception) {
                println("JavaScript execution error: ${e.message}")
            }
        }
        // Wait for Platform.runLater to complete
        Thread.sleep(100)
        result
    }

    override suspend fun captureScreenshot(): ByteArray? = withContext(Dispatchers.IO) {
        var screenshotData: ByteArray? = null
        Platform.runLater {
            try {
                val snapshot = webView.snapshot(null, null)
                val bufferedImage = javafx.embed.swing.SwingFXUtils.fromFXImage(snapshot, null)
                val outputStream = ByteArrayOutputStream()
                ImageIO.write(bufferedImage, "PNG", outputStream)
                screenshotData = outputStream.toByteArray()
            } catch (e: Exception) {
                println("Screenshot capture error: ${e.message}")
            }
        }
        // Wait for Platform.runLater to complete
        Thread.sleep(200)
        screenshotData
    }

    override fun findInPage(text: String) {
        Platform.runLater {
            // JavaFX WebView doesn't have built-in find functionality
            // Implement with JavaScript
            val script = """
                (function() {
                    var text = '$text';
                    if (window.find) {
                        window.find(text, false, false, true, false, true, false);
                        return true;
                    }
                    return false;
                })();
            """.trimIndent()
            webEngine.executeScript(script)
        }
    }

    override fun clearFindInPage() {
        Platform.runLater {
            val script = "window.getSelection().removeAllRanges();"
            webEngine.executeScript(script)
        }
    }

    override fun setUserAgent(userAgent: String) {
        Platform.runLater {
            webEngine.userAgent = userAgent
        }
    }

    override fun setJavaScriptEnabled(enabled: Boolean) {
        Platform.runLater {
            webEngine.isJavaScriptEnabled = enabled
        }
    }

    override fun setDesktopMode(enabled: Boolean) {
        val userAgent = if (enabled) {
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        } else {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        }
        setUserAgent(userAgent)
    }

    override fun zoomIn() {
        Platform.runLater {
            webView.zoom = (webView.zoom * 1.1).coerceAtMost(3.0)
        }
    }

    override fun zoomOut() {
        Platform.runLater {
            webView.zoom = (webView.zoom * 0.9).coerceAtLeast(0.5)
        }
    }

    override fun resetZoom() {
        Platform.runLater {
            webView.zoom = 1.0
        }
    }

    override fun dispose() {
        Platform.runLater {
            webEngine.load(null)
        }
    }

    fun getSwingComponent(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.add(jfxPanel, BorderLayout.CENTER)
        return panel
    }
}

/**
 * Desktop implementation of WebViewFactory
 */
actual class WebViewFactory {
    actual fun createWebView(config: WebViewConfig): WebView {
        return DesktopWebView(config)
    }
}

/**
 * Desktop Composable for WebView using SwingPanel
 */
@Composable
actual fun WebViewComposable(
    url: String,
    modifier: Modifier,
    config: WebViewConfig,
    onEvent: (WebViewEvent) -> Unit
) {
    val desktopWebView = remember {
        // Initialize JavaFX on first use
        initializeJavaFXOnce()
        DesktopWebView(config)
    }

    DisposableEffect(url) {
        desktopWebView.loadUrl(url)

        // Setup event listeners
        desktopWebView.currentUrl.value.let { currentUrl ->
            if (currentUrl != url && currentUrl.isNotBlank()) {
                onEvent(WebViewEvent.PageStarted(currentUrl))
            }
        }

        desktopWebView.isLoading.value.let { loading ->
            if (!loading && desktopWebView.currentUrl.value.isNotBlank()) {
                onEvent(WebViewEvent.PageFinished(desktopWebView.currentUrl.value))
            }
        }

        desktopWebView.pageTitle.value.let { title ->
            if (title.isNotBlank()) {
                onEvent(WebViewEvent.TitleReceived(title))
            }
        }

        desktopWebView.loadingProgress.value.let { progress ->
            onEvent(WebViewEvent.ProgressChanged(progress))
        }

        onDispose {
            desktopWebView.dispose()
        }
    }

    SwingPanel(
        factory = { desktopWebView.getSwingComponent() },
        modifier = modifier,
        update = { panel ->
            // Update panel if needed
        }
    )
}

/**
 * Initialize JavaFX once for the application
 */
private var javaFXInitialized = false

private fun initializeJavaFXOnce() {
    if (!javaFXInitialized) {
        javaFXInitialized = true
        // Initialize JavaFX toolkit
        try {
            Platform.startup {}
        } catch (e: IllegalStateException) {
            // Already initialized
        }
    }
}

/**
 * JCEF (Chromium Embedded Framework) WebView for Desktop - Alternative Implementation
 * This provides better compatibility and performance than JavaFX WebView
 */
class JCEFWebView(
    private val config: WebViewConfig
) : WebView {
    // JCEF implementation would go here for production use
    // Requires additional dependencies and native libraries

    override val currentUrl: StateFlow<String> = MutableStateFlow(config.initialUrl).asStateFlow()
    override val pageTitle: StateFlow<String> = MutableStateFlow("").asStateFlow()
    override val loadingProgress: StateFlow<Int> = MutableStateFlow(0).asStateFlow()
    override val isLoading: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()
    override val canGoBack: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()
    override val canGoForward: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()

    override fun loadUrl(url: String) {
        // JCEF implementation
    }

    override fun reload() {
        // JCEF implementation
    }

    override fun stopLoading() {
        // JCEF implementation
    }

    override fun goBack() {
        // JCEF implementation
    }

    override fun goForward() {
        // JCEF implementation
    }

    override fun clearHistory() {
        // JCEF implementation
    }

    override fun clearCache(includeDiskFiles: Boolean) {
        // JCEF implementation
    }

    override suspend fun evaluateJavaScript(script: String): String? {
        // JCEF implementation
        return null
    }

    override suspend fun captureScreenshot(): ByteArray? {
        // JCEF implementation
        return null
    }

    override fun findInPage(text: String) {
        // JCEF implementation
    }

    override fun clearFindInPage() {
        // JCEF implementation
    }

    override fun setUserAgent(userAgent: String) {
        // JCEF implementation
    }

    override fun setJavaScriptEnabled(enabled: Boolean) {
        // JCEF implementation
    }

    override fun setDesktopMode(enabled: Boolean) {
        // JCEF implementation
    }

    override fun zoomIn() {
        // JCEF implementation
    }

    override fun zoomOut() {
        // JCEF implementation
    }

    override fun resetZoom() {
        // JCEF implementation
    }

    override fun dispose() {
        // JCEF implementation
    }
}
package com.augmentalis.webavanue

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
// Tab model is in the same package (com.augmentalis.webavanue.Tab)
import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.web.WebView
import javax.swing.JPanel
import java.awt.BorderLayout

/**
 * Desktop WebView Implementation using JavaFX WebView
 *
 * Integrates JavaFX WebView via SwingPanel for Compose Desktop
 */
@Composable
actual fun BrowserWebView(
    tab: Tab,
    desktopMode: Boolean,
    onUrlChanged: (String) -> Unit,
    onPageLoaded: (url: String, title: String?) -> Unit,
    modifier: Modifier
) {
    val webViewState = remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf(tab.url) }

    SwingPanel(
        factory = {
            JPanel().apply {
                layout = BorderLayout()

                // JavaFX panel
                val jfxPanel = JFXPanel()
                add(jfxPanel, BorderLayout.CENTER)

                // Initialize JavaFX WebView on JavaFX thread
                Platform.runLater {
                    val webViewInstance = WebView().apply {
                        val webEngine = engine

                        // Setup load state listener
                        webEngine.loadWorker.stateProperty().addListener { _, _, newState ->
                            when (newState) {
                                Worker.State.RUNNING -> {
                                    val url = webEngine.location ?: ""
                                    if (url.isNotEmpty()) {
                                        onUrlChanged(url)
                                    }
                                }
                                Worker.State.SUCCEEDED -> {
                                    val url = webEngine.location ?: ""
                                    val title = webEngine.title
                                    onPageLoaded(url, title)
                                }
                                Worker.State.FAILED -> {
                                    println("WebView load failed: ${webEngine.loadWorker.exception}")
                                }
                                else -> {}
                            }
                        }

                        // Desktop mode user agent (JavaFX WebView uses desktop UA by default)
                        webEngine.userAgent = if (desktopMode) {
                            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                        } else {
                            // Mobile user agent
                            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                        }

                        // Enable JavaScript
                        webEngine.isJavaScriptEnabled = true

                        // Load initial URL
                        webEngine.load(tab.url)
                    }

                    webViewState.value = webViewInstance
                    val scene = Scene(webViewInstance)
                    jfxPanel.scene = scene
                }
            }
        },
        update = { panel ->
            // Update URL if changed
            if (currentUrl != tab.url) {
                currentUrl = tab.url
                Platform.runLater {
                    webViewState.value?.engine?.load(tab.url)
                }
            }

            // Update desktop mode if changed
            Platform.runLater {
                webViewState.value?.engine?.let { engine ->
                    val newUserAgent = if (desktopMode) {
                        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                    } else {
                        "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                    }

                    if (engine.userAgent != newUserAgent) {
                        engine.userAgent = newUserAgent
                        engine.reload()
                    }
                }
            }
        },
        modifier = modifier
    )
}

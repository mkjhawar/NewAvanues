package com.augmentalis.cockpit.mvp.rendering

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import com.avanues.cockpit.core.window.AppWindow
import com.avanues.cockpit.core.window.WindowContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * SpatialWebViewManager - Off-screen WebView rendering for spatial mode
 *
 * Manages WebViews for each window, captures bitmap snapshots,
 * and provides them to SpatialWindowRenderer for 3D projection.
 *
 * Unlike curved mode (ViewPager2 with visible WebViews), this renders
 * WebViews off-screen and captures snapshots for Canvas-based spatial rendering.
 */
class SpatialWebViewManager(private val context: Context) {

    // Hidden container for off-screen WebView rendering
    private val container = FrameLayout(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        visibility = View.INVISIBLE  // Hidden but measured/laid out
    }

    // WebViews for each window (windowId -> WebView)
    private val webViews = mutableMapOf<String, WebView>()

    // Captured bitmaps for each window (windowId -> Bitmap)
    private val bitmaps = mutableMapOf<String, Bitmap>()

    // Scope for async operations
    private val scope = CoroutineScope(Dispatchers.Main)

    /**
     * Get or create hidden container view
     * Must be added to parent ViewGroup for WebViews to render
     */
    fun getContainer(): FrameLayout = container

    /**
     * Update windows and trigger WebView rendering
     *
     * @param windows List of windows to render
     * @param windowColors Map of window colors for top bars
     */
    fun updateWindows(windows: List<AppWindow>, windowColors: Map<String, String>) {
        // Remove WebViews for windows that no longer exist
        val currentWindowIds = windows.map { it.id }.toSet()
        webViews.keys.filter { it !in currentWindowIds }.forEach { windowId ->
            webViews.remove(windowId)?.destroy()
            bitmaps.remove(windowId)
        }

        // Create/update WebViews for each window
        windows.forEach { window ->
            when (val content = window.content) {
                is WindowContent.WebContent -> {
                    getOrCreateWebView(window.id, content.url, windowColors[window.id] ?: "#2D5F7F")
                }
                is WindowContent.MockContent -> {
                    // For mock content, create a placeholder bitmap
                    createPlaceholderBitmap(window.id, window.title, windowColors[window.id] ?: "#2D5F7F")
                }
                else -> {
                    // Other content types - placeholder for now
                    createPlaceholderBitmap(window.id, window.title, windowColors[window.id] ?: "#2D5F7F")
                }
            }
        }
    }

    /**
     * Get bitmap for window (returns null if not yet captured)
     */
    fun getBitmap(windowId: String): Bitmap? = bitmaps[windowId]

    /**
     * Get or create WebView for window
     */
    private fun getOrCreateWebView(windowId: String, url: String, color: String): WebView {
        return webViews.getOrPut(windowId) {
            WebView(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    800,  // Fixed width for consistent bitmap size
                    600   // Fixed height
                )

                // Configure WebView
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    setSupportZoom(false)
                }

                // Capture bitmap after page loads
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // Delay snapshot to ensure rendering is complete
                        view?.postDelayed({
                            captureBitmap(windowId, view, color)
                        }, 500)
                    }
                }

                // Add to hidden container
                container.addView(this)

                // Load URL
                loadUrl(url)
            }
        }
    }

    /**
     * Capture bitmap snapshot of WebView with colored top bar
     */
    private fun captureBitmap(windowId: String, webView: WebView, color: String) {
        scope.launch {
            try {
                val width = webView.width
                val height = webView.height

                if (width <= 0 || height <= 0) return@launch

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)

                // Draw colored top bar (48px height)
                canvas.drawColor(Color.parseColor(color))
                canvas.drawRect(0f, 0f, width.toFloat(), 48f, android.graphics.Paint().apply {
                    this.color = Color.parseColor(color)
                })

                // Draw WebView content below top bar
                canvas.save()
                canvas.translate(0f, 48f)
                webView.draw(canvas)
                canvas.restore()

                // Store bitmap
                bitmaps[windowId] = bitmap

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Create placeholder bitmap for non-web content
     */
    private fun createPlaceholderBitmap(windowId: String, title: String, color: String) {
        val bitmap = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        canvas.drawColor(Color.parseColor("#FFFFFF"))

        // Top bar
        canvas.drawRect(0f, 0f, 800f, 48f, android.graphics.Paint().apply {
            this.color = Color.parseColor(color)
        })

        // Title text
        canvas.drawText(title, 400f, 300f, android.graphics.Paint().apply {
            textSize = 40f
            textAlign = android.graphics.Paint.Align.CENTER
            this.color = Color.BLACK
        })

        bitmaps[windowId] = bitmap
    }

    /**
     * Clean up resources
     */
    fun destroy() {
        webViews.values.forEach { it.destroy() }
        webViews.clear()
        bitmaps.clear()
        container.removeAllViews()
    }
}

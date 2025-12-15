package com.augmentalis.cockpit.mvp.curved

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.avanues.cockpit.core.window.AppWindow
import com.avanues.cockpit.core.window.WindowContent

/**
 * WindowViewPagerAdapter - Adapter for curved workspace ViewPager2
 *
 * Renders actual window content (WebViews, etc.) and captures bitmap
 * snapshots for curved transformation display.
 *
 * Each page contains:
 * 1. Real content view (WebView, ImageView, etc.)
 * 2. CurvedImage overlay for curved snapshot display
 */
class WindowViewPagerAdapter(
    private val theme: CurvedWorkspaceTheme
) : RecyclerView.Adapter<WindowViewPagerAdapter.WindowViewHolder>() {

    private var windows: List<AppWindow> = emptyList()
    private val snapshots = mutableMapOf<String, Bitmap?>()
    private var currentPosition: Int = 0  // Track center position for visibility toggling

    fun submitWindows(newWindows: List<AppWindow>) {
        windows = newWindows
        notifyDataSetChanged()
    }

    /**
     * Update current position and refresh visibility for all holders
     * Called when ViewPager2 page changes
     */
    fun setCurrentPosition(position: Int) {
        val oldPosition = currentPosition
        currentPosition = position

        // Notify only affected positions for efficiency
        notifyItemChanged(oldPosition)  // Old center becomes side
        notifyItemChanged(position)      // New center
    }

    override fun getItemCount(): Int = windows.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WindowViewHolder {
        // Create container layout (always visible, never clipped)
        val container = FrameLayout(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            visibility = View.VISIBLE  // Ensure container is always visible
            clipChildren = false  // Allow curved edges to draw outside
            clipToPadding = false
        }

        // Add content view (will render actual WebView/content)
        val contentView = FrameLayout(parent.context).apply {
            id = View.generateViewId()
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        container.addView(contentView)

        // Add curved image overlay (will show curved snapshot)
        val curvedImage = CurvedImage(parent.context).apply {
            id = View.generateViewId()
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            visibility = View.GONE  // Initially hidden, shown when snapshot ready
        }
        container.addView(curvedImage)

        return WindowViewHolder(container, contentView, curvedImage)
    }

    override fun onBindViewHolder(holder: WindowViewHolder, position: Int) {
        val window = windows[position]
        val color = theme.windowColors[position % theme.windowColors.size]
        val isCenter = (position == currentPosition)

        holder.bind(window, color, isCenter)
    }

    inner class WindowViewHolder(
        private val container: FrameLayout,
        private val contentView: FrameLayout,
        val curvedImage: CurvedImage
    ) : RecyclerView.ViewHolder(container) {

        private var webView: WebView? = null

        fun bind(window: AppWindow, color: String, isCenter: Boolean) {
            // Clear previous content
            contentView.removeAllViews()
            webView?.destroy()
            webView = null

            // Render content based on window type
            when (val content = window.content) {
                is WindowContent.WebContent -> {
                    renderWebView(content.url, color, isCenter)
                }
                is WindowContent.MockContent -> {
                    renderMockContent(window.title, color, isCenter)
                }
                else -> {
                    renderPlaceholder(window.title, color, isCenter)
                }
            }

            // Apply visibility toggling (reference code behavior)
            refreshVisibility(isCenter)
        }

        /**
         * Refresh visibility based on position (reference code pattern)
         * Center page: Show live content, hide curved snapshot
         * Side pages: Hide live content, show curved snapshot
         */
        private fun refreshVisibility(isCenter: Boolean) {
            // ALWAYS capture bitmap first (ensures it's available for curved display)
            if (curvedImage.image == null) {
                contentView.postDelayed({
                    captureImmediateBitmap()
                }, 300)  // Delay to ensure content is rendered
            }

            if (isCenter) {
                // CENTER: Live interactive WebView visible, curved image hidden
                contentView.visibility = View.VISIBLE
                curvedImage.visibility = View.GONE
            } else {
                // SIDE: Curved static snapshot visible, live content hidden
                contentView.visibility = View.INVISIBLE  // Invisible (not GONE) to maintain layout
                curvedImage.visibility = View.VISIBLE
            }
        }

        /**
         * Capture bitmap immediately (reference code pattern)
         * Called when page becomes a side page
         */
        private fun captureImmediateBitmap() {
            try {
                val width = contentView.width
                val height = contentView.height

                if (width <= 0 || height <= 0) return

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                contentView.draw(canvas)

                // Set bitmap immediately
                curvedImage.image = bitmap
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun renderWebView(url: String, color: String, isCenter: Boolean) {
            // Add colored top bar first
            val topBar = View(contentView.context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    48  // 48dp height
                ).apply {
                    gravity = Gravity.TOP
                }
                setBackgroundColor(Color.parseColor(color))
            }
            contentView.addView(topBar)

            // Create WebView below top bar
            webView = WebView(contentView.context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                ).apply {
                    topMargin = 48  // Below colored bar
                }

                // Configure WebView
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    setSupportZoom(false)
                }

                // Capture snapshot after page loads
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // Delay snapshot to ensure rendering is complete
                        view?.postDelayed({
                            captureSnapshot(view, topBar)
                        }, 500)
                    }
                }

                // Load URL
                loadUrl(url)
            }

            contentView.addView(webView)
        }

        private fun renderMockContent(title: String, color: String, isCenter: Boolean) {
            // TODO: Render mock content view
            renderPlaceholder(title, color, isCenter)
        }

        private fun renderPlaceholder(title: String, color: String, isCenter: Boolean) {
            // Simple placeholder for now
            val placeholder = View(contentView.context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(android.graphics.Color.parseColor(color))
                alpha = 0.3f
            }
            contentView.addView(placeholder)
        }

        /**
         * Capture bitmap snapshot of WebView + top bar for curved display
         */
        private fun captureSnapshot(webView: WebView, topBar: View) {
            try {
                // Capture entire content view (WebView + top bar)
                val width = contentView.width
                val height = contentView.height

                if (width <= 0 || height <= 0) return

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)

                // Draw top bar
                canvas.save()
                topBar.draw(canvas)
                canvas.restore()

                // Draw WebView below top bar
                canvas.save()
                canvas.translate(0f, 48f)  // Offset by top bar height
                webView.draw(canvas)
                canvas.restore()

                // Store bitmap for curved transformation (used by side previews)
                curvedImage.post {
                    curvedImage.image = bitmap
                    // Don't show curved image yet - PageTransformer will control visibility
                    // Center page shows live WebView, side pages show curved snapshot
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getCurvedImage(position: Int): CurvedImage? {
        // This will be called by PageTransformer to apply curve
        return null  // TODO: Implement view holder tracking
    }
}

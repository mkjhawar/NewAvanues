package com.avanues.cockpit.core.window

/**
 * WindowContent - Sealed class hierarchy for window content types
 *
 * Defines what content should be rendered inside an AppWindow.
 * Each type has specific configuration for its renderer.
 */
sealed class WindowContent {

    /**
     * Web content rendered in a WebView
     *
     * @param url The URL to load
     * @param javaScriptEnabled Whether to enable JavaScript (default: true)
     * @param domStorageEnabled Whether to enable DOM storage (default: true)
     * @param userAgent Custom user agent string (default: null for default UA)
     * @param scrollX Horizontal scroll position (Phase 3: State Persistence)
     * @param scrollY Vertical scroll position (Phase 3: State Persistence)
     * @param isDesktopMode Whether to use desktop user agent (Phase 4: FR-4.1)
     * @param pageTitle Dynamic page title from WebView (Phase 4: FR-4.2)
     */
    data class WebContent(
        val url: String,
        val javaScriptEnabled: Boolean = true,
        val domStorageEnabled: Boolean = true,
        val userAgent: String? = null,

        // Phase 3: State persistence (FR-3.1)
        val scrollX: Int = 0,
        val scrollY: Int = 0,

        // Phase 4: WebView enhancements (FR-4.1, FR-4.2)
        val isDesktopMode: Boolean = true,  // Default: desktop mode for better AR glasses rendering
        val pageTitle: String? = null  // Updated dynamically from page content
    ) : WindowContent()

    /**
     * Document content (PDF, image, text file, video)
     *
     * @param uri Content URI (file://, content://, or http://)
     * @param mimeType MIME type of the document
     * @param documentType Type of document for renderer selection
     * @param currentPage Current page number for PDF (Phase 3: FR-3.2)
     * @param zoomLevel Zoom level for PDF (1.0 = 100%, Phase 3: FR-3.2)
     * @param scrollX Horizontal scroll for PDF (Phase 3: FR-3.2)
     * @param scrollY Vertical scroll for PDF (Phase 3: FR-3.2)
     * @param playbackPosition Video playback position in milliseconds (Phase 3: FR-3.3)
     */
    data class DocumentContent(
        val uri: String,
        val mimeType: String,
        val documentType: DocumentType,

        // Phase 3: State persistence for PDF (FR-3.2)
        val currentPage: Int = 0,
        val zoomLevel: Float = 1.0f,
        val scrollX: Float = 0f,
        val scrollY: Float = 0f,

        // Phase 3: State persistence for VIDEO (FR-3.3)
        val playbackPosition: Long = 0L
    ) : WindowContent()

    /**
     * Freeform Android app window
     *
     * Requires MediaProjection permission to capture and render.
     *
     * @param packageName Android package name (e.g., "com.android.calculator2")
     * @param activityName Optional activity name to launch (default: main activity)
     * @param launchData Optional Intent extras as JSON string
     */
    data class FreeformAppContent(
        val packageName: String,
        val activityName: String? = null,
        val launchData: String? = null
    ) : WindowContent()

    /**
     * Mock/placeholder content for testing and development
     *
     * Shows window metadata (type, voice name, position) instead of real content.
     */
    object MockContent : WindowContent()
}

/**
 * Document types for DocumentContent renderer selection
 */
enum class DocumentType {
    PDF,
    IMAGE,
    TEXT,
    VIDEO,  // Phase 3: Video playback support
    UNKNOWN
}

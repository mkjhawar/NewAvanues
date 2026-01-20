package com.augmentalis.webavanue

import com.augmentalis.webavanue.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Screenshot manager state
 * Tracks the current state of screenshot operations
 */
sealed class ScreenshotState {
    /**
     * No screenshot operation in progress
     */
    data object Idle : ScreenshotState()

    /**
     * Showing type selection dialog
     */
    data object SelectingType : ScreenshotState()

    /**
     * Capturing screenshot
     * @param progress Progress (0.0 to 1.0)
     * @param message Status message
     */
    data class Capturing(
        val progress: Float,
        val message: String
    ) : ScreenshotState()

    /**
     * Screenshot captured successfully
     * @param data Screenshot data
     * @param filepath Path where saved (if saved)
     */
    data class Success(
        val data: ScreenshotData,
        val filepath: String?
    ) : ScreenshotState()

    /**
     * Screenshot capture failed
     * @param error Error message
     */
    data class Error(
        val error: String
    ) : ScreenshotState()
}

/**
 * Screenshot manager
 *
 * High-level API for screenshot capture workflow:
 * 1. User clicks screenshot button
 * 2. Show type selection dialog (visible area or full page)
 * 3. Capture screenshot with progress updates
 * 4. Show success notification or error dialog
 * 5. Provide share/save actions
 *
 * Usage:
 * ```kotlin
 * val manager = ScreenshotManager(scope)
 * manager.startScreenshotCapture(webView) { type ->
 *     // User selected type
 * }
 * ```
 */
class ScreenshotManager(
    private val scope: CoroutineScope
) {
    private var captureJob: Job? = null
    private var screenshotCapture: ScreenshotCapture? = null

    /**
     * Current screenshot state
     * Observe this to update UI
     */
    var state: ScreenshotState = ScreenshotState.Idle
        private set

    /**
     * Start screenshot capture workflow
     *
     * @param webView Platform-specific WebView instance
     * @param onStateChange Callback when state changes
     */
    fun startScreenshotCapture(
        webView: Any,
        onStateChange: (ScreenshotState) -> Unit
    ) {
        // Initialize screenshot capture
        screenshotCapture = createScreenshotCapture(webView)

        // Show type selection dialog
        updateState(ScreenshotState.SelectingType, onStateChange)
    }

    /**
     * User selected screenshot type
     *
     * @param type Screenshot type (VISIBLE_AREA or FULL_PAGE)
     * @param quality JPEG quality (0-100)
     * @param saveToGallery Whether to save to gallery
     * @param onStateChange Callback when state changes
     */
    fun captureScreenshot(
        type: ScreenshotType,
        quality: Int = 80,
        saveToGallery: Boolean = true,
        onStateChange: (ScreenshotState) -> Unit
    ) {
        val capture = screenshotCapture ?: run {
            Logger.error("ScreenshotManager", "ScreenshotCapture not initialized")
            updateState(ScreenshotState.Error("Screenshot capture not initialized"), onStateChange)
            return
        }

        // Start capture
        captureJob = scope.launch(Dispatchers.Main) {
            val request = ScreenshotRequest(
                type = type,
                quality = quality,
                saveToGallery = saveToGallery
            )

            capture.capture(request)
                .catch { e ->
                    Logger.error("ScreenshotManager", "Screenshot capture failed: ${e.message}", e)
                    updateState(
                        ScreenshotState.Error("Screenshot capture failed: ${e.message}"),
                        onStateChange
                    )
                }
                .collect { result ->
                    when (result) {
                        is ScreenshotResult.Progress -> {
                            updateState(
                                ScreenshotState.Capturing(result.progress, result.message),
                                onStateChange
                            )
                        }

                        is ScreenshotResult.Success -> {
                            updateState(
                                ScreenshotState.Success(result.data, result.filepath),
                                onStateChange
                            )
                        }

                        is ScreenshotResult.Error -> {
                            updateState(
                                ScreenshotState.Error(result.error),
                                onStateChange
                            )
                        }
                    }
                }
        }
    }

    /**
     * Cancel ongoing screenshot capture
     *
     * @param onStateChange Callback when state changes
     */
    fun cancelCapture(onStateChange: (ScreenshotState) -> Unit) {
        captureJob?.cancel()
        captureJob = null

        scope.launch {
            screenshotCapture?.cancel()
            updateState(ScreenshotState.Idle, onStateChange)
        }
    }

    /**
     * Reset to idle state
     *
     * @param onStateChange Callback when state changes
     */
    fun reset(onStateChange: (ScreenshotState) -> Unit) {
        captureJob?.cancel()
        captureJob = null
        screenshotCapture = null
        updateState(ScreenshotState.Idle, onStateChange)
    }

    /**
     * Share a captured screenshot
     *
     * @param filepath Path to screenshot file
     */
    suspend fun shareScreenshot(filepath: String) {
        screenshotCapture?.share(filepath, "Share Screenshot")
    }

    /**
     * Save screenshot data to gallery
     *
     * @param data Screenshot data
     * @param filename Optional filename
     * @param quality JPEG quality (0-100)
     * @return File path or null on failure
     */
    suspend fun saveScreenshot(
        data: ScreenshotData,
        filename: String? = null,
        quality: Int = 80
    ): String? {
        return screenshotCapture?.saveScreenshot(data, filename, quality)
    }

    /**
     * Update state and notify callback
     */
    private fun updateState(
        newState: ScreenshotState,
        onStateChange: (ScreenshotState) -> Unit
    ) {
        state = newState
        onStateChange(newState)
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        captureJob?.cancel()
        captureJob = null
        screenshotCapture = null
        state = ScreenshotState.Idle
    }
}

package com.augmentalis.cockpit.mvp.content.telemetry

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * WebViewTelemetry - Tracks WebView performance and errors
 *
 * Collects telemetry data for:
 * - Page load times
 * - HTTP errors (404, 500, etc.)
 * - SSL certificate errors
 * - JavaScript errors
 * - Network failures
 * - Performance metrics (DOM content loaded, first paint, etc.)
 *
 * Data is logged locally and optionally sent to Firebase Crashlytics
 * for production monitoring.
 *
 * Usage:
 * ```kotlin
 * val telemetry = WebViewTelemetry(windowId = "augmentalis-1")
 * telemetry.onPageLoadStart("https://www.augmentalis.com")
 * telemetry.onPageLoadFinish(loadTimeMs = 1234)
 * telemetry.onHttpError(errorCode = 404, url = "https://example.com/missing")
 * ```
 */
class WebViewTelemetry(
    private val windowId: String,
    private val enableFirebaseCrashlytics: Boolean = false
) {
    companion object {
        private const val TAG = "WebViewTelemetry"

        // Error severity levels
        const val SEVERITY_INFO = "INFO"
        const val SEVERITY_WARNING = "WARNING"
        const val SEVERITY_ERROR = "ERROR"
        const val SEVERITY_CRITICAL = "CRITICAL"

        // Performance thresholds
        private const val SLOW_PAGE_LOAD_MS = 5000L  // 5 seconds
        private const val VERY_SLOW_PAGE_LOAD_MS = 10000L  // 10 seconds
    }

    private val scope = CoroutineScope(Dispatchers.Default)

    // Telemetry state
    private var currentUrl: String? = null
    private var pageLoadStartTime: Long = 0L
    private var errorCount: Int = 0
    private var successfulLoads: Int = 0

    // ========================================
    // Page Load Tracking
    // ========================================

    /**
     * Called when page starts loading
     *
     * @param url URL being loaded
     */
    fun onPageLoadStart(url: String) {
        currentUrl = url
        pageLoadStartTime = System.currentTimeMillis()

        logEvent(
            eventName = "page_load_start",
            severity = SEVERITY_INFO,
            metadata = mapOf(
                "url" to url,
                "window_id" to windowId,
                "timestamp" to pageLoadStartTime
            )
        )
    }

    /**
     * Called when page finishes loading
     *
     * @param loadTimeMs Page load time in milliseconds (if known, 0 otherwise)
     */
    fun onPageLoadFinish(loadTimeMs: Long = 0L) {
        val actualLoadTime = if (loadTimeMs > 0) {
            loadTimeMs
        } else if (pageLoadStartTime > 0) {
            System.currentTimeMillis() - pageLoadStartTime
        } else {
            0L
        }

        successfulLoads++

        val severity = when {
            actualLoadTime > VERY_SLOW_PAGE_LOAD_MS -> SEVERITY_WARNING
            actualLoadTime > SLOW_PAGE_LOAD_MS -> SEVERITY_INFO
            else -> SEVERITY_INFO
        }

        logEvent(
            eventName = "page_load_finish",
            severity = severity,
            metadata = mapOf(
                "url" to (currentUrl ?: "unknown"),
                "window_id" to windowId,
                "load_time_ms" to actualLoadTime,
                "is_slow" to (actualLoadTime > SLOW_PAGE_LOAD_MS),
                "successful_loads" to successfulLoads
            )
        )

        // Reset page load tracking
        pageLoadStartTime = 0L
    }

    // ========================================
    // Error Tracking
    // ========================================

    /**
     * Called when HTTP error occurs (404, 500, etc.)
     *
     * @param errorCode HTTP error code
     * @param url URL that failed
     * @param description Error description
     */
    fun onHttpError(errorCode: Int, url: String, description: String = "") {
        errorCount++

        val severity = when (errorCode) {
            in 400..499 -> SEVERITY_WARNING  // Client errors (404, 403, etc.)
            in 500..599 -> SEVERITY_ERROR    // Server errors (500, 503, etc.)
            else -> SEVERITY_INFO
        }

        logEvent(
            eventName = "http_error",
            severity = severity,
            metadata = mapOf(
                "error_code" to errorCode,
                "url" to url,
                "window_id" to windowId,
                "description" to description,
                "error_count" to errorCount
            )
        )

        if (enableFirebaseCrashlytics) {
            reportToFirebase(
                eventName = "http_error_$errorCode",
                metadata = mapOf(
                    "url" to url,
                    "window_id" to windowId,
                    "error_code" to errorCode.toString()
                )
            )
        }
    }

    /**
     * Called when SSL certificate error occurs
     *
     * @param url URL with SSL error
     * @param error SSL error message
     */
    fun onSslError(url: String, error: String) {
        errorCount++

        logEvent(
            eventName = "ssl_error",
            severity = SEVERITY_CRITICAL,
            metadata = mapOf(
                "url" to url,
                "window_id" to windowId,
                "error" to error,
                "error_count" to errorCount
            )
        )

        if (enableFirebaseCrashlytics) {
            reportToFirebase(
                eventName = "ssl_error",
                metadata = mapOf(
                    "url" to url,
                    "window_id" to windowId,
                    "error" to error
                )
            )
        }
    }

    /**
     * Called when network error occurs (timeout, no connection, etc.)
     *
     * @param url URL that failed
     * @param error Error message
     */
    fun onNetworkError(url: String, error: String) {
        errorCount++

        logEvent(
            eventName = "network_error",
            severity = SEVERITY_ERROR,
            metadata = mapOf(
                "url" to url,
                "window_id" to windowId,
                "error" to error,
                "error_count" to errorCount
            )
        )

        if (enableFirebaseCrashlytics) {
            reportToFirebase(
                eventName = "network_error",
                metadata = mapOf(
                    "url" to url,
                    "window_id" to windowId,
                    "error" to error
                )
            )
        }
    }

    /**
     * Called when JavaScript console error occurs
     *
     * @param message Error message
     * @param sourceId Source file
     * @param lineNumber Line number
     */
    fun onJavaScriptError(message: String, sourceId: String, lineNumber: Int) {
        logEvent(
            eventName = "javascript_error",
            severity = SEVERITY_WARNING,
            metadata = mapOf(
                "message" to message,
                "source" to sourceId,
                "line" to lineNumber,
                "window_id" to windowId,
                "url" to (currentUrl ?: "unknown")
            )
        )
    }

    // ========================================
    // Performance Metrics
    // ========================================

    /**
     * Track custom performance metric
     *
     * @param metricName Metric name (e.g., "dom_content_loaded", "first_paint")
     * @param value Metric value in milliseconds
     */
    fun trackPerformanceMetric(metricName: String, value: Long) {
        logEvent(
            eventName = "performance_metric",
            severity = SEVERITY_INFO,
            metadata = mapOf(
                "metric" to metricName,
                "value_ms" to value,
                "window_id" to windowId,
                "url" to (currentUrl ?: "unknown")
            )
        )
    }

    /**
     * Track memory usage
     *
     * @param usedMemoryMb Memory used in MB
     * @param totalMemoryMb Total available memory in MB
     */
    fun trackMemoryUsage(usedMemoryMb: Double, totalMemoryMb: Double) {
        val memoryPercentage = (usedMemoryMb / totalMemoryMb) * 100

        val severity = when {
            memoryPercentage > 90 -> SEVERITY_WARNING
            memoryPercentage > 95 -> SEVERITY_ERROR
            else -> SEVERITY_INFO
        }

        logEvent(
            eventName = "memory_usage",
            severity = severity,
            metadata = mapOf(
                "used_mb" to usedMemoryMb,
                "total_mb" to totalMemoryMb,
                "percentage" to memoryPercentage,
                "window_id" to windowId
            )
        )
    }

    // ========================================
    // Statistics
    // ========================================

    /**
     * Get telemetry statistics
     *
     * @return Map of telemetry stats
     */
    fun getStatistics(): Map<String, Any> {
        return mapOf(
            "window_id" to windowId,
            "error_count" to errorCount,
            "successful_loads" to successfulLoads,
            "current_url" to (currentUrl ?: "none")
        )
    }

    /**
     * Reset telemetry counters
     */
    fun reset() {
        errorCount = 0
        successfulLoads = 0
        currentUrl = null
        pageLoadStartTime = 0L
    }

    // ========================================
    // Internal Helpers
    // ========================================

    private fun logEvent(eventName: String, severity: String, metadata: Map<String, Any>) {
        val json = JSONObject().apply {
            put("event", eventName)
            put("severity", severity)
            put("timestamp", System.currentTimeMillis())
            metadata.forEach { (key, value) ->
                put(key, value)
            }
        }

        val logMessage = "[$severity] $eventName: ${json.toString(2)}"

        when (severity) {
            SEVERITY_INFO -> Log.i(TAG, logMessage)
            SEVERITY_WARNING -> Log.w(TAG, logMessage)
            SEVERITY_ERROR, SEVERITY_CRITICAL -> Log.e(TAG, logMessage)
        }
    }

    private fun reportToFirebase(eventName: String, metadata: Map<String, String>) {
        scope.launch {
            // TODO: Integrate Firebase Crashlytics
            // FirebaseCrashlytics.getInstance().apply {
            //     setCustomKey("window_id", windowId)
            //     metadata.forEach { (key, value) ->
            //         setCustomKey(key, value)
            //     }
            //     log("$eventName: ${metadata.toString()}")
            // }
            Log.d(TAG, "Firebase telemetry (not yet implemented): $eventName - $metadata")
        }
    }
}

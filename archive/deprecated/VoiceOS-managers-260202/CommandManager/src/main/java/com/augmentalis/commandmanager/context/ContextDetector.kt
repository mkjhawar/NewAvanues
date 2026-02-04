/**
 * ContextDetector.kt - Real-time context detection system
 * Detects current app, screen, time, location, and activity contexts
 *
 * Created: 2025-10-09 12:37:32 PDT
 * Part of Week 4 - Context-Aware Commands implementation
 */

package com.augmentalis.commandmanager.context

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.content.ContextCompat
// Google Play Services imports removed - using stubs for now
// import com.google.android.gms.location.ActivityRecognition
// import com.google.android.gms.location.ActivityRecognitionClient
// import com.google.android.gms.location.ActivityTransition
// import com.google.android.gms.location.DetectedActivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Context detector for real-time context detection
 * Detects: app, screen, time, location, activity
 * Performance target: <100ms for full context detection
 */
class ContextDetector(
    private val androidContext: Context
) {

    companion object {
        private const val TAG = "ContextDetector"
        private const val CONTEXT_CACHE_TIMEOUT_MS = 500L // 500ms cache
        private const val HIGH_CONFIDENCE_THRESHOLD = 0.7f
    }

    // Current context cache
    private val _currentContext = MutableStateFlow<CommandContext.Composite?>(null)
    val currentContext: Flow<CommandContext.Composite?> = _currentContext.asStateFlow()

    // Activity recognition client (stub for now - would use Google Play Services)
    private var activityRecognitionClient: Any? = null

    // Last detected contexts (for performance optimization)
    private var lastAppContext: CommandContext.App? = null
    private var lastScreenContext: CommandContext.Screen? = null
    private var lastLocationContext: CommandContext.Location? = null
    private var lastActivityContext: CommandContext.Activity? = null
    private var lastContextUpdate = 0L

    // Location and activity detection enabled flags
    private var locationDetectionEnabled = false
    private var activityDetectionEnabled = false

    /**
     * Initialize the context detector
     * Checks permissions and initializes activity recognition
     */
    fun initialize() {
        // Check if we have location permission
        locationDetectionEnabled = hasLocationPermission()

        // Check if we have activity recognition permission
        activityDetectionEnabled = hasActivityRecognitionPermission()

        // Activity recognition would be initialized here with Google Play Services
        // if (activityDetectionEnabled) {
        //     activityRecognitionClient = ActivityRecognition.getClient(androidContext)
        // }

        android.util.Log.i(TAG, "ContextDetector initialized (location: $locationDetectionEnabled, activity: $activityDetectionEnabled)")
    }

    /**
     * Detect current composite context
     * Combines all available context types into a single composite
     *
     * @param accessibilityService Optional accessibility service for screen detection
     * @return Composite context containing all detected contexts
     */
    suspend fun detectCurrentContext(accessibilityService: AccessibilityService? = null): CommandContext.Composite {
        val startTime = System.currentTimeMillis()

        // Check cache
        val now = System.currentTimeMillis()
        val cachedContext = _currentContext.value
        if (now - lastContextUpdate < CONTEXT_CACHE_TIMEOUT_MS && cachedContext != null) {
            android.util.Log.v(TAG, "Using cached context (age: ${now - lastContextUpdate}ms)")
            return cachedContext
        }

        val contexts = mutableListOf<CommandContext>()

        // Detect app context (fast)
        detectAppContext(accessibilityService)?.let { contexts.add(it) }

        // Detect screen context (requires accessibility service)
        if (accessibilityService != null) {
            detectScreenContext(accessibilityService)?.let { contexts.add(it) }
        }

        // Detect time context (instant)
        contexts.add(detectTimeContext())

        // Detect location context (if enabled)
        if (locationDetectionEnabled) {
            detectLocationContext()?.let { contexts.add(it) }
        }

        // Detect activity context (if enabled)
        if (activityDetectionEnabled) {
            detectActivityContext()?.let { contexts.add(it) }
        }

        // Create composite context
        val composite = CommandContext.Composite(contexts)
        _currentContext.value = composite
        lastContextUpdate = now

        val elapsedTime = System.currentTimeMillis() - startTime
        android.util.Log.d(TAG, "Context detection completed in ${elapsedTime}ms (${contexts.size} contexts)")

        return composite
    }

    /**
     * Detect foreground app context
     * Uses UsageStatsManager on Android 5.1+ or legacy getRunningTasks
     *
     * @return App context or null if detection fails
     */
    fun detectAppContext(accessibilityService: AccessibilityService? = null): CommandContext.App? {
        try {
            // Try accessibility service first (most reliable)
            if (accessibilityService != null) {
                val rootNode = accessibilityService.rootInActiveWindow
                if (rootNode != null) {
                    val packageName = rootNode.packageName?.toString()
                    if (packageName != null) {
                        val category = detectAppCategory(packageName)
                        val appContext = CommandContext.App(packageName, null, category)
                        lastAppContext = appContext
                        return appContext
                    }
                }
            }

            // Fallback to UsageStatsManager (Android 5.1+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                val usageStatsManager = androidContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                val endTime = System.currentTimeMillis()
                val beginTime = endTime - 1000 // Last 1 second

                val usageStats = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_BEST,
                    beginTime,
                    endTime
                )

                val currentApp = usageStats?.maxByOrNull { it.lastTimeUsed }
                if (currentApp != null) {
                    val category = detectAppCategory(currentApp.packageName)
                    val appContext = CommandContext.App(currentApp.packageName, null, category)
                    lastAppContext = appContext
                    return appContext
                }
            }

            // Last resort: legacy method (deprecated but still works)
            @Suppress("DEPRECATION")
            val activityManager = androidContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            @Suppress("DEPRECATION")
            val runningTasks = activityManager.getRunningTasks(1)
            if (runningTasks.isNotEmpty()) {
                val topTask = runningTasks[0]
                val packageName = topTask.topActivity?.packageName
                val activityName = topTask.topActivity?.className
                if (packageName != null) {
                    val category = detectAppCategory(packageName)
                    val appContext = CommandContext.App(packageName, activityName, category)
                    lastAppContext = appContext
                    return appContext
                }
            }

            return lastAppContext // Return last known if detection fails
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Failed to detect app context", e)
            return lastAppContext
        }
    }

    /**
     * Detect screen context from accessibility service
     * Analyzes UI hierarchy to extract screen information
     *
     * @param accessibilityService Accessibility service instance
     * @return Screen context or null if detection fails
     */
    fun detectScreenContext(accessibilityService: AccessibilityService): CommandContext.Screen? {
        try {
            val rootNode = accessibilityService.rootInActiveWindow ?: return lastScreenContext

            // Generate screen ID from package and activity
            val packageName = rootNode.packageName?.toString() ?: "unknown"
            val screenId = "$packageName:${rootNode.className}"

            // Extract key UI elements
            val elements = mutableListOf<String>()
            extractKeyElements(rootNode, elements, maxDepth = 3, maxElements = 20)

            // Detect UI capabilities
            val hasEditableFields = hasEditableFields(rootNode)
            val hasScrollableContent = hasScrollableContent(rootNode)
            val hasClickableElements = hasClickableElements(rootNode)

            val screenContext = CommandContext.Screen(
                screenId = screenId,
                elements = elements,
                hasEditableFields = hasEditableFields,
                hasScrollableContent = hasScrollableContent,
                hasClickableElements = hasClickableElements
            )

            lastScreenContext = screenContext
            return screenContext
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Failed to detect screen context", e)
            return lastScreenContext
        }
    }

    /**
     * Detect time context
     * Always succeeds as it uses system time
     *
     * @return Current time context
     */
    fun detectTimeContext(): CommandContext.Time {
        return CommandContext.Time.now()
    }

    /**
     * Detect location context
     * Uses last known location to determine location type
     * Requires ACCESS_FINE_LOCATION permission
     *
     * @return Location context or null if unavailable
     */
    fun detectLocationContext(): CommandContext.Location? {
        if (!locationDetectionEnabled) {
            return lastLocationContext
        }

        try {
            val locationManager = androidContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            // Try to get last known location
            @SuppressLint("MissingPermission")
            val lastLocation = if (ContextCompat.checkSelfPermission(
                    androidContext,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            } else {
                null
            }

            if (lastLocation != null) {
                // For now, use a simple heuristic based on speed
                // In production, you'd use geofencing or Places API
                val locationType = when {
                    lastLocation.speed > 10 -> LocationType.VEHICLE // Moving fast
                    lastLocation.accuracy > 100 -> LocationType.OUTDOOR // Low accuracy = outdoor
                    else -> LocationType.UNKNOWN
                }

                val locationContext = CommandContext.Location(locationType, confidence = 0.6f)
                lastLocationContext = locationContext
                return locationContext
            }

            return lastLocationContext
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Failed to detect location context", e)
            return lastLocationContext
        }
    }

    /**
     * Detect activity context
     * Uses Google Activity Recognition API
     * Requires ACTIVITY_RECOGNITION permission (Android 10+)
     *
     * @return Activity context or null if unavailable
     */
    suspend fun detectActivityContext(): CommandContext.Activity? {
        if (!activityDetectionEnabled || activityRecognitionClient == null) {
            return lastActivityContext
        }

        try {
            // Request current activity
            val detectedActivity = requestActivityUpdate()

            if (detectedActivity != null) {
                val activityType = mapDetectedActivityToType(detectedActivity.type)
                val confidence = detectedActivity.confidence / 100f // Convert 0-100 to 0.0-1.0

                if (confidence >= HIGH_CONFIDENCE_THRESHOLD) {
                    val activityContext = CommandContext.Activity(activityType, confidence)
                    lastActivityContext = activityContext
                    return activityContext
                }
            }

            return lastActivityContext
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Failed to detect activity context", e)
            return lastActivityContext
        }
    }

    /**
     * Detect app category from package name
     * Uses simple heuristics based on package name
     */
    private fun detectAppCategory(packageName: String): AppCategory {
        return when {
            packageName.contains("gmail") || packageName.contains("mail") ||
            packageName.contains("calendar") || packageName.contains("notes") ||
            packageName.contains("docs") || packageName.contains("sheets") -> AppCategory.PRODUCTIVITY

            packageName.contains("facebook") || packageName.contains("twitter") ||
            packageName.contains("instagram") || packageName.contains("tiktok") -> AppCategory.SOCIAL

            packageName.contains("youtube") || packageName.contains("spotify") ||
            packageName.contains("music") || packageName.contains("video") ||
            packageName.contains("netflix") -> AppCategory.MEDIA

            packageName.contains("messenger") || packageName.contains("whatsapp") ||
            packageName.contains("telegram") || packageName.contains("signal") ||
            packageName.contains("phone") -> AppCategory.COMMUNICATION

            packageName.contains("chrome") || packageName.contains("browser") ||
            packageName.contains("firefox") || packageName.contains("safari") -> AppCategory.BROWSER

            packageName.contains("amazon") || packageName.contains("ebay") ||
            packageName.contains("shop") || packageName.contains("store") -> AppCategory.SHOPPING

            packageName.contains("maps") || packageName.contains("navigation") ||
            packageName.contains("waze") -> AppCategory.NAVIGATION

            packageName.contains("game") || packageName.contains("play") -> AppCategory.GAMES

            packageName.startsWith("com.android") || packageName.startsWith("com.google.android") -> AppCategory.SYSTEM

            else -> AppCategory.UNKNOWN
        }
    }

    /**
     * Extract key UI elements from accessibility node tree
     */
    private fun extractKeyElements(
        node: AccessibilityNodeInfo?,
        elements: MutableList<String>,
        depth: Int = 0,
        maxDepth: Int = 3,
        maxElements: Int = 20
    ) {
        if (node == null || depth > maxDepth || elements.size >= maxElements) {
            return
        }

        // Extract text content
        node.text?.toString()?.let { text ->
            if (text.isNotBlank() && text.length < 50) {
                elements.add(text.trim())
            }
        }

        // Extract content description
        node.contentDescription?.toString()?.let { desc ->
            if (desc.isNotBlank() && desc.length < 50 && desc !in elements) {
                elements.add(desc.trim())
            }
        }

        // Recurse to children
        for (i in 0 until node.childCount) {
            if (elements.size >= maxElements) break
            node.getChild(i)?.let { child ->
                extractKeyElements(child, elements, depth + 1, maxDepth, maxElements)
            }
        }
    }

    /**
     * Check if node tree has editable fields
     */
    private fun hasEditableFields(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        if (node.isEditable) return true

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                if (hasEditableFields(child)) return true
            }
        }

        return false
    }

    /**
     * Check if node tree has scrollable content
     */
    private fun hasScrollableContent(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        if (node.isScrollable) return true

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                if (hasScrollableContent(child)) return true
            }
        }

        return false
    }

    /**
     * Check if node tree has clickable elements
     */
    private fun hasClickableElements(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        if (node.isClickable) return true

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                if (hasClickableElements(child)) return true
            }
        }

        return false
    }

    /**
     * Check if location permission is granted
     */
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            androidContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if activity recognition permission is granted
     */
    private fun hasActivityRecognitionPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                androidContext,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required before Android 10
        }
    }

    /**
     * Request activity update from Activity Recognition API
     * This is a stub implementation - in production you'd use Google Play Services
     */
    private suspend fun requestActivityUpdate(): DetectedActivityStub? = suspendCancellableCoroutine { continuation ->
        // Note: This is a stub implementation
        // In production, you'd register a PendingIntent to receive activity updates
        // For now, we return a default stationary activity
        continuation.resume(DetectedActivityStub(type = 3, confidence = 75)) // STILL = 3
    }

    /**
     * Map Google Activity Recognition type to our ActivityType
     */
    private fun mapDetectedActivityToType(activityType: Int): ActivityType {
        return when (activityType) {
            7 -> ActivityType.WALKING     // DetectedActivity.WALKING
            8 -> ActivityType.RUNNING     // DetectedActivity.RUNNING
            1 -> ActivityType.CYCLING     // DetectedActivity.ON_BICYCLE
            0 -> ActivityType.DRIVING     // DetectedActivity.IN_VEHICLE
            3 -> ActivityType.STATIONARY  // DetectedActivity.STILL
            5 -> ActivityType.TILTING     // DetectedActivity.TILTING
            else -> ActivityType.UNKNOWN
        }
    }

    /**
     * Stub for Google Play Services DetectedActivity
     * In production, use: com.google.android.gms.location.DetectedActivity
     */
    private data class DetectedActivityStub(
        val type: Int,
        val confidence: Int
    )

    /**
     * Cleanup resources
     */
    fun cleanup() {
        activityRecognitionClient = null
        android.util.Log.i(TAG, "ContextDetector cleaned up")
    }
}

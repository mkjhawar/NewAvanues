package com.augmentalis.voiceoscore.learnapp.window

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.augmentalis.voiceoscore.learnapp.detection.LauncherDetector
import kotlinx.coroutines.delay

/**
 * Multi-window detection and management system for Android accessibility scraping.
 *
 * Replaces the legacy single-window approach (getRootInActiveWindow()) with comprehensive
 * multi-window detection that captures:
 * - Main application windows
 * - Dialog overlays (AlertDialog, custom dialogs)
 * - Bottom sheets and drawer menus
 * - Dropdown menus that create overlay windows
 * - Floating UI elements (FABs, tooltips)
 * - System windows (filtered out)
 * - Launcher windows (filtered out)
 *
 * ## Problem This Solves
 *
 * ### Issue #2: Launcher Contamination
 * Single-window detection (getRootInActiveWindow()) only sees the "active" window, missing:
 * - Launcher screens during BACK recovery attempts
 * - Multiple windows belonging to the same app
 * - Overlay windows (dialogs, menus)
 *
 * Result: Launcher screens get scraped as part of app hierarchy (wrong package association)
 *
 * ### Issue #1: Premature Learning Completion (Partial)
 * Many UI elements hide in overlay windows:
 * - Dropdown menu items (create overlay window when expanded)
 * - Dialog content (separate window from main app)
 * - Bottom sheet content
 *
 * Result: Hidden elements never discovered, incomplete learning
 *
 * ## Architecture
 *
 * ```
 * AccessibilityService.getWindows()
 *          ↓
 *    WindowManager.getAppWindows()
 *          ↓
 *    ┌─────────────────┐
 *    │  Classify Each  │
 *    │     Window      │
 *    └─────────────────┘
 *          ↓
 *    ┌─────────────────────────────┐
 *    │  Filter by:                 │
 *    │  - Target package           │
 *    │  - Not launcher             │
 *    │  - Not system UI            │
 *    │  - Not input method         │
 *    └─────────────────────────────┘
 *          ↓
 *    ┌─────────────────────────────┐
 *    │  Return WindowInfo List     │
 *    │  (sorted by layer/z-order)  │
 *    └─────────────────────────────┘
 * ```
 *
 * ## Usage Example
 *
 * ```kotlin
 * val windowManager = WindowManager(accessibilityService)
 * val launcherDetector = LauncherDetector(context)
 *
 * // Get all windows for target package
 * val windows = windowManager.getAppWindows("com.microsoft.teams", launcherDetector)
 *
 * // Windows are sorted by layer (z-order), so iterate bottom-to-top
 * for (window in windows) {
 *     when (window.type) {
 *         WindowType.MAIN_APP -> {
 *             Log.d(TAG, "Main window: ${window.title}")
 *             scrapeWindow(window)
 *         }
 *         WindowType.DIALOG, WindowType.OVERLAY -> {
 *             Log.d(TAG, "Overlay window: ${window.title}")
 *             scrapeWindow(window) // Don't skip! Contains UI elements
 *         }
 *         WindowType.LAUNCHER -> {
 *             Log.w(TAG, "Launcher detected, skipping")
 *             // This shouldn't happen (filtered), but defensive
 *         }
 *     }
 * }
 * ```
 *
 * ## Thread Safety
 * Not thread-safe. Should be called from the main thread or AccessibilityService's handler thread.
 * AccessibilityWindowInfo and AccessibilityNodeInfo are not thread-safe.
 *
 * @param accessibilityService The accessibility service for querying windows
 *
 * @see LauncherDetector For launcher package filtering
 * @see com.augmentalis.voiceoscore.learnapp.exploration.ExplorationEngine Primary consumer
 */
class WindowManager(private val accessibilityService: AccessibilityService) {

    private val TAG = "WindowManager"

    /**
     * Classification types for detected windows.
     *
     * Determines how each window should be handled during scraping.
     */
    enum class WindowType {
        /**
         * Main application window (TYPE_APPLICATION).
         * Contains the primary UI of the target app.
         */
        MAIN_APP,

        /**
         * Overlay window (TYPE_APPLICATION_OVERLAY, TYPE_APPLICATION above main).
         * Includes dialogs, bottom sheets, dropdown menus, FABs.
         * MUST be scraped - contains important UI elements.
         */
        OVERLAY,

        /**
         * Dialog window (TYPE_APPLICATION with specific characteristics).
         * Modal dialogs, alert dialogs, confirmation prompts.
         * MUST be scraped - contains interactive elements.
         */
        DIALOG,

        /**
         * Launcher/home screen window.
         * Should be filtered out (not part of target app).
         */
        LAUNCHER,

        /**
         * System UI window (status bar, navigation bar, notifications).
         * Should be filtered out (not part of target app).
         */
        SYSTEM,

        /**
         * Input method window (keyboard, IME).
         * Should be filtered out (transient, not part of app hierarchy).
         */
        INPUT_METHOD,

        /**
         * Unknown/unclassified window type.
         * Treated conservatively (included in results for analysis).
         */
        UNKNOWN
    }

    /**
     * Metadata about a detected window.
     *
     * Contains all information needed to scrape and classify a window.
     *
     * @property window The AccessibilityWindowInfo from Android
     * @property type Classification of this window
     * @property packageName Package name of the window owner
     * @property rootNode Root node of the accessibility tree for this window
     * @property title Window title (for logging/debugging)
     * @property layer Z-order layer of the window (higher = on top)
     * @property isActive Whether this is the currently active/focused window
     * @property isFocused Whether this window has input focus
     * @property bounds Screen bounds of the window (for debugging)
     */
    data class WindowInfo(
        val window: AccessibilityWindowInfo,
        val type: WindowType,
        val packageName: String,
        val rootNode: AccessibilityNodeInfo?,
        val title: String,
        val layer: Int,
        val isActive: Boolean,
        val isFocused: Boolean,
        val bounds: android.graphics.Rect
    ) {
        /**
         * Checks if this window should be scraped.
         * Only LAUNCHER, SYSTEM, and INPUT_METHOD should be skipped.
         */
        fun shouldScrape(): Boolean {
            return when (type) {
                WindowType.MAIN_APP, WindowType.OVERLAY, WindowType.DIALOG, WindowType.UNKNOWN -> true
                WindowType.LAUNCHER, WindowType.SYSTEM, WindowType.INPUT_METHOD -> false
            }
        }

        /**
         * Returns a human-readable summary of this window for logging.
         */
        fun toLogString(): String {
            return "WindowInfo(type=$type, pkg=$packageName, title='$title', layer=$layer, active=$isActive, bounds=$bounds)"
        }
    }

    /**
     * Get all windows belonging to a specific app package WITH RETRY LOGIC.
     *
     * ## Why Retry Logic?
     *
     * When VoiceOSService starts, FLAG_RETRIEVE_INTERACTIVE_WINDOWS is set in onServiceConnected().
     * However, Android needs time (500-1500ms) to process this flag before windows become available.
     * During this period, accessibilityService.windows returns an empty list (not null!).
     *
     * This function implements exponential backoff retry to handle this race condition:
     * - Attempt 1: Immediate (0ms delay)
     * - Attempt 2: 200ms delay
     * - Attempt 3: 400ms delay
     * - Attempt 4: 800ms delay
     * - Attempt 5: 1600ms delay
     * Total: Up to ~3 seconds of retry attempts
     *
     * @param targetPackage Package name of the app to scrape
     * @param launcherDetector Launcher detector for filtering launcher packages
     * @param includeSystemWindows If true, includes SYSTEM and INPUT_METHOD windows
     * @param maxRetries Maximum number of retry attempts (default: 5)
     * @param initialDelayMs Initial delay in milliseconds (default: 200ms)
     * @return List of WindowInfo objects, or empty list if no windows found after all retries
     */
    suspend fun getAppWindowsWithRetry(
        targetPackage: String,
        launcherDetector: LauncherDetector,
        includeSystemWindows: Boolean = false,
        maxRetries: Int = 5,
        initialDelayMs: Long = 200L
    ): List<WindowInfo> {
        var attempt = 0
        var delayMs = 0L

        while (attempt < maxRetries) {
            attempt++

            if (delayMs > 0) {
                Log.d(TAG, "Retry attempt $attempt/$maxRetries after ${delayMs}ms delay")
                delay(delayMs)
            }

            val windows = getAppWindows(targetPackage, launcherDetector, includeSystemWindows)

            if (windows.isNotEmpty()) {
                if (attempt > 1) {
                    Log.i(TAG, "✅ Windows found on attempt $attempt/$maxRetries")
                }
                return windows
            }

            // Exponential backoff: 200ms, 400ms, 800ms, 1600ms
            delayMs = if (attempt == 1) initialDelayMs else delayMs * 2

            Log.v(TAG, "No windows found (attempt $attempt/$maxRetries), retrying in ${delayMs}ms...")
        }

        Log.w(TAG, "❌ No windows found after $maxRetries attempts")
        return emptyList()
    }

    /**
     * Gets all windows belonging to the target app package.
     *
     * This is the primary API for multi-window detection. Replaces the legacy single-window
     * approach with comprehensive multi-window scanning.
     *
     * ## Process
     * 1. Query AccessibilityService.getWindows() for ALL windows
     * 2. For each window:
     *    - Check if package matches target package
     *    - Check if package is launcher (filter out)
     *    - Check if package is system UI (filter out)
     *    - Classify window type (MAIN_APP, OVERLAY, DIALOG, etc.)
     * 3. Sort windows by layer (z-order) - bottom to top
     * 4. Return list of WindowInfo objects
     *
     * ## Filtering Logic
     * - ✅ Include: Windows with packageName == targetPackage
     * - ✅ Include: MAIN_APP, OVERLAY, DIALOG, UNKNOWN types
     * - ❌ Exclude: Launcher packages (detected by LauncherDetector)
     * - ❌ Exclude: System UI packages (com.android.systemui)
     * - ❌ Exclude: Input method windows (keyboards)
     * - ❌ Exclude: Windows without root node (can't be scraped)
     *
     * ## Edge Cases Handled
     * - No windows available (returns empty list)
     * - Windows without root nodes (skipped)
     * - Windows with null package names (skipped)
     * - Multiple overlays stacked (all included, sorted by layer)
     * - Launcher appears during BACK recovery (filtered out)
     *
     * ## Performance
     * - O(n) where n = total windows on screen (~5-20 typically)
     * - Each window: 1 root node access + package name comparison
     * - Typical execution: 5-15ms
     *
     * @param targetPackage Package name of the app to scrape (e.g., "com.microsoft.teams")
     * @param launcherDetector Launcher detector for filtering launcher packages
     * @param includeSystemWindows If true, includes SYSTEM and INPUT_METHOD windows (for debugging)
     * @return List of WindowInfo objects belonging to target app, sorted by layer (bottom to top)
     */
    fun getAppWindows(
        targetPackage: String,
        launcherDetector: LauncherDetector,
        includeSystemWindows: Boolean = false
    ): List<WindowInfo> {
        try {
            // Get all windows from accessibility service
            val allWindows = accessibilityService.windows
            if (allWindows == null || allWindows.isEmpty()) {
                Log.v(TAG, "No windows available from AccessibilityService")
                return emptyList()
            }

            Log.v(TAG, "🔍 Scanning ${allWindows.size} windows for package: $targetPackage")

            val appWindows = mutableListOf<WindowInfo>()

            for (window in allWindows) {
                try {
                    // Get root node (required for scraping)
                    val rootNode = window.root
                    if (rootNode == null) {
                        Log.v(TAG, "   ⏭️ Window has no root node, skipping")
                        continue
                    }

                    // Get package name
                    val windowPackage = rootNode.packageName?.toString()
                    if (windowPackage == null) {
                        Log.v(TAG, "   ⏭️ Window has no package name, skipping")
                        continue
                    }

                    // Check if this window belongs to target package
                    if (windowPackage != targetPackage) {
                        Log.v(TAG, "   ⏭️ Window package '$windowPackage' != target '$targetPackage', skipping")
                        continue
                    }

                    // Check if this is a launcher (should never scrape launchers)
                    if (launcherDetector.isLauncher(windowPackage)) {
                        Log.w(TAG, "   🏠 Launcher window detected: $windowPackage (FILTERED)")
                        continue
                    }

                    // Classify window type
                    val windowType = classifyWindow(window, windowPackage, launcherDetector)

                    // Filter system windows unless explicitly requested
                    if (!includeSystemWindows) {
                        if (windowType == WindowType.SYSTEM || windowType == WindowType.INPUT_METHOD) {
                            Log.v(TAG, "   ⏭️ System/IME window, skipping (type: $windowType)")
                            continue
                        }
                    }

                    // Get window metadata
                    val title = window.title?.toString() ?: "(no title)"
                    val bounds = android.graphics.Rect()
                    window.getBoundsInScreen(bounds)

                    // Create WindowInfo
                    val windowInfo = WindowInfo(
                        window = window,
                        type = windowType,
                        packageName = windowPackage,
                        rootNode = rootNode,
                        title = title,
                        layer = window.layer,
                        isActive = window.isActive,
                        isFocused = window.isFocused,
                        bounds = bounds
                    )

                    appWindows.add(windowInfo)
                    Log.d(TAG, "   ✅ Found window: ${windowInfo.toLogString()}")

                } catch (e: Exception) {
                    Log.e(TAG, "   ❌ Error processing window", e)
                    // Continue to next window (don't let one bad window break everything)
                }
            }

            // Sort windows by layer (z-order) - bottom to top
            // Lower layer number = behind, higher layer number = in front
            appWindows.sortBy { it.layer }

            Log.i(TAG, "📊 Found ${appWindows.size} window(s) for package: $targetPackage")
            if (appWindows.isNotEmpty()) {
                Log.i(TAG, "   Types: ${appWindows.groupingBy { it.type }.eachCount()}")
            }

            return appWindows

        } catch (e: Exception) {
            Log.e(TAG, "❌ Fatal error in getAppWindows()", e)
            return emptyList() // Graceful degradation
        }
    }

    /**
     * Classifies a window into one of the WindowType categories.
     *
     * Uses multiple heuristics to determine window purpose:
     * - Android window type (TYPE_APPLICATION, TYPE_APPLICATION_OVERLAY, etc.)
     * - Package name (launcher check, system UI check)
     * - Window properties (layer, title, bounds)
     *
     * ## Classification Logic
     *
     * 1. **INPUT_METHOD** - TYPE_INPUT_METHOD (keyboards)
     * 2. **LAUNCHER** - Package is in launcher detector list
     * 3. **SYSTEM** - Package is system UI (com.android.systemui)
     * 4. **OVERLAY** - TYPE_APPLICATION_OVERLAY or high layer number
     * 5. **DIALOG** - Small bounds + TYPE_APPLICATION (modal dialogs)
     * 6. **MAIN_APP** - TYPE_APPLICATION with normal layer
     * 7. **UNKNOWN** - Fallback for unrecognized patterns
     *
     * @param window The AccessibilityWindowInfo to classify
     * @param packageName Package name of the window
     * @param launcherDetector Launcher detector for launcher identification
     * @return WindowType classification
     */
    private fun classifyWindow(
        window: AccessibilityWindowInfo,
        packageName: String,
        launcherDetector: LauncherDetector
    ): WindowType {
        // Check window type from Android
        val androidType = window.type

        // 1. Input method (keyboard)
        if (androidType == AccessibilityWindowInfo.TYPE_INPUT_METHOD) {
            return WindowType.INPUT_METHOD
        }

        // 2. Launcher (home screen)
        if (launcherDetector.isLauncher(packageName)) {
            return WindowType.LAUNCHER
        }

        // 3. System UI
        if (LauncherDetector.SYSTEM_UI_PACKAGES.contains(packageName)) {
            return WindowType.SYSTEM
        }

        // 4. Overlay window
        // TYPE_APPLICATION_OVERLAY was added in API 26
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
            androidType == 0x00000004 /* AccessibilityWindowInfo.TYPE_APPLICATION_OVERLAY */) {
            return WindowType.OVERLAY
        }

        // 5. Application window - further classify
        if (androidType == AccessibilityWindowInfo.TYPE_APPLICATION) {
            // Check if this is likely a dialog or overlay based on layer
            // Main app windows are typically on lower layers (0-5)
            // Overlays/dialogs are on higher layers (>5)
            if (window.layer > 5) {
                return WindowType.OVERLAY
            }

            // Check if small bounds suggest a dialog
            val bounds = android.graphics.Rect()
            window.getBoundsInScreen(bounds)
            val width = bounds.width()
            val height = bounds.height()

            // Very small windows are likely dialogs or floating UI
            if (width < 800 || height < 600) {
                return WindowType.DIALOG
            }

            // Default: main app window
            return WindowType.MAIN_APP
        }

        // 6. System window (other types)
        if (androidType == AccessibilityWindowInfo.TYPE_SYSTEM) {
            return WindowType.SYSTEM
        }

        // 7. Accessibility overlay (rarely used)
        if (androidType == AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY) {
            return WindowType.OVERLAY
        }

        // 8. Split screen (treat as main app)
        if (androidType == AccessibilityWindowInfo.TYPE_SPLIT_SCREEN_DIVIDER) {
            return WindowType.SYSTEM
        }

        // Unknown type
        Log.w(TAG, "⚠️ Unknown window type: $androidType for package: $packageName")
        return WindowType.UNKNOWN
    }

    /**
     * Gets the currently active (focused) window for the target package.
     *
     * Convenience method for getting just the active window instead of all windows.
     * Useful for simpler scraping scenarios where only the foreground window matters.
     *
     * ## When to Use
     * - Quick scraping of currently visible content
     * - Single-screen apps without overlays
     * - Performance-critical paths where full multi-window scan is expensive
     *
     * ## When NOT to Use
     * - Apps with dialogs, bottom sheets, or overlays (will miss content)
     * - LearnApp comprehensive learning (use getAppWindows instead)
     * - Any scenario requiring complete UI discovery
     *
     * @param targetPackage Package name of the app
     * @param launcherDetector Launcher detector for filtering
     * @return WindowInfo of active window, or null if not found
     */
    fun getActiveWindow(
        targetPackage: String,
        launcherDetector: LauncherDetector
    ): WindowInfo? {
        val windows = getAppWindows(targetPackage, launcherDetector)
        return windows.firstOrNull { it.isActive }
    }

    /**
     * Gets diagnostic information about all windows currently on screen.
     *
     * Useful for debugging, logging, and understanding window state.
     *
     * @return Map containing:
     *         - "totalWindows": Total windows from AccessibilityService
     *         - "windowsByPackage": Map of package name to window count
     *         - "windowsByType": Map of Android window type to count
     */
    fun getDiagnostics(): Map<String, Any> {
        try {
            val allWindows = accessibilityService.windows ?: emptyList()
            val windowsByPackage = mutableMapOf<String, Int>()
            val windowsByType = mutableMapOf<Int, Int>()

            for (window in allWindows) {
                val pkg = window.root?.packageName?.toString() ?: "(unknown)"
                windowsByPackage[pkg] = (windowsByPackage[pkg] ?: 0) + 1

                val type = window.type
                windowsByType[type] = (windowsByType[type] ?: 0) + 1
            }

            return mapOf(
                "totalWindows" to allWindows.size,
                "windowsByPackage" to windowsByPackage,
                "windowsByType" to windowsByType
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting diagnostics", e)
            return mapOf("error" to e.message.orEmpty())
        }
    }

    companion object {
        /**
         * Window layer thresholds for classification heuristics.
         */
        const val OVERLAY_LAYER_THRESHOLD = 5  // Windows above this layer are likely overlays
        const val DIALOG_MIN_WIDTH = 800       // Pixels - smaller windows are likely dialogs
        const val DIALOG_MIN_HEIGHT = 600      // Pixels - smaller windows are likely dialogs
    }
}

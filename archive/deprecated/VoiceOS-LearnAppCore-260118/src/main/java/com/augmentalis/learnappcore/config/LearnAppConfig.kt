/**
 * LearnAppConfig.kt - Configuration data class for LearnAppPro
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-12-11
 * Updated: 2025-12-11 (v1.0 - Initial configuration externalization)
 *
 * Configuration container for LearnAppPro exploration parameters.
 * Replaces hardcoded values with configurable settings that can be
 * persisted and modified at runtime.
 *
 * ## Architecture Improvement: Phase 5
 *
 * This is part of the LearnAppPro architecture improvement plan that
 * externalizes all configuration values to:
 * - Enable runtime configuration changes
 * - Support A/B testing of exploration strategies
 * - Allow per-app custom settings
 * - Facilitate debugging and tuning
 *
 * ## Configuration Categories
 *
 * 1. **Thresholds** - Quality filters and limits
 * 2. **Timeouts** - Timing and delays
 * 3. **Safety** - Feature flags for safety systems
 * 4. **Fallback** - Grid sizes for game engines
 * 5. **Logging** - Debug output control
 *
 * ## Usage
 *
 * ```kotlin
 * // Use default configuration
 * val config = LearnAppConfig.DEFAULT
 *
 * // Create custom configuration
 * val customConfig = LearnAppConfig(
 *     minLabelLength = 2,
 *     maxBatchSize = 200,
 *     verboseLogging = true
 * )
 *
 * // Modify specific values
 * val modifiedConfig = config.copy(
 *     screenChangeTimeoutMs = 5000,
 *     enableLoopDetection = false
 * )
 * ```
 *
 * @property minLabelLength Minimum character length for generated labels (filters noise)
 * @property maxBatchSize Maximum voice commands per batch insert (memory management)
 * @property maxScrollCount Maximum scroll attempts before giving up (infinite scroll protection)
 * @property maxScreenVisits Maximum visits to same screen before marking as explored
 * @property screenChangeTimeoutMs Milliseconds to wait for screen transitions
 * @property actionDelayMs Milliseconds to wait after performing actions
 * @property scrollDelayMs Milliseconds to wait between scroll operations
 * @property enableDoNotClick Enable Do-Not-Click safety system (destructive action detection)
 * @property enableDynamicDetection Enable dynamic content detection (ads, carousels, live data)
 * @property enableLoopDetection Enable infinite loop detection (navigation cycles)
 * @property unityGridSize Grid size for Unity game fallback labeling (3x3)
 * @property unrealGridSize Grid size for Unreal Engine fallback labeling (4x4)
 * @property verboseLogging Enable detailed debug logging
 *
 * @since 1.0.0
 */

package com.augmentalis.learnappcore.config

/**
 * LearnAppPro configuration data class
 *
 * Immutable configuration container with sensible defaults.
 * Use [copy] to create modified configurations.
 */
data class LearnAppConfig(
    // ========== THRESHOLDS ==========

    /**
     * Minimum label length for voice command generation
     *
     * Filters out noise like single characters, emojis, and numeric-only labels.
     *
     * Default: 3 characters
     * Range: 1-10 characters
     * Impact: Lower values generate more commands (more noise), higher values miss short labels
     */
    val minLabelLength: Int = 3,

    /**
     * Maximum voice commands per batch insert
     *
     * Controls memory usage during exploration. Auto-flush when reached.
     *
     * Default: 100 commands
     * Range: 10-1000 commands
     * Impact: Higher values use more memory, lower values cause more frequent flushes
     */
    val maxBatchSize: Int = 100,

    /**
     * Maximum scroll attempts per scrollable container
     *
     * Protects against infinite scroll (social media feeds, news apps).
     *
     * Default: 20 scrolls
     * Range: 5-100 scrolls
     * Impact: Higher values explore more content, lower values prevent timeout on infinite feeds
     */
    val maxScrollCount: Int = 20,

    /**
     * Maximum visits to same screen before marking as fully explored
     *
     * Prevents getting stuck on dynamic screens (login, loading, error states).
     *
     * Default: 3 visits
     * Range: 1-10 visits
     * Impact: Higher values re-explore dynamic screens, lower values skip screens faster
     */
    val maxScreenVisits: Int = 3,

    // ========== TIMEOUTS ==========

    /**
     * Screen transition timeout in milliseconds
     *
     * How long to wait for screen changes after actions (clicks, scrolls).
     *
     * Default: 3000ms (3 seconds)
     * Range: 1000-10000ms (1-10 seconds)
     * Impact: Higher values wait longer for slow apps, lower values may miss transitions
     */
    val screenChangeTimeoutMs: Long = 3000,

    /**
     * Action delay in milliseconds
     *
     * Delay after clicks, long presses, etc. to allow UI to settle.
     *
     * Default: 300ms
     * Range: 100-2000ms
     * Impact: Higher values slow exploration, lower values may miss UI state changes
     */
    val actionDelayMs: Long = 300,

    /**
     * Scroll delay in milliseconds
     *
     * Delay between consecutive scroll operations.
     *
     * Default: 300ms
     * Range: 100-2000ms
     * Impact: Higher values slow scroll exploration, lower values may cause scroll conflicts
     */
    val scrollDelayMs: Long = 300,

    // ========== SAFETY ==========

    /**
     * Enable Do-Not-Click safety system
     *
     * Detects and skips destructive actions:
     * - Logout/Sign out buttons
     * - Delete/Remove buttons
     * - Uninstall/Clear data
     * - Purchase/Buy buttons
     * - Share/Send buttons
     *
     * Default: true (enabled)
     * Impact: Disable only for testing, exploration will skip safety-critical elements
     */
    val enableDoNotClick: Boolean = true,

    /**
     * Enable dynamic content detection
     *
     * Detects and handles dynamic regions:
     * - Advertisements (skip)
     * - Carousels (scroll through)
     * - Live data (timestamps, counters)
     * - Infinite scrolls (limit visits)
     *
     * Default: true (enabled)
     * Impact: Disable to explore dynamic content (may cause loops)
     */
    val enableDynamicDetection: Boolean = true,

    /**
     * Enable loop detection system
     *
     * Detects and breaks out of navigation loops:
     * - Screen visit tracking
     * - Navigation path analysis
     * - Back button recovery
     *
     * Default: true (enabled)
     * Impact: Disable only for debugging, exploration may get stuck
     */
    val enableLoopDetection: Boolean = true,

    // ========== FALLBACK GRID SIZES ==========

    /**
     * Grid size for Unity game fallback labeling
     *
     * Unity apps lack accessibility tree, so we use spatial grid labeling.
     * Grid divides screen into NxN regions for voice commands.
     *
     * Default: 3 (3x3 grid = 9 regions)
     * Range: 2-5 (4-25 regions)
     * Impact: Higher values provide more granular control, lower values simpler commands
     *
     * Examples (3x3):
     * - "Top Left Button"
     * - "Center Middle Button"
     * - "Bottom Right Icon"
     */
    val unityGridSize: Int = 3,

    /**
     * Grid size for Unreal Engine fallback labeling
     *
     * Unreal apps have more complex UI than Unity (AAA mobile games).
     * Uses finer grid with edge/corner detection for HUD elements.
     *
     * Default: 4 (4x4 grid = 16 regions)
     * Range: 3-6 (9-36 regions)
     * Impact: Higher values for complex games (PUBG, Fortnite), lower for simpler games
     *
     * Examples (4x4):
     * - "Corner Top Far Left Button"
     * - "Edge Lower Right Icon"
     * - "Large Center Widget"
     */
    val unrealGridSize: Int = 4,

    // ========== LOGGING ==========

    /**
     * Enable verbose debug logging
     *
     * Logs detailed information:
     * - Element processing steps
     * - UUID generation
     * - Voice command creation
     * - Batch operations
     * - Safety decisions
     *
     * Default: false (disabled)
     * Impact: Enable for debugging, slows exploration and fills logs
     */
    val verboseLogging: Boolean = false
) {
    companion object {
        /**
         * Default configuration with sensible values
         *
         * Recommended for most apps. Balances exploration speed,
         * memory usage, and safety.
         */
        val DEFAULT = LearnAppConfig()

        /**
         * Fast exploration configuration
         *
         * Optimized for speed over thoroughness.
         * Use for quick app scans or simple apps.
         */
        val FAST = LearnAppConfig(
            minLabelLength = 2,
            maxBatchSize = 200,
            maxScrollCount = 10,
            maxScreenVisits = 2,
            screenChangeTimeoutMs = 2000,
            actionDelayMs = 200,
            scrollDelayMs = 200
        )

        /**
         * Thorough exploration configuration
         *
         * Optimized for completeness over speed.
         * Use for complex apps or when you need full coverage.
         */
        val THOROUGH = LearnAppConfig(
            minLabelLength = 2,
            maxBatchSize = 50,
            maxScrollCount = 50,
            maxScreenVisits = 5,
            screenChangeTimeoutMs = 5000,
            actionDelayMs = 500,
            scrollDelayMs = 500
        )

        /**
         * Debug configuration
         *
         * Enables verbose logging for troubleshooting.
         * Use during development or when investigating issues.
         */
        val DEBUG = LearnAppConfig(
            verboseLogging = true,
            enableLoopDetection = true,
            enableDynamicDetection = true,
            enableDoNotClick = true
        )

        /**
         * Unsafe configuration
         *
         * Disables all safety systems for testing.
         * WARNING: May trigger destructive actions!
         */
        val UNSAFE = LearnAppConfig(
            enableDoNotClick = false,
            enableDynamicDetection = false,
            enableLoopDetection = false
        )
    }

    /**
     * Validate configuration values
     *
     * Ensures all values are within acceptable ranges.
     * Throws [IllegalArgumentException] if validation fails.
     */
    fun validate() {
        require(minLabelLength in 1..10) {
            "minLabelLength must be between 1 and 10, got $minLabelLength"
        }
        require(maxBatchSize in 10..1000) {
            "maxBatchSize must be between 10 and 1000, got $maxBatchSize"
        }
        require(maxScrollCount in 5..100) {
            "maxScrollCount must be between 5 and 100, got $maxScrollCount"
        }
        require(maxScreenVisits in 1..10) {
            "maxScreenVisits must be between 1 and 10, got $maxScreenVisits"
        }
        require(screenChangeTimeoutMs in 1000..10000) {
            "screenChangeTimeoutMs must be between 1000 and 10000, got $screenChangeTimeoutMs"
        }
        require(actionDelayMs in 100..2000) {
            "actionDelayMs must be between 100 and 2000, got $actionDelayMs"
        }
        require(scrollDelayMs in 100..2000) {
            "scrollDelayMs must be between 100 and 2000, got $scrollDelayMs"
        }
        require(unityGridSize in 2..5) {
            "unityGridSize must be between 2 and 5, got $unityGridSize"
        }
        require(unrealGridSize in 3..6) {
            "unrealGridSize must be between 3 and 6, got $unrealGridSize"
        }
    }

    /**
     * Get configuration summary for logging
     *
     * Returns human-readable summary of key settings.
     */
    fun toSummary(): String = buildString {
        appendLine("LearnAppConfig:")
        appendLine("  Thresholds: minLabel=$minLabelLength, maxBatch=$maxBatchSize, maxScroll=$maxScrollCount")
        appendLine("  Timeouts: screen=${screenChangeTimeoutMs}ms, action=${actionDelayMs}ms, scroll=${scrollDelayMs}ms")
        appendLine("  Safety: doNotClick=$enableDoNotClick, dynamic=$enableDynamicDetection, loop=$enableLoopDetection")
        appendLine("  Grids: unity=${unityGridSize}x$unityGridSize, unreal=${unrealGridSize}x$unrealGridSize")
        appendLine("  Logging: verbose=$verboseLogging")
    }
}

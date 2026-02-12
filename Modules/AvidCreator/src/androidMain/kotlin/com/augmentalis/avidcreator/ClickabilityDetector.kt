package com.augmentalis.avidcreator.core

import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat

/**
 * Multi-signal clickability detector for Android accessibility elements
 *
 * Implements smart clickability detection using 5 weighted signals:
 * 1. isClickable flag (weight: 1.0) - Explicit Android clickability
 * 2. isFocusable flag (weight: 0.3) - Often indicates interactive elements
 * 3. ACTION_CLICK present (weight: 0.4) - Has click action handler
 * 4. Clickable resource ID hints (weight: 0.2) - Resource name patterns
 * 5. Clickable container detection (weight: 0.3) - Container with click hints
 *
 * **Performance**: <10ms per element (optimized for real-time exploration)
 *
 * @property context Application context for resource access
 */
class ClickabilityDetector(
    private val context: Context
) {

    companion object {
        private const val TAG = "ClickabilityDetector"

        /**
         * Threshold for clickability score (0.0 - 1.0+)
         * Elements scoring >= 0.5 are considered clickable
         */
        const val CLICKABILITY_THRESHOLD = 0.5

        /**
         * Signal weights (must remain consistent for scoring)
         */
        private const val WEIGHT_EXPLICIT_CLICKABLE = 1.0     // isClickable=true
        private const val WEIGHT_FOCUSABLE = 0.3              // isFocusable=true
        private const val WEIGHT_CLICK_ACTION = 0.4           // ACTION_CLICK present
        private const val WEIGHT_RESOURCE_ID = 0.2            // Clickable ID pattern
        private const val WEIGHT_CONTAINER = 0.3              // Container with hints

        /**
         * Container types that may be clickable despite isClickable=false
         */
        private val CONTAINER_TYPES = setOf(
            "android.widget.LinearLayout",
            "android.widget.FrameLayout",
            "android.widget.RelativeLayout",
            "android.widget.ConstraintLayout",
            "androidx.cardview.widget.CardView",
            "com.google.android.material.card.MaterialCardView",
            "androidx.constraintlayout.widget.ConstraintLayout"
        )

        /**
         * Resource ID patterns that indicate clickability
         * Examples: "button_submit", "btn_ok", "tab_home", "card_item"
         */
        private val CLICKABLE_RESOURCE_PATTERNS = setOf(
            "button",
            "btn",
            "tab",
            "card",
            "item",
            "action",
            "clickable",
            "select",
            "choose",
            "option"
        )
    }

    /**
     * Calculate clickability score using multi-signal detection
     *
     * Fast path: If isClickable=true → immediate return (score=1.0)
     * Otherwise: Evaluate 4 remaining signals and combine scores
     *
     * Enhanced for cross-platform apps:
     * - Boosts clickable elements in Flutter/React Native/Cordova apps
     * - Adds cross-platform framework bonus to score
     *
     * **Performance**: Target <10ms per element
     *
     * @param element Accessibility node to evaluate
     * @param isGameEngine True if app is Unity/Unreal game engine
     * @param needsCrossPlatformBoost True if app is cross-platform (Flutter/RN/Cordova)
     * @return Clickability score with confidence level and reasoning
     */
    fun calculateScore(
        element: AccessibilityNodeInfo,
        isGameEngine: Boolean = false,
        needsCrossPlatformBoost: Boolean = false
    ): ClickabilityScore {
        val startTime = System.nanoTime()

        try {
            // GAME ENGINE SPECIAL CASE: Unity/Unreal render to graphics surface
            // Elements with meaningful bounds are likely clickable
            if (isGameEngine) {
                return calculateGameEngineClickability(element, startTime)
            }

            // SIGNAL 1: Explicit isClickable flag (fast path)
            if (element.isClickable) {
                return ClickabilityScore(
                    score = WEIGHT_EXPLICIT_CLICKABLE,
                    confidence = ClickabilityConfidence.EXPLICIT,
                    reasons = listOf("isClickable=true"),
                    computationTimeNs = System.nanoTime() - startTime
                )
            }

            // Multi-signal scoring for non-explicit elements
            var score = 0.0
            val reasons = mutableListOf<String>()

            // SIGNAL 2: Focusable flag
            if (element.isFocusable) {
                score += WEIGHT_FOCUSABLE
                reasons.add("isFocusable=true (+${WEIGHT_FOCUSABLE})")
            }

            // SIGNAL 3: Click action present
            if (hasClickAction(element)) {
                score += WEIGHT_CLICK_ACTION
                reasons.add("hasClickAction (+${WEIGHT_CLICK_ACTION})")
            }

            // SIGNAL 4: Resource ID hints
            val resourceId = element.viewIdResourceName
            if (hasClickableResourceId(resourceId)) {
                score += WEIGHT_RESOURCE_ID
                reasons.add("clickableResourceId=$resourceId (+${WEIGHT_RESOURCE_ID})")
            }

            // SIGNAL 5: Clickable container
            if (isClickableContainer(element)) {
                score += WEIGHT_CONTAINER
                reasons.add("clickableContainer (+${WEIGHT_CONTAINER})")
            }

            // SIGNAL 6: Cross-platform framework boost (NEW)
            // Give extra weight to clickable elements in cross-platform apps
            // because they often lack other signals (labels, resource IDs)
            if (element.isClickable && needsCrossPlatformBoost) {
                val boost = 0.3
                score += boost
                reasons.add("crossPlatformBoost (+$boost)")
            }

            // Determine confidence level
            val confidence = when {
                score >= 0.9 -> ClickabilityConfidence.HIGH
                score >= 0.7 -> ClickabilityConfidence.MEDIUM
                score >= CLICKABILITY_THRESHOLD -> ClickabilityConfidence.LOW
                else -> ClickabilityConfidence.NONE
            }

            val computationTime = System.nanoTime() - startTime
            logScore(element, score, confidence, reasons, computationTime)

            return ClickabilityScore(score, confidence, reasons, computationTime)

        } catch (e: Exception) {
            Log.e(TAG, "Error calculating clickability score", e)
            return ClickabilityScore(
                score = 0.0,
                confidence = ClickabilityConfidence.NONE,
                reasons = listOf("Error: ${e.message}"),
                computationTimeNs = System.nanoTime() - startTime
            )
        }
    }

    /**
     * Calculate clickability for Unity/Unreal game engines
     *
     * Unity and Unreal apps render entire UI to OpenGL/Vulkan surface.
     * The accessibility framework sees VERY FEW elements (often just 1-2).
     * Individual buttons/menus are NOT exposed to accessibility.
     *
     * Strategy:
     * - HIGH base score (0.6) for ANY element with meaningful bounds
     * - EXPLICIT score (1.0) if element is marked clickable
     * - Use bounds size as clickability signal (larger = more likely interactive)
     *
     * This aggressive approach is necessary because Unity games have
     * almost NO accessibility metadata to work with.
     *
     * @param element Element to score
     * @param startTime Start timestamp for performance tracking
     * @return Clickability score
     */
    private fun calculateGameEngineClickability(
        element: AccessibilityNodeInfo,
        startTime: Long
    ): ClickabilityScore {
        var score = 0.0
        val reasons = mutableListOf<String>()

        // Game engine elements marked clickable are DEFINITELY clickable
        if (element.isClickable) {
            score = 1.0
            reasons.add("isClickable=true (game_engine)")
            return ClickabilityScore(
                score = score,
                confidence = ClickabilityConfidence.EXPLICIT,
                reasons = reasons,
                computationTimeNs = System.nanoTime() - startTime
            )
        }

        // Check for meaningful bounds
        if (hasMeaningfulBounds(element)) {
            score += 0.6  // High base score for game engine elements with bounds
            reasons.add("game_engine_bounds (+0.6)")
        }

        // Boost score for focusable elements
        if (element.isFocusable) {
            score += 0.3
            reasons.add("isFocusable (+0.3)")
        }

        // Boost for click action
        if (hasClickAction(element)) {
            score += 0.4
            reasons.add("hasClickAction (+0.4)")
        }

        // Game engine HUD pattern: Boost for edge/corner elements (common HUD pattern)
        if (isEdgeOrCornerElement(element)) {
            score += 0.1
            reasons.add("game_hud_position (+0.1)")
        }

        // Size-based boost (larger elements more likely to be interactive)
        val sizeBoost = calculateSizeBoost(element)
        if (sizeBoost > 0) {
            score += sizeBoost
            reasons.add("sizeBoost (+$sizeBoost)")
        }

        val confidence = when {
            score >= 0.9 -> ClickabilityConfidence.HIGH
            score >= 0.7 -> ClickabilityConfidence.MEDIUM
            score >= CLICKABILITY_THRESHOLD -> ClickabilityConfidence.LOW
            else -> ClickabilityConfidence.NONE
        }

        val computationTime = System.nanoTime() - startTime
        logScore(element, score, confidence, reasons, computationTime)

        return ClickabilityScore(score, confidence, reasons, computationTime)
    }

    /**
     * Check if element has meaningful bounds for Unity detection
     *
     * Unity elements must be at least 20dp x 20dp to be considered interactive.
     * Smaller elements are likely decorative or text.
     *
     * @param element Element to check
     * @return true if bounds are large enough to be interactive
     */
    private fun hasMeaningfulBounds(element: AccessibilityNodeInfo): Boolean {
        val bounds = Rect()
        element.getBoundsInScreen(bounds)

        // Meaningful if larger than 20dp x 20dp
        val density = context.resources.displayMetrics.density
        val minSize = (20 * density).toInt()

        return bounds.width() >= minSize && bounds.height() >= minSize
    }

    /**
     * Calculate size-based clickability boost
     *
     * Larger elements are more likely to be interactive in Unity games.
     * Boost scale: 0.0 - 0.2 based on element size.
     *
     * @param element Element to analyze
     * @return Size boost (0.0 - 0.2)
     */
    private fun calculateSizeBoost(element: AccessibilityNodeInfo): Double {
        val bounds = Rect()
        element.getBoundsInScreen(bounds)

        val elementArea = bounds.width() * bounds.height()
        val screenWidth = context.resources.displayMetrics.widthPixels
        val screenHeight = context.resources.displayMetrics.heightPixels
        val screenArea = screenWidth * screenHeight

        // Calculate size ratio
        val sizeRatio = elementArea.toFloat() / screenArea.toFloat()

        return when {
            sizeRatio > 0.25 -> 0.0  // Too large (likely container)
            sizeRatio > 0.1 -> 0.2   // Large element (likely button)
            sizeRatio > 0.05 -> 0.15  // Medium element
            sizeRatio > 0.02 -> 0.1   // Small element
            else -> 0.0  // Too small (likely decorative)
        }
    }

    /**
     * Check if element is at screen edge or corner
     *
     * Common pattern for Unreal Engine HUD elements (health bars, mini-maps, buttons).
     * Elements within 50px of screen edge are considered edge elements.
     * Elements in corner quadrants are considered corner elements.
     *
     * @param element Node to check
     * @return true if element is at edge or corner
     */
    private fun isEdgeOrCornerElement(element: AccessibilityNodeInfo): Boolean {
        val bounds = Rect()
        element.getBoundsInScreen(bounds)

        val screenWidth = context.resources.displayMetrics.widthPixels
        val screenHeight = context.resources.displayMetrics.heightPixels
        val edgeThreshold = 50  // pixels

        // Check if at edge
        return bounds.left < edgeThreshold ||
                bounds.right > screenWidth - edgeThreshold ||
                bounds.top < edgeThreshold ||
                bounds.bottom > screenHeight - edgeThreshold
    }

    /**
     * Check if element has ACTION_CLICK in its action list
     *
     * @param element Node to check
     * @return true if ACTION_CLICK present
     */
    private fun hasClickAction(element: AccessibilityNodeInfo): Boolean {
        return try {
            element.actionList?.any { action ->
                action.id == AccessibilityNodeInfo.ACTION_CLICK
            } ?: false
        } catch (e: Exception) {
            Log.w(TAG, "Error checking click action", e)
            false
        }
    }

    /**
     * Check if resource ID contains clickable patterns
     *
     * Examples:
     * - "com.example:id/button_submit" → true (contains "button")
     * - "com.example:id/tab_home" → true (contains "tab")
     * - "com.example:id/text_label" → false (no clickable pattern)
     *
     * @param resourceId Full resource ID string
     * @return true if matches clickable pattern
     */
    private fun hasClickableResourceId(resourceId: String?): Boolean {
        if (resourceId.isNullOrBlank()) return false

        val lowercaseId = resourceId.lowercase()
        return CLICKABLE_RESOURCE_PATTERNS.any { pattern ->
            lowercaseId.contains(pattern)
        }
    }

    /**
     * Detect if container should be clickable based on heuristics
     *
     * Criteria:
     * 1. Must be a known container type (LinearLayout, CardView, etc.)
     * 2. Must have at least one clickability hint:
     *    - isFocusable=true
     *    - ACTION_CLICK present
     *    - Clickable resource ID
     *    - Single clickable child (wrapper pattern)
     *
     * @param element Node to evaluate
     * @return true if container exhibits clickable behavior
     */
    private fun isClickableContainer(element: AccessibilityNodeInfo): Boolean {
        val className = element.className?.toString() ?: return false

        // Must be a container type
        if (className !in CONTAINER_TYPES) return false

        // Check for clickability hints
        return element.isFocusable ||
                hasClickAction(element) ||
                hasClickableResourceId(element.viewIdResourceName) ||
                hasSingleClickableChild(element)
    }

    /**
     * Check if element has exactly one clickable child (wrapper pattern)
     *
     * Common pattern: FrameLayout wrapping a Button
     * The wrapper should be considered clickable if its only child is clickable
     *
     * @param element Parent node to check
     * @return true if has single clickable child
     */
    private fun hasSingleClickableChild(element: AccessibilityNodeInfo): Boolean {
        return try {
            if (element.childCount != 1) return false

            val child = element.getChild(0) ?: return false
            val isChildClickable = child.isClickable

            // Recycle child node to prevent memory leaks
            @Suppress("DEPRECATION")
            child.recycle()

            isChildClickable
        } catch (e: Exception) {
            Log.w(TAG, "Error checking single clickable child", e)
            false
        }
    }

    /**
     * Log score calculation details for debugging
     *
     * Logs only when score is interesting (>0 or explicit)
     *
     * @param element Node being scored
     * @param score Final score
     * @param confidence Confidence level
     * @param reasons List of scoring reasons
     * @param computationTimeNs Time taken in nanoseconds
     */
    private fun logScore(
        element: AccessibilityNodeInfo,
        score: Double,
        confidence: ClickabilityConfidence,
        reasons: List<String>,
        computationTimeNs: Long
    ) {
        // Only log if score > 0 (avoid spam from non-clickable elements)
        if (score > 0.0 || Log.isLoggable(TAG, Log.VERBOSE)) {
            val elementName = element.text?.toString()
                ?: element.contentDescription?.toString()
                ?: element.className?.toString()
                ?: "Unknown"

            val timeMs = computationTimeNs / 1_000_000.0

            Log.d(
                TAG,
                "Clickability: $elementName | Score: %.2f | Confidence: %s | Reasons: %s | Time: %.2fms"
                    .format(score, confidence, reasons.joinToString(", "), timeMs)
            )
        }
    }
}

/**
 * Clickability score result
 *
 * @property score Combined weighted score (0.0 - 1.0+)
 * @property confidence Confidence level classification
 * @property reasons List of signals that contributed to score
 * @property computationTimeNs Time taken to calculate score (nanoseconds)
 */
data class ClickabilityScore(
    val score: Double,
    val confidence: ClickabilityConfidence,
    val reasons: List<String>,
    val computationTimeNs: Long = 0
) {
    /**
     * Computation time in milliseconds
     */
    val computationTimeMs: Double
        get() = computationTimeNs / 1_000_000.0

    /**
     * Check if element should have VUID created
     *
     * @return true if score >= threshold
     */
    fun shouldCreateVUID(): Boolean {
        return score >= ClickabilityDetector.CLICKABILITY_THRESHOLD
    }
}

/**
 * Clickability confidence levels
 *
 * - EXPLICIT: isClickable=true (100% confidence)
 * - HIGH: score >= 0.9 (90%+ confidence)
 * - MEDIUM: score >= 0.7 (70%+ confidence)
 * - LOW: score >= 0.5 (50%+ confidence, threshold)
 * - NONE: score < 0.5 (<50% confidence, filtered)
 */
enum class ClickabilityConfidence {
    EXPLICIT,   // isClickable=true (score=1.0)
    HIGH,       // score >= 0.9
    MEDIUM,     // score >= 0.7
    LOW,        // score >= 0.5 (threshold)
    NONE        // score < 0.5 (below threshold)
}

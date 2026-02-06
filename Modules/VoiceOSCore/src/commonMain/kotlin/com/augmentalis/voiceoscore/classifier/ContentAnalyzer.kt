package com.augmentalis.voiceoscore

/**
 * Categorizes text content by length for stability analysis.
 *
 * Short text (< 20 chars) typically indicates stable UI elements like buttons and labels.
 * Medium text (20-100 chars) could be either static or dynamic content.
 * Long text (> 100 chars) usually indicates dynamic content like message previews.
 */
enum class TextLength {
    /** Less than 20 characters - likely a button or label (stable) */
    SHORT,
    /** 20-100 characters - could be either static or dynamic */
    MEDIUM,
    /** More than 100 characters - likely dynamic content (emails, messages) */
    LONG
}

/**
 * Represents content analysis signals used to determine element stability.
 *
 * This data class captures key indicators that help distinguish between
 * static UI elements (buttons, menus) and dynamic content (list items, messages).
 *
 * @property textLength The categorized length of the element's text content
 * @property hasResourceId Whether the element has a resource ID (indicates stability)
 * @property hasDynamicPatterns Whether the text contains dynamic patterns (timestamps, counters)
 * @property stabilityScore A composite score from 0-100 indicating element stability
 */
data class ContentSignal(
    val textLength: TextLength,
    val hasResourceId: Boolean,
    val hasDynamicPatterns: Boolean,
    val stabilityScore: Int
)

/**
 * Analyzes UI element content to determine stability characteristics.
 *
 * ContentAnalyzer provides comprehensive analysis of UI elements to help
 * distinguish between static elements (suitable for persistence) and
 * dynamic content (should remain in memory only).
 *
 * ## Stability Scoring
 *
 * The stability score is calculated based on:
 * - **Positive factors**: resourceId (+30), short text (+20), contentDescription (+15)
 * - **Negative factors**: dynamic container (-20), dynamic patterns (-15), long text (-10), large bounds (-5)
 *
 * ## Dynamic Pattern Detection
 *
 * Detects patterns that indicate frequently-changing content:
 * - Time patterns: "10:30 AM", "2:45 pm"
 * - Date patterns: "1/15", "Jan", "Feb", etc.
 * - Email previews: "Unread,", email addresses
 * - Counter patterns: "5 new", "12 messages"
 * - Status indicators: "typing...", "online", "last seen"
 *
 * @see ContentSignal
 * @see ElementInfo
 */
object ContentAnalyzer {

    // ============================================
    // Dynamic Pattern Regex (compiled once for efficiency)
    // ============================================

    /** Time patterns: "10:30 AM", "2:45 pm", "14:30" */
    private val TIME_PATTERN = Regex("""\d{1,2}:\d{2}\s*(AM|PM|am|pm)?""")

    /** Date patterns: "1/15", "12/31/2024" */
    private val DATE_SLASH_PATTERN = Regex("""\d{1,2}/\d{1,2}(/\d{2,4})?""")

    /** Month name patterns */
    private val MONTH_PATTERN = Regex(
        """\b(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\b""",
        RegexOption.IGNORE_CASE
    )

    /** Email address pattern */
    private val EMAIL_PATTERN = Regex("""[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}""")

    /** Counter patterns: "5 new", "12 messages", "3 unread" */
    private val COUNTER_PATTERN = Regex("""\d+\s+(new|messages?|unread|items?|notifications?)""", RegexOption.IGNORE_CASE)

    /** Status indicators (trailing boundary uses \b|$ to handle "typing..." at end of string) */
    private val STATUS_PATTERN = Regex(
        """\b(typing\.{2,3}|online|offline|last seen|active|away|busy)(?:\b|$)""",
        RegexOption.IGNORE_CASE
    )

    /** Email preview indicator */
    private val EMAIL_PREVIEW_PATTERN = Regex("""^Unread,""")

    // ============================================
    // Stability Score Constants
    // ============================================

    /** Bonus for having a resource ID */
    private const val RESOURCE_ID_BONUS = 30

    /** Bonus for having short text */
    private const val SHORT_TEXT_BONUS = 20

    /** Bonus for having content description */
    private const val CONTENT_DESC_BONUS = 15

    /** Penalty for being in a dynamic container */
    private const val DYNAMIC_CONTAINER_PENALTY = 20

    /** Penalty for having dynamic patterns */
    private const val DYNAMIC_PATTERN_PENALTY = 15

    /** Penalty for long text */
    private const val LONG_TEXT_PENALTY = 10

    /** Penalty for large bounds (> 500x500) */
    private const val LARGE_BOUNDS_PENALTY = 5

    /** Size threshold for large bounds */
    private const val LARGE_BOUNDS_THRESHOLD = 500

    /** Short text threshold (< 20 chars) */
    private const val SHORT_TEXT_THRESHOLD = 20

    /** Long text threshold (> 100 chars) */
    private const val LONG_TEXT_THRESHOLD = 100

    /** Baseline stability score */
    private const val BASELINE_SCORE = 50

    /** Minimum stability score */
    private const val MIN_SCORE = 0

    /** Maximum stability score */
    private const val MAX_SCORE = 100

    // ============================================
    // Public API
    // ============================================

    /**
     * Analyzes an element and returns comprehensive content signals.
     *
     * This is the primary entry point for content analysis. It examines
     * the element's text, resource ID, bounds, and container context
     * to produce a complete stability assessment.
     *
     * @param element The UI element to analyze
     * @return ContentSignal containing all analysis results
     *
     * Example:
     * ```kotlin
     * val element = ElementInfo(
     *     className = "Button",
     *     text = "Submit",
     *     resourceId = "com.app:id/submit_btn"
     * )
     * val signal = ContentAnalyzer.analyze(element)
     * // signal.stabilityScore would be high (stable button)
     * ```
     */
    fun analyze(element: ElementInfo): ContentSignal {
        val combinedText = getCombinedText(element)
        val textLength = categorizeTextLength(combinedText)
        val hasResourceId = element.resourceId.isNotBlank()
        val hasDynamicPatterns = detectDynamicPatterns(combinedText)
        val stabilityScore = calculateStabilityScore(element)

        return ContentSignal(
            textLength = textLength,
            hasResourceId = hasResourceId,
            hasDynamicPatterns = hasDynamicPatterns,
            stabilityScore = stabilityScore
        )
    }

    /**
     * Calculates a stability score for an element.
     *
     * The score ranges from 0 (highly dynamic) to 100 (very stable).
     * Higher scores indicate elements suitable for persistence.
     *
     * ## Scoring Factors
     *
     * **Positive (increase stability):**
     * - +30: Has resource ID
     * - +20: Has short text (< 20 chars)
     * - +15: Has content description
     *
     * **Negative (decrease stability):**
     * - -20: Inside dynamic container (RecyclerView, etc.)
     * - -15: Contains dynamic patterns (timestamps, counters)
     * - -10: Has long text (> 100 chars)
     * - -5: Has large bounds (> 500x500)
     *
     * @param element The UI element to score
     * @return Stability score from 0 to 100
     */
    fun calculateStabilityScore(element: ElementInfo): Int {
        var score = BASELINE_SCORE
        val combinedText = getCombinedText(element)

        // Positive factors
        if (element.resourceId.isNotBlank()) {
            score += RESOURCE_ID_BONUS
        }

        if (combinedText.isNotBlank() && combinedText.length < SHORT_TEXT_THRESHOLD) {
            score += SHORT_TEXT_BONUS
        }

        if (element.contentDescription.isNotBlank()) {
            score += CONTENT_DESC_BONUS
        }

        // Negative factors
        if (element.isInDynamicContainer) {
            score -= DYNAMIC_CONTAINER_PENALTY
        }

        if (detectDynamicPatterns(combinedText)) {
            score -= DYNAMIC_PATTERN_PENALTY
        }

        if (combinedText.length > LONG_TEXT_THRESHOLD) {
            score -= LONG_TEXT_PENALTY
        }

        if (element.bounds.width > LARGE_BOUNDS_THRESHOLD &&
            element.bounds.height > LARGE_BOUNDS_THRESHOLD) {
            score -= LARGE_BOUNDS_PENALTY
        }

        // Clamp to valid range
        return score.coerceIn(MIN_SCORE, MAX_SCORE)
    }

    /**
     * Detects dynamic content patterns in text.
     *
     * Dynamic patterns indicate content that changes frequently,
     * such as timestamps, message counters, or status indicators.
     *
     * ## Detected Patterns
     *
     * - **Time**: "10:30 AM", "2:45 pm", "14:30"
     * - **Date**: "1/15", "12/31/2024", "Jan", "Feb", etc.
     * - **Email**: "user@example.com", "Unread,"
     * - **Counters**: "5 new", "12 messages", "3 unread"
     * - **Status**: "typing...", "online", "last seen"
     *
     * @param text The text to analyze
     * @return true if any dynamic patterns are detected
     *
     * Example:
     * ```kotlin
     * ContentAnalyzer.detectDynamicPatterns("Meeting at 10:30 AM") // true
     * ContentAnalyzer.detectDynamicPatterns("Submit")              // false
     * ContentAnalyzer.detectDynamicPatterns("5 new messages")      // true
     * ```
     */
    fun detectDynamicPatterns(text: String): Boolean {
        if (text.isBlank()) return false

        return TIME_PATTERN.containsMatchIn(text) ||
                DATE_SLASH_PATTERN.containsMatchIn(text) ||
                MONTH_PATTERN.containsMatchIn(text) ||
                EMAIL_PATTERN.containsMatchIn(text) ||
                COUNTER_PATTERN.containsMatchIn(text) ||
                STATUS_PATTERN.containsMatchIn(text) ||
                EMAIL_PREVIEW_PATTERN.containsMatchIn(text)
    }

    // ============================================
    // Internal Helpers
    // ============================================

    /**
     * Combines text and content description for analysis.
     */
    private fun getCombinedText(element: ElementInfo): String {
        return buildString {
            append(element.text)
            if (element.contentDescription.isNotBlank()) {
                if (isNotEmpty()) append(" ")
                append(element.contentDescription)
            }
        }
    }

    /**
     * Categorizes text length into SHORT, MEDIUM, or LONG.
     */
    private fun categorizeTextLength(text: String): TextLength {
        return when {
            text.length < SHORT_TEXT_THRESHOLD -> TextLength.SHORT
            text.length <= LONG_TEXT_THRESHOLD -> TextLength.MEDIUM
            else -> TextLength.LONG
        }
    }
}

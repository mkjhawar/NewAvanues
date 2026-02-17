package com.augmentalis.noteavanue.voice

/**
 * Format detected from a speech utterance prefix.
 *
 * @param format The block format to apply
 * @param cleanText The text with the trigger prefix stripped
 * @param headingLevel For HEADING format, the level (1-3). 0 otherwise.
 */
data class FormatResult(
    val format: DetectedFormat,
    val cleanText: String,
    val headingLevel: Int = 0
)

/**
 * Block formats that can be detected from speech.
 */
enum class DetectedFormat {
    /** Plain paragraph (no trigger detected) */
    PARAGRAPH,
    /** Heading (level in FormatResult.headingLevel) */
    HEADING,
    /** Unordered (bullet) list item */
    BULLET,
    /** Ordered (numbered) list item */
    NUMBERED,
    /** Checklist / task item */
    CHECKLIST,
    /** Blockquote */
    BLOCKQUOTE,
    /** Code block */
    CODE
}

/**
 * NoteFormatDetector — FSM keyword trigger detection for voice-dictated text.
 *
 * Detects structural formatting intent from speech utterance prefixes.
 * Runs in O(1) time against a fixed set of trigger patterns.
 * No ML, no network — pure string matching.
 *
 * Trigger patterns (case-insensitive, colon optional):
 * - "title:" / "heading one:" / "heading 1:" → H1
 * - "heading two:" / "heading 2:" / "subtitle:" → H2
 * - "heading three:" / "heading 3:" / "section:" → H3
 * - "bullet:" / "dash:" / "point:" → Bullet list item
 * - "number X:" / "first:" / "second:" / ordinals → Numbered list
 * - "todo:" / "task:" / "checkbox:" → Checklist item
 * - "quote:" / "quotation:" → Blockquote
 * - "code:" / "snippet:" → Code block
 *
 * Usage:
 * ```kotlin
 * val result = NoteFormatDetector.detect("heading one: Project Overview")
 * // result.format == DetectedFormat.HEADING
 * // result.headingLevel == 1
 * // result.cleanText == "Project Overview"
 * ```
 */
object NoteFormatDetector {

    private data class TriggerPattern(
        val prefixes: List<String>,
        val format: DetectedFormat,
        val headingLevel: Int = 0
    )

    private val triggers: List<TriggerPattern> = listOf(
        // H1 triggers
        TriggerPattern(
            prefixes = listOf("title:", "title ", "heading one:", "heading one ", "heading 1:", "heading 1 ", "h1:", "h1 "),
            format = DetectedFormat.HEADING,
            headingLevel = 1
        ),
        // H2 triggers
        TriggerPattern(
            prefixes = listOf("heading two:", "heading two ", "heading 2:", "heading 2 ", "subtitle:", "subtitle ", "h2:", "h2 "),
            format = DetectedFormat.HEADING,
            headingLevel = 2
        ),
        // H3 triggers
        TriggerPattern(
            prefixes = listOf("heading three:", "heading three ", "heading 3:", "heading 3 ", "section:", "section ", "h3:", "h3 "),
            format = DetectedFormat.HEADING,
            headingLevel = 3
        ),
        // Bullet list triggers
        TriggerPattern(
            prefixes = listOf("bullet:", "bullet ", "dash:", "dash ", "point:", "point "),
            format = DetectedFormat.BULLET
        ),
        // Checklist triggers (before numbered to avoid "todo 1" matching numbered)
        TriggerPattern(
            prefixes = listOf("todo:", "todo ", "task:", "task ", "checkbox:", "checkbox ", "check:", "check "),
            format = DetectedFormat.CHECKLIST
        ),
        // Blockquote triggers
        TriggerPattern(
            prefixes = listOf("quote:", "quote ", "quotation:", "quotation "),
            format = DetectedFormat.BLOCKQUOTE
        ),
        // Code block triggers
        TriggerPattern(
            prefixes = listOf("code:", "code ", "snippet:", "snippet "),
            format = DetectedFormat.CODE
        )
    )

    /** Ordinal starters for numbered lists (first through tenth) */
    private val ordinalPrefixes = listOf(
        "first:", "first ",
        "second:", "second ",
        "third:", "third ",
        "fourth:", "fourth ",
        "fifth:", "fifth ",
        "sixth:", "sixth ",
        "seventh:", "seventh ",
        "eighth:", "eighth ",
        "ninth:", "ninth ",
        "tenth:", "tenth ",
        "number one:", "number one ",
        "number two:", "number two ",
        "number three:", "number three "
    )

    /** Regex for "number N:" pattern (e.g., "number 42:") */
    private val numberPattern = Regex("^number\\s+\\d+[:\\s]\\s*", RegexOption.IGNORE_CASE)

    /**
     * Detect format intent from a speech utterance.
     *
     * @param utterance Raw speech text (may include trigger prefix)
     * @return FormatResult with detected format and cleaned text
     */
    fun detect(utterance: String): FormatResult {
        val trimmed = utterance.trimStart()
        val lower = trimmed.lowercase()

        // Check static triggers
        for (trigger in triggers) {
            for (prefix in trigger.prefixes) {
                if (lower.startsWith(prefix)) {
                    val cleanText = trimmed.substring(prefix.length).trimStart()
                    return FormatResult(
                        format = trigger.format,
                        cleanText = cleanText,
                        headingLevel = trigger.headingLevel
                    )
                }
            }
        }

        // Check ordinal starters for numbered lists
        for (prefix in ordinalPrefixes) {
            if (lower.startsWith(prefix)) {
                val cleanText = trimmed.substring(prefix.length).trimStart()
                return FormatResult(
                    format = DetectedFormat.NUMBERED,
                    cleanText = cleanText
                )
            }
        }

        // Check "number N:" pattern
        val numberMatch = numberPattern.find(lower)
        if (numberMatch != null) {
            val cleanText = trimmed.substring(numberMatch.range.last + 1).trimStart()
            return FormatResult(
                format = DetectedFormat.NUMBERED,
                cleanText = cleanText
            )
        }

        // No trigger found → plain paragraph
        return FormatResult(
            format = DetectedFormat.PARAGRAPH,
            cleanText = trimmed
        )
    }
}

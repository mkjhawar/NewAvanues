/**
 * MetadataSuggestionGenerator.kt - Generates metadata improvement suggestions
 * Path: modules/apps/LearnApp/src/main/java/com/augmentalis/learnapp/metadata/MetadataSuggestionGenerator.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-13
 *
 * Generates context-aware suggestions for improving element metadata
 */

package com.augmentalis.voiceoscore.learnapp.metadata

import com.augmentalis.voiceoscore.learnapp.models.ElementInfo

/**
 * Metadata Suggestion Generator
 *
 * Analyzes elements and generates actionable suggestions for improving
 * their metadata quality. Suggestions are context-aware and prioritized.
 *
 * ## Usage Example
 *
 * ```kotlin
 * val suggestions = MetadataSuggestionGenerator.generateSuggestions(element)
 * suggestions.forEach { suggestion ->
 *     println("Priority ${suggestion.priority}: ${suggestion.text}")
 * }
 * ```
 *
 * @since 1.0.0
 */
object MetadataSuggestionGenerator {

    /**
     * Generate suggestions for element
     *
     * @param element Element to analyze
     * @return List of prioritized suggestions
     */
    fun generateSuggestions(element: ElementInfo): List<MetadataSuggestion> {
        val suggestions = mutableListOf<MetadataSuggestion>()

        // Check for missing text/contentDescription
        if (element.text.isNullOrBlank() && element.contentDescription.isNullOrBlank()) {
            if (element.isClickable == true) {
                suggestions.add(
                    MetadataSuggestion(
                        text = "Add button label",
                        priority = SuggestionPriority.HIGH,
                        category = SuggestionCategory.TEXT,
                        example = "Submit, Cancel, Next"
                    )
                )
                suggestions.add(
                    MetadataSuggestion(
                        text = "Add content description",
                        priority = SuggestionPriority.HIGH,
                        category = SuggestionCategory.CONTENT_DESCRIPTION,
                        example = "Submit form button"
                    )
                )
            } else {
                suggestions.add(
                    MetadataSuggestion(
                        text = "Add descriptive text",
                        priority = SuggestionPriority.MEDIUM,
                        category = SuggestionCategory.TEXT,
                        example = "User profile, Settings"
                    )
                )
            }
        } else if (element.text.isNullOrBlank()) {
            suggestions.add(
                MetadataSuggestion(
                    text = "Add visible text label",
                    priority = SuggestionPriority.MEDIUM,
                    category = SuggestionCategory.TEXT,
                    example = "Based on: ${element.contentDescription}"
                )
            )
        } else if (element.contentDescription.isNullOrBlank()) {
            suggestions.add(
                MetadataSuggestion(
                    text = "Add content description",
                    priority = SuggestionPriority.MEDIUM,
                    category = SuggestionCategory.CONTENT_DESCRIPTION,
                    example = "Describe: ${element.text}"
                )
            )
        }

        // Check for missing resource ID
        if (element.resourceId.isNullOrBlank()) {
            if (element.isClickable == true) {
                suggestions.add(
                    MetadataSuggestion(
                        text = "Add resource ID",
                        priority = SuggestionPriority.HIGH,
                        category = SuggestionCategory.RESOURCE_ID,
                        example = "btn_submit, img_profile"
                    )
                )
            } else {
                suggestions.add(
                    MetadataSuggestion(
                        text = "Add resource ID",
                        priority = SuggestionPriority.LOW,
                        category = SuggestionCategory.RESOURCE_ID,
                        example = "txt_title, layout_header"
                    )
                )
            }
        }

        // Context-specific suggestions based on className
        when {
            element.className?.contains("ImageButton") == true ||
            element.className?.contains("ImageView") == true -> {
                if (element.contentDescription.isNullOrBlank()) {
                    suggestions.add(
                        MetadataSuggestion(
                            text = "Add image description",
                            priority = SuggestionPriority.CRITICAL,
                            category = SuggestionCategory.CONTENT_DESCRIPTION,
                            example = "Profile picture, Search icon"
                        )
                    )
                }
            }
            element.className?.contains("EditText") == true -> {
                if (element.text.isNullOrBlank()) {
                    suggestions.add(
                        MetadataSuggestion(
                            text = "Add hint text",
                            priority = SuggestionPriority.HIGH,
                            category = SuggestionCategory.TEXT,
                            example = "Enter username, Type message"
                        )
                    )
                }
            }
            element.className?.contains("CheckBox") == true ||
            element.className?.contains("RadioButton") == true -> {
                if (element.text.isNullOrBlank() && element.contentDescription.isNullOrBlank()) {
                    suggestions.add(
                        MetadataSuggestion(
                            text = "Add selection option label",
                            priority = SuggestionPriority.HIGH,
                            category = SuggestionCategory.TEXT,
                            example = "Remember me, Agree to terms"
                        )
                    )
                }
            }
        }

        // Sort by priority
        return suggestions.sortedByDescending { it.priority.level }
    }

    /**
     * Generate summary text for suggestions
     *
     * @param suggestions List of suggestions
     * @return Summary string
     */
    fun generateSummary(suggestions: List<MetadataSuggestion>): String {
        if (suggestions.isEmpty()) {
            return "No suggestions available"
        }

        val criticalCount = suggestions.count { it.priority == SuggestionPriority.CRITICAL }
        val highCount = suggestions.count { it.priority == SuggestionPriority.HIGH }
        val mediumCount = suggestions.count { it.priority == SuggestionPriority.MEDIUM }
        val lowCount = suggestions.count { it.priority == SuggestionPriority.LOW }

        return buildString {
            append("${suggestions.size} suggestions")
            if (criticalCount > 0) append(" ($criticalCount critical)")
            if (highCount > 0) append(" ($highCount high)")
            if (mediumCount > 0) append(" ($mediumCount medium)")
            if (lowCount > 0) append(" ($lowCount low)")
        }
    }
}

/**
 * Metadata Suggestion
 *
 * Represents a single suggestion for improving metadata
 *
 * @property text Suggestion text
 * @property priority Priority level
 * @property category Suggestion category
 * @property example Example implementation
 */
data class MetadataSuggestion(
    val text: String,
    val priority: SuggestionPriority,
    val category: SuggestionCategory,
    val example: String? = null
)

/**
 * Suggestion Priority
 */
enum class SuggestionPriority(val level: Int) {
    CRITICAL(4),  // Must fix (e.g., image with no description)
    HIGH(3),      // Should fix (e.g., button with no label)
    MEDIUM(2),    // Nice to fix (e.g., missing contentDescription when text exists)
    LOW(1)        // Optional (e.g., missing ID for static elements)
}

/**
 * Suggestion Category
 */
enum class SuggestionCategory {
    TEXT,                    // Visible text label
    CONTENT_DESCRIPTION,     // Accessibility description
    RESOURCE_ID,             // Android resource ID
    ACTIONABLE               // Clickable/actionable state
}

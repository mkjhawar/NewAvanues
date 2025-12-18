/**
 * WebCommandGenerator.kt - Natural language command generation for web elements
 * Path: modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnweb/WebCommandGenerator.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-13
 *
 * Generates natural language voice commands from web elements with synonyms
 */

package com.augmentalis.voiceoscore.learnweb

import android.util.Log

/**
 * Data class for generated web command (mirrors GeneratedWebCommand SQLDelight table)
 */
data class GeneratedWebCommand(
    val id: Long = 0,
    val websiteUrlHash: String,
    val elementHash: String,
    val commandText: String,
    val synonyms: String,
    val action: String,
    val xpath: String,
    val generatedAt: Long,
    val usageCount: Long = 0,
    val lastUsedAt: Long? = null
)

/**
 * Web Command Generator
 *
 * Generates natural language voice commands from scraped web elements.
 * Creates commands based on element type, text, ARIA labels, and roles.
 *
 * @since 1.0.0
 */
class WebCommandGenerator {

    companion object {
        private const val TAG = "WebCommandGenerator"

        /**
         * Common action verbs for different element types
         */
        private val ACTION_VERBS = mapOf(
            "BUTTON" to listOf("click", "press", "tap", "select"),
            "A" to listOf("go to", "open", "visit", "navigate to"),
            "INPUT" to listOf("fill", "enter", "type in", "focus on"),
            "SELECT" to listOf("choose", "select", "pick"),
            "TEXTAREA" to listOf("write in", "type in", "fill"),
            "DEFAULT" to listOf("click", "select", "tap")
        )

        /**
         * Common synonyms for web actions
         */
        private val ACTION_SYNONYMS = mapOf(
            "login" to listOf("sign in", "log in"),
            "logout" to listOf("sign out", "log out"),
            "submit" to listOf("send", "confirm"),
            "search" to listOf("find", "look for"),
            "cancel" to listOf("close", "dismiss"),
            "next" to listOf("continue", "proceed"),
            "previous" to listOf("back", "go back"),
            "menu" to listOf("navigation", "nav")
        )
    }

    /**
     * Generate commands for web elements
     *
     * @param elements List of scraped web elements
     * @param websiteUrlHash Website URL hash
     * @return List of generated commands
     */
    fun generateCommands(
        elements: List<ScrapedWebElement>,
        websiteUrlHash: String
    ): List<GeneratedWebCommand> {
        val commands = mutableListOf<GeneratedWebCommand>()
        val now = System.currentTimeMillis()

        elements.forEach { element ->
            try {
                val generatedCommands = generateCommandsForElement(element, websiteUrlHash, now)
                commands.addAll(generatedCommands)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate commands for element: ${element.elementHash}", e)
            }
        }

        Log.d(TAG, "Generated ${commands.size} commands from ${elements.size} elements")
        return commands
    }

    /**
     * Generate commands for a single element
     *
     * @param element Scraped web element
     * @param websiteUrlHash Website URL hash
     * @param timestamp Current timestamp
     * @return List of generated commands
     */
    private fun generateCommandsForElement(
        element: ScrapedWebElement,
        websiteUrlHash: String,
        timestamp: Long
    ): List<GeneratedWebCommand> {
        val commands = mutableListOf<GeneratedWebCommand>()

        // Determine action based on element type and role
        val action = determineAction(element)
        val verbs = getActionVerbs(element.tagName, action)

        // Generate commands from different sources
        val commandSources = buildList {
            // 1. ARIA label (highest priority)
            element.ariaLabel?.let { add(it) }

            // 2. Visible text
            element.text?.takeIf { it.isNotBlank() }?.let { add(it) }

            // 3. Role-based description
            element.role?.let { add("$it element") }
        }

        // Generate command for each source with each verb
        commandSources.forEach { source ->
            verbs.forEach { verb ->
                val commandText = "$verb $source"
                val synonyms = generateSynonyms(source)

                commands.add(
                    GeneratedWebCommand(
                        websiteUrlHash = websiteUrlHash,
                        elementHash = element.elementHash,
                        commandText = commandText,
                        synonyms = synonyms.joinToString(","),
                        action = action,
                        xpath = element.xpath,
                        generatedAt = timestamp
                    )
                )
            }
        }

        return commands
    }

    /**
     * Determine action type for element
     *
     * @param element Scraped web element
     * @return Action type (CLICK, SCROLL_TO, FOCUS, etc.)
     */
    private fun determineAction(element: ScrapedWebElement): String {
        return when {
            element.tagName == "INPUT" || element.tagName == "TEXTAREA" -> "FOCUS"
            element.tagName == "SELECT" -> "CLICK"
            element.tagName == "A" -> "NAVIGATE"
            element.clickable -> "CLICK"
            element.visible -> "SCROLL_TO"
            else -> "CLICK"
        }
    }

    /**
     * Get action verbs for element type
     *
     * @param tagName HTML tag name
     * @param action Action type
     * @return List of action verbs
     */
    private fun getActionVerbs(tagName: String, action: String): List<String> {
        return when (action) {
            "FOCUS" -> listOf("focus on", "select", "go to")
            "NAVIGATE" -> listOf("go to", "open", "visit")
            "SCROLL_TO" -> listOf("scroll to", "show", "view")
            else -> ACTION_VERBS[tagName] ?: ACTION_VERBS["DEFAULT"]!!
        }
    }

    /**
     * Generate synonyms for text
     *
     * @param text Source text
     * @return List of synonyms
     */
    private fun generateSynonyms(text: String): List<String> {
        val synonyms = mutableSetOf<String>()

        // Add original text
        synonyms.add(text.trim())

        // Add lowercase version
        synonyms.add(text.lowercase().trim())

        // Check for known synonyms
        val lowerText = text.lowercase()
        ACTION_SYNONYMS.forEach { (key, values) ->
            if (lowerText.contains(key)) {
                values.forEach { synonym ->
                    synonyms.add(lowerText.replace(key, synonym))
                }
            }
        }

        // Remove articles for shorter commands
        val withoutArticles = removeArticles(text)
        if (withoutArticles != text) {
            synonyms.add(withoutArticles)
        }

        return synonyms.toList()
    }

    /**
     * Remove common articles from text
     *
     * @param text Source text
     * @return Text without articles
     */
    private fun removeArticles(text: String): String {
        val articles = listOf("the ", "a ", "an ")
        var result = text.lowercase()

        articles.forEach { article ->
            if (result.startsWith(article)) {
                result = result.substring(article.length)
            }
        }

        return result.trim()
    }

    /**
     * Filter commands by minimum quality threshold
     *
     * Removes commands that are:
     * - Too short (< 3 characters)
     * - Too generic (e.g., "click element")
     * - Duplicates
     *
     * @param commands List of commands
     * @return Filtered list of commands
     */
    fun filterCommands(commands: List<GeneratedWebCommand>): List<GeneratedWebCommand> {
        val seen = mutableSetOf<String>()
        val filtered = mutableListOf<GeneratedWebCommand>()

        commands.forEach { command ->
            val normalizedText = command.commandText.lowercase().trim()

            // Skip if too short
            if (normalizedText.length < 3) {
                return@forEach
            }

            // Skip if too generic
            if (isGenericCommand(normalizedText)) {
                return@forEach
            }

            // Skip duplicates
            if (seen.contains(normalizedText)) {
                return@forEach
            }

            seen.add(normalizedText)
            filtered.add(command)
        }

        Log.d(TAG, "Filtered ${commands.size} commands to ${filtered.size} unique commands")
        return filtered
    }

    /**
     * Check if command is too generic
     *
     * @param commandText Command text (normalized)
     * @return True if generic
     */
    private fun isGenericCommand(commandText: String): Boolean {
        val genericPatterns = listOf(
            "click element",
            "select element",
            "tap element",
            "button element",
            "link element"
        )

        return genericPatterns.any { commandText.contains(it) }
    }

    /**
     * Group commands by element
     *
     * @param commands List of commands
     * @return Map of element hash to commands
     */
    fun groupByElement(commands: List<GeneratedWebCommand>): Map<String, List<GeneratedWebCommand>> {
        return commands.groupBy { it.elementHash }
    }

    /**
     * Get statistics for generated commands
     *
     * @param commands List of commands
     * @return Command statistics
     */
    fun getStatistics(commands: List<GeneratedWebCommand>): CommandStats {
        val actionCounts = commands.groupingBy { it.action }.eachCount()
        val avgSynonyms = commands.map { it.synonyms.split(",").size }.average()

        return CommandStats(
            totalCommands = commands.size,
            uniqueElements = commands.map { it.elementHash }.distinct().size,
            actionCounts = actionCounts,
            averageSynonymsPerCommand = avgSynonyms
        )
    }
}

/**
 * Command Statistics
 *
 * @property totalCommands Total number of commands
 * @property uniqueElements Number of unique elements
 * @property actionCounts Map of action type to count
 * @property averageSynonymsPerCommand Average synonyms per command
 */
data class CommandStats(
    val totalCommands: Int,
    val uniqueElements: Int,
    val actionCounts: Map<String, Int>,
    val averageSynonymsPerCommand: Double
)

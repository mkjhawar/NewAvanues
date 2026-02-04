/**
 * AIContextProvider.kt - AI/NLU Context Provider
 *
 * Central provider for screen context to AI/NLU systems including:
 * - Natural Language Understanding (NLU) intent recognition
 * - Large Language Model (LLM) context
 * - Voice command disambiguation
 * - Context-aware assistance
 *
 * Created: 2025-12-06
 * Part of: Universal Screen Hierarchy System
 *
 * @author IDEACODE v10.3
 */

package com.augmentalis.avaelements.context

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * AI Context Provider for NLU/LLM integration
 *
 * This class provides the bridge between the screen hierarchy system
 * and AI/NLU systems. It:
 * - Maintains current screen state
 * - Generates optimized context for different AI systems
 * - Resolves ambiguous voice commands
 * - Tracks context history
 *
 * Example usage:
 * ```kotlin
 * val provider = AIContextProvider()
 *
 * // Update screen
 * provider.updateScreen(screenHierarchy)
 *
 * // Get context for NLU
 * val nluContext = provider.getContextForNLU()
 *
 * // Get context for LLM
 * val llmContext = provider.getContextForLLM()
 *
 * // Resolve voice command
 * val resolution = provider.resolveCommand("click submit", emptyMap())
 * ```
 */
class AIContextProvider {

    // Current screen hierarchy
    private val _currentHierarchy = MutableStateFlow<ScreenHierarchy?>(null)
    val currentHierarchy: StateFlow<ScreenHierarchy?> = _currentHierarchy.asStateFlow()

    // Screen history (last 5 screens)
    private val _screenHistory = MutableStateFlow<List<ScreenHierarchy>>(emptyList())
    val screenHistory: StateFlow<List<ScreenHierarchy>> = _screenHistory.asStateFlow()

    private val maxHistorySize = 5

    // Context statistics
    private var totalUpdates = 0
    private var commandResolutions = 0
    private var successfulResolutions = 0

    /**
     * Update current screen context
     *
     * Call this whenever the screen changes or is re-rendered.
     *
     * @param hierarchy New screen hierarchy
     * @param reason Reason for update (default: USER_INTERACTION)
     */
    fun updateScreen(
        hierarchy: ScreenHierarchy,
        reason: ScreenUpdateReason = ScreenUpdateReason.USER_INTERACTION
    ) {
        // Add current to history if different
        _currentHierarchy.value?.let { current ->
            if (current.screenHash != hierarchy.screenHash) {
                addToHistory(current)
            }
        }

        _currentHierarchy.value = hierarchy
        totalUpdates++
    }

    /**
     * Get context for NLU (Natural Language Understanding)
     *
     * Returns a compact, structured representation optimized for
     * NLU systems that need to recognize intents and extract entities.
     *
     * Output is JSON-serializable and typically 100-300 tokens.
     *
     * @return NLU context structure
     */
    fun getContextForNLU(): NLUContext {
        val hierarchy = _currentHierarchy.value
            ?: return NLUContext.empty()

        val quantized = ScreenQuantizer.quantize(hierarchy)

        return NLUContext(
            screen = quantized.screen,
            commands = quantized.commands.take(20), // Limit for performance
            entities = ScreenQuantizer.extractEntities(hierarchy).take(30),
            formMode = quantized.context.formMode,
            canGoBack = quantized.context.canGoBack,
            previousScreen = quantized.context.previousScreen,
            intents = generateIntents(hierarchy)
        )
    }

    /**
     * Get context for LLM (Large Language Model)
     *
     * Returns a natural language summary optimized for LLM context windows.
     * Typically 50-200 tokens, human-readable.
     *
     * @param maxTokens Maximum token budget (approximate)
     * @return Natural language context string
     */
    fun getContextForLLM(maxTokens: Int = 200): String {
        val hierarchy = _currentHierarchy.value
            ?: return "No screen currently loaded."

        val quantized = ScreenQuantizer.quantize(hierarchy)

        return buildString {
            // Screen summary
            appendLine(quantized.summary)
            appendLine()

            // Available commands (top 10)
            val topCommands = quantized.commands
                .sortedByDescending { it.priority }
                .take(10)

            if (topCommands.isNotEmpty()) {
                appendLine("Available voice commands:")
                topCommands.forEach { cmd ->
                    appendLine("  - \"${cmd.label}\": ${cmd.action}")
                }
                appendLine()
            }

            // Form info (if applicable)
            quantized.formInfo?.let { formInfo ->
                appendLine("Form: ${formInfo.fieldCount} fields")
                if (formInfo.requiredFieldCount > 0) {
                    appendLine("  ${formInfo.requiredFieldCount} required")
                }
                appendLine()
            }

            // Navigation context
            if (quantized.context.canGoBack) {
                appendLine("User can go back to previous screen")
            }

            // Complexity indicator
            if (quantized.metadata.complexity in setOf("COMPLEX", "VERY_COMPLEX")) {
                appendLine("Note: Complex interface with ${quantized.metadata.interactiveCount} interactive elements")
            }
        }
    }

    /**
     * Get compact context for token-constrained systems
     *
     * Ultra-compact representation (typically 20-50 tokens).
     *
     * @return Compact context string
     */
    fun getCompactContext(): String {
        val hierarchy = _currentHierarchy.value
            ?: return "no_screen"

        return ScreenQuantizer.generateCompactSummary(hierarchy)
    }

    /**
     * Resolve ambiguous command using context
     *
     * Uses screen context to disambiguate voice commands:
     * - "click submit" when multiple submit buttons exist
     * - "type email" when multiple email fields exist
     * - "select option" when multiple options available
     *
     * @param command Primary command ("click", "type", "select")
     * @param parameters Command parameters (e.g., target label)
     * @param threshold Minimum confidence threshold (0.0-1.0)
     * @return Command resolution result
     */
    fun resolveCommand(
        command: String,
        parameters: Map<String, Any>,
        threshold: Float = 0.6f
    ): CommandResolution {
        commandResolutions++

        val hierarchy = _currentHierarchy.value
            ?: return CommandResolution.Failed("No screen context available")

        // Extract target from parameters
        val target = parameters["target"]?.toString()?.lowercase()

        // Find matching commandable elements
        val matches = findMatches(hierarchy, command, target)

        return when {
            matches.isEmpty() -> {
                CommandResolution.Failed("No matching elements found")
            }

            matches.size == 1 -> {
                successfulResolutions++
                CommandResolution.Success(
                    elementId = matches[0].id,
                    confidence = 1.0f,
                    element = matches[0]
                )
            }

            else -> {
                // Multiple matches - use scoring
                val scored = scoreMatches(matches, command, target)
                val best = scored.maxByOrNull { it.second }

                if (best != null && best.second >= threshold) {
                    val confidence = CommandConfidence.fromScore(best.second)

                    if (confidence >= CommandConfidence.HIGH) {
                        successfulResolutions++
                        CommandResolution.Success(
                            elementId = best.first.id,
                            confidence = best.second,
                            element = best.first
                        )
                    } else {
                        CommandResolution.Ambiguous(
                            suggestions = scored.map { (elem, score) ->
                                CommandSuggestion(
                                    elementId = elem.id,
                                    label = elem.voiceLabel,
                                    confidence = score
                                )
                            },
                            bestMatch = best.first.id,
                            bestConfidence = best.second
                        )
                    }
                } else {
                    CommandResolution.Failed("No confident match found")
                }
            }
        }
    }

    /**
     * Find matches for command
     */
    private fun findMatches(
        hierarchy: ScreenHierarchy,
        command: String,
        target: String?
    ): List<CommandableElement> {
        return hierarchy.commandableElements.filter { element ->
            // Match by command
            val commandMatches = element.matchesCommand(command)

            // Match by target label (if provided)
            val targetMatches = target == null || element.matchesLabel(target)

            commandMatches && targetMatches
        }
    }

    /**
     * Score matches for disambiguation
     */
    private fun scoreMatches(
        matches: List<CommandableElement>,
        command: String,
        target: String?
    ): List<Pair<CommandableElement, Float>> {
        return matches.map { element ->
            var score = 0.5f // Base score

            // Priority boost
            score += (element.priority / 20f) * 0.3f

            // Exact label match boost
            if (target != null) {
                val labelLower = element.voiceLabel.lowercase()
                when {
                    labelLower == target -> score += 0.3f
                    labelLower.contains(target) -> score += 0.2f
                    target.contains(labelLower) -> score += 0.1f
                }
            }

            // Primary command match boost
            if (element.primaryCommand.equals(command, ignoreCase = true)) {
                score += 0.1f
            }

            element to score.coerceIn(0f, 1f)
        }
    }

    /**
     * Generate intents for current screen
     *
     * Creates intent definitions that NLU systems can use.
     */
    private fun generateIntents(hierarchy: ScreenHierarchy): List<Intent> {
        return ScreenQuantizer.generateIntentSchema(hierarchy).intents
    }

    /**
     * Get screen context history
     *
     * Returns list of recent screens (up to 5) for context.
     *
     * @return List of recent screen hierarchies
     */
    fun getHistory(): List<ScreenHierarchy> {
        return _screenHistory.value
    }

    /**
     * Get navigation context
     *
     * Returns information about current and previous screens.
     */
    fun getNavigationContext(): NavigationContextInfo? {
        val current = _currentHierarchy.value ?: return null
        val history = _screenHistory.value

        return NavigationContextInfo(
            currentScreen = current.screenType.displayName,
            previousScreen = history.lastOrNull()?.screenType?.displayName,
            canGoBack = current.navigationContext.canNavigateBack,
            historyDepth = history.size
        )
    }

    /**
     * Clear context
     */
    fun clear() {
        _currentHierarchy.value = null
        _screenHistory.value = emptyList()
    }

    /**
     * Get statistics
     */
    fun getStatistics(): ContextStatistics {
        val successRate = if (commandResolutions > 0) {
            (successfulResolutions.toFloat() / commandResolutions) * 100
        } else {
            0f
        }

        return ContextStatistics(
            totalUpdates = totalUpdates,
            totalResolutions = commandResolutions,
            successfulResolutions = successfulResolutions,
            successRate = successRate,
            currentScreenType = _currentHierarchy.value?.screenType?.displayName,
            historySize = _screenHistory.value.size
        )
    }

    /**
     * Add to history
     */
    private fun addToHistory(hierarchy: ScreenHierarchy) {
        val current = _screenHistory.value.toMutableList()
        current.add(hierarchy)

        // Keep only last N
        if (current.size > maxHistorySize) {
            current.removeAt(0)
        }

        _screenHistory.value = current
    }
}

/**
 * NLU Context structure
 */
data class NLUContext(
    val screen: ScreenInfo,
    val commands: List<Command>,
    val entities: List<Entity>,
    val formMode: Boolean,
    val canGoBack: Boolean,
    val previousScreen: String?,
    val intents: List<Intent>
) {
    companion object {
        fun empty() = NLUContext(
            screen = ScreenInfo("UNKNOWN", null, null),
            commands = emptyList(),
            entities = emptyList(),
            formMode = false,
            canGoBack = false,
            previousScreen = null,
            intents = emptyList()
        )
    }
}

/**
 * Command resolution result
 */
sealed class CommandResolution {
    /**
     * Successfully resolved to single element
     */
    data class Success(
        val elementId: String,
        val confidence: Float,
        val element: CommandableElement
    ) : CommandResolution()

    /**
     * Multiple possible matches (ambiguous)
     */
    data class Ambiguous(
        val suggestions: List<CommandSuggestion>,
        val bestMatch: String,
        val bestConfidence: Float
    ) : CommandResolution()

    /**
     * Failed to resolve
     */
    data class Failed(
        val reason: String
    ) : CommandResolution()
}

/**
 * Command suggestion
 */
data class CommandSuggestion(
    val elementId: String,
    val label: String,
    val confidence: Float
)

/**
 * Navigation context info
 */
data class NavigationContextInfo(
    val currentScreen: String,
    val previousScreen: String?,
    val canGoBack: Boolean,
    val historyDepth: Int
)

/**
 * Context statistics
 */
data class ContextStatistics(
    val totalUpdates: Int,
    val totalResolutions: Int,
    val successfulResolutions: Int,
    val successRate: Float,
    val currentScreenType: String?,
    val historySize: Int
)

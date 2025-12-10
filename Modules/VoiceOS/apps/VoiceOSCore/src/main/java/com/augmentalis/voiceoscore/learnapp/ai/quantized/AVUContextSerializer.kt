/**
 * AVUContextSerializer.kt - Serialization for quantized AVU format
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-08
 *
 * Handles serialization/deserialization of quantized AVU format:
 * - Save to .vosq files (quantized format)
 * - Load from .vosq files
 * - Convert between formats (AIContext <-> QuantizedContext)
 * - Generate various LLM prompt formats
 *
 * Part of LearnApp NLU Integration feature
 */
package com.augmentalis.voiceoscore.learnapp.ai.quantized

import android.content.Context
import android.util.Log
import com.augmentalis.voiceoscore.learnapp.ai.AIContext
import com.augmentalis.voiceoscore.learnapp.ai.AIElement
import com.augmentalis.voiceoscore.learnapp.ai.AIGraphStats
import com.augmentalis.voiceoscore.learnapp.ai.AILocation
import com.augmentalis.voiceoscore.learnapp.ai.AINavigationPath
import com.augmentalis.voiceoscore.learnapp.ai.AIScreen
import com.augmentalis.voiceoscore.learnapp.ai.AITriggerElement
import com.augmentalis.voiceoscore.learnapp.ai.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * AVU Context Serializer
 *
 * Handles serialization and format conversion for quantized AVU data.
 *
 * ## File Formats:
 * - `.vos` - Standard AVU format (existing)
 * - `.vosq` - Quantized AVU format (new, more compact)
 *
 * ## LLM Prompt Formats:
 * - Compact (~50-100 tokens)
 * - HTML-like (research-backed, ~200 tokens)
 * - Full context (~500+ tokens)
 *
 * ## Usage:
 * ```kotlin
 * val serializer = AVUContextSerializer(context)
 *
 * // Save quantized context
 * serializer.saveQuantizedContext(quantizedContext)
 *
 * // Load quantized context
 * val loaded = serializer.loadQuantizedContext("com.example.app")
 *
 * // Generate LLM prompt
 * val prompt = serializer.toLLMPrompt(quantizedContext, "open settings")
 * ```
 */
class AVUContextSerializer(private val context: Context) {

    companion object {
        private const val TAG = "AVUContextSerializer"
        private const val LEARNED_APPS_DIR = "learned_apps"
        private const val QUANTIZED_EXTENSION = ".vosq"
    }

    /**
     * Save quantized context to .vosq file
     *
     * @param quantizedContext Quantized context to save
     * @return File if saved successfully, null on error
     */
    suspend fun saveQuantizedContext(quantizedContext: QuantizedContext): File? = withContext(Dispatchers.IO) {
        try {
            val learnedAppsDir = File(context.filesDir, LEARNED_APPS_DIR)
            if (!learnedAppsDir.exists()) {
                learnedAppsDir.mkdirs()
            }

            val packageName = quantizedContext.appContext.packageName
            val safePackageName = packageName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val fileName = "$safePackageName$QUANTIZED_EXTENSION"
            val file = File(learnedAppsDir, fileName)

            val content = quantizedContext.toAVU()
            file.writeText(content)

            Log.i(TAG, "Saved quantized context: ${file.absolutePath}")
            Log.i(TAG, "  Screens: ${quantizedContext.screens.size}")
            Log.i(TAG, "  Elements: ${quantizedContext.elements.values.sumOf { it.size }}")
            Log.i(TAG, "  Navigation: ${quantizedContext.navigation.size}")
            Log.i(TAG, "  File size: ${file.length()} bytes")

            file
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save quantized context", e)
            null
        }
    }

    /**
     * Load quantized context from .vosq file
     *
     * @param packageName Package name to load
     * @return QuantizedContext if found and valid, null otherwise
     */
    suspend fun loadQuantizedContext(packageName: String): QuantizedContext? = withContext(Dispatchers.IO) {
        try {
            val learnedAppsDir = File(context.filesDir, LEARNED_APPS_DIR)
            val safePackageName = packageName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val fileName = "$safePackageName$QUANTIZED_EXTENSION"
            val file = File(learnedAppsDir, fileName)

            if (!file.exists()) {
                Log.w(TAG, "Quantized context not found: ${file.absolutePath}")
                return@withContext null
            }

            val content = file.readText()
            QuantizedContext.fromAVU(content)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load quantized context", e)
            null
        }
    }

    /**
     * Check if quantized context exists for package
     */
    fun quantizedContextExists(packageName: String): Boolean {
        val learnedAppsDir = File(context.filesDir, LEARNED_APPS_DIR)
        val safePackageName = packageName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val fileName = "$safePackageName$QUANTIZED_EXTENSION"
        return File(learnedAppsDir, fileName).exists()
    }

    /**
     * Delete quantized context file
     */
    suspend fun deleteQuantizedContext(packageName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val learnedAppsDir = File(context.filesDir, LEARNED_APPS_DIR)
            val safePackageName = packageName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val fileName = "$safePackageName$QUANTIZED_EXTENSION"
            val file = File(learnedAppsDir, fileName)

            if (file.exists()) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete quantized context", e)
            false
        }
    }

    /**
     * Convert QuantizedContext to standard AIContext
     *
     * Useful for backward compatibility with existing systems.
     */
    fun toAIContext(quantized: QuantizedContext): AIContext {
        val screens = quantized.screens.map { qScreen ->
            val elements = quantized.elements[qScreen.screenId]?.map { qElement ->
                AIElement(
                    uuid = qElement.elementId,
                    label = qElement.label,
                    type = qElement.semanticType.description,
                    actions = qElement.getActionList(),
                    location = null // Quadrant doesn't map directly
                )
            } ?: emptyList()

            AIScreen(
                screenHash = qScreen.screenId,
                activityName = qScreen.purpose,
                elements = elements,
                discoveredAt = quantized.appContext.lastUpdated
            )
        }

        val paths = quantized.navigation.map { qNav ->
            AINavigationPath(
                fromScreen = qNav.fromScreenId,
                toScreen = qNav.toScreenId,
                triggerElement = AITriggerElement(
                    uuid = qNav.triggerElementId,
                    label = qNav.triggerLabel
                ),
                timestamp = quantized.appContext.lastUpdated
            )
        }

        return AIContext(
            appInfo = AppInfo(
                packageName = quantized.appContext.packageName,
                appName = quantized.appContext.appName
            ),
            screens = screens,
            navigationPaths = paths,
            stats = AIGraphStats(
                totalScreens = quantized.screens.size,
                totalElements = quantized.elements.values.sumOf { it.size },
                totalPaths = quantized.navigation.size,
                averageElementsPerScreen = if (quantized.screens.isNotEmpty()) {
                    quantized.elements.values.sumOf { it.size }.toFloat() / quantized.screens.size
                } else 0f,
                maxDepth = quantized.screens.maxOfOrNull { it.depth } ?: 0,
                coverage = quantized.appContext.coverage
            ),
            timestamp = quantized.appContext.lastUpdated
        )
    }

    /**
     * Generate compact LLM prompt (~50-100 tokens)
     *
     * For quick context loading in token-limited scenarios.
     */
    fun toCompactPrompt(quantized: QuantizedContext, userGoal: String): String = buildString {
        appendLine("App: ${quantized.appContext.appName} (${quantized.appContext.appType})")
        appendLine("Goal: $userGoal")
        appendLine()

        // Key screens
        appendLine("Screens (${quantized.screens.size}):")
        quantized.screens.take(5).forEach { screen ->
            append("- ${screen.screenType}: ${screen.purpose}")
            val keyElements = quantized.elements[screen.screenId]?.take(3)
            if (!keyElements.isNullOrEmpty()) {
                append(" [${keyElements.joinToString(", ") { it.label }}]")
            }
            appendLine()
        }

        // Top actions
        appendLine()
        appendLine("Actions: ${quantized.actions.take(5).joinToString(", ") { it.label }}")

        appendLine()
        appendLine("Plan: Navigate to achieve goal using above actions.")
    }

    /**
     * Generate HTML-like LLM prompt (~200 tokens)
     *
     * Research-backed format optimal for LLM understanding.
     */
    fun toHTMLPrompt(quantized: QuantizedContext, userGoal: String): String = buildString {
        appendLine("<!-- App Context for: $userGoal -->")
        appendLine(quantized.toHTMLRepresentation())
        appendLine()
        appendLine("<!-- Goal: $userGoal -->")
        appendLine("<!-- Provide: action sequence with element IDs -->")
    }

    /**
     * Generate full context LLM prompt (~500+ tokens)
     *
     * Complete context for complex reasoning tasks.
     */
    fun toFullPrompt(quantized: QuantizedContext, userGoal: String): String = buildString {
        appendLine("# App Navigation Context")
        appendLine()
        appendLine("**Application**: ${quantized.appContext.appName}")
        appendLine("**Package**: ${quantized.appContext.packageName}")
        appendLine("**Type**: ${quantized.appContext.appType.description}")
        appendLine("**User Goal**: $userGoal")
        appendLine()

        // Statistics
        appendLine("## Statistics")
        appendLine("- Screens: ${quantized.screens.size}")
        appendLine("- Elements: ${quantized.elements.values.sumOf { it.size }}")
        appendLine("- Navigation Paths: ${quantized.navigation.size}")
        appendLine("- Coverage: ${"%.1f".format(quantized.appContext.coverage)}%")
        appendLine("- Complexity: ${quantized.appContext.navigationComplexity}/5")
        appendLine()

        // Screens with elements
        appendLine("## Screen Hierarchy")
        appendLine()

        for (screen in quantized.screens.sortedBy { it.depth }) {
            val indent = "  ".repeat(screen.depth)
            appendLine("$indent### ${screen.screenType}: ${screen.purpose}")
            appendLine("${indent}ID: ${screen.screenId}, Depth: ${screen.depth}")
            if (screen.scrollDirection != ScrollDirection.NONE) {
                appendLine("${indent}Scrollable: ${screen.scrollDirection}")
            }
            appendLine()

            quantized.elements[screen.screenId]?.let { elements ->
                appendLine("${indent}**Elements (${elements.size}):**")
                elements.take(10).forEach { element ->
                    val actions = element.getActionList().joinToString("/")
                    appendLine("$indent- [${element.semanticType.code}] ${element.label} ($actions) Q${element.quadrant}")
                }
                if (elements.size > 10) {
                    appendLine("$indent- ... and ${elements.size - 10} more")
                }
            }
            appendLine()
        }

        // Navigation paths
        if (quantized.navigation.isNotEmpty()) {
            appendLine("## Navigation Paths")
            appendLine()
            quantized.navigation.sortedByDescending { it.frequency }.take(15).forEach { nav ->
                appendLine("- ${nav.fromScreenId} → ${nav.toScreenId} via \"${nav.triggerLabel}\" (${nav.pathType})")
            }
            appendLine()
        }

        // Semantic clusters
        if (quantized.clusters.isNotEmpty()) {
            appendLine("## Functional Areas")
            appendLine()
            quantized.clusters.forEach { cluster ->
                appendLine("- **${cluster.name}**: ${cluster.elements.size} elements across ${cluster.screens.size} screens")
            }
            appendLine()
        }

        // Instructions
        appendLine("## Task")
        appendLine()
        appendLine("Determine the action sequence to achieve: \"$userGoal\"")
        appendLine()
        appendLine("Provide:")
        appendLine("1. **Step-by-step plan** with screen and element IDs")
        appendLine("2. **Confidence level** (low/medium/high)")
        appendLine("3. **Alternative paths** if available")
    }

    /**
     * Generate action prediction prompt
     *
     * Optimized for NLU action prediction tasks.
     */
    fun toActionPredictionPrompt(
        quantized: QuantizedContext,
        currentScreenId: String,
        userIntent: String
    ): String = buildString {
        val currentScreen = quantized.screens.find { it.screenId == currentScreenId }
        val currentElements = quantized.elements[currentScreenId] ?: emptyList()

        appendLine("Current: ${currentScreen?.purpose ?: "Unknown"}")
        appendLine("Intent: $userIntent")
        appendLine()
        appendLine("Available actions:")

        currentElements.sortedByDescending { it.importance }.take(10).forEach { element ->
            val actions = element.getActionList().joinToString("/")
            appendLine("- ${element.elementId}: ${element.label} [$actions]")
        }

        // Add navigation options
        val navOptions = quantized.navigation.filter { it.fromScreenId == currentScreenId }
        if (navOptions.isNotEmpty()) {
            appendLine()
            appendLine("Navigation:")
            navOptions.forEach { nav ->
                val destScreen = quantized.screens.find { it.screenId == nav.toScreenId }
                appendLine("- ${nav.triggerLabel} → ${destScreen?.purpose ?: nav.toScreenId}")
            }
        }

        appendLine()
        appendLine("Select: element_id or describe action")
    }

    /**
     * Get token estimate for a prompt
     *
     * Approximate token count (1 token ~= 4 chars for English)
     */
    fun estimateTokens(text: String): Int {
        return (text.length / 4.0).toInt()
    }

    /**
     * Get all saved quantized contexts
     */
    suspend fun listQuantizedContexts(): List<String> = withContext(Dispatchers.IO) {
        try {
            val learnedAppsDir = File(context.filesDir, LEARNED_APPS_DIR)
            if (!learnedAppsDir.exists()) return@withContext emptyList()

            learnedAppsDir.listFiles()
                ?.filter { it.extension == "vosq" }
                ?.map { it.nameWithoutExtension }
                ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list quantized contexts", e)
            emptyList()
        }
    }
}

/**
 * Extension function to generate prompt based on format preference
 */
fun QuantizedContext.toLLMPrompt(
    userGoal: String,
    format: LLMPromptFormat = LLMPromptFormat.COMPACT
): String = when (format) {
    LLMPromptFormat.COMPACT -> toCompactLLMContext() + "\nGoal: $userGoal"
    LLMPromptFormat.HTML -> toHTMLRepresentation() + "\n<!-- Goal: $userGoal -->"
    LLMPromptFormat.FULL -> buildString {
        appendLine("App: ${appContext.appName}")
        appendLine("Screens: ${screens.size}")
        appendLine("Goal: $userGoal")
        appendLine()
        screens.forEach { screen ->
            appendLine("Screen: ${screen.purpose}")
            elements[screen.screenId]?.forEach { el ->
                appendLine("  - ${el.label} (${el.semanticType.code})")
            }
        }
    }
}

/**
 * LLM Prompt Format Options
 */
enum class LLMPromptFormat {
    COMPACT,  // ~50-100 tokens
    HTML,     // ~200 tokens, research-backed
    FULL      // ~500+ tokens
}

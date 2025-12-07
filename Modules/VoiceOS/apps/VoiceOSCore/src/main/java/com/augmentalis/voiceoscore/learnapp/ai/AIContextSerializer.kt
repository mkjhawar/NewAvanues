/**
 * AIContextSerializer.kt - AI context serialization and prompt generation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-12-02
 * Updated: 2025-12-05
 *
 * Converts NavigationGraph to AI-consumable formats:
 * - AVU format (.vos) for persistence
 * - Natural language prompts for LLMs
 * - Structured context for AI agents
 * - Deserialization from .vos files
 *
 * Part of Voice Command AI Integration feature
 */
package com.augmentalis.voiceoscore.learnapp.ai

import android.util.Log
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.voiceoscore.learnapp.navigation.NavigationGraph
import com.augmentalis.voiceoscore.learnapp.navigation.ScreenNode
import com.augmentalis.voiceoscore.learnapp.models.NavigationEdge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * AI Context Serializer
 *
 * Converts NavigationGraph to AI-consumable formats.
 *
 * ## Features:
 * - **JSON Export**: Machine-readable JSON format
 * - **File Export**: Save to .vos files in learned_apps directory
 * - **LLM Prompts**: Natural language context for language models
 * - **Element Details**: Joins graph data with element properties from database
 * - **Path Finding**: Provides navigation paths for AI planning
 *
 * ## Usage:
 * ```kotlin
 * val serializer = AIContextSerializer(context, databaseManager)
 *
 * // Generate AI context from graph
 * val aiContext = serializer.generateContext(navigationGraph)
 *
 * // Export to JSON
 * val json = serializer.toJSON(aiContext)
 *
 * // Save to .vos file
 * val file = serializer.saveToFile(aiContext)
 *
 * // Generate LLM prompt
 * val prompt = serializer.toLLMPrompt(aiContext, "Open settings")
 * ```
 *
 * @param context Android context for file access
 * @param databaseManager Database manager for element lookups
 */
class AIContextSerializer(
    private val context: android.content.Context,
    private val databaseManager: VoiceOSDatabaseManager
) {
    companion object {
        private const val TAG = "AIContextSerializer"
        private const val LEARNED_APPS_DIR = "learned_apps"
    }

    /**
     * Generate AI Context from NavigationGraph
     *
     * Converts NavigationGraph to AIContext by:
     * 1. Extracting screen nodes
     * 2. Looking up element details from database
     * 3. Mapping navigation edges to paths
     * 4. Calculating statistics
     *
     * @param graph Navigation graph
     * @return AI-consumable context
     */
    suspend fun generateContext(graph: NavigationGraph): AIContext = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        try {
            // Convert screens
            val aiScreens = graph.nodes.values.map { screenNode ->
                convertScreenNode(screenNode, graph.packageName)
            }

            // Convert navigation paths
            val aiPaths = graph.edges.map { edge ->
                convertNavigationEdge(edge, graph.packageName)
            }

            // Calculate stats
            val graphStats = graph.getStats()
            val aiStats = AIGraphStats(
                totalScreens = graphStats.totalScreens,
                totalElements = aiScreens.sumOf { it.elements.size },
                totalPaths = aiPaths.size,
                averageElementsPerScreen = if (aiScreens.isEmpty()) 0f else {
                    aiScreens.sumOf { it.elements.size }.toFloat() / aiScreens.size.toFloat()
                },
                maxDepth = graphStats.maxDepth,
                coverage = estimateCoverage(graph)
            )

            val elapsed = System.currentTimeMillis() - startTime
            Log.i(TAG, "Generated AI context in ${elapsed}ms: ${aiStats.totalScreens} screens, ${aiStats.totalElements} elements")

            AIContext(
                appInfo = AppInfo(
                    packageName = graph.packageName,
                    appName = null  // TODO: Look up from database if needed
                ),
                screens = aiScreens,
                navigationPaths = aiPaths,
                stats = aiStats
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error generating AI context", e)
            // Return empty context on error
            AIContext(
                appInfo = AppInfo(graph.packageName),
                screens = emptyList(),
                navigationPaths = emptyList(),
                stats = AIGraphStats(0, 0, 0, 0f, 0)
            )
        }
    }

    /**
     * Convert ScreenNode to AIScreen
     *
     * Looks up element details from database and converts to AI format.
     */
    private suspend fun convertScreenNode(screenNode: ScreenNode, packageName: String): AIScreen {
        val aiElements = screenNode.elements.mapNotNull { uuid ->
            try {
                // Look up element details from database
                val element = databaseManager.scrapedElements.getByUuid(packageName, uuid)
                if (element != null) {
                    AIElement(
                        uuid = uuid,
                        label = element.text ?: element.contentDescription ?: element.viewIdResourceName ?: "Unknown",
                        type = element.className,
                        actions = buildActionsList(
                            isClickable = element.isClickable > 0,
                            isLongClickable = element.isLongClickable > 0,
                            isEditable = element.isEditable > 0,
                            isScrollable = element.isScrollable > 0
                        ),
                        location = parseLocation(element.bounds)
                    )
                } else {
                    Log.w(TAG, "Element not found in database: $uuid")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error converting element $uuid", e)
                null
            }
        }

        return AIScreen(
            screenHash = screenNode.screenHash,
            activityName = screenNode.activityName,
            elements = aiElements,
            discoveredAt = screenNode.timestamp
        )
    }

    /**
     * Convert NavigationEdge to AINavigationPath
     *
     * Looks up clicked element details from database.
     */
    private suspend fun convertNavigationEdge(edge: NavigationEdge, packageName: String): AINavigationPath {
        // Look up clicked element for label
        val element = try {
            databaseManager.scrapedElements.getByUuid(packageName, edge.clickedElementUuid)
        } catch (e: Exception) {
            Log.e(TAG, "Error looking up element ${edge.clickedElementUuid}", e)
            null
        }

        val label = element?.text ?: element?.contentDescription ?: element?.viewIdResourceName ?: "Unknown"

        return AINavigationPath(
            fromScreen = edge.fromScreenHash,
            toScreen = edge.toScreenHash,
            triggerElement = AITriggerElement(
                uuid = edge.clickedElementUuid,
                label = label
            ),
            timestamp = edge.timestamp
        )
    }

    /**
     * Build list of available actions for element
     */
    private fun buildActionsList(
        isClickable: Boolean,
        isLongClickable: Boolean,
        isEditable: Boolean,
        isScrollable: Boolean
    ): List<String> {
        val actions = mutableListOf<String>()
        if (isClickable) actions.add("click")
        if (isLongClickable) actions.add("longClick")
        if (isEditable) actions.add("edit")
        if (isScrollable) actions.add("scroll")
        return actions
    }

    /**
     * Parse location from bounds string
     *
     * Format: "left,top,right,bottom"
     */
    private fun parseLocation(bounds: String?): AILocation? {
        if (bounds == null) return null

        return try {
            val parts = bounds.split(",")
            if (parts.size == 4) {
                AILocation(
                    left = parts[0].toInt(),
                    top = parts[1].toInt(),
                    right = parts[2].toInt(),
                    bottom = parts[3].toInt()
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse bounds: $bounds")
            null
        }
    }

    /**
     * Estimate coverage percentage
     *
     * Simple heuristic: coverage increases with edge density
     */
    private fun estimateCoverage(graph: NavigationGraph): Float {
        if (graph.nodes.isEmpty()) return 0f

        // Coverage based on edge density
        val avgOutDegree = graph.getStats().averageOutDegree

        // Heuristic: 2+ edges per screen = ~80% coverage
        // 1 edge per screen = ~50% coverage
        // 0.5 edges per screen = ~25% coverage
        val coverage = (avgOutDegree / 2.5f) * 100f

        return coverage.coerceIn(0f, 100f)
    }

    /**
     * Serialize AIContext to AVU format (.vos)
     *
     * Generates AVU (Avanues Universal Format) compliant .vos file.
     * Schema: avu-1.0
     *
     * IPC Codes for LearnApp:
     * - APP: App metadata (package:name:learned_at)
     * - STA: Statistics (screens:elements:paths:avg_elements:max_depth:coverage)
     * - SCR: Screen definition (hash:activity:discovered_at:element_count)
     * - ELM: Element definition (uuid:label:type:actions:location)
     * - NAV: Navigation path (from_hash:to_hash:trigger_uuid:trigger_label:timestamp)
     *
     * @param context AI context
     * @return AVU-formatted string
     */
    fun toJSON(context: AIContext): String {
        val output = StringBuilder()

        // AVU Header
        output.appendLine("# Avanues Universal Format v1.0")
        output.appendLine("# Type: VOS")
        output.appendLine("# Extension: .vos")
        output.appendLine("---")

        // Schema and metadata
        output.appendLine("schema: avu-1.0")
        output.appendLine("version: 1.0.0")
        output.appendLine("locale: en-US")
        output.appendLine("project: voiceos")
        output.appendLine("metadata:")

        val safePackageName = context.appInfo.packageName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        output.appendLine("  file: $safePackageName.vos")
        output.appendLine("  category: learned_app")

        // Total item count (app + stats + screens + elements + paths)
        val totalItems = 1 + 1 + context.screens.size +
                        context.screens.sumOf { it.elements.size } +
                        context.navigationPaths.size
        output.appendLine("  count: $totalItems")
        output.appendLine("---")

        // App metadata (APP:package:name:learned_at)
        val appName = context.appInfo.appName ?: "Unknown"
        output.appendLine("APP:${context.appInfo.packageName}:$appName:${context.timestamp}")

        // Statistics (STA:screens:elements:paths:avg_elements:max_depth:coverage)
        output.appendLine("STA:${context.stats.totalScreens}:${context.stats.totalElements}:${context.stats.totalPaths}:${context.stats.averageElementsPerScreen}:${context.stats.maxDepth}:${context.stats.coverage}")

        // Screens (SCR:hash:activity:discovered_at:element_count)
        for (screen in context.screens) {
            val activity = screen.activityName ?: "Unknown"
            output.appendLine("SCR:${screen.screenHash}:$activity:${screen.discoveredAt}:${screen.elements.size}")

            // Elements for this screen (ELM:uuid:label:type:actions:location)
            for (element in screen.elements) {
                val actions = element.actions.joinToString("+")
                val location = element.location?.let { "${it.left},${it.top},${it.right},${it.bottom}" } ?: ""
                output.appendLine("ELM:${element.uuid}:${element.label}:${element.type}:$actions:$location")
            }
        }

        // Navigation paths (NAV:from_hash:to_hash:trigger_uuid:trigger_label:timestamp)
        for (path in context.navigationPaths) {
            output.appendLine("NAV:${path.fromScreen}:${path.toScreen}:${path.triggerElement.uuid}:${path.triggerElement.label}:${path.timestamp}")
        }

        output.appendLine("---")

        // Synonyms section (for common element labels)
        // This can be expanded to include learned synonyms
        output.appendLine("synonyms:")
        output.appendLine("  settings: [preferences, options, config]")
        output.appendLine("  back: [return, previous, go back]")
        output.appendLine("  next: [continue, forward, proceed]")

        return output.toString()
    }

    /**
     * Save AI Context to .vos file
     *
     * Saves the AI context to a .vos file in the learned_apps directory.
     * File location: <app_files>/learned_apps/<package_name>.vos
     *
     * @param aiContext AI context to save
     * @return File reference if successful, null if error
     */
    suspend fun saveToFile(aiContext: AIContext): java.io.File? = withContext(Dispatchers.IO) {
        try {
            // Create learned_apps directory if it doesn't exist
            val learnedAppsDir = java.io.File(context.filesDir, LEARNED_APPS_DIR)
            if (!learnedAppsDir.exists()) {
                if (!learnedAppsDir.mkdirs()) {
                    Log.e(TAG, "Failed to create learned_apps directory: ${learnedAppsDir.absolutePath}")
                    return@withContext null
                }
                Log.i(TAG, "Created learned_apps directory: ${learnedAppsDir.absolutePath}")
            }

            // Generate filename from package name
            val packageName = aiContext.appInfo.packageName
            val safePackageName = packageName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val fileName = "$safePackageName.vos"
            val vosFile = java.io.File(learnedAppsDir, fileName)

            // Generate JSON content
            val jsonContent = toJSON(aiContext)

            // Write to file
            vosFile.writeText(jsonContent)

            Log.i(TAG, "✅ Saved AI context to: ${vosFile.absolutePath}")
            Log.i(TAG, "   Package: $packageName")
            Log.i(TAG, "   Screens: ${aiContext.stats.totalScreens}")
            Log.i(TAG, "   Elements: ${aiContext.stats.totalElements}")
            Log.i(TAG, "   File size: ${vosFile.length()} bytes")

            vosFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save AI context to file", e)
            null
        }
    }

    /**
     * Load AI Context from .vos file
     *
     * Loads a previously saved AI context from a .vos file.
     *
     * @param packageName Package name of the app
     * @return AIContext if file exists and is valid, null otherwise
     */
    suspend fun loadFromFile(packageName: String): AIContext? = withContext(Dispatchers.IO) {
        try {
            val learnedAppsDir = java.io.File(context.filesDir, LEARNED_APPS_DIR)
            val safePackageName = packageName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val fileName = "$safePackageName.vos"
            val vosFile = java.io.File(learnedAppsDir, fileName)

            if (!vosFile.exists()) {
                Log.w(TAG, "AI context file not found: ${vosFile.absolutePath}")
                return@withContext null
            }

            val vosContent = vosFile.readText()
            deserializeContext(vosContent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load AI context from file", e)
            null
        }
    }

    /**
     * Deserialize AIContext from AVU format
     *
     * Parses AVU (Avanues Universal Format) .vos content back to AIContext.
     *
     * IPC Codes:
     * - APP: App metadata (package:name:learned_at)
     * - STA: Statistics (screens:elements:paths:avg_elements:max_depth:coverage)
     * - SCR: Screen definition (hash:activity:discovered_at:element_count)
     * - ELM: Element definition (uuid:label:type:actions:location)
     * - NAV: Navigation path (from_hash:to_hash:trigger_uuid:trigger_label:timestamp)
     *
     * @param vosContent AVU-formatted string
     * @return AIContext if valid, null if parsing fails
     */
    private fun deserializeContext(vosContent: String): AIContext? {
        if (vosContent.isBlank()) {
            Log.w(TAG, "Empty VOS content")
            return null
        }

        try {
            val lines = vosContent.lines()
            var appInfo: AppInfo? = null
            var stats: AIGraphStats? = null
            var timestamp: Long = System.currentTimeMillis()
            val screens = mutableListOf<AIScreen>()
            val navigationPaths = mutableListOf<AINavigationPath>()
            var currentScreen: AIScreen? = null
            val currentElements = mutableListOf<AIElement>()

            for (line in lines) {
                // Skip comments, empty lines, and metadata lines
                if (line.isBlank() || line.startsWith("#") || line.startsWith("---") ||
                    line.startsWith("schema:") || line.startsWith("version:") ||
                    line.startsWith("locale:") || line.startsWith("project:") ||
                    line.startsWith("metadata:") || line.startsWith("  ") ||
                    line.startsWith("synonyms:")
                ) {
                    continue
                }

                val parts = line.split(":", limit = 2)
                if (parts.size < 2) continue

                val code = parts[0]
                val data = parts[1]

                when (code) {
                    "APP" -> {
                        // APP:package:name:learned_at
                        val appParts = data.split(":")
                        if (appParts.size >= 3) {
                            appInfo = AppInfo(
                                packageName = appParts[0],
                                appName = appParts[1].takeIf { it != "Unknown" }
                            )
                            timestamp = appParts[2].toLongOrNull() ?: System.currentTimeMillis()
                        }
                    }

                    "STA" -> {
                        // STA:screens:elements:paths:avg_elements:max_depth:coverage
                        val statParts = data.split(":")
                        if (statParts.size >= 6) {
                            stats = AIGraphStats(
                                totalScreens = statParts[0].toIntOrNull() ?: 0,
                                totalElements = statParts[1].toIntOrNull() ?: 0,
                                totalPaths = statParts[2].toIntOrNull() ?: 0,
                                averageElementsPerScreen = statParts[3].toFloatOrNull() ?: 0f,
                                maxDepth = statParts[4].toIntOrNull() ?: 0,
                                coverage = statParts[5].toFloatOrNull() ?: 0f
                            )
                        }
                    }

                    "SCR" -> {
                        // Save previous screen if exists
                        if (currentScreen != null) {
                            screens.add(currentScreen.copy(elements = currentElements.toList()))
                            currentElements.clear()
                        }

                        // SCR:hash:activity:discovered_at:element_count
                        val scrParts = data.split(":")
                        if (scrParts.size >= 4) {
                            currentScreen = AIScreen(
                                screenHash = scrParts[0],
                                activityName = scrParts[1].takeIf { it != "Unknown" },
                                elements = emptyList(), // Will be populated by ELM entries
                                discoveredAt = scrParts[2].toLongOrNull() ?: System.currentTimeMillis()
                            )
                        }
                    }

                    "ELM" -> {
                        // ELM:uuid:label:type:actions:location
                        val elmParts = data.split(":")
                        if (elmParts.size >= 5) {
                            val actions = if (elmParts[3].isNotBlank()) {
                                elmParts[3].split("+")
                            } else {
                                emptyList()
                            }

                            val location = if (elmParts[4].isNotBlank()) {
                                val locParts = elmParts[4].split(",")
                                if (locParts.size == 4) {
                                    AILocation(
                                        left = locParts[0].toIntOrNull() ?: 0,
                                        top = locParts[1].toIntOrNull() ?: 0,
                                        right = locParts[2].toIntOrNull() ?: 0,
                                        bottom = locParts[3].toIntOrNull() ?: 0
                                    )
                                } else null
                            } else null

                            currentElements.add(
                                AIElement(
                                    uuid = elmParts[0],
                                    label = elmParts[1],
                                    type = elmParts[2],
                                    actions = actions,
                                    location = location
                                )
                            )
                        }
                    }

                    "NAV" -> {
                        // NAV:from_hash:to_hash:trigger_uuid:trigger_label:timestamp
                        val navParts = data.split(":")
                        if (navParts.size >= 5) {
                            navigationPaths.add(
                                AINavigationPath(
                                    fromScreen = navParts[0],
                                    toScreen = navParts[1],
                                    triggerElement = AITriggerElement(
                                        uuid = navParts[2],
                                        label = navParts[3]
                                    ),
                                    timestamp = navParts[4].toLongOrNull() ?: System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }
            }

            // Save last screen if exists
            if (currentScreen != null) {
                screens.add(currentScreen.copy(elements = currentElements.toList()))
            }

            // Validate required fields
            if (appInfo == null) {
                Log.e(TAG, "Missing APP entry in VOS file")
                return null
            }

            // Use default stats if not provided
            val finalStats = stats ?: AIGraphStats(
                totalScreens = screens.size,
                totalElements = screens.sumOf { it.elements.size },
                totalPaths = navigationPaths.size,
                averageElementsPerScreen = if (screens.isEmpty()) 0f else {
                    screens.sumOf { it.elements.size }.toFloat() / screens.size.toFloat()
                },
                maxDepth = 0,
                coverage = 0f
            )

            val aiContext = AIContext(
                appInfo = appInfo,
                screens = screens,
                navigationPaths = navigationPaths,
                stats = finalStats,
                timestamp = timestamp
            )

            Log.i(TAG, "Successfully deserialized AI context: ${finalStats.totalScreens} screens, ${finalStats.totalElements} elements")
            return aiContext

        } catch (e: Exception) {
            Log.e(TAG, "Error deserializing VOS content", e)
            return null
        }
    }

    /**
     * Check if AI context exists for package
     *
     * @param packageName Package name to check
     * @return True if .vos file exists
     */
    fun contextExists(packageName: String): Boolean {
        val learnedAppsDir = java.io.File(context.filesDir, LEARNED_APPS_DIR)
        val safePackageName = packageName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val fileName = "$safePackageName.vos"
        val vosFile = java.io.File(learnedAppsDir, fileName)
        return vosFile.exists()
    }

    /**
     * Delete AI context file for package
     *
     * @param packageName Package name
     * @return True if deleted successfully
     */
    suspend fun deleteContext(packageName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val learnedAppsDir = java.io.File(context.filesDir, LEARNED_APPS_DIR)
            val safePackageName = packageName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val fileName = "$safePackageName.vos"
            val vosFile = java.io.File(learnedAppsDir, fileName)

            if (vosFile.exists()) {
                val deleted = vosFile.delete()
                if (deleted) {
                    Log.i(TAG, "Deleted AI context file: ${vosFile.absolutePath}")
                } else {
                    Log.w(TAG, "Failed to delete AI context file: ${vosFile.absolutePath}")
                }
                deleted
            } else {
                Log.w(TAG, "AI context file not found for deletion: ${vosFile.absolutePath}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting AI context file", e)
            false
        }
    }

    /**
     * Generate LLM Prompt from AIContext
     *
     * Creates natural language context for language models to understand
     * the app structure and available actions.
     *
     * @param context AI context
     * @param userGoal User's goal (e.g., "Open settings")
     * @return LLM-ready prompt
     */
    fun toLLMPrompt(context: AIContext, userGoal: String): String {
        val prompt = StringBuilder()

        // Header
        prompt.appendLine("# App Navigation Context")
        prompt.appendLine()
        prompt.appendLine("**App**: ${context.appInfo.packageName}")
        context.appInfo.appName?.let { prompt.appendLine("**Name**: $it") }
        prompt.appendLine("**User Goal**: $userGoal")
        prompt.appendLine()

        // Statistics
        prompt.appendLine("## Statistics")
        prompt.appendLine("- **Screens Discovered**: ${context.stats.totalScreens}")
        prompt.appendLine("- **Total Actionable Elements**: ${context.stats.totalElements}")
        prompt.appendLine("- **Navigation Paths**: ${context.stats.totalPaths}")
        prompt.appendLine("- **Coverage**: ${"%.1f".format(context.stats.coverage)}%")
        prompt.appendLine()

        // Screens and elements
        prompt.appendLine("## Available Screens and Actions")
        prompt.appendLine()

        for ((index, screen) in context.screens.withIndex()) {
            prompt.appendLine("### Screen ${index + 1}")
            screen.activityName?.let { prompt.appendLine("**Activity**: $it") }
            prompt.appendLine("**Screen ID**: ${screen.screenHash.take(8)}...")
            prompt.appendLine()

            if (screen.elements.isNotEmpty()) {
                prompt.appendLine("**Actionable Elements**:")
                for (element in screen.elements) {
                    val actions = element.actions.joinToString(", ")
                    prompt.appendLine("- **${element.label}** (${element.type})")
                    prompt.appendLine("  - UUID: ${element.uuid}")
                    prompt.appendLine("  - Actions: $actions")
                }
            } else {
                prompt.appendLine("*No actionable elements found*")
            }
            prompt.appendLine()
        }

        // Navigation paths
        if (context.navigationPaths.isNotEmpty()) {
            prompt.appendLine("## Navigation Paths")
            prompt.appendLine()

            for (path in context.navigationPaths) {
                val fromScreen = context.screens.find { it.screenHash == path.fromScreen }
                val toScreen = context.screens.find { it.screenHash == path.toScreen }

                val fromLabel = fromScreen?.activityName ?: path.fromScreen.take(8)
                val toLabel = toScreen?.activityName ?: path.toScreen.take(8)

                prompt.appendLine("- **$fromLabel** → **$toLabel**")
                prompt.appendLine("  - Trigger: ${path.triggerElement.label}")
            }
            prompt.appendLine()
        }

        // Instructions for AI
        prompt.appendLine("## Instructions")
        prompt.appendLine()
        prompt.appendLine("Based on the above navigation context, determine the sequence of actions")
        prompt.appendLine("needed to achieve the user goal: \"$userGoal\"")
        prompt.appendLine()
        prompt.appendLine("Provide:")
        prompt.appendLine("1. **Action Plan**: Step-by-step navigation path")
        prompt.appendLine("2. **Element UUIDs**: Specific elements to interact with")
        prompt.appendLine("3. **Confidence**: Your confidence level (low/medium/high)")
        prompt.appendLine()

        return prompt.toString()
    }

    /**
     * Generate Compact Prompt
     *
     * Shorter version for token-limited LLMs.
     *
     * @param context AI context
     * @param userGoal User's goal
     * @return Compact LLM prompt
     */
    fun toCompactPrompt(context: AIContext, userGoal: String): String {
        val prompt = StringBuilder()

        prompt.appendLine("App: ${context.appInfo.packageName}")
        prompt.appendLine("Goal: $userGoal")
        prompt.appendLine("Screens: ${context.stats.totalScreens}, Elements: ${context.stats.totalElements}")
        prompt.appendLine()

        // Screens with key elements only
        for (screen in context.screens) {
            prompt.append("Screen ${screen.screenHash.take(8)}: ")
            val labels = screen.elements.map { it.label }.take(5)
            prompt.appendLine(labels.joinToString(", "))
        }

        prompt.appendLine()
        prompt.appendLine("Provide action plan to achieve goal.")

        return prompt.toString()
    }
}

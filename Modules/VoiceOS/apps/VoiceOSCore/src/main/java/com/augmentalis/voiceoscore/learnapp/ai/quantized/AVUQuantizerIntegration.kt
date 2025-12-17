/**
 * AVUQuantizerIntegration.kt - Integration layer for AVU Quantizer
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-17
 *
 * Provides NLU-optimized context from learned app data.
 * Converts raw exploration data into compact, LLM-friendly representations.
 *
 * Key capabilities:
 * - Generate QuantizedContext from learned app data
 * - Create LLM prompts with configurable formats
 * - Action prediction for voice commands
 * - Context caching for performance
 */
package com.augmentalis.voiceoscore.learnapp.ai.quantized

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * AVU Quantizer Integration
 *
 * Main entry point for quantized context generation and LLM prompt creation.
 * Converts exploration data into compact representations suitable for NLU/LLM consumption.
 */
class AVUQuantizerIntegration(private val context: Context) {

    companion object {
        private const val TAG = "AVUQuantizerIntegration"
        private const val CACHE_EXPIRY_MS = 5 * 60 * 1000L // 5 minutes
    }

    // Cache for quantized contexts
    private val contextCache = ConcurrentHashMap<String, CachedContext>()
    private val cacheMutex = Mutex()

    // Package manager for app info
    private val packageManager: PackageManager = context.packageManager

    /**
     * Get quantized context for an app
     *
     * Returns a compact, NLU-optimized representation of the app's learned UI structure.
     *
     * @param packageName Package name of the learned app
     * @return QuantizedContext if available, null if app not learned
     */
    suspend fun getQuantizedContext(packageName: String): QuantizedContext? = withContext(Dispatchers.IO) {
        // Check cache first
        val cached = getCachedContext(packageName)
        if (cached != null) {
            Log.d(TAG, "Returning cached context for $packageName")
            return@withContext cached
        }

        // Generate new context from learned data
        val quantizedContext = generateQuantizedContext(packageName)

        // Cache the result
        if (quantizedContext != null) {
            cacheContext(packageName, quantizedContext)
        }

        quantizedContext
    }

    /**
     * Check if quantized context exists for an app
     *
     * @param packageName Package name to check
     * @return true if context is available (either cached or can be generated)
     */
    fun hasQuantizedContext(packageName: String): Boolean {
        // Check cache
        if (contextCache.containsKey(packageName)) {
            return true
        }

        // Check if app has been learned (has exploration data)
        return hasLearnedData(packageName)
    }

    /**
     * List all packages with quantized contexts available
     *
     * @return List of package names
     */
    suspend fun listQuantizedPackages(): List<String> = withContext(Dispatchers.IO) {
        // Return packages that have learned data
        getLearnedPackages()
    }

    /**
     * Generate LLM prompt for a user goal
     *
     * Creates a prompt suitable for LLM consumption based on the app's quantized context.
     *
     * @param packageName Package name of the learned app
     * @param userGoal User's goal (e.g., "open settings", "send message")
     * @param format Prompt format (COMPACT, HTML, or FULL)
     * @return LLM-ready prompt string, or null if no context available
     */
    suspend fun generateLLMPrompt(
        packageName: String,
        userGoal: String,
        format: LLMPromptFormat
    ): String? = withContext(Dispatchers.IO) {
        val quantizedContext = getQuantizedContext(packageName) ?: return@withContext null

        when (format) {
            LLMPromptFormat.COMPACT -> generateCompactPrompt(quantizedContext, userGoal)
            LLMPromptFormat.HTML -> generateHtmlPrompt(quantizedContext, userGoal)
            LLMPromptFormat.FULL -> generateFullPrompt(quantizedContext, userGoal)
        }
    }

    /**
     * Generate action prediction prompt for current screen
     *
     * Creates a prompt optimized for predicting the next action based on user intent.
     *
     * @param packageName Package name of the app
     * @param currentScreenHash Hash of the current screen
     * @param userIntent User's intent (e.g., "go back", "tap search")
     * @return Action prediction prompt, or null if no context available
     */
    suspend fun generateActionPredictionPrompt(
        packageName: String,
        currentScreenHash: String,
        userIntent: String
    ): String? = withContext(Dispatchers.IO) {
        val quantizedContext = getQuantizedContext(packageName) ?: return@withContext null
        val currentScreen = quantizedContext.findScreen(currentScreenHash)
            ?: return@withContext null

        buildString {
            appendLine("App: ${quantizedContext.appName}")
            appendLine("Screen: ${currentScreen.screenTitle}")
            appendLine("User wants: $userIntent")
            appendLine()
            appendLine("Available actions:")

            currentScreen.elements.take(10).forEachIndexed { index, element ->
                appendLine("${index + 1}. [${element.type.name}] ${element.label}")
            }

            appendLine()
            appendLine("Navigation options:")
            quantizedContext.getNavigationFrom(currentScreenHash).take(5).forEach { nav ->
                appendLine("- ${nav.triggerLabel} -> ${nav.toScreenHash.take(8)}")
            }

            appendLine()
            appendLine("Predict the best action to achieve the user's goal.")
        }
    }

    /**
     * Invalidate cached context for an app
     *
     * Call this when app data is updated through new learning.
     *
     * @param packageName Package name to invalidate
     */
    suspend fun invalidateCache(packageName: String) = cacheMutex.withLock {
        contextCache.remove(packageName)
        Log.d(TAG, "Invalidated cache for $packageName")
    }

    /**
     * Clear all cached contexts
     */
    suspend fun clearCache() = cacheMutex.withLock {
        contextCache.clear()
        Log.d(TAG, "Cleared all cached contexts")
    }

    // ============================================
    // Private Implementation
    // ============================================

    private suspend fun getCachedContext(packageName: String): QuantizedContext? = cacheMutex.withLock {
        val cached = contextCache[packageName] ?: return@withLock null

        // Check if cache has expired
        if (System.currentTimeMillis() - cached.timestamp > CACHE_EXPIRY_MS) {
            contextCache.remove(packageName)
            return@withLock null
        }

        cached.context
    }

    private suspend fun cacheContext(packageName: String, quantizedContext: QuantizedContext) = cacheMutex.withLock {
        contextCache[packageName] = CachedContext(
            context = quantizedContext,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun generateQuantizedContext(packageName: String): QuantizedContext? {
        try {
            // Get app info
            val appInfo = try {
                packageManager.getApplicationInfo(packageName, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                Log.w(TAG, "App not found: $packageName")
                return null
            }

            val appName = packageManager.getApplicationLabel(appInfo).toString()
            val packageInfo = packageManager.getPackageInfo(packageName, 0)

            // Build quantized context from learned data
            // In a real implementation, this would query the database for learned screens/elements
            val screens = buildQuantizedScreens(packageName)
            val navigation = buildQuantizedNavigation(packageName, screens)
            val vocabulary = buildVocabulary(screens)
            val commands = buildKnownCommands(packageName)

            return QuantizedContext(
                packageName = packageName,
                appName = appName,
                versionCode = packageInfo.longVersionCode,
                versionName = packageInfo.versionName ?: "unknown",
                generatedAt = System.currentTimeMillis(),
                screens = screens,
                navigation = navigation,
                vocabulary = vocabulary,
                knownCommands = commands
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate quantized context for $packageName", e)
            return null
        }
    }

    private fun buildQuantizedScreens(packageName: String): List<QuantizedScreen> {
        // In production, this queries the SQLDelight database for learned screens
        // For now, return empty list - will be populated when exploration data is available
        return emptyList()
    }

    private fun buildQuantizedNavigation(
        packageName: String,
        screens: List<QuantizedScreen>
    ): List<QuantizedNavigation> {
        // In production, this queries navigation edges from database
        return emptyList()
    }

    private fun buildVocabulary(screens: List<QuantizedScreen>): Set<String> {
        // Extract unique element labels for vocabulary
        return screens.flatMap { screen ->
            screen.elements.flatMap { element ->
                listOf(element.label) + element.aliases
            }
        }.toSet()
    }

    private fun buildKnownCommands(packageName: String): List<QuantizedCommand> {
        // In production, this queries discovered commands from database
        return emptyList()
    }

    private fun hasLearnedData(packageName: String): Boolean {
        // Check if database has exploration data for this package
        // Placeholder - in production, query database
        return false
    }

    private fun getLearnedPackages(): List<String> {
        // Query database for all learned packages
        // Placeholder - in production, query database
        return emptyList()
    }

    // ============================================
    // Prompt Generation
    // ============================================

    private fun generateCompactPrompt(context: QuantizedContext, userGoal: String): String {
        return buildString {
            appendLine("App: ${context.appName}")
            appendLine("Goal: $userGoal")
            appendLine("Screens: ${context.screens.size}")

            val relevantScreens = context.findScreensWithElement(userGoal)
            if (relevantScreens.isNotEmpty()) {
                appendLine("Relevant: ${relevantScreens.joinToString { it.screenTitle }}")
            }

            appendLine("Commands: ${context.vocabulary.take(10).joinToString()}")
        }
    }

    private fun generateHtmlPrompt(context: QuantizedContext, userGoal: String): String {
        return buildString {
            appendLine("<app name=\"${context.appName}\" pkg=\"${context.packageName}\">")
            appendLine("  <goal>$userGoal</goal>")
            appendLine("  <screens count=\"${context.screens.size}\">")

            context.screens.take(5).forEach { screen ->
                appendLine("    <screen title=\"${screen.screenTitle}\">")
                screen.elements.take(5).forEach { element ->
                    appendLine("      <${element.type.name.lowercase()} label=\"${element.label}\"/>")
                }
                appendLine("    </screen>")
            }

            appendLine("  </screens>")
            appendLine("  <nav count=\"${context.navigation.size}\"/>")
            appendLine("</app>")
        }
    }

    private fun generateFullPrompt(context: QuantizedContext, userGoal: String): String {
        return buildString {
            appendLine("# Application Context")
            appendLine("- App: ${context.appName}")
            appendLine("- Package: ${context.packageName}")
            appendLine("- Version: ${context.versionName} (${context.versionCode})")
            appendLine()
            appendLine("## User Goal")
            appendLine(userGoal)
            appendLine()
            appendLine("## Available Screens (${context.screens.size})")

            context.screens.forEach { screen ->
                appendLine()
                appendLine("### ${screen.screenTitle}")
                appendLine("Hash: ${screen.screenHash}")
                if (screen.activityName != null) {
                    appendLine("Activity: ${screen.activityName}")
                }
                appendLine()
                appendLine("Elements:")

                screen.elements.forEach { element ->
                    append("- [${element.type.name}] ${element.label}")
                    if (element.aliases.isNotEmpty()) {
                        append(" (aliases: ${element.aliases.joinToString()})")
                    }
                    appendLine()
                }
            }

            appendLine()
            appendLine("## Navigation Graph")
            context.navigation.forEach { nav ->
                appendLine("- ${nav.triggerLabel}: ${nav.fromScreenHash.take(8)} -> ${nav.toScreenHash.take(8)}")
            }

            appendLine()
            appendLine("## Known Commands")
            context.knownCommands.forEach { cmd ->
                appendLine("- \"${cmd.phrase}\" -> ${cmd.actionType.name}")
            }

            appendLine()
            appendLine("## Vocabulary")
            appendLine(context.vocabulary.joinToString(", "))
        }
    }

    /**
     * Cached context wrapper
     */
    private data class CachedContext(
        val context: QuantizedContext,
        val timestamp: Long
    )
}

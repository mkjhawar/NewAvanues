/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 */

package com.augmentalis.Avanues.web.universal.actions

import com.augmentalis.Avanues.web.universal.platform.Log
import com.augmentalis.Avanues.web.universal.platform.loadResourceAsText
import com.augmentalis.webavanue.platform.WebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * WebActionExtractor - Extracts actionable elements from web pages (KMP)
 *
 * Uses JavaScript injection to scan the current page for clickable
 * elements (buttons, links, inputs, etc.) and converts them to
 * voice-friendly commands.
 *
 * Part of AVA + WebAvanue integration for voice-controlled web browsing.
 *
 * Usage:
 * ```kotlin
 * val extractor = WebActionExtractor(webView)
 * val commands = extractor.extractVoiceCommands()
 * commands.forEach { println("Say: '${it.command}' to ${it.type}") }
 * ```
 *
 * @created 2025-12-01
 */
class WebActionExtractor(
    private val webView: WebView
) {
    companion object {
        private const val TAG = "WebActionExtractor"
        private const val RESOURCE_PATH = "webactions.js"

        // JavaScript to check if library is loaded
        private const val CHECK_LIBRARY_JS = "typeof window.AvanuesWebActions !== 'undefined'"

        // JavaScript to extract all actions
        private const val EXTRACT_ALL_JS = "JSON.stringify(window.AvanuesWebActions.extractAll())"

        // JavaScript to get voice commands
        private const val GET_COMMANDS_JS = "JSON.stringify(window.AvanuesWebActions.getVoiceCommands())"

        // JavaScript to click by command
        private fun clickByCommandJs(command: String): String {
            val escaped = command.replace("'", "\\'")
            return "JSON.stringify(window.AvanuesWebActions.clickByCommand('$escaped'))"
        }

        // JavaScript to click at coordinates
        private fun clickAtJs(x: Int, y: Int): String {
            return "JSON.stringify(window.AvanuesWebActions.clickAt($x, $y))"
        }

        // JavaScript to type text
        private fun typeTextJs(text: String, selector: String?): String {
            val escapedText = text.replace("'", "\\'")
            val selectorArg = selector?.let { "'${it.replace("'", "\\'")}'" } ?: "null"
            return "JSON.stringify(window.AvanuesWebActions.typeText('$escapedText', $selectorArg))"
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var libraryLoaded = false
    private var cachedLibraryJs: String? = null

    /**
     * Ensure the webactions.js library is loaded in the WebView
     */
    private suspend fun ensureLibraryLoaded(): Boolean {
        if (libraryLoaded) {
            // Verify it's still loaded (page may have reloaded)
            val isLoaded = webView.evaluateJavaScript(CHECK_LIBRARY_JS)
            if (isLoaded == "true") return true
            libraryLoaded = false
        }

        // Load the library (cache it for reuse)
        val libraryJs = cachedLibraryJs ?: loadResourceAsText(RESOURCE_PATH)?.also {
            cachedLibraryJs = it
        }

        if (libraryJs == null) {
            Log.e(TAG, "webactions.js not found in resources")
            return false
        }

        // Inject library
        webView.evaluateJavaScript(libraryJs)

        // Verify injection
        val isLoaded = webView.evaluateJavaScript(CHECK_LIBRARY_JS)
        libraryLoaded = isLoaded == "true"

        if (libraryLoaded) {
            Log.i(TAG, "webactions.js library loaded successfully")
        } else {
            Log.e(TAG, "Failed to inject webactions.js library")
        }

        return libraryLoaded
    }

    /**
     * Extract all actionable elements from the current page
     *
     * @return WebActionsResult containing all extracted actions, or null on failure
     */
    suspend fun extractAll(): WebActionsResult? {
        if (!ensureLibraryLoaded()) {
            Log.e(TAG, "Cannot extract: library not loaded")
            return null
        }

        val result = webView.evaluateJavaScript(EXTRACT_ALL_JS)
        if (result == null || result == "null" || result == "undefined") {
            Log.w(TAG, "Extraction returned null")
            return null
        }

        return try {
            json.decodeFromString<WebActionsResult>(result)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse extraction result: ${e.message}", e)
            Log.d(TAG, "Raw result: $result")
            null
        }
    }

    /**
     * Get simplified voice commands for the current page
     *
     * @return List of voice commands, or empty list on failure
     */
    suspend fun extractVoiceCommands(): List<VoiceCommand> {
        if (!ensureLibraryLoaded()) {
            return emptyList()
        }

        val result = webView.evaluateJavaScript(GET_COMMANDS_JS)
        if (result == null || result == "null" || result == "undefined") {
            return emptyList()
        }

        return try {
            json.decodeFromString<List<VoiceCommand>>(result)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse voice commands: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Click an element by voice command
     *
     * @param command The voice command to match
     * @return ClickResult indicating success/failure
     */
    suspend fun clickByCommand(command: String): ClickResult {
        if (!ensureLibraryLoaded()) {
            return ClickResult(
                success = false,
                error = "Web actions library not loaded"
            )
        }

        val result = webView.evaluateJavaScript(clickByCommandJs(command))
        if (result == null || result == "null") {
            return ClickResult(
                success = false,
                error = "No response from page"
            )
        }

        return try {
            json.decodeFromString<ClickResult>(result)
        } catch (e: Exception) {
            ClickResult(
                success = false,
                error = "Failed to parse result: ${e.message}"
            )
        }
    }

    /**
     * Click element at specific coordinates
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @return ClickResult
     */
    suspend fun clickAt(x: Int, y: Int): ClickResult {
        if (!ensureLibraryLoaded()) {
            return ClickResult(
                success = false,
                error = "Web actions library not loaded"
            )
        }

        val result = webView.evaluateJavaScript(clickAtJs(x, y))
        if (result == null) {
            return ClickResult(success = false, error = "No response")
        }

        return try {
            json.decodeFromString<ClickResult>(result)
        } catch (e: Exception) {
            ClickResult(success = false, error = e.message)
        }
    }

    /**
     * Type text into the currently focused input or specified element
     *
     * @param text Text to type
     * @param selector Optional CSS selector to target specific element
     * @return ClickResult
     */
    suspend fun typeText(text: String, selector: String? = null): ClickResult {
        if (!ensureLibraryLoaded()) {
            return ClickResult(
                success = false,
                error = "Web actions library not loaded"
            )
        }

        val result = webView.evaluateJavaScript(typeTextJs(text, selector))
        if (result == null) {
            return ClickResult(success = false, error = "No response")
        }

        return try {
            json.decodeFromString<ClickResult>(result)
        } catch (e: Exception) {
            ClickResult(success = false, error = e.message)
        }
    }

    /**
     * Find the best matching action for a voice query
     *
     * @param query Voice query to match
     * @return Best matching VoiceCommand, or null if no match
     */
    suspend fun findBestMatch(query: String): VoiceCommand? {
        val commands = extractVoiceCommands()
        if (commands.isEmpty()) return null

        val normalizedQuery = query.lowercase().trim()

        // Score each command
        val scored = commands.map { cmd ->
            val command = cmd.command.lowercase()
            val label = cmd.label?.lowercase() ?: ""

            val score = when {
                command == normalizedQuery -> 100
                label == normalizedQuery -> 95
                command.startsWith(normalizedQuery) -> 80
                label.startsWith(normalizedQuery) -> 75
                command.contains(normalizedQuery) -> 60
                label.contains(normalizedQuery) -> 55
                normalizedQuery.split(" ").any { command.contains(it) } -> 40
                normalizedQuery.split(" ").any { label.contains(it) } -> 35
                else -> 0
            }

            cmd to score
        }

        // Return best match above threshold
        return scored
            .filter { it.second > 30 }
            .maxByOrNull { it.second }
            ?.first
    }

    /**
     * Execute action for a voice query (find + click)
     *
     * @param query Voice query
     * @return Pair of (matched command, click result)
     */
    suspend fun executeVoiceCommand(query: String): Pair<VoiceCommand?, ClickResult> {
        val match = findBestMatch(query)
        if (match == null) {
            return null to ClickResult(
                success = false,
                error = "No matching command found for: $query"
            )
        }

        val result = clickByCommand(match.command)
        return match to result
    }

    /**
     * Get available commands as spoken list (for TTS)
     *
     * @param maxCommands Maximum number of commands to include
     * @return Spoken list of available commands
     */
    suspend fun getSpokenCommandList(maxCommands: Int = 10): String {
        val commands = extractVoiceCommands().take(maxCommands)
        if (commands.isEmpty()) {
            return "No actions available on this page."
        }

        return buildString {
            append("Available commands: ")
            commands.forEachIndexed { index, cmd ->
                if (index > 0) append(", ")
                append(cmd.command)
            }
            if (commands.size == maxCommands) {
                append(", and more.")
            }
        }
    }

    /**
     * Reset library state (call after page navigation)
     */
    fun reset() {
        libraryLoaded = false
    }
}

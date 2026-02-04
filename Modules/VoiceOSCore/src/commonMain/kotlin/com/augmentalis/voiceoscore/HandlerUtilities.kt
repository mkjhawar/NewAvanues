/**
 * HandlerUtilities.kt - Common utilities for action handlers
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-02-03
 *
 * Provides common extension functions and utilities to reduce boilerplate
 * across handler implementations. Consolidates 12 common patterns found
 * across 18+ handlers.
 */
package com.augmentalis.voiceoscore

/**
 * Normalize a command string for consistent matching.
 *
 * Converts to lowercase and trims whitespace.
 * Used across 15+ handlers.
 *
 * Usage:
 * ```kotlin
 * val normalized = command.phrase.normalizeCommand()
 * ```
 */
fun String.normalizeCommand(): String = this.lowercase().trim()

/**
 * Extract the target text after removing known command prefixes.
 *
 * Used in NavigationHandler, InputHandler, UIHandler, DeviceHandler.
 *
 * Usage:
 * ```kotlin
 * val target = "click submit button".extractTarget("click ", "tap ", "press ")
 * // Result: "submit button"
 * ```
 *
 * @param prefixes Command prefixes to remove
 * @return Target text with prefix removed, or original if no prefix matches
 */
fun String.extractTarget(vararg prefixes: String): String {
    val normalized = this.normalizeCommand()
    for (prefix in prefixes) {
        if (normalized.startsWith(prefix.lowercase())) {
            return normalized.removePrefix(prefix.lowercase()).trim()
        }
    }
    return normalized
}

/**
 * Convert a boolean executor result to a HandlerResult.
 *
 * Reduces boilerplate like:
 * ```kotlin
 * if (executor.scrollUp()) {
 *     HandlerResult.success("Scrolled up")
 * } else {
 *     HandlerResult.failure("Could not scroll up")
 * }
 * ```
 *
 * To:
 * ```kotlin
 * executor.scrollUp().toHandlerResult(
 *     successMessage = "Scrolled up",
 *     failureMessage = "Could not scroll up"
 * )
 * ```
 *
 * @param successMessage Message for success case
 * @param failureMessage Message for failure case
 * @param data Optional data to include on success
 */
fun Boolean.toHandlerResult(
    successMessage: String,
    failureMessage: String,
    data: Map<String, Any?> = emptyMap()
): HandlerResult = if (this) {
    HandlerResult.success(successMessage, data)
} else {
    HandlerResult.failure(failureMessage)
}

/**
 * Execute a handler operation with standardized error handling.
 *
 * Catches exceptions and converts to HandlerResult.Failure.
 *
 * Usage:
 * ```kotlin
 * return runHandlerCatching("scroll operation") {
 *     executor.scrollUp().toHandlerResult("Scrolled up", "Could not scroll")
 * }
 * ```
 *
 * @param operationName Name for error logging
 * @param block Operation to execute
 * @return HandlerResult from block or Failure on exception
 */
inline fun runHandlerCatching(
    operationName: String,
    block: () -> HandlerResult
): HandlerResult = try {
    block()
} catch (e: Exception) {
    HandlerResult.failure(
        reason = "Error during $operationName: ${e.message ?: "Unknown error"}",
        recoverable = true
    )
}

/**
 * Check if the command matches any of the given patterns.
 *
 * Supports exact match and prefix match.
 *
 * Usage:
 * ```kotlin
 * if (command.matchesAny("scroll up", "page up")) {
 *     // Handle scroll up
 * }
 * ```
 *
 * @param patterns Patterns to match against
 * @return true if command matches any pattern
 */
fun String.matchesAny(vararg patterns: String): Boolean {
    val normalized = this.normalizeCommand()
    return patterns.any { pattern ->
        val p = pattern.lowercase()
        normalized == p || normalized.startsWith("$p ")
    }
}

/**
 * Check if the command starts with any of the given prefixes.
 *
 * Usage:
 * ```kotlin
 * if (command.startsWithAny("click ", "tap ", "press ")) {
 *     val target = command.extractTarget("click ", "tap ", "press ")
 * }
 * ```
 *
 * @param prefixes Prefixes to check
 * @return true if command starts with any prefix
 */
fun String.startsWithAny(vararg prefixes: String): Boolean {
    val normalized = this.normalizeCommand()
    return prefixes.any { normalized.startsWith(it.lowercase()) }
}

/**
 * Parse a numeric value from command parameters or string.
 *
 * Used in DeviceHandler, VolumeHandler for parsing values like "set volume to 50".
 *
 * Usage:
 * ```kotlin
 * val volume = "50".parseIntOrNull(0, 100) ?: return HandlerResult.failure("Invalid volume")
 * ```
 *
 * @param min Minimum allowed value
 * @param max Maximum allowed value
 * @return Parsed and clamped value, or null if parsing fails
 */
fun String.parseIntInRange(min: Int, max: Int): Int? {
    return this.trim().toIntOrNull()?.coerceIn(min, max)
}

/**
 * Builder for command routing with DSL syntax.
 *
 * Provides a cleaner alternative to when expressions.
 *
 * Usage:
 * ```kotlin
 * return commandRouter(command.phrase) {
 *     on("scroll up", "page up") { executor.scrollUp().toResult("Scrolled up", "Could not scroll") }
 *     on("scroll down", "page down") { executor.scrollDown().toResult("Scrolled down", "Could not scroll") }
 *     onPrefix("click ") { target -> handleClick(target) }
 *     otherwise { HandlerResult.notHandled() }
 * }
 * ```
 */
class CommandRouter(private val command: String) {
    private val normalized = command.normalizeCommand()
    private var result: HandlerResult? = null

    /**
     * Match exact commands.
     */
    suspend fun on(vararg patterns: String, handler: suspend () -> HandlerResult) {
        if (result != null) return
        if (normalized.matchesAny(*patterns)) {
            result = handler()
        }
    }

    /**
     * Match commands starting with prefix and extract target.
     */
    fun onPrefix(vararg prefixes: String, handler: (target: String) -> HandlerResult) {
        if (result != null) return
        if (normalized.startsWithAny(*prefixes)) {
            val target = normalized.extractTarget(*prefixes)
            result = handler(target)
        }
    }

    /**
     * Default handler if no match.
     */
    fun otherwise(handler: () -> HandlerResult) {
        if (result == null) {
            result = handler()
        }
    }

    fun getResult(): HandlerResult = result ?: HandlerResult.notHandled()
}

/**
 * DSL entry point for command routing.
 *
 * @param command The command string to route
 * @param block Router configuration block
 * @return HandlerResult from matched handler or NotHandled
 */
inline fun commandRouter(command: String, block: CommandRouter.() -> Unit): HandlerResult {
    return CommandRouter(command).apply(block).getResult()
}

/**
 * Extension to create HandlerResult from nullable value.
 *
 * Usage:
 * ```kotlin
 * val app = appRegistry.findApp(name)
 * return app.toHandlerResult(
 *     successMessage = { "Launched ${it.name}" },
 *     failureMessage = "App not found"
 * )
 * ```
 */
inline fun <T> T?.toHandlerResult(
    successMessage: (T) -> String,
    failureMessage: String,
    data: (T) -> Map<String, Any?> = { emptyMap() }
): HandlerResult = if (this != null) {
    HandlerResult.success(successMessage(this), data(this))
} else {
    HandlerResult.failure(failureMessage)
}

/**
 * Wrap a suspend operation with error handling.
 *
 * Usage:
 * ```kotlin
 * return withHandlerErrorHandling("launch app") {
 *     launchApp(packageName)
 *     HandlerResult.success("App launched")
 * }
 * ```
 */
suspend inline fun withHandlerErrorHandling(
    operationName: String,
    crossinline block: suspend () -> HandlerResult
): HandlerResult = try {
    block()
} catch (e: Exception) {
    HandlerResult.failure(
        reason = "Failed to $operationName: ${e.message ?: "Unknown error"}",
        recoverable = true
    )
}

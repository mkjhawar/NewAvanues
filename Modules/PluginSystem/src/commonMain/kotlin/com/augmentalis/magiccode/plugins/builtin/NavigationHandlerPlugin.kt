/**
 * NavigationHandlerPlugin.kt - Navigation Handler as Universal Plugin
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * This is the first migrated plugin from VoiceOSCore's handler system to the
 * Universal Plugin Architecture. It demonstrates the migration pattern for
 * converting existing handlers to plugins.
 *
 * Migration from: Modules/VoiceOSCore/src/commonMain/.../NavigationHandler.kt
 */
package com.augmentalis.magiccode.plugins.builtin

import com.augmentalis.magiccode.plugins.sdk.BasePlugin
import com.augmentalis.magiccode.plugins.universal.*
import com.augmentalis.magiccode.plugins.universal.contracts.voiceoscore.*
import com.augmentalis.voiceoscore.ActionResult
import com.augmentalis.voiceoscore.QuantizedCommand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Navigation Handler Plugin - First migrated handler from VoiceOSCore.
 *
 * Handles navigation actions:
 * - Scroll actions: scroll up/down/left/right
 * - Swipe actions: swipe up/down/left/right
 * - Page navigation: page up/down, next/previous
 *
 * ## Migration Notes
 * This plugin wraps the original NavigationHandler logic from VoiceOSCore,
 * adapting it to the Universal Plugin interface while maintaining identical
 * behavior.
 *
 * ## Usage
 * ```kotlin
 * val plugin = NavigationHandlerPlugin(executor)
 * plugin.initialize(config, context)
 *
 * val command = QuantizedCommand(phrase = "scroll down", ...)
 * if (plugin.canHandle(command, handlerContext)) {
 *     val result = plugin.handle(command, handlerContext)
 * }
 * ```
 *
 * @since 1.0.0
 * @see HandlerPlugin
 * @see BasePlugin
 */
class NavigationHandlerPlugin(
    private val executorProvider: () -> NavigationPluginExecutor
) : BasePlugin(), HandlerPlugin {

    // =========================================================================
    // Identity
    // =========================================================================

    override val pluginId: String = PLUGIN_ID
    override val pluginName: String = "Navigation Handler"
    override val version: String = "1.0.0"

    override val capabilities: Set<PluginCapability> = setOf(
        PluginCapability(
            id = PluginCapability.ACCESSIBILITY_HANDLER,
            name = "Navigation Handler",
            version = "1.0.0",
            interfaces = setOf("HandlerPlugin"),
            metadata = mapOf(
                "handlerType" to "NAVIGATION",
                "supportsScroll" to "true",
                "supportsSwipe" to "true",
                "supportsPagination" to "true"
            )
        )
    )

    // =========================================================================
    // Handler Properties
    // =========================================================================

    override val handlerType: HandlerType = HandlerType.NAVIGATION

    override val patterns: List<CommandPattern> = listOf(
        CommandPattern(
            regex = Regex("^scroll\\s+(up|down|left|right)$", RegexOption.IGNORE_CASE),
            intent = "SCROLL",
            requiredEntities = setOf("direction"),
            examples = listOf("scroll up", "scroll down", "scroll left", "scroll right")
        ),
        CommandPattern(
            regex = Regex("^swipe\\s+(up|down|left|right)$", RegexOption.IGNORE_CASE),
            intent = "SWIPE",
            requiredEntities = setOf("direction"),
            examples = listOf("swipe up", "swipe down", "swipe left", "swipe right")
        ),
        CommandPattern(
            regex = Regex("^page\\s+(up|down)$", RegexOption.IGNORE_CASE),
            intent = "PAGE",
            requiredEntities = setOf("direction"),
            examples = listOf("page up", "page down")
        ),
        CommandPattern(
            regex = Regex("^(next|previous)$", RegexOption.IGNORE_CASE),
            intent = "NAVIGATE",
            requiredEntities = emptySet(),
            examples = listOf("next", "previous")
        )
    )

    // =========================================================================
    // Supported Actions (for discovery)
    // =========================================================================

    /**
     * List of supported action phrases.
     * Used for command discovery and help systems.
     */
    val supportedActions: List<String> = listOf(
        "scroll up", "scroll down",
        "scroll left", "scroll right",
        "swipe up", "swipe down",
        "swipe left", "swipe right",
        "next", "previous",
        "page up", "page down"
    )

    // =========================================================================
    // Executor Reference
    // =========================================================================

    private lateinit var executor: NavigationPluginExecutor

    // =========================================================================
    // Lifecycle
    // =========================================================================

    override suspend fun onInitialize(): InitResult {
        return try {
            executor = executorProvider()
            InitResult.success("NavigationHandlerPlugin initialized")
        } catch (e: Exception) {
            InitResult.failure(e, recoverable = true)
        }
    }

    override suspend fun onShutdown() {
        // No resources to release
    }

    override fun getHealthDiagnostics(): Map<String, String> = mapOf(
        "supportedActions" to supportedActions.size.toString(),
        "patterns" to patterns.size.toString(),
        "executorInitialized" to (::executor.isInitialized).toString()
    )

    // =========================================================================
    // Handler Implementation
    // =========================================================================

    override fun canHandle(command: QuantizedCommand, context: HandlerContext): Boolean {
        val phrase = command.phrase.lowercase().trim()
        return patterns.any { it.matches(phrase) } ||
                supportedActions.any { phrase == it.lowercase() }
    }

    override suspend fun handle(
        command: QuantizedCommand,
        context: HandlerContext
    ): ActionResult {
        val normalizedAction = command.phrase.lowercase().trim()

        return when (normalizedAction) {
            "scroll up", "page up" -> {
                if (executor.scrollUp()) {
                    ActionResult.Success("Scrolled up")
                } else {
                    ActionResult.Error("Could not scroll up - no scrollable content")
                }
            }

            "scroll down", "page down" -> {
                if (executor.scrollDown()) {
                    ActionResult.Success("Scrolled down")
                } else {
                    ActionResult.Error("Could not scroll down - no scrollable content")
                }
            }

            "scroll left" -> {
                if (executor.scrollLeft()) {
                    ActionResult.Success("Scrolled left")
                } else {
                    ActionResult.Error("Could not scroll left - no scrollable content")
                }
            }

            "scroll right" -> {
                if (executor.scrollRight()) {
                    ActionResult.Success("Scrolled right")
                } else {
                    ActionResult.Error("Could not scroll right - no scrollable content")
                }
            }

            "swipe up" -> {
                // Swipe up = scroll down (content moves up)
                if (executor.scrollDown()) {
                    ActionResult.Success("Swiped up")
                } else {
                    ActionResult.Error("Could not swipe up")
                }
            }

            "swipe down" -> {
                // Swipe down = scroll up (content moves down)
                if (executor.scrollUp()) {
                    ActionResult.Success("Swiped down")
                } else {
                    ActionResult.Error("Could not swipe down")
                }
            }

            "swipe left" -> {
                if (executor.scrollRight()) {
                    ActionResult.Success("Swiped left")
                } else {
                    ActionResult.Error("Could not swipe left")
                }
            }

            "swipe right" -> {
                if (executor.scrollLeft()) {
                    ActionResult.Success("Swiped right")
                } else {
                    ActionResult.Error("Could not swipe right")
                }
            }

            "next" -> {
                if (executor.next()) {
                    ActionResult.Success("Moved to next")
                } else {
                    ActionResult.Error("Could not move to next")
                }
            }

            "previous" -> {
                if (executor.previous()) {
                    ActionResult.Success("Moved to previous")
                } else {
                    ActionResult.Error("Could not move to previous")
                }
            }

            else -> ActionResult.Error("Unknown navigation action: $normalizedAction")
        }
    }

    override fun getConfidence(command: QuantizedCommand, context: HandlerContext): Float {
        val phrase = command.phrase.lowercase().trim()

        // Exact match with supported actions
        if (supportedActions.any { it.lowercase() == phrase }) {
            return 1.0f
        }

        // Pattern match
        for (pattern in patterns) {
            if (pattern.matches(phrase)) {
                return 0.95f
            }
        }

        // Partial match (starts with navigation verb)
        if (phrase.startsWith("scroll") ||
            phrase.startsWith("swipe") ||
            phrase.startsWith("page") ||
            phrase == "next" ||
            phrase == "previous"
        ) {
            return 0.7f
        }

        return 0.0f
    }

    companion object {
        /** Plugin ID for registration and discovery */
        const val PLUGIN_ID = "com.augmentalis.voiceoscore.handler.navigation"
    }
}

/**
 * Platform-specific executor interface for navigation actions.
 *
 * This interface mirrors the original NavigationExecutor from VoiceOSCore
 * but is defined here for the plugin system to avoid circular dependencies.
 *
 * Implementations are platform-specific:
 * - Android: Uses AccessibilityService for scroll/swipe gestures
 * - iOS: Uses UIAccessibility APIs
 * - Desktop: Uses native accessibility APIs or simulated input
 */
interface NavigationPluginExecutor {
    /**
     * Scroll the current view upward.
     * @return true if scroll was performed successfully
     */
    suspend fun scrollUp(): Boolean

    /**
     * Scroll the current view downward.
     * @return true if scroll was performed successfully
     */
    suspend fun scrollDown(): Boolean

    /**
     * Scroll the current view leftward.
     * @return true if scroll was performed successfully
     */
    suspend fun scrollLeft(): Boolean

    /**
     * Scroll the current view rightward.
     * @return true if scroll was performed successfully
     */
    suspend fun scrollRight(): Boolean

    /**
     * Navigate to the next item/page.
     * @return true if navigation was performed successfully
     */
    suspend fun next(): Boolean

    /**
     * Navigate to the previous item/page.
     * @return true if navigation was performed successfully
     */
    suspend fun previous(): Boolean
}

// =============================================================================
// Factory Functions
// =============================================================================

/**
 * Create a NavigationHandlerPlugin with a pre-configured executor.
 *
 * @param executor The navigation executor implementation
 * @return Configured NavigationHandlerPlugin
 */
fun createNavigationHandlerPlugin(
    executor: NavigationPluginExecutor
): NavigationHandlerPlugin {
    return NavigationHandlerPlugin { executor }
}

/**
 * Create a NavigationHandlerPlugin with a lazy executor provider.
 *
 * Useful when the executor depends on platform services that may
 * not be available at plugin creation time.
 *
 * @param executorProvider Function that returns the executor when needed
 * @return Configured NavigationHandlerPlugin
 */
fun createNavigationHandlerPlugin(
    executorProvider: () -> NavigationPluginExecutor
): NavigationHandlerPlugin {
    return NavigationHandlerPlugin(executorProvider)
}

// =============================================================================
// Testing Support
// =============================================================================

/**
 * Mock executor for testing NavigationHandlerPlugin.
 *
 * Records all actions and can be configured to succeed or fail.
 */
class MockNavigationExecutor(
    private val shouldSucceed: Boolean = true
) : NavigationPluginExecutor {

    private val _actions = mutableListOf<String>()

    /** List of recorded actions */
    val actions: List<String> get() = _actions.toList()

    /** Clear recorded actions */
    fun clearActions() = _actions.clear()

    override suspend fun scrollUp(): Boolean {
        _actions.add("scrollUp")
        return shouldSucceed
    }

    override suspend fun scrollDown(): Boolean {
        _actions.add("scrollDown")
        return shouldSucceed
    }

    override suspend fun scrollLeft(): Boolean {
        _actions.add("scrollLeft")
        return shouldSucceed
    }

    override suspend fun scrollRight(): Boolean {
        _actions.add("scrollRight")
        return shouldSucceed
    }

    override suspend fun next(): Boolean {
        _actions.add("next")
        return shouldSucceed
    }

    override suspend fun previous(): Boolean {
        _actions.add("previous")
        return shouldSucceed
    }
}

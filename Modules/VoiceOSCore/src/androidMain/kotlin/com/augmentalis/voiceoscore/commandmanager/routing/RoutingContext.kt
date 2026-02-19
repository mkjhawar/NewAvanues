/**
 * RoutingContext.kt - Context information for command routing
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-10-10
 *
 * Contains context information used for intelligent command routing
 * Based on Q4 Decision: Context-Aware Routing with Smart Dispatcher
 */
package com.augmentalis.voiceoscore.commandmanager.routing

/**
 * Context information for command routing
 *
 * @param currentApp Package name of foreground app (e.g., "com.android.chrome")
 * @param screenState Current screen/activity identifier (optional)
 * @param userContext Additional context key-value pairs for advanced routing
 */
data class RoutingContext(
    val currentApp: String? = null,
    val screenState: String? = null,
    val userContext: Map<String, Any> = emptyMap()
) {
    /**
     * Check if context has specific app information
     */
    fun hasAppContext(): Boolean = currentApp != null

    /**
     * Check if context has screen information
     */
    fun hasScreenContext(): Boolean = screenState != null

    /**
     * Get context value by key
     */
    fun <T> getContextValue(key: String): T? {
        @Suppress("UNCHECKED_CAST")
        return userContext[key] as? T
    }

    /**
     * Create new context with additional values
     */
    fun withContext(vararg pairs: Pair<String, Any>): RoutingContext {
        return copy(userContext = userContext + pairs.toMap())
    }

    companion object {
        /**
         * Create empty routing context
         */
        fun empty(): RoutingContext = RoutingContext()

        /**
         * Create routing context from app package name
         */
        fun fromApp(packageName: String): RoutingContext {
            return RoutingContext(currentApp = packageName)
        }

        /**
         * Create routing context from app and screen
         */
        fun fromAppAndScreen(packageName: String, screen: String): RoutingContext {
            return RoutingContext(currentApp = packageName, screenState = screen)
        }
    }
}

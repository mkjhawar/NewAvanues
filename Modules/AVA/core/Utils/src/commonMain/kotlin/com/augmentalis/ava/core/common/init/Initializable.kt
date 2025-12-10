/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 */

package com.augmentalis.ava.core.common.init

/**
 * Interface for components that require ordered initialization.
 *
 * Components implementing this interface participate in coordinated
 * initialization to prevent race conditions during app startup.
 *
 * Priority Ranges:
 * - 0-99: Core services (database, preferences)
 * - 100-199: AI models (NLU, LLM)
 * - 200-299: Feature services (TTS, Actions)
 * - 300+: UI-related initialization
 *
 * Thread Safety: All implementations must be thread-safe.
 * initialize() may be called from any dispatcher.
 */
interface Initializable {
    /**
     * Initialization priority (lower = initialize first).
     * Components with equal priority may initialize in any order.
     */
    val initPriority: Int

    /**
     * Unique name for logging and debugging.
     */
    val initName: String

    /**
     * Initialize this component.
     *
     * @return true if initialization succeeded, false otherwise
     * @throws Exception if initialization fails fatally
     */
    suspend fun initialize(): Boolean

    /**
     * Shutdown this component and release resources.
     * Called in reverse priority order during app shutdown.
     */
    suspend fun shutdown()

    /**
     * Check if this component is currently initialized.
     */
    fun isInitialized(): Boolean
}

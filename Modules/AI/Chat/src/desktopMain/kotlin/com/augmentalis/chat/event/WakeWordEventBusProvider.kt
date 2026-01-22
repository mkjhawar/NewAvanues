/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * Desktop implementation of WakeWordEventBus provider.
 */

package com.augmentalis.chat.event

/**
 * Desktop provider for WakeWordEventBus.
 *
 * On Desktop, we use a singleton pattern instead of Hilt DI.
 * This provides the same functionality as the Android WakeWordEventBusProvider
 * but without the Hilt dependency.
 *
 * Usage:
 * ```kotlin
 * val eventBus = WakeWordEventBusProvider.getInstance().eventBus
 * eventBus.events.collect { event -> ... }
 * ```
 *
 * @author Manoj Jhawar
 * @since 2025-01-16
 */
class WakeWordEventBusProvider private constructor() {

    /**
     * The shared WakeWordEventBus instance.
     */
    val eventBus = WakeWordEventBus()

    companion object {
        @Volatile
        private var INSTANCE: WakeWordEventBusProvider? = null

        /**
         * Get singleton instance of WakeWordEventBusProvider.
         * Thread-safe using double-checked locking.
         *
         * @return Singleton WakeWordEventBusProvider instance
         */
        fun getInstance(): WakeWordEventBusProvider {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WakeWordEventBusProvider().also {
                    INSTANCE = it
                    println("[WakeWordEventBusProvider] Desktop singleton instance created")
                }
            }
        }
    }
}

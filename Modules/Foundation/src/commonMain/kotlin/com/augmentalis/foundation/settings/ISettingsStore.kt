/**
 * ISettingsStore.kt - Cross-platform reactive settings persistence interface
 *
 * Generic typed interface for reading and writing application settings.
 * Android implements via Jetpack DataStore, iOS via UserDefaults,
 * Desktop via file-backed preferences.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.foundation.settings

import kotlinx.coroutines.flow.Flow

/**
 * Platform-agnostic settings persistence interface.
 *
 * Implementations provide reactive observation via [Flow] and atomic
 * updates via [update]. The type parameter [T] is typically a settings
 * data class (e.g., [AvanuesSettings], [DeveloperSettings]).
 *
 * @param T The settings data class type
 */
interface ISettingsStore<T> {

    /**
     * Observe the current settings state reactively.
     *
     * Emits the current value immediately, then emits again whenever
     * any setting changes. Implementations should handle migration
     * from legacy formats transparently.
     *
     * Declared as a property (not a function) so that implementing classes
     * can use `override val settings` — preserving idiomatic Kotlin property
     * syntax and backward compatibility with existing callers.
     */
    val settings: Flow<T>

    /**
     * Atomically update settings.
     *
     * The [block] receives the current settings state and returns the
     * new state. The implementation ensures the read-modify-write cycle
     * is atomic (no lost updates under concurrent access).
     *
     * @param block Transform function: current settings → updated settings
     */
    suspend fun update(block: (T) -> T)
}

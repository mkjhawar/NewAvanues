/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 */

package com.augmentalis.ava.core.common.init

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Coordinates ordered initialization of app components.
 *
 * Prevents race conditions by ensuring:
 * 1. Components initialize in priority order (lower first)
 * 2. Each component initializes exactly once
 * 3. Concurrent initialization calls wait for completion
 * 4. Shutdown occurs in reverse order
 *
 * Issue Resolution:
 * - C-01: Initialization race in ChatViewModel
 * - I-01: Singleton/Hilt conflict
 * - I-02: DatabaseProvider before Hilt ready
 * - I-03: LocalLLMProvider lazy properties
 * - I-04: TTSManager constructor init
 * - I-05: EntryPoint bypasses DI graph
 *
 * Usage:
 * ```kotlin
 * @Singleton
 * class AppInitCoordinator @Inject constructor() : InitializationCoordinator()
 *
 * // In ViewModel or Application
 * coordinator.ensureInitialized(
 *     nluInitializer,
 *     llmInitializer,
 *     ttsInitializer
 * )
 * ```
 */
open class InitializationCoordinator {

    /**
     * Initialization state
     */
    enum class InitState {
        NOT_STARTED,
        IN_PROGRESS,
        READY,
        FAILED,
        SHUTDOWN
    }

    private val mutex = Mutex()
    private var state = InitState.NOT_STARTED
    private val initialized = mutableSetOf<String>()
    private val components = mutableListOf<Initializable>()
    private var failureReason: String? = null

    /**
     * Current initialization state
     */
    fun getState(): InitState = state

    /**
     * Get failure reason if state is FAILED
     */
    fun getFailureReason(): String? = failureReason

    /**
     * Check if all components are initialized
     */
    fun isReady(): Boolean = state == InitState.READY

    /**
     * Get list of initialized component names
     */
    fun getInitializedComponents(): List<String> = initialized.toList()

    /**
     * Ensure all components are initialized in priority order.
     *
     * Thread-safe: Multiple callers will wait for the first caller to complete.
     * Idempotent: If already initialized, returns immediately.
     *
     * @param componentsToInit Components to initialize (sorted by priority)
     * @return Result.success if all components initialized, Result.failure otherwise
     */
    suspend fun ensureInitialized(
        vararg componentsToInit: Initializable
    ): Result<Unit> = mutex.withLock {
        // Already ready - return immediately
        if (state == InitState.READY) {
            return@withLock Result.success(Unit)
        }

        // Failed previously - return cached failure
        if (state == InitState.FAILED) {
            return@withLock Result.failure(
                IllegalStateException("Initialization failed: $failureReason")
            )
        }

        // Shutdown - cannot reinitialize
        if (state == InitState.SHUTDOWN) {
            return@withLock Result.failure(
                IllegalStateException("Cannot initialize after shutdown")
            )
        }

        state = InitState.IN_PROGRESS
        components.clear()
        components.addAll(componentsToInit.toList())

        try {
            // Sort by priority (lower first)
            val sorted = componentsToInit.sortedBy { it.initPriority }

            for (component in sorted) {
                if (component.initName !in initialized) {
                    println("[InitCoordinator] Initializing ${component.initName} (priority=${component.initPriority})")

                    val success = component.initialize()

                    if (success) {
                        initialized.add(component.initName)
                        println("[InitCoordinator] ✓ ${component.initName} initialized")
                    } else {
                        failureReason = "${component.initName} returned false"
                        state = InitState.FAILED
                        println("[InitCoordinator] ✗ ${component.initName} failed")
                        return@withLock Result.failure(
                            IllegalStateException("${component.initName} initialization failed")
                        )
                    }
                }
            }

            state = InitState.READY
            println("[InitCoordinator] ✓ All ${initialized.size} components initialized")
            Result.success(Unit)

        } catch (e: Exception) {
            failureReason = e.message ?: "Unknown error"
            state = InitState.FAILED
            println("[InitCoordinator] ✗ Initialization failed: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Initialize a single component if not already initialized.
     *
     * Use this for late-binding components that aren't available at startup.
     *
     * @param component Component to initialize
     * @return Result.success if initialized, Result.failure otherwise
     */
    suspend fun initializeIfNeeded(component: Initializable): Result<Unit> = mutex.withLock {
        if (component.initName in initialized) {
            return@withLock Result.success(Unit)
        }

        if (state == InitState.SHUTDOWN) {
            return@withLock Result.failure(
                IllegalStateException("Cannot initialize after shutdown")
            )
        }

        try {
            println("[InitCoordinator] Late-initializing ${component.initName}")

            val success = component.initialize()

            if (success) {
                initialized.add(component.initName)
                components.add(component)
                println("[InitCoordinator] ✓ ${component.initName} late-initialized")
                Result.success(Unit)
            } else {
                println("[InitCoordinator] ✗ ${component.initName} late-init failed")
                Result.failure(
                    IllegalStateException("${component.initName} initialization failed")
                )
            }
        } catch (e: Exception) {
            println("[InitCoordinator] ✗ ${component.initName} threw: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Shutdown all components in reverse priority order.
     *
     * Thread-safe: Only one shutdown can proceed at a time.
     */
    suspend fun shutdown() = mutex.withLock {
        if (state == InitState.SHUTDOWN) {
            return@withLock
        }

        println("[InitCoordinator] Shutting down ${components.size} components...")

        state = InitState.SHUTDOWN

        // Shutdown in reverse priority order (highest first)
        val sorted = components.sortedByDescending { it.initPriority }

        for (component in sorted) {
            try {
                println("[InitCoordinator] Shutting down ${component.initName}")
                component.shutdown()
                println("[InitCoordinator] ✓ ${component.initName} shutdown")
            } catch (e: Exception) {
                println("[InitCoordinator] ✗ ${component.initName} shutdown error: ${e.message}")
                // Continue shutting down other components
            }
        }

        initialized.clear()
        components.clear()
        println("[InitCoordinator] ✓ Shutdown complete")
    }

    /**
     * Reset coordinator to NOT_STARTED state.
     *
     * Use for testing or recovery scenarios.
     */
    suspend fun reset() = mutex.withLock {
        state = InitState.NOT_STARTED
        failureReason = null
        initialized.clear()
        components.clear()
        println("[InitCoordinator] Reset to NOT_STARTED")
    }
}

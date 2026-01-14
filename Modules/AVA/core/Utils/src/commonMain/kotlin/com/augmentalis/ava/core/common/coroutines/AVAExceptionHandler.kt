/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 */

package com.augmentalis.ava.core.common.coroutines

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlin.coroutines.cancellation.CancellationException

/**
 * Factory for creating standardized CoroutineExceptionHandlers.
 *
 * Issue Resolution:
 * - E-01: No CoroutineExceptionHandler in WakeWordService
 * - E-02: No CoroutineExceptionHandler in SettingsViewModel
 * - E-03: TVMRuntime Throwable handling
 * - E-04: Tensor creation crashes
 *
 * Usage:
 * ```kotlin
 * private val exceptionHandler = AVAExceptionHandler.create(
 *     tag = "WakeWordService",
 *     onError = { e -> _serviceState.value = ServiceState.Error(e) }
 * )
 *
 * private val scope = CoroutineScope(
 *     SupervisorJob() + Dispatchers.Default + exceptionHandler
 * )
 * ```
 */
object AVAExceptionHandler {

    /**
     * Callback interface for error handling
     */
    fun interface ErrorCallback {
        fun onError(throwable: Throwable)
    }

    /**
     * Callback interface for low memory cleanup
     */
    fun interface LowMemoryCallback {
        fun onLowMemory()
    }

    // Global low memory callback (set by Application class)
    private var lowMemoryCallback: LowMemoryCallback? = null

    /**
     * Set the global low memory callback.
     * Call this from Application.onCreate()
     */
    fun setLowMemoryCallback(callback: LowMemoryCallback) {
        lowMemoryCallback = callback
    }

    /**
     * Create a CoroutineExceptionHandler with standard error handling.
     *
     * @param tag Tag for logging (typically class name)
     * @param onError Optional callback for error handling
     * @return CoroutineExceptionHandler with standardized behavior
     */
    fun create(
        tag: String,
        onError: ErrorCallback? = null
    ): CoroutineExceptionHandler {
        return CoroutineExceptionHandler { _, throwable ->
            when {
                throwable is CancellationException -> {
                    // Normal cancellation - don't log as error
                    println("[$tag] Coroutine cancelled: ${throwable.message}")
                }

                throwable::class.simpleName == "OutOfMemoryError" -> {
                    // OOM - trigger cleanup and report (platform-agnostic check)
                    println("[$tag] OOM in coroutine: ${throwable.message}")
                    triggerLowMemoryCleanup()
                    onError?.onError(throwable)
                }

                throwable::class.simpleName == "StackOverflowError" -> {
                    // SOE - log and report (platform-agnostic check)
                    println("[$tag] StackOverflow in coroutine: ${throwable.message}")
                    onError?.onError(throwable)
                }

                else -> {
                    // Standard exception - log and report
                    println("[$tag] Coroutine exception: ${throwable.message}")
                    println(throwable.stackTraceToString())
                    onError?.onError(throwable)
                }
            }
        }
    }

    /**
     * Create a CoroutineExceptionHandler that converts errors to user-friendly messages.
     *
     * @param tag Tag for logging
     * @param onUserError Callback that receives a user-friendly error message
     * @return CoroutineExceptionHandler
     */
    fun createWithUserError(
        tag: String,
        onUserError: (String) -> Unit
    ): CoroutineExceptionHandler {
        return create(tag) { throwable ->
            val userMessage = when {
                throwable::class.simpleName == "OutOfMemoryError" ->
                    "Device is low on memory. Please close other apps."
                throwable::class.simpleName?.contains("IOException") == true ->
                    "Network error. Please check your connection."
                throwable::class.simpleName?.contains("SocketTimeoutException") == true ->
                    "Request timed out. Please try again."
                throwable is IllegalStateException ->
                    "Something went wrong. Please try again."
                else -> "An error occurred: ${throwable.message}"
            }
            onUserError(userMessage)
        }
    }

    /**
     * Trigger low memory cleanup.
     * Calls the global callback if set.
     * Note: GC is platform-specific - callback should handle it if needed.
     */
    private fun triggerLowMemoryCleanup() {
        lowMemoryCallback?.onLowMemory()
        // Note: System.gc() removed - platform-specific GC should be handled
        // by the lowMemoryCallback implementation on each platform
    }
}

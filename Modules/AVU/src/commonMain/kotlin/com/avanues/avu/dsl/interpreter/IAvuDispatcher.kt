package com.avanues.avu.dsl.interpreter

/**
 * Result of dispatching a wire protocol code invocation.
 */
sealed class DispatchResult {
    /** Code executed successfully with optional return data. */
    data class Success(val data: Any? = null) : DispatchResult()

    /** Code execution failed. */
    data class Error(val message: String, val cause: Throwable? = null) : DispatchResult()

    /** Code execution timed out. */
    data class Timeout(val timeoutMs: Long) : DispatchResult()
}

/**
 * Interface for dispatching AVU wire protocol codes to platform-specific implementations.
 *
 * Platform dispatchers implement this to handle codes like VCM, AAC, SCR, etc.
 * The interpreter calls [dispatch] for every code invocation in the DSL.
 *
 * ## Testability
 * Mock dispatchers enable unit tests without a real device.
 *
 * ## Platform Abstraction
 * Single interpreter across Android, iOS, Web, Desktop — only the dispatcher changes.
 *
 * ## Example
 * ```kotlin
 * class MockDispatcher : IAvuDispatcher {
 *     override suspend fun dispatch(code: String, arguments: Map<String, Any?>) =
 *         DispatchResult.Success()
 *     override fun canDispatch(code: String) = true
 * }
 * ```
 */
interface IAvuDispatcher {
    /**
     * Dispatch a wire protocol code with the given arguments.
     *
     * @param code 3-letter code (e.g., "VCM", "AAC", "SCR", "QRY")
     * @param arguments Named arguments from the code invocation
     * @return The result of the dispatch
     */
    suspend fun dispatch(code: String, arguments: Map<String, Any?>): DispatchResult

    /**
     * Check if this dispatcher can handle the given code.
     *
     * @param code 3-letter code
     * @return true if this dispatcher supports the code
     */
    fun canDispatch(code: String): Boolean
}

/**
 * Composite dispatcher that delegates to the first capable sub-dispatcher.
 * Implements the chain-of-responsibility pattern for multi-platform or multi-module dispatch.
 *
 * ## Usage
 * ```kotlin
 * val dispatcher = CompositeDispatcher(listOf(
 *     AccessibilityDispatcher(),
 *     VoiceCommandDispatcher(),
 *     QueryDispatcher()
 * ))
 * ```
 */
class CompositeDispatcher(
    private val dispatchers: List<IAvuDispatcher>
) : IAvuDispatcher {

    override suspend fun dispatch(code: String, arguments: Map<String, Any?>): DispatchResult {
        for (dispatcher in dispatchers) {
            if (dispatcher.canDispatch(code)) {
                return dispatcher.dispatch(code, arguments)
            }
        }
        return DispatchResult.Error("No dispatcher found for code: $code")
    }

    override fun canDispatch(code: String): Boolean =
        dispatchers.any { it.canDispatch(code) }
}

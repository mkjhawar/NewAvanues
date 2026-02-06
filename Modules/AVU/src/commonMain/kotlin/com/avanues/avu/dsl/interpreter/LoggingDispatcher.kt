package com.avanues.avu.dsl.interpreter
import com.avanues.avu.dsl.currentTimeMillis

/**
 * Decorator dispatcher that logs all dispatch calls before delegating to a wrapped dispatcher.
 *
 * Useful for debugging, testing, and workflow recording. Captures a chronological log
 * of every code invocation with arguments and results.
 *
 * ## Usage
 * ```kotlin
 * val inner = MyPlatformDispatcher()
 * val logging = LoggingDispatcher(inner)
 * val interpreter = AvuInterpreter(logging)
 * // ... execute DSL ...
 * val log = logging.getLog() // inspect all dispatched codes
 * ```
 */
class LoggingDispatcher(
    private val delegate: IAvuDispatcher
) : IAvuDispatcher {

    private val _log = mutableListOf<DispatchLogEntry>()

    /** Get a snapshot of all logged dispatch entries. */
    fun getLog(): List<DispatchLogEntry> = _log.toList()

    /** Clear the dispatch log. */
    fun clearLog() = _log.clear()

    /** Number of dispatches recorded. */
    val logSize: Int get() = _log.size

    override suspend fun dispatch(code: String, arguments: Map<String, Any?>): DispatchResult {
        val result = delegate.dispatch(code, arguments)
        _log.add(
            DispatchLogEntry(
                code = code,
                arguments = arguments.toMap(),
                result = result,
                timestampMs = currentTimeMillis()
            )
        )
        return result
    }

    override fun canDispatch(code: String): Boolean = delegate.canDispatch(code)
}

/**
 * A single recorded dispatch entry.
 */
data class DispatchLogEntry(
    val code: String,
    val arguments: Map<String, Any?>,
    val result: DispatchResult,
    val timestampMs: Long
) {
    val isSuccess: Boolean get() = result is DispatchResult.Success
    val isError: Boolean get() = result is DispatchResult.Error

    override fun toString(): String = buildString {
        append("$code(")
        append(arguments.entries.joinToString(", ") { "${it.key}: ${formatValue(it.value)}" })
        append(") -> ")
        append(when (result) {
            is DispatchResult.Success -> "OK${result.data?.let { ": $it" } ?: ""}"
            is DispatchResult.Error -> "ERROR: ${result.message}"
            is DispatchResult.Timeout -> "TIMEOUT: ${result.timeoutMs}ms"
        })
    }

    private fun formatValue(value: Any?): String = when (value) {
        null -> "null"
        is String -> "\"$value\""
        is List<*> -> "[${value.joinToString(", ") { formatValue(it) }}]"
        is Map<*, *> -> "{${value.entries.joinToString(", ") { "${it.key}: ${formatValue(it.value)}" }}}"
        else -> value.toString()
    }
}

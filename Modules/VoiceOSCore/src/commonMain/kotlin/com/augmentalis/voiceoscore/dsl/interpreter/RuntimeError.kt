package com.augmentalis.voiceoscore.dsl.interpreter

import com.augmentalis.voiceoscore.dsl.ast.SourceLocation

/**
 * Runtime errors raised during AVU DSL interpretation.
 * All errors carry a [SourceLocation] for precise error reporting.
 *
 * Error format example:
 * ```
 * TypeError: Cannot apply '+' to String and Boolean
 *   at line 23, column 15 in workflow "calculate"
 * ```
 */
sealed class RuntimeError(
    override val message: String,
    val location: SourceLocation?
) : Exception(message) {

    /**
     * Sandbox limit exceeded (step count, execution time, nesting depth, variable count, loop iterations).
     */
    class SandboxViolation(
        val violation: String,
        val limit: Long,
        val current: Long,
        loc: SourceLocation?
    ) : RuntimeError("Sandbox violation: $violation (limit=$limit, current=$current)", loc) {
        companion object {
            fun stepLimit(limit: Int, location: SourceLocation?) =
                SandboxViolation("step limit exceeded", limit.toLong(), limit.toLong(), location)

            fun timeLimit(limitMs: Long, elapsedMs: Long, location: SourceLocation?) =
                SandboxViolation("execution time exceeded", limitMs, elapsedMs, location)

            fun nestingLimit(limit: Int, location: SourceLocation?) =
                SandboxViolation("nesting depth exceeded", limit.toLong(), limit.toLong(), location)

            fun variableLimit(limit: Int, location: SourceLocation?) =
                SandboxViolation("variable limit exceeded", limit.toLong(), limit.toLong(), location)

            fun loopLimit(limit: Int, iterations: Int, location: SourceLocation?) =
                SandboxViolation("loop iteration limit exceeded", limit.toLong(), iterations.toLong(), location)
        }
    }

    /** Wire protocol code invocation failed. */
    class DispatchError(
        val code: String,
        val reason: String,
        loc: SourceLocation?
    ) : RuntimeError("Dispatch error for '$code': $reason", loc)

    /** Type mismatch in expression evaluation. */
    class TypeError(
        val expected: String,
        val actual: String,
        val operation: String,
        loc: SourceLocation?
    ) : RuntimeError("TypeError: Cannot apply '$operation' to $expected and $actual", loc)

    /** Variable reference to undefined name. */
    class UndefinedVariable(
        val name: String,
        loc: SourceLocation?
    ) : RuntimeError("Undefined variable: \$$name", loc)

    /** Call to undefined function. */
    class UndefinedFunction(
        val name: String,
        loc: SourceLocation?
    ) : RuntimeError("Undefined function: $name", loc)

    /** @wait condition exceeded timeout. */
    class TimeoutError(
        val timeoutMs: Long,
        val description: String,
        loc: SourceLocation?
    ) : RuntimeError("Timeout after ${timeoutMs}ms: $description", loc)

    /** General runtime error with optional cause. */
    class General(
        val reason: String,
        loc: SourceLocation?,
        override val cause: Throwable? = null
    ) : RuntimeError(reason, loc)
}

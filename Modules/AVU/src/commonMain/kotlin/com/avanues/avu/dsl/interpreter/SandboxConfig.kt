package com.avanues.avu.dsl.interpreter

/**
 * Configuration for the AVU DSL execution sandbox.
 * Controls resource limits to prevent runaway scripts.
 *
 * @property maxExecutionTimeMs Wall-clock timeout in milliseconds
 * @property maxSteps Maximum interpreter steps (statement executions)
 * @property maxLoopIterations Maximum iterations per @repeat/@while
 * @property maxNestingDepth Maximum call stack depth (@define nesting)
 * @property maxVariables Maximum variables across all scopes
 */
data class SandboxConfig(
    val maxExecutionTimeMs: Long = 10_000,
    val maxSteps: Int = 1_000,
    val maxLoopIterations: Int = 100,
    val maxNestingDepth: Int = 10,
    val maxVariables: Int = 100
) {
    companion object {
        /** Default sandbox for general use. */
        val DEFAULT = SandboxConfig()

        /** Strict sandbox for untrusted user plugins. */
        val STRICT = SandboxConfig(
            maxExecutionTimeMs = 5_000,
            maxSteps = 500,
            maxLoopIterations = 50,
            maxNestingDepth = 5,
            maxVariables = 50
        )

        /** Relaxed sandbox for system workflows. */
        val SYSTEM = SandboxConfig(
            maxExecutionTimeMs = 60_000,
            maxSteps = 10_000,
            maxLoopIterations = 1_000,
            maxNestingDepth = 20,
            maxVariables = 500
        )

        /** Testing sandbox with generous limits. */
        val TESTING = SandboxConfig(
            maxExecutionTimeMs = 60_000,
            maxSteps = 10_000,
            maxLoopIterations = 1_000,
            maxNestingDepth = 50,
            maxVariables = 500
        )
    }
}

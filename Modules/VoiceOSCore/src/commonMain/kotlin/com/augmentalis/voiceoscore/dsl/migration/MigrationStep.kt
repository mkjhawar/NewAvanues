package com.augmentalis.voiceoscore.dsl.migration

/**
 * Platform-agnostic representation of a macro step for migration to AVU DSL.
 *
 * Android code maps `MacroStep` → `MigrationStep`, then [MacroDslMigrator]
 * generates `.vos` text. This decoupling keeps the migration logic in commonMain.
 */
sealed class MigrationStep {

    /** A wire protocol code invocation: `CODE(key: "value", ...)` */
    data class Action(
        val code: String,
        val arguments: Map<String, String>
    ) : MigrationStep()

    /** A delay in milliseconds: `@wait N` */
    data class Delay(val millis: Long) : MigrationStep()

    /** Conditional execution: `@if condition ... @else ...` */
    data class Conditional(
        val condition: String,
        val thenSteps: List<MigrationStep>,
        val elseSteps: List<MigrationStep> = emptyList()
    ) : MigrationStep()

    /** Fixed-count loop: `@repeat N ...` */
    data class Loop(val count: Int, val steps: List<MigrationStep>) : MigrationStep()

    /** Condition-based loop: `@while condition ...` */
    data class LoopWhile(
        val condition: String,
        val steps: List<MigrationStep>,
        val maxIterations: Int = 50
    ) : MigrationStep()

    /** Wait for a condition: `@wait condition timeout N` */
    data class WaitFor(
        val condition: String,
        val timeoutMs: Long = 5000
    ) : MigrationStep()

    /** Variable assignment: `@set name = "value"` */
    data class Variable(val name: String, val value: String) : MigrationStep()
}

/**
 * Macro definition for migration — represents a complete macro.
 */
data class MigrationMacro(
    val name: String,
    val description: String = "",
    val trigger: String = "",
    val author: String = "",
    val tags: List<String> = emptyList(),
    val steps: List<MigrationStep>
)

package com.augmentalis.voiceoscore.dsl.migration

/**
 * Converts macro definitions into AVU DSL (.vos) text format.
 *
 * This is the migration bridge from the old MacroDSL/MacroStep system
 * (Android-only, compiled Kotlin DSL) to the new AVU DSL system
 * (cross-platform, declarative `.vos` text files).
 *
 * ## Usage
 * ```kotlin
 * val macro = MigrationMacro(
 *     name = "Login",
 *     trigger = "login",
 *     steps = listOf(
 *         MigrationStep.Action("VCM", mapOf("action" to "open_app")),
 *         MigrationStep.Delay(1000),
 *         MigrationStep.Action("AAC", mapOf("action" to "CLICK", "target" to "login_btn"))
 *     )
 * )
 * val result = MacroDslMigrator.migrate(macro)
 * // result.contentOrNull() contains the .vos file text
 * ```
 */
object MacroDslMigrator {

    private const val SCHEMA = "avu-2.2"
    private const val INDENT = "  "

    /**
     * Migrate a single macro definition to .vos file content.
     */
    fun migrate(macro: MigrationMacro): MigrationResult {
        val codes = collectCodes(macro.steps)
        if (codes.isEmpty()) {
            return MigrationResult.Error("Macro has no code invocations to migrate")
        }

        val sb = StringBuilder()

        // Header
        generateHeader(sb, macro, codes)
        sb.appendLine()

        // Workflow
        sb.appendLine("@workflow \"${escapeString(macro.name)}\"")
        for (step in macro.steps) {
            generateStep(sb, step, 1)
        }

        // Trigger handler if trigger is defined
        if (macro.trigger.isNotBlank()) {
            val funcName = macro.name.toFunctionName()
            sb.appendLine()
            sb.appendLine("@define $funcName()")
            for (step in macro.steps) {
                generateStep(sb, step, 1)
            }
            sb.appendLine()
            sb.appendLine("@on \"${escapeString(macro.trigger)}\"")
            sb.appendLine("$INDENT$funcName()")
        }

        return MigrationResult.Success(
            content = sb.toString(),
            codesUsed = codes,
            warnings = collectWarnings(macro)
        )
    }

    /**
     * Migrate multiple macros into a single .vos file.
     */
    fun migrateMultiple(macros: List<MigrationMacro>): MigrationResult {
        if (macros.isEmpty()) {
            return MigrationResult.Error("No macros to migrate")
        }

        val allCodes = macros.flatMap { collectCodes(it.steps) }.toSet()
        val sb = StringBuilder()
        val allWarnings = mutableListOf<String>()
        val primary = macros.first()

        generateHeader(sb, primary.copy(
            name = "${primary.name} Collection",
            description = "Migrated from ${macros.size} MacroDSL macros"
        ), allCodes)

        for (macro in macros) {
            val funcName = macro.name.toFunctionName()
            sb.appendLine()
            sb.appendLine("@define $funcName()")
            for (step in macro.steps) {
                generateStep(sb, step, 1)
            }

            if (macro.trigger.isNotBlank()) {
                sb.appendLine()
                sb.appendLine("@on \"${escapeString(macro.trigger)}\"")
                sb.appendLine("$INDENT$funcName()")
            }

            allWarnings.addAll(collectWarnings(macro))
        }

        return MigrationResult.Success(
            content = sb.toString(),
            codesUsed = allCodes,
            warnings = allWarnings
        )
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private fun generateHeader(
        sb: StringBuilder,
        macro: MigrationMacro,
        codes: Set<String>
    ) {
        sb.appendLine("---")
        sb.appendLine("schema: $SCHEMA")
        sb.appendLine("version: 1.0.0")
        sb.appendLine("type: workflow")
        if (macro.description.isNotBlank() || macro.author.isNotBlank() || macro.tags.isNotEmpty()) {
            sb.appendLine("metadata:")
            if (macro.description.isNotBlank()) {
                sb.appendLine("  description: ${macro.description}")
            }
            if (macro.author.isNotBlank()) {
                sb.appendLine("  author: ${macro.author}")
            }
            if (macro.tags.isNotEmpty()) {
                sb.appendLine("  tags: ${macro.tags.joinToString(", ")}")
            }
        }
        sb.appendLine("codes:")
        for (code in codes.sorted()) {
            sb.appendLine("  $code: $code")
        }
        if (macro.trigger.isNotBlank()) {
            sb.appendLine("triggers:")
            sb.appendLine("  ${macro.trigger}")
        }
        sb.appendLine("---")
    }

    private fun generateStep(sb: StringBuilder, step: MigrationStep, depth: Int) {
        val indent = INDENT.repeat(depth)
        when (step) {
            is MigrationStep.Action -> {
                val args = step.arguments.entries.joinToString(", ") { (k, v) ->
                    "$k: \"${escapeString(v)}\""
                }
                sb.appendLine("$indent${step.code}($args)")
            }
            is MigrationStep.Delay -> {
                sb.appendLine("$indent@wait ${step.millis}")
            }
            is MigrationStep.Conditional -> {
                sb.appendLine("$indent@if ${step.condition}")
                for (s in step.thenSteps) generateStep(sb, s, depth + 1)
                if (step.elseSteps.isNotEmpty()) {
                    sb.appendLine("$indent@else")
                    for (s in step.elseSteps) generateStep(sb, s, depth + 1)
                }
            }
            is MigrationStep.Loop -> {
                sb.appendLine("$indent@repeat ${step.count}")
                for (s in step.steps) generateStep(sb, s, depth + 1)
            }
            is MigrationStep.LoopWhile -> {
                sb.appendLine("$indent@while ${step.condition}")
                for (s in step.steps) generateStep(sb, s, depth + 1)
            }
            is MigrationStep.WaitFor -> {
                sb.appendLine("$indent@wait ${step.condition} timeout ${step.timeoutMs}")
            }
            is MigrationStep.Variable -> {
                sb.appendLine("$indent@set ${step.name} = \"${escapeString(step.value)}\"")
            }
        }
    }

    private fun collectCodes(steps: List<MigrationStep>): Set<String> {
        val codes = mutableSetOf<String>()
        for (step in steps) {
            when (step) {
                is MigrationStep.Action -> codes.add(step.code)
                is MigrationStep.Conditional -> {
                    codes.addAll(collectCodes(step.thenSteps))
                    codes.addAll(collectCodes(step.elseSteps))
                }
                is MigrationStep.Loop -> codes.addAll(collectCodes(step.steps))
                is MigrationStep.LoopWhile -> codes.addAll(collectCodes(step.steps))
                else -> { /* no codes */ }
            }
        }
        return codes
    }

    private fun collectWarnings(macro: MigrationMacro): List<String> {
        val warnings = mutableListOf<String>()
        if (macro.trigger.isBlank()) {
            warnings.add("No trigger defined for macro '${macro.name}' - workflow won't be voice-activated")
        }
        return warnings
    }

    private fun escapeString(s: String): String =
        s.replace("\\", "\\\\").replace("\"", "\\\"")

    private fun String.toFunctionName(): String =
        this.lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
}

/**
 * Result of migrating a macro to AVU DSL format.
 */
sealed class MigrationResult {
    data class Success(
        val content: String,
        val codesUsed: Set<String>,
        val warnings: List<String> = emptyList()
    ) : MigrationResult()

    data class Error(val message: String) : MigrationResult()

    val isSuccess: Boolean get() = this is Success
    fun contentOrNull(): String? = (this as? Success)?.content
}

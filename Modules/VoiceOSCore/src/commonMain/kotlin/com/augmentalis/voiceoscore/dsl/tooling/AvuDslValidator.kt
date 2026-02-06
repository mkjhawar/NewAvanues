package com.augmentalis.voiceoscore.dsl.tooling

import com.augmentalis.voiceoscore.dsl.ast.*

/**
 * Severity of a validation diagnostic.
 */
enum class DiagnosticSeverity {
    ERROR,    // Prevents execution
    WARNING,  // May cause issues at runtime
    INFO      // Suggestion / best practice
}

/**
 * A single validation diagnostic.
 */
data class Diagnostic(
    val severity: DiagnosticSeverity,
    val message: String,
    val location: SourceLocation?,
    val code: String  // Machine-readable diagnostic code, e.g., "W001"
)

/**
 * Result of validating an AVU DSL file.
 */
data class ValidationReport(
    val diagnostics: List<Diagnostic>,
    val isValid: Boolean = diagnostics.none { it.severity == DiagnosticSeverity.ERROR }
) {
    val errors: List<Diagnostic> get() =
        diagnostics.filter { it.severity == DiagnosticSeverity.ERROR }
    val warnings: List<Diagnostic> get() =
        diagnostics.filter { it.severity == DiagnosticSeverity.WARNING }
    val infos: List<Diagnostic> get() =
        diagnostics.filter { it.severity == DiagnosticSeverity.INFO }

    fun summary(): String = buildString {
        appendLine("Validation: ${if (isValid) "PASS" else "FAIL"}")
        appendLine("  ${errors.size} error(s), ${warnings.size} warning(s), ${infos.size} info(s)")
        diagnostics.forEach { d ->
            val loc = d.location?.let { " [${it.line}:${it.column}]" } ?: ""
            appendLine("  ${d.severity.name}$loc: ${d.message} (${d.code})")
        }
    }
}

/**
 * Static validator for AVU DSL files.
 *
 * Performs semantic analysis on a parsed [AvuDslFile] AST without executing it.
 * Catches issues that the parser accepts but would fail at runtime.
 *
 * ## Checks Performed
 * - **E001**: Undeclared code used in body (code not in header `codes:` section)
 * - **E002**: Undefined function called
 * - **E003**: Empty workflow / function body
 * - **W001**: Unused declared code (declared in header but never used)
 * - **W002**: Duplicate trigger patterns
 * - **W003**: Variable set but never read
 * - **W004**: Unreachable code after @return
 * - **I001**: No triggers defined (workflow won't respond to voice)
 * - **I002**: Function defined but never called
 */
object AvuDslValidator {

    /**
     * Validate an AVU DSL file.
     *
     * @param file The parsed AVU DSL file
     * @return Validation report with diagnostics
     */
    fun validate(file: AvuDslFile): ValidationReport {
        val diagnostics = mutableListOf<Diagnostic>()
        val declaredCodes = file.header.codes.keys
        val declaredTriggers = file.header.triggers

        // Collect used codes, defined functions, called functions, variables
        val usedCodes = mutableSetOf<String>()
        val definedFunctions = mutableMapOf<String, AvuAstNode.Declaration.FunctionDef>()
        val calledFunctions = mutableSetOf<String>()
        val assignedVars = mutableSetOf<String>()
        val readVars = mutableSetOf<String>()
        val triggerPatterns = mutableListOf<Pair<String, SourceLocation>>()

        // Phase 1: Collect declarations
        for (decl in file.declarations) {
            when (decl) {
                is AvuAstNode.Declaration.FunctionDef -> {
                    definedFunctions[decl.name] = decl
                    collectFromStatements(
                        decl.body, usedCodes, calledFunctions, assignedVars, readVars
                    )
                }
                is AvuAstNode.Declaration.Workflow -> {
                    if (decl.body.isEmpty()) {
                        diagnostics.add(Diagnostic(
                            DiagnosticSeverity.ERROR, "Empty workflow body: '${decl.name}'",
                            decl.location, "E003"
                        ))
                    }
                    collectFromStatements(
                        decl.body, usedCodes, calledFunctions, assignedVars, readVars
                    )
                }
                is AvuAstNode.Declaration.TriggerHandler -> {
                    triggerPatterns.add(decl.pattern to decl.location)
                    collectFromStatements(
                        decl.body, usedCodes, calledFunctions, assignedVars, readVars
                    )
                }
            }
        }

        // Phase 2: Check errors

        // E001: Undeclared codes
        if (declaredCodes.isNotEmpty()) {
            for (code in usedCodes) {
                if (code !in declaredCodes) {
                    diagnostics.add(Diagnostic(
                        DiagnosticSeverity.ERROR,
                        "Code '$code' used but not declared in header codes section",
                        null, "E001"
                    ))
                }
            }
        }

        // E002: Undefined functions
        for (name in calledFunctions) {
            if (name !in definedFunctions) {
                diagnostics.add(Diagnostic(
                    DiagnosticSeverity.ERROR,
                    "Function '$name' called but not defined with @define",
                    null, "E002"
                ))
            }
        }

        // Phase 3: Check warnings

        // W001: Unused declared codes
        for (code in declaredCodes) {
            if (code !in usedCodes) {
                diagnostics.add(Diagnostic(
                    DiagnosticSeverity.WARNING,
                    "Code '$code' declared in header but never used in body",
                    null, "W001"
                ))
            }
        }

        // W002: Duplicate trigger patterns
        val seenTriggers = mutableSetOf<String>()
        for ((pattern, location) in triggerPatterns) {
            if (pattern in seenTriggers) {
                diagnostics.add(Diagnostic(
                    DiagnosticSeverity.WARNING,
                    "Duplicate trigger pattern: '$pattern'",
                    location, "W002"
                ))
            }
            seenTriggers.add(pattern)
        }

        // W003: Variables set but never read
        for (varName in assignedVars) {
            if (varName !in readVars) {
                diagnostics.add(Diagnostic(
                    DiagnosticSeverity.WARNING,
                    "Variable '$varName' is set but never read",
                    null, "W003"
                ))
            }
        }

        // W004: Unreachable code after @return
        for (decl in file.declarations) {
            val body = when (decl) {
                is AvuAstNode.Declaration.Workflow -> decl.body
                is AvuAstNode.Declaration.FunctionDef -> decl.body
                is AvuAstNode.Declaration.TriggerHandler -> decl.body
            }
            checkUnreachableCode(body, diagnostics)
        }

        // Phase 4: Info

        // I001: No triggers
        if (declaredTriggers.isEmpty() &&
            file.declarations.none { it is AvuAstNode.Declaration.TriggerHandler }
        ) {
            diagnostics.add(Diagnostic(
                DiagnosticSeverity.INFO,
                "No triggers defined - this workflow won't respond to voice commands",
                null, "I001"
            ))
        }

        // I002: Unused functions
        for ((name, funcDef) in definedFunctions) {
            if (name !in calledFunctions) {
                diagnostics.add(Diagnostic(
                    DiagnosticSeverity.INFO,
                    "Function '$name' is defined but never called",
                    funcDef.location, "I002"
                ))
            }
        }

        return ValidationReport(diagnostics.sortedBy {
            when (it.severity) {
                DiagnosticSeverity.ERROR -> 0
                DiagnosticSeverity.WARNING -> 1
                DiagnosticSeverity.INFO -> 2
            }
        })
    }

    // =========================================================================
    // COLLECTION HELPERS
    // =========================================================================

    private fun collectFromStatements(
        stmts: List<AvuAstNode.Statement>,
        codes: MutableSet<String>,
        functions: MutableSet<String>,
        assigned: MutableSet<String>,
        read: MutableSet<String>
    ) {
        for (stmt in stmts) {
            collectFromStatement(stmt, codes, functions, assigned, read)
        }
    }

    private fun collectFromStatement(
        stmt: AvuAstNode.Statement,
        codes: MutableSet<String>,
        functions: MutableSet<String>,
        assigned: MutableSet<String>,
        read: MutableSet<String>
    ) {
        when (stmt) {
            is AvuAstNode.Statement.CodeInvocation -> {
                codes.add(stmt.code)
                stmt.arguments.forEach { collectFromExpression(it.value, read) }
            }
            is AvuAstNode.Statement.FunctionCall -> {
                functions.add(stmt.name)
                stmt.arguments.forEach { collectFromExpression(it.value, read) }
            }
            is AvuAstNode.Statement.WaitDelay -> {
                collectFromExpression(stmt.milliseconds, read)
            }
            is AvuAstNode.Statement.WaitCondition -> {
                collectFromExpression(stmt.condition, read)
                collectFromExpression(stmt.timeoutMs, read)
            }
            is AvuAstNode.Statement.IfElse -> {
                collectFromExpression(stmt.condition, read)
                collectFromStatements(stmt.thenBody, codes, functions, assigned, read)
                collectFromStatements(stmt.elseBody, codes, functions, assigned, read)
            }
            is AvuAstNode.Statement.Repeat -> {
                collectFromExpression(stmt.count, read)
                collectFromStatements(stmt.body, codes, functions, assigned, read)
            }
            is AvuAstNode.Statement.While -> {
                collectFromExpression(stmt.condition, read)
                collectFromStatements(stmt.body, codes, functions, assigned, read)
            }
            is AvuAstNode.Statement.Sequence -> {
                collectFromStatements(stmt.body, codes, functions, assigned, read)
            }
            is AvuAstNode.Statement.Assignment -> {
                assigned.add(stmt.variableName)
                collectFromExpression(stmt.value, read)
            }
            is AvuAstNode.Statement.Log -> {
                collectFromExpression(stmt.message, read)
            }
            is AvuAstNode.Statement.Return -> {
                stmt.value?.let { collectFromExpression(it, read) }
            }
            is AvuAstNode.Statement.Emit -> {
                stmt.data?.let { collectFromExpression(it, read) }
            }
        }
    }

    private fun collectFromExpression(expr: AvuAstNode.Expression, read: MutableSet<String>) {
        when (expr) {
            is AvuAstNode.Expression.VariableRef -> read.add(expr.name)
            is AvuAstNode.Expression.BinaryOp -> {
                collectFromExpression(expr.left, read)
                collectFromExpression(expr.right, read)
            }
            is AvuAstNode.Expression.UnaryOp -> collectFromExpression(expr.operand, read)
            is AvuAstNode.Expression.MemberAccess -> collectFromExpression(expr.target, read)
            is AvuAstNode.Expression.CallExpression -> {
                collectFromExpression(expr.callee, read)
                expr.arguments.forEach { collectFromExpression(it, read) }
            }
            is AvuAstNode.Expression.Grouped -> collectFromExpression(expr.inner, read)
            // Literals and identifiers don't read variables
            is AvuAstNode.Expression.StringLiteral,
            is AvuAstNode.Expression.IntLiteral,
            is AvuAstNode.Expression.FloatLiteral,
            is AvuAstNode.Expression.BooleanLiteral,
            is AvuAstNode.Expression.Identifier -> { }
        }
    }

    private fun checkUnreachableCode(
        statements: List<AvuAstNode.Statement>,
        diagnostics: MutableList<Diagnostic>
    ) {
        for (i in statements.indices) {
            if (statements[i] is AvuAstNode.Statement.Return && i < statements.size - 1) {
                diagnostics.add(Diagnostic(
                    DiagnosticSeverity.WARNING,
                    "Unreachable code after @return",
                    statements[i + 1].location, "W004"
                ))
                break
            }
        }
    }
}

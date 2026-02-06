package com.avanues.avu.dsl.tooling

import com.avanues.avu.dsl.ast.*

/**
 * Configuration for AVU DSL code formatting.
 *
 * @property indentSize Number of spaces per indentation level
 * @property maxLineWidth Maximum line width before wrapping (0 = no limit)
 * @property emptyLineBetweenDeclarations Insert blank line between top-level declarations
 */
data class FormatterConfig(
    val indentSize: Int = 2,
    val maxLineWidth: Int = 100,
    val emptyLineBetweenDeclarations: Boolean = true
)

/**
 * AVU DSL code formatter / pretty-printer.
 *
 * Takes a parsed [AvuDslFile] AST and produces consistently formatted source text.
 * Ensures uniform indentation, spacing, and structure.
 *
 * ## Usage
 * ```kotlin
 * val tokens = AvuDslLexer(source).tokenize()
 * val file = AvuDslParser(tokens).parse().file
 * val formatted = AvuDslFormatter.format(file)
 * ```
 */
object AvuDslFormatter {

    /**
     * Format an AVU DSL file AST to source text.
     *
     * @param file The parsed AVU DSL file
     * @param config Formatting configuration
     * @return Formatted source text
     */
    fun format(file: AvuDslFile, config: FormatterConfig = FormatterConfig()): String {
        val sb = StringBuilder()
        formatHeader(file.header, sb)
        sb.appendLine()
        formatDeclarations(file.declarations, sb, config)
        return sb.toString()
    }

    /**
     * Format just the header section.
     */
    fun formatHeader(header: AvuDslHeader, sb: StringBuilder) {
        sb.appendLine("---")
        sb.appendLine("schema: ${header.schema}")
        sb.appendLine("version: ${header.version}")
        sb.appendLine("type: ${header.type.name.lowercase()}")

        if (header.metadata.isNotEmpty()) {
            sb.appendLine("metadata:")
            header.metadata.forEach { (key, value) ->
                sb.appendLine("  $key: $value")
            }
        }

        if (header.codes.isNotEmpty()) {
            sb.appendLine("codes:")
            header.codes.forEach { (code, description) ->
                sb.appendLine("  $code: $description")
            }
        }

        if (header.permissions.isNotEmpty()) {
            sb.appendLine("permissions:")
            header.permissions.forEach { perm ->
                sb.appendLine("  $perm")
            }
        }

        if (header.triggers.isNotEmpty()) {
            sb.appendLine("triggers:")
            header.triggers.forEach { trigger ->
                sb.appendLine("  $trigger")
            }
        }

        sb.appendLine("---")
    }

    /**
     * Format all declarations.
     */
    private fun formatDeclarations(
        declarations: List<AvuAstNode.Declaration>,
        sb: StringBuilder,
        config: FormatterConfig
    ) {
        declarations.forEachIndexed { index, decl ->
            if (index > 0 && config.emptyLineBetweenDeclarations) {
                sb.appendLine()
            }
            formatDeclaration(decl, sb, config, 0)
        }
    }

    private fun formatDeclaration(
        decl: AvuAstNode.Declaration,
        sb: StringBuilder,
        config: FormatterConfig,
        indent: Int
    ) {
        when (decl) {
            is AvuAstNode.Declaration.Workflow -> {
                sb.appendLine("${pad(indent, config)}@workflow \"${decl.name}\"")
                formatStatements(decl.body, sb, config, indent + 1)
            }
            is AvuAstNode.Declaration.FunctionDef -> {
                val params = decl.parameters.joinToString(", ")
                sb.appendLine("${pad(indent, config)}@define ${decl.name}($params)")
                formatStatements(decl.body, sb, config, indent + 1)
            }
            is AvuAstNode.Declaration.TriggerHandler -> {
                sb.appendLine("${pad(indent, config)}@on \"${decl.pattern}\"")
                formatStatements(decl.body, sb, config, indent + 1)
            }
        }
    }

    // =========================================================================
    // STATEMENT FORMATTING
    // =========================================================================

    private fun formatStatements(
        statements: List<AvuAstNode.Statement>,
        sb: StringBuilder,
        config: FormatterConfig,
        indent: Int
    ) {
        statements.forEach { formatStatement(it, sb, config, indent) }
    }

    private fun formatStatement(
        stmt: AvuAstNode.Statement,
        sb: StringBuilder,
        config: FormatterConfig,
        indent: Int
    ) {
        val prefix = pad(indent, config)
        when (stmt) {
            is AvuAstNode.Statement.CodeInvocation -> {
                sb.appendLine("$prefix${stmt.code}(${formatArguments(stmt.arguments)})")
            }
            is AvuAstNode.Statement.FunctionCall -> {
                sb.appendLine("$prefix${stmt.name}(${formatArguments(stmt.arguments)})")
            }
            is AvuAstNode.Statement.WaitDelay -> {
                sb.appendLine("$prefix@wait ${formatExpression(stmt.milliseconds)}")
            }
            is AvuAstNode.Statement.WaitCondition -> {
                sb.appendLine(
                    "$prefix@wait ${formatExpression(stmt.condition)} " +
                        "timeout ${formatExpression(stmt.timeoutMs)}"
                )
            }
            is AvuAstNode.Statement.IfElse -> {
                sb.appendLine("$prefix@if ${formatExpression(stmt.condition)}")
                formatStatements(stmt.thenBody, sb, config, indent + 1)
                if (stmt.elseBody.isNotEmpty()) {
                    sb.appendLine("$prefix@else")
                    formatStatements(stmt.elseBody, sb, config, indent + 1)
                }
            }
            is AvuAstNode.Statement.Repeat -> {
                sb.appendLine("$prefix@repeat ${formatExpression(stmt.count)}")
                formatStatements(stmt.body, sb, config, indent + 1)
            }
            is AvuAstNode.Statement.While -> {
                sb.appendLine("$prefix@while ${formatExpression(stmt.condition)}")
                formatStatements(stmt.body, sb, config, indent + 1)
            }
            is AvuAstNode.Statement.Sequence -> {
                sb.appendLine("$prefix@sequence")
                formatStatements(stmt.body, sb, config, indent + 1)
            }
            is AvuAstNode.Statement.Assignment -> {
                sb.appendLine(
                    "$prefix@set ${stmt.variableName} = ${formatExpression(stmt.value)}"
                )
            }
            is AvuAstNode.Statement.Log -> {
                sb.appendLine("$prefix@log ${formatExpression(stmt.message)}")
            }
            is AvuAstNode.Statement.Return -> {
                val value = stmt.value?.let { " ${formatExpression(it)}" } ?: ""
                sb.appendLine("$prefix@return$value")
            }
            is AvuAstNode.Statement.Emit -> {
                val data = stmt.data?.let { " ${formatExpression(it)}" } ?: ""
                sb.appendLine("$prefix@emit \"${stmt.eventName}\"$data")
            }
        }
    }

    // =========================================================================
    // EXPRESSION FORMATTING
    // =========================================================================

    /**
     * Format an expression to source text.
     * Public so other tools can use expression formatting.
     */
    fun formatExpression(expr: AvuAstNode.Expression): String = when (expr) {
        is AvuAstNode.Expression.StringLiteral -> "\"${escapeString(expr.value)}\""
        is AvuAstNode.Expression.IntLiteral -> expr.value.toString()
        is AvuAstNode.Expression.FloatLiteral -> expr.value.toString()
        is AvuAstNode.Expression.BooleanLiteral -> expr.value.toString()
        is AvuAstNode.Expression.VariableRef -> "\$${expr.name}"
        is AvuAstNode.Expression.Identifier -> expr.name
        is AvuAstNode.Expression.BinaryOp -> {
            val left = formatExpression(expr.left)
            val right = formatExpression(expr.right)
            val op = formatOperator(expr.operator)
            "$left $op $right"
        }
        is AvuAstNode.Expression.UnaryOp -> {
            val operand = formatExpression(expr.operand)
            when (expr.operator) {
                UnaryOperator.NOT -> "not $operand"
                UnaryOperator.NEGATE -> "-$operand"
            }
        }
        is AvuAstNode.Expression.MemberAccess -> {
            "${formatExpression(expr.target)}.${expr.member}"
        }
        is AvuAstNode.Expression.CallExpression -> {
            val callee = formatExpression(expr.callee)
            val args = expr.arguments.joinToString(", ") { formatExpression(it) }
            "$callee($args)"
        }
        is AvuAstNode.Expression.Grouped -> "(${formatExpression(expr.inner)})"
    }

    private fun formatArguments(arguments: List<AvuAstNode.NamedArgument>): String {
        return arguments.joinToString(", ") { arg ->
            if (arg.name != null) {
                "${arg.name}: ${formatExpression(arg.value)}"
            } else {
                formatExpression(arg.value)
            }
        }
    }

    private fun formatOperator(op: BinaryOperator): String = when (op) {
        BinaryOperator.PLUS -> "+"
        BinaryOperator.MINUS -> "-"
        BinaryOperator.STAR -> "*"
        BinaryOperator.SLASH -> "/"
        BinaryOperator.EQ -> "=="
        BinaryOperator.NEQ -> "!="
        BinaryOperator.LT -> "<"
        BinaryOperator.GT -> ">"
        BinaryOperator.LTE -> "<="
        BinaryOperator.GTE -> ">="
        BinaryOperator.AND -> "and"
        BinaryOperator.OR -> "or"
    }

    private fun escapeString(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")

    private fun pad(indent: Int, config: FormatterConfig): String =
        " ".repeat(indent * config.indentSize)
}

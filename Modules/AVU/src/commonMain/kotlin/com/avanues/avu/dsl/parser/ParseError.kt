package com.avanues.avu.dsl.parser

import com.avanues.avu.dsl.ast.AvuDslFile

/**
 * Severity level for parse diagnostics.
 */
enum class ErrorSeverity {
    WARNING,
    ERROR
}

/**
 * A parse error or warning with precise source location.
 */
data class ParseError(
    val message: String,
    val line: Int,
    val column: Int,
    val severity: ErrorSeverity = ErrorSeverity.ERROR
) {
    override fun toString(): String = "[$severity] Line $line:$column - $message"
}

/**
 * Internal exception used for control flow during parsing.
 */
class ParseException(val error: ParseError) : Exception(error.message)

/**
 * Result of parsing an AVU DSL file.
 *
 * @property file The parsed AST (may be partial if errors occurred)
 * @property errors All parse errors and warnings collected during parsing
 */
data class ParseResult(
    val file: AvuDslFile,
    val errors: List<ParseError>
) {
    val hasErrors: Boolean get() = errors.any { it.severity == ErrorSeverity.ERROR }
    val hasWarnings: Boolean get() = errors.any { it.severity == ErrorSeverity.WARNING }
}

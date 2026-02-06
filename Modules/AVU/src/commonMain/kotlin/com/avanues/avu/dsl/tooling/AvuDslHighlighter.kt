package com.avanues.avu.dsl.tooling

import com.avanues.avu.dsl.lexer.AvuDslLexer
import com.avanues.avu.dsl.lexer.Token
import com.avanues.avu.dsl.lexer.TokenType

/**
 * Syntax highlighting category for AVU DSL tokens.
 * Platform UIs map these to colors/styles.
 */
enum class HighlightCategory {
    DIRECTIVE,      // @workflow, @define, @if, @while, etc.
    CODE_NAME,      // VCM, AAC, SCR (3-letter codes)
    STRING,         // "string literals"
    NUMBER,         // 42, 3.14
    BOOLEAN,        // true, false
    KEYWORD,        // timeout, and, or, not
    VARIABLE,       // $variable_name
    COMMENT,        // # comment
    OPERATOR,       // ==, !=, +, -, *, /
    DELIMITER,      // (, ), {, }, :, ,
    IDENTIFIER,     // function names, parameter names
    HEADER_KEY,     // schema:, version:, type:, etc.
    HEADER_VALUE,   // header values
    HEADER_MARKER,  // --- separator
    ERROR,          // lexer errors
    PLAIN           // whitespace, newlines
}

/**
 * A highlighted span in AVU DSL source text.
 *
 * @property startOffset Character offset in source (0-based)
 * @property endOffset End character offset (exclusive)
 * @property category The highlight category for this span
 * @property text The actual text of the span
 */
data class HighlightSpan(
    val startOffset: Int,
    val endOffset: Int,
    val category: HighlightCategory,
    val text: String
)

/**
 * Syntax highlighter for AVU DSL source text.
 *
 * Tokenizes source and classifies each token into a [HighlightCategory].
 * Platform-agnostic: UI frameworks map categories to colors.
 *
 * ## Usage
 * ```kotlin
 * val spans = AvuDslHighlighter.highlight(sourceText)
 * spans.forEach { span ->
 *     applyColor(span.startOffset, span.endOffset, colorFor(span.category))
 * }
 * ```
 */
object AvuDslHighlighter {

    /**
     * Highlight AVU DSL source text.
     *
     * @param source The AVU DSL source text
     * @return List of highlight spans covering the entire source
     */
    fun highlight(source: String): List<HighlightSpan> {
        val tokens = try {
            AvuDslLexer(source).tokenize()
        } catch (_: Exception) {
            return listOf(HighlightSpan(0, source.length, HighlightCategory.PLAIN, source))
        }

        return tokens.mapNotNull { token ->
            if (token.type == TokenType.EOF || token.type == TokenType.INDENT ||
                token.type == TokenType.DEDENT || token.type == TokenType.NEWLINE
            ) {
                return@mapNotNull null
            }

            val category = categorize(token)
            val startOffset = findTokenOffset(source, token)
            if (startOffset < 0) return@mapNotNull null

            HighlightSpan(
                startOffset = startOffset,
                endOffset = startOffset + token.lexeme.length,
                category = category,
                text = token.lexeme
            )
        }
    }

    /**
     * Classify a token into a highlight category.
     */
    fun categorize(token: Token): HighlightCategory = when (token.type) {
        // Directives
        TokenType.AT_WORKFLOW, TokenType.AT_DEFINE, TokenType.AT_ON,
        TokenType.AT_IF, TokenType.AT_ELSE, TokenType.AT_WAIT,
        TokenType.AT_REPEAT, TokenType.AT_WHILE, TokenType.AT_SEQUENCE,
        TokenType.AT_LOG, TokenType.AT_SET, TokenType.AT_RETURN,
        TokenType.AT_EMIT -> HighlightCategory.DIRECTIVE

        // Code names
        TokenType.CODE_NAME -> HighlightCategory.CODE_NAME

        // Literals
        TokenType.STRING_LITERAL -> HighlightCategory.STRING
        TokenType.INT_LITERAL, TokenType.FLOAT_LITERAL -> HighlightCategory.NUMBER
        TokenType.BOOLEAN_LITERAL -> HighlightCategory.BOOLEAN

        // Keywords
        TokenType.KW_TIMEOUT, TokenType.AND, TokenType.OR,
        TokenType.NOT -> HighlightCategory.KEYWORD

        // Variable references
        TokenType.VARIABLE_REF -> HighlightCategory.VARIABLE

        // Operators
        TokenType.PLUS, TokenType.MINUS, TokenType.STAR, TokenType.SLASH,
        TokenType.EQ, TokenType.NEQ, TokenType.LT, TokenType.GT,
        TokenType.LTE, TokenType.GTE, TokenType.ASSIGN -> HighlightCategory.OPERATOR

        // Delimiters
        TokenType.LPAREN, TokenType.RPAREN, TokenType.LBRACE, TokenType.RBRACE,
        TokenType.COMMA, TokenType.COLON, TokenType.DOT -> HighlightCategory.DELIMITER

        // Identifiers
        TokenType.IDENTIFIER -> HighlightCategory.IDENTIFIER

        // Header
        TokenType.HEADER_SEPARATOR -> HighlightCategory.HEADER_MARKER
        TokenType.HEADER_LINE -> categorizeHeaderLine(token.lexeme)

        // Comments
        TokenType.COMMENT -> HighlightCategory.COMMENT

        // Error
        TokenType.ERROR -> HighlightCategory.ERROR

        // Structural tokens
        TokenType.INDENT, TokenType.DEDENT, TokenType.NEWLINE, TokenType.EOF ->
            HighlightCategory.PLAIN
    }

    private fun categorizeHeaderLine(content: String): HighlightCategory {
        val trimmed = content.trim()
        return if (trimmed.contains(':')) {
            HighlightCategory.HEADER_KEY
        } else {
            HighlightCategory.HEADER_VALUE
        }
    }

    /**
     * Find the character offset of a token in the source text.
     * Uses line and column information from the token.
     */
    private fun findTokenOffset(source: String, token: Token): Int {
        var line = 1
        var offset = 0
        while (offset < source.length && line < token.line) {
            if (source[offset] == '\n') line++
            offset++
        }
        return offset + token.column - 1
    }
}

package com.augmentalis.voiceoscore.dsl.lexer

/**
 * Tokenizer for the AVU DSL format.
 *
 * Converts source text into a list of [Token]s. Handles two phases:
 * 1. Header section (between --- markers): emits raw [TokenType.HEADER_LINE] tokens
 * 2. Body section: full tokenization with Python-style [TokenType.INDENT]/[TokenType.DEDENT]
 *
 * Usage:
 * ```kotlin
 * val tokens = AvuDslLexer(sourceText).tokenize()
 * ```
 */
class AvuDslLexer(private val source: String) {

    private val tokens = mutableListOf<Token>()
    private var start = 0
    private var current = 0
    private var line = 1
    private var column = 1

    // Indentation tracking
    private val indentStack = mutableListOf(0)
    private var atLineStart = true

    // Header state
    private var headerSeparatorCount = 0
    private val inHeader: Boolean get() = headerSeparatorCount == 1

    fun tokenize(): List<Token> {
        while (!isAtEnd()) {
            start = current
            if (atLineStart && !inHeader) {
                handleIndentation()
            }
            if (!isAtEnd()) {
                start = current
                scanToken()
            }
        }

        // Close remaining indentation levels
        while (indentStack.size > 1) {
            indentStack.removeLast()
            emit(TokenType.DEDENT, "")
        }
        emit(TokenType.EOF, "")
        return tokens
    }

    // =========================================================================
    // INDENTATION
    // =========================================================================

    private fun handleIndentation() {
        var spaces = 0
        while (!isAtEnd() && (peek() == ' ' || peek() == '\t')) {
            spaces += if (peek() == '\t') 4 else 1
            advance()
        }

        // Skip blank lines and comment-only lines
        if (isAtEnd() || peek() == '\n' || peek() == '\r') {
            atLineStart = true
            return
        }
        if (peek() == '#') {
            atLineStart = false
            return
        }

        val currentIndent = indentStack.last()
        when {
            spaces > currentIndent -> {
                indentStack.add(spaces)
                emit(TokenType.INDENT, "")
            }
            spaces < currentIndent -> {
                while (indentStack.size > 1 && indentStack.last() > spaces) {
                    indentStack.removeLast()
                    emit(TokenType.DEDENT, "")
                }
                if (indentStack.last() != spaces) {
                    emit(TokenType.ERROR, "Inconsistent indentation")
                }
            }
        }
        atLineStart = false
    }

    // =========================================================================
    // TOKEN SCANNING
    // =========================================================================

    private fun scanToken() {
        val c = advance()
        when {
            c == '\n' -> handleNewline()
            c == '\r' -> {
                if (!isAtEnd() && peek() == '\n') advance()
                handleNewline()
            }
            c == '#' -> scanComment()
            c == '-' && canMatch('-') && canMatchAt(current, '-') -> scanHeaderSeparator()
            inHeader -> scanHeaderLine(c)
            c == '"' -> scanString('"')
            c == '\'' -> scanString('\'')
            c == '@' -> scanDirective()
            c == '$' -> scanVariableRef()
            c.isDigit() -> scanNumber()
            c.isLetter() || c == '_' -> scanIdentifierOrKeyword()
            c == '(' -> emit(TokenType.LPAREN)
            c == ')' -> emit(TokenType.RPAREN)
            c == '{' -> emit(TokenType.LBRACE)
            c == '}' -> emit(TokenType.RBRACE)
            c == ',' -> emit(TokenType.COMMA)
            c == ':' -> emit(TokenType.COLON)
            c == '.' -> emit(TokenType.DOT)
            c == '+' -> emit(TokenType.PLUS)
            c == '*' -> emit(TokenType.STAR)
            c == '/' -> emit(TokenType.SLASH)
            c == '-' -> emit(TokenType.MINUS)
            c == '=' && !isAtEnd() && peek() == '=' -> {
                advance()
                emit(TokenType.EQ)
            }
            c == '=' -> emit(TokenType.ASSIGN)
            c == '!' && !isAtEnd() && peek() == '=' -> {
                advance()
                emit(TokenType.NEQ)
            }
            c == '<' && !isAtEnd() && peek() == '=' -> {
                advance()
                emit(TokenType.LTE)
            }
            c == '<' -> emit(TokenType.LT)
            c == '>' && !isAtEnd() && peek() == '=' -> {
                advance()
                emit(TokenType.GTE)
            }
            c == '>' -> emit(TokenType.GT)
            c == ' ' || c == '\t' -> { /* skip whitespace within lines */ }
            else -> emit(TokenType.ERROR, "Unexpected character: $c")
        }
    }

    private fun handleNewline() {
        if (!inHeader) {
            // Only emit NEWLINE if the last token was not already a NEWLINE/INDENT/DEDENT
            val lastType = tokens.lastOrNull()?.type
            if (lastType != null && lastType != TokenType.NEWLINE &&
                lastType != TokenType.INDENT && lastType != TokenType.DEDENT
            ) {
                emit(TokenType.NEWLINE)
            }
        }
        line++
        column = 1
        atLineStart = true
    }

    // =========================================================================
    // HEADER HANDLING
    // =========================================================================

    private fun scanHeaderSeparator() {
        // Already consumed first '-', consume remaining '--'
        advance() // second -
        advance() // third -
        // Consume any trailing dashes
        while (!isAtEnd() && peek() == '-') advance()
        headerSeparatorCount++
        emit(TokenType.HEADER_SEPARATOR)
    }

    private fun scanHeaderLine(firstChar: Char) {
        // Consume rest of line as raw header content, preserving leading whitespace
        // so that AvuHeader.parse() can detect indented section entries.
        val sb = StringBuilder()
        sb.append(firstChar)
        while (!isAtEnd() && peek() != '\n' && peek() != '\r') {
            sb.append(advance())
        }
        val content = sb.toString().trimEnd()
        if (content.isNotBlank()) {
            tokens.add(Token(TokenType.HEADER_LINE, content, line, start - lineStartOffset() + 1))
        }
    }

    // =========================================================================
    // STRING SCANNING
    // =========================================================================

    private fun scanString(quote: Char) {
        val sb = StringBuilder()
        while (!isAtEnd() && peek() != quote) {
            val c = peek()
            if (c == '\n' || c == '\r') {
                emit(TokenType.ERROR, "Unterminated string")
                return
            }
            if (c == '\\') {
                advance() // consume backslash
                if (isAtEnd()) {
                    emit(TokenType.ERROR, "Unterminated escape in string")
                    return
                }
                when (val escaped = advance()) {
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    '\\' -> sb.append('\\')
                    '"' -> sb.append('"')
                    '\'' -> sb.append('\'')
                    else -> {
                        sb.append('\\')
                        sb.append(escaped)
                    }
                }
            } else {
                sb.append(advance())
            }
        }
        if (isAtEnd()) {
            emit(TokenType.ERROR, "Unterminated string")
            return
        }
        advance() // closing quote
        val value = sb.toString()
        tokens.add(Token(TokenType.STRING_LITERAL, source.substring(start, current), line, startColumn(), value))
    }

    // =========================================================================
    // NUMBER SCANNING
    // =========================================================================

    private fun scanNumber() {
        while (!isAtEnd() && peek().isDigit()) advance()

        if (!isAtEnd() && peek() == '.' && peekNext()?.isDigit() == true) {
            advance() // consume '.'
            while (!isAtEnd() && peek().isDigit()) advance()
            val text = source.substring(start, current)
            val value = text.toDoubleOrNull() ?: 0.0
            tokens.add(Token(TokenType.FLOAT_LITERAL, text, line, startColumn(), value))
        } else {
            val text = source.substring(start, current)
            val value = text.toIntOrNull() ?: 0
            tokens.add(Token(TokenType.INT_LITERAL, text, line, startColumn(), value))
        }
    }

    // =========================================================================
    // DIRECTIVE SCANNING
    // =========================================================================

    private fun scanDirective() {
        while (!isAtEnd() && (peek().isLetterOrDigit() || peek() == '_')) advance()
        val text = source.substring(start, current)
        val type = DIRECTIVE_MAP[text] ?: TokenType.ERROR
        if (type == TokenType.ERROR) {
            tokens.add(Token(TokenType.ERROR, "Unknown directive: $text", line, startColumn()))
        } else {
            emit(type)
        }
    }

    // =========================================================================
    // VARIABLE REF SCANNING
    // =========================================================================

    private fun scanVariableRef() {
        if (isAtEnd() || (!peek().isLetter() && peek() != '_')) {
            emit(TokenType.ERROR, "Expected variable name after $")
            return
        }
        while (!isAtEnd() && (peek().isLetterOrDigit() || peek() == '_')) advance()
        val text = source.substring(start, current)
        tokens.add(Token(TokenType.VARIABLE_REF, text, line, startColumn(), text.substring(1)))
    }

    // =========================================================================
    // IDENTIFIER / KEYWORD SCANNING
    // =========================================================================

    private fun scanIdentifierOrKeyword() {
        while (!isAtEnd() && (peek().isLetterOrDigit() || peek() == '_')) advance()
        val text = source.substring(start, current)
        when {
            text == "true" -> tokens.add(Token(TokenType.BOOLEAN_LITERAL, text, line, startColumn(), true))
            text == "false" -> tokens.add(Token(TokenType.BOOLEAN_LITERAL, text, line, startColumn(), false))
            text == "and" -> emit(TokenType.AND)
            text == "or" -> emit(TokenType.OR)
            text == "not" -> emit(TokenType.NOT)
            text == "timeout" -> emit(TokenType.KW_TIMEOUT)
            text.length == 3 && text.all { it.isUpperCase() } -> emit(TokenType.CODE_NAME)
            else -> emit(TokenType.IDENTIFIER)
        }
    }

    // =========================================================================
    // COMMENT SCANNING
    // =========================================================================

    private fun scanComment() {
        while (!isAtEnd() && peek() != '\n' && peek() != '\r') advance()
        // Comments are discarded (not added to token list)
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private fun isAtEnd(): Boolean = current >= source.length

    private fun peek(): Char = source[current]

    private fun peekNext(): Char? = if (current + 1 < source.length) source[current + 1] else null

    private fun advance(): Char {
        val c = source[current++]
        column++
        return c
    }

    private fun canMatch(expected: Char): Boolean {
        return current < source.length && source[current] == expected
    }

    private fun canMatchAt(index: Int, expected: Char): Boolean {
        return index < source.length && source[index] == expected
    }

    private fun emit(type: TokenType) {
        val lexeme = source.substring(start, current)
        tokens.add(Token(type, lexeme, line, startColumn()))
    }

    private fun emit(type: TokenType, lexeme: String) {
        tokens.add(Token(type, lexeme, line, startColumn()))
    }

    private fun startColumn(): Int {
        // Calculate column of token start on current line
        var col = 1
        var i = start
        while (i > 0 && source[i - 1] != '\n' && source[i - 1] != '\r') {
            i--
            col++
        }
        return col
    }

    private fun lineStartOffset(): Int {
        var i = start
        while (i > 0 && source[i - 1] != '\n' && source[i - 1] != '\r') i--
        return i
    }

    companion object {
        private val DIRECTIVE_MAP = mapOf(
            "@workflow" to TokenType.AT_WORKFLOW,
            "@define" to TokenType.AT_DEFINE,
            "@on" to TokenType.AT_ON,
            "@if" to TokenType.AT_IF,
            "@else" to TokenType.AT_ELSE,
            "@wait" to TokenType.AT_WAIT,
            "@repeat" to TokenType.AT_REPEAT,
            "@while" to TokenType.AT_WHILE,
            "@sequence" to TokenType.AT_SEQUENCE,
            "@log" to TokenType.AT_LOG,
            "@set" to TokenType.AT_SET,
            "@return" to TokenType.AT_RETURN,
            "@emit" to TokenType.AT_EMIT,
        )
    }
}

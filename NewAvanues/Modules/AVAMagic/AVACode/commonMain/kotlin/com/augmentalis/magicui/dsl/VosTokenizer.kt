package com.augmentalis.avamagic.dsl

/**
 * Token types recognized by the VosTokenizer.
 *
 * The AvaUI DSL supports various token types for parsing .vos files including
 * literals (identifiers, strings, numbers, booleans), symbols (braces, parentheses,
 * operators), and special tokens (newlines, comments, EOF).
 */
enum class TokenType {
    // Literals
    /** Identifier token (e.g., App, ColorPicker, color) */
    IDENTIFIER,
    /** String literal token (e.g., "com.test.app", "#FF5722") */
    STRING,
    /** Numeric literal token (e.g., 123, 45.67) */
    NUMBER,
    /** Boolean literal token (true or false) */
    BOOLEAN,
    /** True keyword */
    TRUE,
    /** False keyword */
    FALSE,

    // Symbols
    /** Left brace symbol { */
    LBRACE,
    /** Right brace symbol } */
    RBRACE,
    /** Left parenthesis symbol ( */
    LPAREN,
    /** Right parenthesis symbol ) */
    RPAREN,
    /** Left bracket symbol [ */
    LBRACKET,
    /** Right bracket symbol ] */
    RBRACKET,
    /** Comma symbol , */
    COMMA,
    /** Colon symbol : */
    COLON,
    /** Equals symbol = */
    EQUALS,
    /** Arrow symbol => */
    ARROW,
    /** Dot symbol . */
    DOT,

    // Special
    /** Newline character */
    NEWLINE,
    /** End of file marker */
    EOF,
    /** Comment token (# to end of line) */
    COMMENT
}

/**
 * Represents a single token in the VosTokenizer output.
 *
 * @property type The type of the token
 * @property value The string value of the token
 * @property line The line number where the token appears (1-indexed)
 * @property column The column number where the token starts (1-indexed)
 */
data class Token(
    val type: TokenType,
    val value: String,
    val line: Int,
    val column: Int
) {
    override fun toString(): String = "Token($type, '$value', $line:$column)"
}

/**
 * Tokenizer (lexer) for the AvaUI DSL (.vos files).
 *
 * The tokenizer breaks raw source text into a sequence of tokens that can be
 * consumed by a parser. It handles:
 * - String literals with escape sequences
 * - Numeric literals (integers and floats)
 * - Identifiers and keywords (true/false)
 * - Symbols and operators
 * - Comments (# to end of line)
 * - Line and column tracking for error reporting
 *
 * Example usage:
 * ```kotlin
 * val source = """
 *     App {
 *       id: "com.test.app"
 *       name: "Test App"
 *     }
 * """.trimIndent()
 *
 * val tokenizer = VosTokenizer(source)
 * val tokens = tokenizer.tokenize()
 * ```
 *
 * @property source The source code to tokenize
 * @throws TokenizerException If invalid syntax is encountered
 */
class VosTokenizer(private val source: String) {
    private var position = 0
    private var line = 1
    private var column = 1
    private val tokens = mutableListOf<Token>()

    /**
     * Tokenizes the entire source code and returns a list of tokens.
     *
     * Comments are filtered out from the token stream. The returned list
     * always ends with an EOF token.
     *
     * @return List of tokens (excluding comments) with EOF as the final token
     * @throws TokenizerException If invalid syntax is encountered
     */
    fun tokenize(): List<Token> {
        while (!isAtEnd()) {
            skipWhitespace()
            if (isAtEnd()) break

            val token = scanToken()
            if (token.type != TokenType.COMMENT) {
                tokens.add(token)
            }
        }
        tokens.add(Token(TokenType.EOF, "", line, column))
        return tokens
    }

    /**
     * Scans and returns the next token from the source.
     *
     * @return The next token
     * @throws TokenizerException If an unexpected character is encountered
     */
    private fun scanToken(): Token {
        val start = position
        val startColumn = column
        val char = advance()

        return when (char) {
            '{' -> Token(TokenType.LBRACE, "{", line, startColumn)
            '}' -> Token(TokenType.RBRACE, "}", line, startColumn)
            '(' -> Token(TokenType.LPAREN, "(", line, startColumn)
            ')' -> Token(TokenType.RPAREN, ")", line, startColumn)
            '[' -> Token(TokenType.LBRACKET, "[", line, startColumn)
            ']' -> Token(TokenType.RBRACKET, "]", line, startColumn)
            ',' -> Token(TokenType.COMMA, ",", line, startColumn)
            ':' -> Token(TokenType.COLON, ":", line, startColumn)
            '.' -> Token(TokenType.DOT, ".", line, startColumn)
            '=' -> {
                if (peek() == '>') {
                    advance()
                    Token(TokenType.ARROW, "=>", line, startColumn)
                } else {
                    Token(TokenType.EQUALS, "=", line, startColumn)
                }
            }
            '"' -> scanString(startColumn)
            '#' -> scanComment(startColumn)
            '\n' -> {
                line++
                column = 1
                Token(TokenType.NEWLINE, "\n", line - 1, startColumn)
            }
            else -> {
                if (char.isDigit() || (char == '-' && peek().isDigit())) {
                    scanNumber(start, startColumn)
                } else if (char.isLetter() || char == '_') {
                    scanIdentifier(start, startColumn)
                } else {
                    throw TokenizerException("Unexpected character '$char' at $line:$startColumn")
                }
            }
        }
    }

    /**
     * Scans a string literal enclosed in double quotes.
     *
     * Supports escape sequences:
     * - \n (newline)
     * - \t (tab)
     * - \r (carriage return)
     * - \\ (backslash)
     * - \" (double quote)
     *
     * @param startColumn The column where the string starts
     * @return A STRING token
     * @throws TokenizerException If the string is unterminated or contains invalid escape sequences
     */
    private fun scanString(startColumn: Int): Token {
        val startLine = line
        val builder = StringBuilder()

        while (!isAtEnd() && peek() != '"') {
            val char = advance()

            if (char == '\n') {
                throw TokenizerException("Unterminated string at $startLine:$startColumn (newline encountered)")
            }

            if (char == '\\') {
                if (isAtEnd()) {
                    throw TokenizerException("Unterminated string at $startLine:$startColumn (ends with backslash)")
                }

                val escaped = advance()
                val escapedChar = when (escaped) {
                    'n' -> '\n'
                    't' -> '\t'
                    'r' -> '\r'
                    '\\' -> '\\'
                    '"' -> '"'
                    else -> throw TokenizerException("Invalid escape sequence '\\$escaped' at $line:${column - 1}")
                }
                builder.append(escapedChar)
            } else {
                builder.append(char)
            }
        }

        if (isAtEnd()) {
            throw TokenizerException("Unterminated string at $startLine:$startColumn (reached EOF)")
        }

        // Consume closing quote
        advance()

        return Token(TokenType.STRING, builder.toString(), startLine, startColumn)
    }

    /**
     * Scans a numeric literal (integer or floating-point).
     *
     * Supports:
     * - Integers: 123, -456
     * - Floats: 45.67, -123.456
     * - Scientific notation is NOT supported in this version
     *
     * @param start The position where the number starts
     * @param startColumn The column where the number starts
     * @return A NUMBER token
     * @throws TokenizerException If the number format is invalid
     */
    private fun scanNumber(start: Int, startColumn: Int): Token {
        val startLine = line

        // Consume digits
        while (!isAtEnd() && peek().isDigit()) {
            advance()
        }

        // Check for decimal point
        if (!isAtEnd() && peek() == '.' && position + 1 < source.length && source[position + 1].isDigit()) {
            // Consume the '.'
            advance()

            // Consume fractional digits
            while (!isAtEnd() && peek().isDigit()) {
                advance()
            }
        }

        val value = source.substring(start, position)

        // Validate the number format
        try {
            if ('.' in value) {
                value.toDouble()
            } else {
                value.toLong()
            }
        } catch (e: NumberFormatException) {
            throw TokenizerException("Invalid number format '$value' at $startLine:$startColumn")
        }

        return Token(TokenType.NUMBER, value, startLine, startColumn)
    }

    /**
     * Scans an identifier or keyword.
     *
     * Identifiers start with a letter or underscore and can contain letters,
     * digits, and underscores.
     *
     * Keywords recognized:
     * - true (BOOLEAN token)
     * - false (BOOLEAN token)
     *
     * @param start The position where the identifier starts
     * @param startColumn The column where the identifier starts
     * @return An IDENTIFIER or BOOLEAN token
     */
    private fun scanIdentifier(start: Int, startColumn: Int): Token {
        val startLine = line

        while (!isAtEnd() && (peek().isLetterOrDigit() || peek() == '_')) {
            advance()
        }

        val value = source.substring(start, position)

        // Check for keywords
        val type = when (value) {
            "true" -> TokenType.TRUE
            "false" -> TokenType.FALSE
            else -> TokenType.IDENTIFIER
        }

        return Token(type, value, startLine, startColumn)
    }

    /**
     * Scans a comment (# to end of line).
     *
     * Comments are consumed but filtered out from the token stream by the
     * tokenize() method.
     *
     * @param startColumn The column where the comment starts
     * @return A COMMENT token
     */
    private fun scanComment(startColumn: Int): Token {
        val startLine = line
        val start = position - 1 // Include the '#' character

        // Consume until end of line or EOF
        while (!isAtEnd() && peek() != '\n') {
            advance()
        }

        val value = source.substring(start, position)
        return Token(TokenType.COMMENT, value, startLine, startColumn)
    }

    /**
     * Advances the position by one character and updates line/column tracking.
     *
     * @return The character that was consumed
     */
    private fun advance(): Char {
        val char = source[position]
        position++
        column++
        return char
    }

    /**
     * Returns the current character without advancing the position.
     *
     * @return The current character, or '\u0000' if at end of file
     */
    private fun peek(): Char {
        return if (isAtEnd()) '\u0000' else source[position]
    }

    /**
     * Checks if the tokenizer has reached the end of the source.
     *
     * @return true if at end of file, false otherwise
     */
    private fun isAtEnd(): Boolean {
        return position >= source.length
    }

    /**
     * Skips whitespace characters (spaces, tabs, carriage returns).
     *
     * Newlines are NOT skipped as they are significant tokens in the DSL.
     */
    private fun skipWhitespace() {
        while (!isAtEnd()) {
            when (peek()) {
                ' ', '\t', '\r' -> advance()
                else -> return
            }
        }
    }
}

/**
 * Exception thrown when the tokenizer encounters invalid syntax.
 *
 * @property message Detailed error message including line and column information
 */
class TokenizerException(message: String) : Exception(message)

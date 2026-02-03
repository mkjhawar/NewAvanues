package com.augmentalis.magicelements.core.mel

/**
 * Lexer for MagicUI Expression Language (MEL).
 *
 * Tokenizes MEL expressions into a stream of tokens for parsing.
 *
 * Supported syntax:
 * - State references: `$state.path.to.value`
 * - Function calls: `$math.add(1, 2)`
 * - Parameter references: `$paramName`
 * - Literals: numbers, strings (single/double quotes), booleans, null
 * - Operators: +, -, *, /, %, ==, !=, >, <, >=, <=, &&, ||, !
 * - Delimiters: (, ), [, ], {, }, ,, :
 *
 * Example:
 * ```
 * val lexer = ExpressionLexer("$math.add($state.count, 1)")
 * val tokens = lexer.tokenize()
 * ```
 */
class ExpressionLexer(private val input: String) {

    private var position = 0
    private val length = input.length

    /**
     * Tokenize the input string into a list of tokens.
     */
    fun tokenize(): List<Token> {
        val tokens = mutableListOf<Token>()

        while (position < length) {
            skipWhitespace()

            if (position >= length) break

            val token = nextToken()
            tokens.add(token)
        }

        tokens.add(Token(TokenType.EOF, "", position))
        return tokens
    }

    /**
     * Read the next token from the input.
     */
    private fun nextToken(): Token {
        val start = position
        val char = peek()

        return when {
            char == '$' -> readDollarToken()
            char == '@' -> readAtToken()
            char == '.' -> Token(TokenType.DOT, consume().toString(), start)
            char == '(' -> Token(TokenType.LPAREN, consume().toString(), start)
            char == ')' -> Token(TokenType.RPAREN, consume().toString(), start)
            char == '[' -> Token(TokenType.LBRACKET, consume().toString(), start)
            char == ']' -> Token(TokenType.RBRACKET, consume().toString(), start)
            char == '{' -> Token(TokenType.LBRACE, consume().toString(), start)
            char == '}' -> Token(TokenType.RBRACE, consume().toString(), start)
            char == ',' -> Token(TokenType.COMMA, consume().toString(), start)
            char == ':' -> Token(TokenType.COLON, consume().toString(), start)
            char == '"' || char == '\'' -> readString(char)
            char == '!' -> readExclamation()
            char == '=' -> readEquals()
            char == '>' -> readGreaterThan()
            char == '<' -> readLessThan()
            char == '&' -> readAmpersand()
            char == '|' -> readPipe()
            char == '+' -> Token(TokenType.PLUS, consume().toString(), start)
            char == '-' -> readMinus()
            char == '*' -> Token(TokenType.STAR, consume().toString(), start)
            char == '/' -> Token(TokenType.SLASH, consume().toString(), start)
            char == '%' -> Token(TokenType.PERCENT, consume().toString(), start)
            char.isDigit() -> readNumber()
            char.isLetter() || char == '_' -> readIdentifier()
            else -> throw LexerException("Unexpected character: '$char' at position $position")
        }
    }

    /**
     * Read a dollar token ($state, $math, $param, etc.)
     */
    private fun readDollarToken(): Token {
        val start = position
        consume() // consume '$'

        if (position >= length || (!peek().isLetter() && peek() != '_')) {
            throw LexerException("Expected identifier after '$' at position $position")
        }

        val identifier = readIdentifierString()
        return Token(TokenType.DOLLAR, identifier, start)
    }

    /**
     * Read an at token (@module for module calls)
     */
    private fun readAtToken(): Token {
        val start = position
        consume() // consume '@'

        if (position >= length || (!peek().isLetter() && peek() != '_')) {
            throw LexerException("Expected module identifier after '@' at position $position")
        }

        val identifier = readIdentifierString()
        return Token(TokenType.AT, identifier, start)
    }

    /**
     * Read a string literal (single or double quoted).
     */
    private fun readString(quote: Char): Token {
        val start = position
        consume() // consume opening quote

        val builder = StringBuilder()
        while (position < length && peek() != quote) {
            if (peek() == '\\' && position + 1 < length) {
                consume() // consume backslash
                builder.append(readEscapeSequence())
            } else {
                builder.append(consume())
            }
        }

        if (position >= length) {
            throw LexerException("Unterminated string at position $start")
        }

        consume() // consume closing quote
        return Token(TokenType.STRING, builder.toString(), start)
    }

    /**
     * Read an escape sequence after a backslash.
     */
    private fun readEscapeSequence(): Char {
        return when (val char = consume()) {
            'n' -> '\n'
            't' -> '\t'
            'r' -> '\r'
            '\\' -> '\\'
            '"' -> '"'
            '\'' -> '\''
            else -> char // Keep as-is for unsupported escapes
        }
    }

    /**
     * Read a number (integer or floating point).
     */
    private fun readNumber(): Token {
        val start = position
        val builder = StringBuilder()

        while (position < length && (peek().isDigit() || peek() == '.')) {
            builder.append(consume())
        }

        // Handle scientific notation (e.g., 1e10, 2.5e-3)
        if (position < length && (peek() == 'e' || peek() == 'E')) {
            builder.append(consume())
            if (position < length && (peek() == '+' || peek() == '-')) {
                builder.append(consume())
            }
            while (position < length && peek().isDigit()) {
                builder.append(consume())
            }
        }

        return Token(TokenType.NUMBER, builder.toString(), start)
    }

    /**
     * Read an identifier (variable name, keyword, etc.)
     */
    private fun readIdentifier(): Token {
        val start = position
        val identifier = readIdentifierString()

        // Check for keywords
        val type = when (identifier) {
            "true", "false" -> TokenType.BOOLEAN
            "null" -> TokenType.NULL
            else -> TokenType.IDENTIFIER
        }

        return Token(type, identifier, start)
    }

    /**
     * Read an identifier string (letters, digits, underscores).
     */
    private fun readIdentifierString(): String {
        val builder = StringBuilder()
        while (position < length && (peek().isLetterOrDigit() || peek() == '_')) {
            builder.append(consume())
        }
        return builder.toString()
    }

    /**
     * Read ! or !=
     */
    private fun readExclamation(): Token {
        val start = position
        consume() // consume '!'
        if (position < length && peek() == '=') {
            consume()
            return Token(TokenType.NOT_EQUALS, "!=", start)
        }
        return Token(TokenType.BANG, "!", start)
    }

    /**
     * Read = or ==
     */
    private fun readEquals(): Token {
        val start = position
        consume() // consume '='
        if (position < length && peek() == '=') {
            consume()
            return Token(TokenType.EQUALS, "==", start)
        }
        throw LexerException("Unexpected '=' at position $start. Use '==' for equality.")
    }

    /**
     * Read > or >=
     */
    private fun readGreaterThan(): Token {
        val start = position
        consume() // consume '>'
        if (position < length && peek() == '=') {
            consume()
            return Token(TokenType.GREATER_THAN_OR_EQUAL, ">=", start)
        }
        return Token(TokenType.GREATER_THAN, ">", start)
    }

    /**
     * Read < or <=
     */
    private fun readLessThan(): Token {
        val start = position
        consume() // consume '<'
        if (position < length && peek() == '=') {
            consume()
            return Token(TokenType.LESS_THAN_OR_EQUAL, "<=", start)
        }
        return Token(TokenType.LESS_THAN, "<", start)
    }

    /**
     * Read && (logical AND)
     */
    private fun readAmpersand(): Token {
        val start = position
        consume() // consume '&'
        if (position < length && peek() == '&') {
            consume()
            return Token(TokenType.AND, "&&", start)
        }
        throw LexerException("Unexpected '&' at position $start. Use '&&' for logical AND.")
    }

    /**
     * Read || (logical OR)
     */
    private fun readPipe(): Token {
        val start = position
        consume() // consume '|'
        if (position < length && peek() == '|') {
            consume()
            return Token(TokenType.OR, "||", start)
        }
        throw LexerException("Unexpected '|' at position $start. Use '||' for logical OR.")
    }

    /**
     * Read - (could be minus operator or negative number)
     */
    private fun readMinus(): Token {
        val start = position
        consume() // consume '-'
        return Token(TokenType.MINUS, "-", start)
    }

    /**
     * Skip whitespace characters.
     */
    private fun skipWhitespace() {
        while (position < length && peek().isWhitespace()) {
            position++
        }
    }

    /**
     * Peek at the current character without consuming it.
     */
    private fun peek(): Char {
        return if (position < length) input[position] else '\u0000'
    }

    /**
     * Consume and return the current character.
     */
    private fun consume(): Char {
        return input[position++]
    }
}

/**
 * Token types for MEL.
 */
enum class TokenType {
    // Special
    DOLLAR,           // $state, $math, $param
    AT,               // @module (module call)
    DOT,              // .
    EOF,              // End of input

    // Delimiters
    LPAREN,           // (
    RPAREN,           // )
    LBRACKET,         // [
    RBRACKET,         // ]
    LBRACE,           // {
    RBRACE,           // }
    COMMA,            // ,
    COLON,            // :

    // Literals
    STRING,           // "hello" or 'hello'
    NUMBER,           // 42, 3.14, 1e10
    BOOLEAN,          // true, false
    NULL,             // null
    IDENTIFIER,       // variableName, functionName

    // Operators
    PLUS,             // +
    MINUS,            // -
    STAR,             // *
    SLASH,            // /
    PERCENT,          // %
    EQUALS,           // ==
    NOT_EQUALS,       // !=
    GREATER_THAN,     // >
    LESS_THAN,        // <
    GREATER_THAN_OR_EQUAL,  // >=
    LESS_THAN_OR_EQUAL,     // <=
    AND,              // &&
    OR,               // ||
    BANG,             // !
}

/**
 * Token representing a lexical unit.
 */
data class Token(
    val type: TokenType,
    val value: String,
    val position: Int
) {
    override fun toString(): String = "Token($type, '$value', pos=$position)"
}

/**
 * Exception thrown during lexical analysis.
 */
class LexerException(message: String) : Exception(message)
